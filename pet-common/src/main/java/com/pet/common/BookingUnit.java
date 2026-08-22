package com.pet.common;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 【多计费单位契约注册表】
 *
 * 业务作用：
 * 为 day/session/hour（日/次/小时）三类计费单位提供唯一的规范化入口：
 * - 别名归一（天→day、次→session、小时→hour、days→day 等，大小写不敏感）
 * - 未知/空白/超长单位拒绝
 * - 从单位推导预约模式（date_range：按日期区间；slot：按时长槽位）
 * - 单位时长校验与默认值（对齐测试矩阵边界：15/60/120/1440 分钟）
 *
 * 调用链：
 * ServiceItemServiceImpl（服务创建/编辑）→ normalize + resolveDurationMinutes
 * OrderServiceImpl（下单计价）→ normalize + bookingMode + resolveDurationMinutes
 * ServiceAvailabilityServiceImpl（可用性）→ bookingMode 决定槽位过滤规则
 *
 * 约定：
 * - 只允许三种规范化单位；其余一律视为非法，不得静默放行。
 * - 历史数据 unit_wsh 为 NULL/未知时一律按 day 语义兼容，不回写旧订单。
 */
public final class BookingUnit {

    public static final String DAY = "day";
    public static final String SESSION = "session";
    public static final String HOUR = "hour";

    /** date_range：按日期区间预约并计算天数 */
    public static final String MODE_DATE_RANGE = "date_range";
    /** slot：按时长槽位预约，占用的是一段连续时间窗口 */
    public static final String MODE_SLOT = "slot";

    /** 单位字符串最大长度，超过即拒绝（防过胖输入） */
    public static final int MAX_UNIT_LENGTH = 20;

    /** 单位时长边界（分钟） */
    public static final int MIN_DURATION_MINUTES = 15;
    public static final int MAX_DURATION_MINUTES = 1440;
    public static final int DEFAULT_SESSION_MINUTES = 60;
    public static final int DEFAULT_HOUR_MINUTES = 60;
    public static final int DAY_DURATION_MINUTES = 1440;

    private static final Set<String> CANONICAL = Set.of(DAY, SESSION, HOUR);

    private static final Map<String, String> ALIASES = Map.ofEntries(
        Map.entry("day", DAY),
        Map.entry("days", DAY),
        Map.entry("日", DAY),
        Map.entry("天", DAY),
        Map.entry("按天", DAY),
        Map.entry("按日", DAY),
        Map.entry("session", SESSION),
        Map.entry("sessions", SESSION),
        Map.entry("次", SESSION),
        Map.entry("按次", SESSION),
        Map.entry("hour", HOUR),
        Map.entry("hours", HOUR),
        Map.entry("小时", HOUR),
        Map.entry("按时", HOUR),
        Map.entry("按小时", HOUR)
    );

    private BookingUnit() {
    }

    /**
     * 将任意输入归一为规范化单位；未知/空白/超长返回 null。
     * 英文别名大小写不敏感，中文别名直接映射。
     */
    public static String normalize(String unit) {
        if (unit == null || unit.isBlank()) {
            return null;
        }
        String trimmed = unit.trim();
        if (trimmed.length() > MAX_UNIT_LENGTH) {
            return null;
        }
        String key = trimmed.toLowerCase(Locale.ROOT);
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
        if (DAY.equals(unit)) {
            return MODE_DATE_RANGE;
        }
        if (SESSION.equals(unit) || HOUR.equals(unit)) {
            return MODE_SLOT;
        }
        return null;
    }

    /** 单位默认时长（分钟）。 */
    public static int defaultDurationMinutes(String unit) {
        if (DAY.equals(unit)) {
            return DAY_DURATION_MINUTES;
        }
        if (SESSION.equals(unit)) {
            return DEFAULT_SESSION_MINUTES;
        }
        if (HOUR.equals(unit)) {
            return DEFAULT_HOUR_MINUTES;
        }
        throw new BusinessException(400, BookingErrorCode.UNSUPPORTED_SERVICE_UNIT, "未知服务计费单位");
    }

    /**
     * 校验并归一时长：非法时长抛出业务异常（错误码 DURATION_INVALID）。
     * - day：固定 1440 分钟
     * - session：15..1440，缺省 60
     * - hour：必须是 60 的正整数倍且落在 15..1440，缺省 60
     */
    public static int resolveDurationMinutes(String unit, Integer durationMinutes) {
        if (DAY.equals(unit)) {
            if (durationMinutes == null || durationMinutes == DAY_DURATION_MINUTES) {
                return DAY_DURATION_MINUTES;
            }
            throw new BusinessException(400, BookingErrorCode.DURATION_INVALID,
                    "按日计费服务的时长必须为全天(1440分钟)");
        }
        validateUnitSupported(unit);
        int duration = durationMinutes == null
                ? defaultDurationMinutes(unit)
                : durationMinutes;
        if (duration < MIN_DURATION_MINUTES || duration > MAX_DURATION_MINUTES) {
            throw invalidDuration(unit, duration);
        }
        if (HOUR.equals(unit) && duration % 60 != 0) {
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

    private static BusinessException invalidDuration(String unit, int duration) {
        return new BusinessException(400, BookingErrorCode.DURATION_INVALID,
                "服务时长不合法(" + unit + ", " + duration + "分钟)，要求 15..1440 分钟"
                        + (HOUR.equals(unit) ? "且为整小时" : ""));
    }
}