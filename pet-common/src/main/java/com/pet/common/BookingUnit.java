package com.pet.common;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 【多计费单位契约注册表】
 *
 * 业务作用：
 * 为 night/session/hour（夜/次/小时）三类计费单位提供唯一的规范化入口：
 * - 别名归一（晚/夜/按夜→night、次/课时→session、小时→hour，大小写不敏感）
 * - 未知/空白/超长单位拒绝（旧的 day/boarding 一律拒绝，不再静默兼容）
 * - 从单位推导预约模式（interval：按夜连续区间；slot：按时长槽位）
 * - 单位时长校验与默认值
 *
 * 调用链：
 * ServiceItemServiceImpl（服务创建/编辑）→ normalize + resolveDurationMinutes
 * OrderServiceImpl（下单计价）→ normalize + bookingMode + resolveDurationMinutes
 * ServiceAvailabilityServiceImpl（可用性）→ bookingMode 决定槽位过滤规则
 *
 * 约定：
 * - 只允许三种规范化单位；其余一律视为非法，不得静默放行。
 * - 过夜寄养只有 night 一种口径：按夜收费，提前送达只加收费用、不顺延离店。
 */
public final class BookingUnit {

    /** 过夜寄养：按夜计费（一夜 24 小时，14:00 入住锚点） */
    public static final String NIGHT = "night";
    /** 按次：一次服务一个连续时段 */
    public static final String SESSION = "session";
    /** 按小时：按时长槽位计费 */
    public static final String HOUR = "hour";

    /** slot：按时长槽位预约，占用的是一段连续时间窗口 */
    public static final String MODE_SLOT = "slot";
    /** interval：由用户给定起止时刻的连续区间（按夜寄养） */
    public static final String MODE_INTERVAL = "interval";

    /** 单位字符串最大长度，超过即拒绝（防过胖输入） */
    public static final int MAX_UNIT_LENGTH = 20;

    /** 单位时长边界（分钟） */
    public static final int MIN_DURATION_MINUTES = 15;
    public static final int MAX_DURATION_MINUTES = 1440;
    public static final int DEFAULT_SESSION_MINUTES = 60;
    public static final int DEFAULT_HOUR_MINUTES = 60;
    /**
     * 一夜的分钟数：只是 night 的单位元数据（duration_minutes_wsh 存 1440），
     * 不能用它要求实际住满 1440 分钟。
     */
    public static final int NIGHT_DURATION_MINUTES = 1440;

    private static final Set<String> CANONICAL = Set.of(NIGHT, SESSION, HOUR);

    private static final Map<String, String> ALIASES = Map.ofEntries(
        // 按夜寄养
        Map.entry("night", NIGHT),
        Map.entry("nights", NIGHT),
        Map.entry("晚", NIGHT),
        Map.entry("夜", NIGHT),
        Map.entry("按晚", NIGHT),
        Map.entry("按夜", NIGHT),
        Map.entry("过夜", NIGHT),
        // 按次
        Map.entry("session", SESSION),
        Map.entry("sessions", SESSION),
        Map.entry("次", SESSION),
        Map.entry("按次", SESSION),
        // 「课时」是课程节次，按次收费：一次订单对应一节课，单价语义为「每节课」
        Map.entry("课时", SESSION),
        Map.entry("节", SESSION),
        // 按小时
        Map.entry("hour", HOUR),
        Map.entry("hours", HOUR),
        Map.entry("小时", HOUR),
        Map.entry("按时", HOUR),
        Map.entry("按小时", HOUR)
    );

    private BookingUnit() {
    }

    /**
     * 将任意输入归一为规范化单位；未知/空白/超长/旧单位（day、boarding）返回 null。
     * 英文别名大小写不敏感，中文别名直接映射。
     */
    public static String normalize(String unit) {
        if (unit == null || unit.isBlank()) {
            return null;
        }
        String trimmed = unit.trim();
        // 超长输入直接判非法
        if (trimmed.length() > MAX_UNIT_LENGTH) {
            return null;
        }
        String key = trimmed.toLowerCase(Locale.ROOT);
        // 先查别名表，未命中再比规范单位
        String mapped = ALIASES.get(key);
        if (mapped != null) {
            return mapped;
        }
        return CANONICAL.contains(key) ? key : null;
    }

    /** 仅接受规范化单位；别名请先调用 normalize。 */
    public static boolean isSupported(String unit) {
        return unit != null && CANONICAL.contains(unit);
    }

    /** 按单位推导预约模式；非法/未知单位返回 null。 */
    public static String bookingMode(String unit) {
        // 次与小时都按时间槽位占用
        if (SESSION.equals(unit) || HOUR.equals(unit)) {
            return MODE_SLOT;
        }
        // 寄养：用户给定起止时刻的连续区间
        if (NIGHT.equals(unit)) {
            return MODE_INTERVAL;
        }
        return null;
    }

    /**
     * 按小时计费的最小粒度步长（分钟）。
     * <p>
     * 30 = 支持半小时粒度；服务把 {@code duration_minutes_wsh} 配成 30 时，
     * 一个「单位」就是半小时，金额按实际分钟折算（单价语义仍是「每小时」）。
     */
    public static final int HOUR_GRANULARITY_STEP_MINUTES = 30;

    /** 单位默认时长（分钟）。 */
    public static int defaultDurationMinutes(String unit) {
        if (NIGHT.equals(unit)) {
            return NIGHT_DURATION_MINUTES;
        }
        if (SESSION.equals(unit)) {
            return DEFAULT_SESSION_MINUTES;
        }
        if (HOUR.equals(unit)) {
            return DEFAULT_HOUR_MINUTES;
        }
        // 未知单位不静默兜底，直接拒绝
        throw new BusinessException(400, BookingErrorCode.UNSUPPORTED_SERVICE_UNIT, "未知服务计费单位");
    }

    /**
     * 校验并归一时长：非法时长抛出业务异常（错误码 DURATION_INVALID）。
     * - night：固定 1440 分钟（单位元数据，不是最低入住时长）
     * - session：15..1440，缺省 60
     * - hour：必须是 30 分钟粒度步长的整数倍且落在 15..1440，缺省 60
     */
    public static int resolveDurationMinutes(String unit, Integer durationMinutes) {
        // night 的 1440 只是单位元数据，不要求实际住满 1440 分钟
        if (NIGHT.equals(unit)) {
            if (durationMinutes == null || durationMinutes == NIGHT_DURATION_MINUTES) {
                return NIGHT_DURATION_MINUTES;
            }
            throw new BusinessException(400, BookingErrorCode.DURATION_INVALID,
                    "按夜服务的计费基数必须为24小时(1440分钟)");
        }
        validateUnitSupported(unit);
        int duration = durationMinutes == null
                ? defaultDurationMinutes(unit)
                : durationMinutes;
        if (duration < MIN_DURATION_MINUTES || duration > MAX_DURATION_MINUTES) {
            throw invalidDuration(unit, duration);
        }
        // 按小时计费：时长即「一个计费单位的分钟数」，必须是粒度步长的整数倍。
        if (HOUR.equals(unit) && duration % HOUR_GRANULARITY_STEP_MINUTES != 0) {
            throw invalidDuration(unit, duration);
        }
        return duration;
    }

    /** 单位上限外的额外语义校验入参校验（数量等），无多余副作用。 */
    public static void validateUnitSupported(String unit) {
        if (!isSupported(unit)) {
            throw new BusinessException(400, BookingErrorCode.UNSUPPORTED_SERVICE_UNIT,
                    "不支持的服务计费单位: " + unit);
        }
    }

    /**
     * 构造时长非法异常：统一生成 DURATION_INVALID，并按单位补充不同提示。
     */
    private static BusinessException invalidDuration(String unit, int duration) {
        return new BusinessException(400, BookingErrorCode.DURATION_INVALID,
                "服务时长不合法(" + unit + ", " + duration + "分钟)，要求 15..1440 分钟"
                        + (HOUR.equals(unit) ? "且为 " + HOUR_GRANULARITY_STEP_MINUTES + " 分钟的整数倍" : ""));
    }
}
