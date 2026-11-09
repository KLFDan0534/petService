package com.pet.order.pricing;

import com.pet.common.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 【计价内核】
 *
 * 业务作用：
 * 集中所有「时间 → 金额 / 计费数量」的换算，供下单（OrderServiceImpl）与价格预览
 * （OrderController.preview）共同调用，保证「预览金额 = 实际扣款」由编译期同一个方法
 * 保证，而不是靠两处实现保持一致。
 *
 * 调用链：
 * OrderServiceImpl.quoteOrder → nightInitialAmount / overtimeAmount（按夜）
 * OrderServiceImpl.quoteOrder → hourBase（按小时）
 * OrderServiceImpl.completeOrder → overtimeHours / overtimeAmount（实际晚接）
 *
 * 约定：
 * - 纯静态、无状态、无数据库依赖，可直接单元测试。
 * - 金额换算只在最后一步做一次 setScale(2, HALF_UP)，中间结果不得提前舍入。
 * - 这里不放校验逻辑（能不能约），只放换算（该收多少）。
 */
public final class OrderPricing {

    private OrderPricing() {
    }

    // ==================== 按小时 HOUR ====================

    /** 小时的分钟数：单价语义是「每小时」，所有折算都以它为分母。 */
    public static final int HOUR_MINUTES = 60;

    /**
     * 按小时服务的基础价：单价 × (数量 × 单位分钟数) ÷ 60，仅在最后一步 HALF_UP 保留两位。
     * <p>
     * 为什么不能直接「单价 × 数量」：服务可以把一个计费单位配成 30 分钟（半小时粒度），
     * 此时 3 个单位 = 90 分钟 = 1.5 小时，直接乘数量会按 3 小时收钱，多收一倍。
     * <p>
     * 单位分钟数是 60 的整数倍时，本式与「单价 × 小时数」完全等价（除法精确，仅补零）。
     *
     * @param unitPrice       每小时单价
     * @param quantity        计费单位个数
     * @param durationMinutes 一个计费单位的分钟数（30/60/90/120…）
     * @return 基础价，两位小数
     */
    public static BigDecimal hourBase(BigDecimal unitPrice, int quantity, int durationMinutes) {
        BigDecimal price = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        long totalMinutes = (long) quantity * durationMinutes;
        return price.multiply(BigDecimal.valueOf(totalMinutes))
                .divide(BigDecimal.valueOf(HOUR_MINUTES), 2, RoundingMode.HALF_UP);
    }

    // ==================== 按夜 NIGHT ====================

    /** 一夜的小时数：夜价 ÷ 24 得到派生小时价（只用于计算，不作为独立配置项）。 */
    public static final int NIGHT_HOURS = 24;

    /** 入住锚点时刻：入住日 D 的 14:00。 */
    public static final LocalTime CHECKIN_ANCHOR_TIME = LocalTime.of(14, 0);

    /** 免费接走宽限：标准离店锚点后 30 分钟，真正免费且仍占容量。 */
    public static final int CHECKOUT_GRACE_MINUTES = 30;

    /** 提前送达按小时计费的门槛：0 < E ≤ 3 小时按小时，E > 3 小时加收一夜。 */
    public static final int EARLY_LIMIT_MINUTES = 180;

    /** 价格快照里的计价规则版本号。 */
    public static final String PRICING_RULE_VERSION = "night-v1";

    /** 晚接费的派生单价模式：夜价 ÷ 24。 */
    public static final String OVERTIME_PRICE_MODE = "night_div_24";

    private static final long NANOS_PER_HOUR = Duration.ofHours(1).toNanos();

    /** 入住锚点 I：入住日 D 的 14:00。 */
    public static LocalDateTime checkinAnchor(LocalDate checkinDate) {
        return checkinDate == null ? null : checkinDate.atTime(CHECKIN_ANCHOR_TIME);
    }

    /**
     * 标准离店锚点 O：D+N 日 14:00。
     * <p>
     * 只依赖预订夜数 N，与提前送达加收的一夜无关——提前加收不改离店日期。
     */
    public static LocalDateTime standardCheckoutAt(LocalDate checkinDate, int nights) {
        requireNights(nights);
        return checkinAnchor(checkinDate).plusDays(nights);
    }

    /** 免费接走截止 G：O + 30 分钟。 */
    public static LocalDateTime freeCheckoutUntil(LocalDate checkinDate, int nights) {
        return standardCheckoutAt(checkinDate, nights).plusMinutes(CHECKOUT_GRACE_MINUTES);
    }

    /**
     * 提前时长 E = max(0, I − A)。
     * <p>
     * 保留秒/更高精度：10:59:59 送达时 E 已超过 3 小时，不能先截断成整分钟再比较。
     */
    public static Duration earlyDuration(LocalDateTime checkinAnchor, LocalDateTime deliveryTime) {
        if (checkinAnchor == null || deliveryTime == null) {
            return Duration.ZERO;
        }
        Duration d = Duration.between(deliveryTime, checkinAnchor);
        return d.isNegative() ? Duration.ZERO : d;
    }

    /** 提前加收的整夜数：E > 3 小时为 1，否则 0。 */
    public static int earlyExtraNights(Duration early) {
        Duration e = early == null ? Duration.ZERO : early;
        return e.compareTo(Duration.ofMinutes(EARLY_LIMIT_MINUTES)) > 0 ? 1 : 0;
    }

    /** 提前按小时计费的小时数：0 < E ≤ 3 小时向上取整；E=0 或 E>3 小时为 0。 */
    public static int earlyExtraHours(Duration early) {
        Duration e = early == null ? Duration.ZERO : early;
        if (e.isZero() || e.isNegative() || earlyExtraNights(e) > 0) {
            return 0;
        }
        return (int) ceilHours(e);
    }

    /** 预订夜费 P×N（不含提前费），两位小数。 */
    public static BigDecimal nightBase(BigDecimal nightPrice, int nights) {
        requireNights(nights);
        return money(nightPrice).multiply(BigDecimal.valueOf(nights)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 预付原价：P×N + 提前费。
     * <p>
     * - E=0：P×N
     * - 0 < E ≤ 3 小时：round2(P×(24×N + 提前小时数) ÷ 24)
     * - E > 3 小时：round2(P×(N+1))，但存储的 N 不变、离店锚点不顺延
     * <p>
     * 禁止先算出「每小时价」并舍入再乘：P=100、N=1、提前 1 小时必须得 104.17 而不是 104.25。
     */
    public static BigDecimal nightInitialAmount(BigDecimal nightPrice, int nights, Duration early) {
        requireNights(nights);
        BigDecimal price = money(nightPrice);
        if (earlyExtraNights(early) > 0) {
            return price.multiply(BigDecimal.valueOf(nights + 1L)).setScale(2, RoundingMode.HALF_UP);
        }
        long earlyHours = earlyExtraHours(early);
        if (earlyHours == 0) {
            return nightBase(price, nights);
        }
        long totalHours = (long) NIGHT_HOURS * nights + earlyHours;
        return price.multiply(BigDecimal.valueOf(totalHours))
                .divide(BigDecimal.valueOf(NIGHT_HOURS), 2, RoundingMode.HALF_UP);
    }

    /**
     * 提前费分项金额 = 预付原价 − P×N。
     * <p>
     * 由后端分配 1 分钱残差，保证「夜费 + 提前费 = 预付原价」，前端不自行相减。
     */
    public static BigDecimal earlyAmount(BigDecimal nightPrice, int nights, Duration early) {
        return nightInitialAmount(nightPrice, nights, early).subtract(nightBase(nightPrice, nights));
    }

    /**
     * 实际晚接小时数 H：T ≤ G 时为 0，否则 ceil((T−G) ÷ 1 小时)。
     * <p>
     * 宽限只扣一次；即使逾期超过 24 小时也不自动转成夜数。
     */
    public static int overtimeHours(LocalDateTime freeCheckoutUntil, LocalDateTime checkoutAt) {
        if (freeCheckoutUntil == null || checkoutAt == null || !checkoutAt.isAfter(freeCheckoutUntil)) {
            return 0;
        }
        return (int) ceilHours(Duration.between(freeCheckoutUntil, checkoutAt));
    }

    /** 实际晚接补费 = round2(P × H ÷ 24)，单独结算，不享受任何优惠。 */
    public static BigDecimal overtimeAmount(BigDecimal nightPrice, int hours) {
        if (hours <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return money(nightPrice).multiply(BigDecimal.valueOf(hours))
                .divide(BigDecimal.valueOf(NIGHT_HOURS), 2, RoundingMode.HALF_UP);
    }

    /**
     * 派生小时价的展示值（P ÷ 24，两位小数）。
     * <p>
     * 仅用于页面文案「夜价÷24，按小时计费」；禁止把该舍入后的值再拿去乘小时数。
     */
    public static BigDecimal nightHourlyPrice(BigDecimal nightPrice) {
        return money(nightPrice).divide(BigDecimal.valueOf(NIGHT_HOURS), 2, RoundingMode.HALF_UP);
    }

    /** 时长向上取整为整小时（保留秒/纳秒精度，整小时边界不额外进位）。 */
    private static long ceilHours(Duration d) {
        long seconds = d.getSeconds();
        if (d.getNano() > 0) {
            seconds += 1;
        }
        if (seconds <= 0) {
            return 0;
        }
        return (seconds + 3599) / 3600;
    }

    private static void requireNights(int nights) {
        if (nights < 1) {
            throw new BusinessException(400, "预订夜数必须为正整数");
        }
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
