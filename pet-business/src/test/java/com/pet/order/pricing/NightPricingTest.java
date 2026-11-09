package com.pet.order.pricing;

import com.pet.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 按夜计价内核护栏。
 * <p>
 * 覆盖方案验收例 AE3—AE9、AE18—AE20，以及秒级边界、0 提前、非法夜数、
 * 非整小时单价（100 ÷ 24）与「提前加收不顺延离店」。
 * 全部使用显式时间参数，不依赖系统时钟，不 sleep。
 */
class NightPricingTest {

    private static final BigDecimal P150 = new BigDecimal("150.00");
    private static final BigDecimal P100 = new BigDecimal("100.00");
    private static final LocalDate D = LocalDate.of(2026, 6, 1);

    private static LocalDateTime at(int month, int day, int hour, int minute) {
        return LocalDateTime.of(2026, month, day, hour, minute);
    }

    private static LocalDateTime at(int month, int day, int hour, int minute, int second) {
        return LocalDateTime.of(2026, month, day, hour, minute, second);
    }

    // ============ 锚点 ============

    @Test
    void anchorsFollowCheckinDateAndNights() {
        assertEquals(LocalDateTime.of(2026, 6, 1, 14, 0), OrderPricing.checkinAnchor(D));
        assertEquals(LocalDateTime.of(2026, 6, 4, 14, 0), OrderPricing.standardCheckoutAt(D, 3));
        assertEquals(LocalDateTime.of(2026, 6, 4, 14, 30), OrderPricing.freeCheckoutUntil(D, 3));
        // 1 夜：次日 14:00 标准离店，14:30 免费截止
        assertEquals(LocalDateTime.of(2026, 6, 2, 14, 0), OrderPricing.standardCheckoutAt(D, 1));
        assertEquals(LocalDateTime.of(2026, 6, 2, 14, 30), OrderPricing.freeCheckoutUntil(D, 1));
    }

    @Test
    void invalidNightsRejected() {
        assertThrows(BusinessException.class, () -> OrderPricing.standardCheckoutAt(D, 0));
        assertThrows(BusinessException.class, () -> OrderPricing.standardCheckoutAt(D, -1));
        assertThrows(BusinessException.class, () -> OrderPricing.nightBase(P150, 0));
        assertThrows(BusinessException.class, () -> OrderPricing.nightInitialAmount(P150, 0, Duration.ZERO));
    }

    // ============ 提前送达 ============

    @Test
    void zeroEarlyChargesNothing() {
        // AE1/AE2：14:00 或更晚送达，不收提前费
        assertEquals(Duration.ZERO, OrderPricing.earlyDuration(OrderPricing.checkinAnchor(D), at(6, 1, 14, 0)));
        assertEquals(0, OrderPricing.earlyExtraHours(OrderPricing.earlyDuration(
                OrderPricing.checkinAnchor(D), at(6, 1, 14, 0))));
        assertEquals(0, OrderPricing.earlyExtraNights(OrderPricing.earlyDuration(
                OrderPricing.checkinAnchor(D), at(6, 1, 14, 0))));
        assertEquals(new BigDecimal("450.00"), OrderPricing.nightInitialAmount(P150, 3, Duration.ZERO));

        // AE2：20:00 送达仍是 1 夜 150，不因不足 24 小时拒绝
        Duration early = OrderPricing.earlyDuration(OrderPricing.checkinAnchor(D), at(6, 1, 20, 0));
        assertEquals(Duration.ZERO, early);
        assertEquals(new BigDecimal("150.00"), OrderPricing.nightInitialAmount(P150, 1, early));
    }

    @Test
    void earlyWithinThreeHoursChargedByHourCeiling() {
        // AE3：13:59 送达 → 提前 1 分钟 → 按 1 小时收
        Duration e1 = OrderPricing.earlyDuration(OrderPricing.checkinAnchor(D), at(6, 1, 13, 59));
        assertEquals(1, OrderPricing.earlyExtraHours(e1));
        assertEquals(0, OrderPricing.earlyExtraNights(e1));
        assertEquals(new BigDecimal("456.25"), OrderPricing.nightInitialAmount(P150, 3, e1));
        assertEquals(new BigDecimal("6.25"), OrderPricing.earlyAmount(P150, 3, e1));

        // AE4：11:00 送达 → 正好 3 小时（整小时边界不额外进位）
        Duration e3 = OrderPricing.earlyDuration(OrderPricing.checkinAnchor(D), at(6, 1, 11, 0));
        assertEquals(3, OrderPricing.earlyExtraHours(e3));
        assertEquals(new BigDecimal("468.75"), OrderPricing.nightInitialAmount(P150, 3, e3));
        assertEquals(new BigDecimal("18.75"), OrderPricing.earlyAmount(P150, 3, e3));

        // 12:00 送达 → 2 小时
        Duration e2 = OrderPricing.earlyDuration(OrderPricing.checkinAnchor(D), at(6, 1, 12, 0));
        assertEquals(2, OrderPricing.earlyExtraHours(e2));
        assertEquals(new BigDecimal("462.50"), OrderPricing.nightInitialAmount(P150, 3, e2));
    }

    @Test
    void earlyBeyondThreeHoursChargesOneNightOnly() {
        // AE5：10:59:59 送达 → 已超过 3 小时 → 加收一夜，且不顺延离店日期
        Duration e = OrderPricing.earlyDuration(OrderPricing.checkinAnchor(D), at(6, 1, 10, 59, 59));
        assertEquals(1, OrderPricing.earlyExtraNights(e));
        assertEquals(0, OrderPricing.earlyExtraHours(e));
        assertEquals(new BigDecimal("600.00"), OrderPricing.nightInitialAmount(P150, 3, e));
        // 提前段不再重复收小时费：600 = 150 × 4
        assertEquals(new BigDecimal("150.00"), OrderPricing.earlyAmount(P150, 3, e));
        // 离店锚点仍只依赖预订 N
        assertEquals(LocalDateTime.of(2026, 6, 4, 14, 0), OrderPricing.standardCheckoutAt(D, 3));
    }

    @Test
    void earlyBoundaryJustUnderThreeHoursStillHourly() {
        // 11:00:01 送达 → 提前 2:59:59 → 按 3 小时收，不加夜
        Duration e = OrderPricing.earlyDuration(OrderPricing.checkinAnchor(D), at(6, 1, 11, 0, 1));
        assertEquals(0, OrderPricing.earlyExtraNights(e));
        assertEquals(3, OrderPricing.earlyExtraHours(e));
        assertEquals(new BigDecimal("468.75"), OrderPricing.nightInitialAmount(P150, 3, e));
    }

    @Test
    void earlyExtraNightDoesNotRaiseLongStayTier() {
        // AE20：N=6 提前超过 3 小时只加收 1 夜费用，预订夜数仍是 6
        Duration e = OrderPricing.earlyDuration(OrderPricing.checkinAnchor(D), at(6, 1, 9, 0));
        assertEquals(1, OrderPricing.earlyExtraNights(e));
        assertEquals(new BigDecimal("1050.00"), OrderPricing.nightInitialAmount(P150, 6, e));
        assertEquals(new BigDecimal("900.00"), OrderPricing.nightBase(P150, 6));
        assertEquals(LocalDateTime.of(2026, 6, 7, 14, 0), OrderPricing.standardCheckoutAt(D, 6));
    }

    @Test
    void nonIntegerHourlyPriceKeepsPrecision() {
        // AE18：100/夜 + 提前 1 小时 = 104.17，不能是 104.25
        Duration e = OrderPricing.earlyDuration(OrderPricing.checkinAnchor(D), at(6, 1, 13, 0));
        assertEquals(1, OrderPricing.earlyExtraHours(e));
        assertEquals(new BigDecimal("104.17"), OrderPricing.nightInitialAmount(P100, 1, e));
        assertEquals(new BigDecimal("4.17"), OrderPricing.earlyAmount(P100, 1, e));
        // 展示用派生小时价标「约」
        assertEquals(new BigDecimal("4.17"), OrderPricing.nightHourlyPrice(P100));
        assertEquals(new BigDecimal("6.25"), OrderPricing.nightHourlyPrice(P150));
    }

    // ============ 晚接 ============

    @Test
    void overtimeFreeUntilGraceDeadline() {
        // AE7：14:30:00 确认 → 0；14:30:01 → 1 小时
        LocalDateTime g = OrderPricing.freeCheckoutUntil(D, 3);
        assertEquals(0, OrderPricing.overtimeHours(g, LocalDateTime.of(2026, 6, 4, 14, 30, 0)));
        assertEquals(1, OrderPricing.overtimeHours(g, LocalDateTime.of(2026, 6, 4, 14, 30, 1)));
        assertEquals(new BigDecimal("0.00"), OrderPricing.overtimeAmount(P150, 0));
        assertEquals(new BigDecimal("6.25"), OrderPricing.overtimeAmount(P150, 1));
        // 早于免费截止也不收
        assertEquals(0, OrderPricing.overtimeHours(g, LocalDateTime.of(2026, 6, 4, 13, 0)));
    }

    @Test
    void overtimeHourBoundaryDoesNotRoundUp() {
        // AE8：15:30:00 → 1 小时；15:30:01 → 2 小时
        LocalDateTime g = OrderPricing.freeCheckoutUntil(D, 3);
        assertEquals(1, OrderPricing.overtimeHours(g, LocalDateTime.of(2026, 6, 4, 15, 30, 0)));
        assertEquals(2, OrderPricing.overtimeHours(g, LocalDateTime.of(2026, 6, 4, 15, 30, 1)));
        assertEquals(new BigDecimal("6.25"), OrderPricing.overtimeAmount(P150, 1));
        assertEquals(new BigDecimal("12.50"), OrderPricing.overtimeAmount(P150, 2));
    }

    @Test
    void overtimeAcrossDaysDeductsGraceOnlyOnce() {
        // AE9：6/5 15:30 确认 → H=25，只减一次 30 分钟宽限，不转成夜数
        LocalDateTime g = OrderPricing.freeCheckoutUntil(D, 3);
        assertEquals(25, OrderPricing.overtimeHours(g, LocalDateTime.of(2026, 6, 5, 15, 30)));
        assertEquals(new BigDecimal("156.25"), OrderPricing.overtimeAmount(P150, 25));
    }

    @Test
    void overtimeIndependentFromEarlySurcharge() {
        // AE6：提前加收一夜的订单，16:00 接走仍要另补 2 小时 12.50
        Duration e = OrderPricing.earlyDuration(OrderPricing.checkinAnchor(D), at(6, 1, 10, 59, 59));
        BigDecimal initial = OrderPricing.nightInitialAmount(P150, 3, e);
        assertEquals(new BigDecimal("600.00"), initial);
        LocalDateTime g = OrderPricing.freeCheckoutUntil(D, 3);
        int hours = OrderPricing.overtimeHours(g, LocalDateTime.of(2026, 6, 4, 16, 0));
        assertEquals(2, hours);
        assertEquals(new BigDecimal("12.50"), OrderPricing.overtimeAmount(P150, hours));
    }

    @Test
    void overtimeSeparateFromPrepaid() {
        // AE19：P=100、N=1、正常入住、晚接 1 小时 → 预付 100.00 + 独立补费 4.17 = 104.17
        LocalDateTime g = OrderPricing.freeCheckoutUntil(D, 1);
        BigDecimal prepaid = OrderPricing.nightInitialAmount(P100, 1, Duration.ZERO);
        int hours = OrderPricing.overtimeHours(g, LocalDateTime.of(2026, 6, 2, 15, 30));
        assertEquals(1, hours);
        BigDecimal overtime = OrderPricing.overtimeAmount(P100, hours);
        assertEquals(new BigDecimal("100.00"), prepaid);
        assertEquals(new BigDecimal("4.17"), overtime);
        assertEquals(new BigDecimal("104.17"), prepaid.add(overtime));
    }
}
