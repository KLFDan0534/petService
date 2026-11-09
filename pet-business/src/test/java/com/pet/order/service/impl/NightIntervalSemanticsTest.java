package com.pet.order.service.impl;

import com.pet.common.OrderStatus;
import com.pet.order.entity.PetOrder;
import com.pet.order.occupancy.OrderOccupancy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 占用区间语义护栏（按夜口径）。
 * <p>
 * 覆盖：night 预约占用 [A, max(B, G))、宽限期仍占容量、14:30 首尾相接合法、
 * 完成立即释放、在店逾期跨日持续占用、峰值并发不把互不重叠的订单算成同时占用、
 * session/hour 不附加 night 宽限。
 */
class NightIntervalSemanticsTest {

    private static final LocalDate D = LocalDate.of(2026, 6, 1);

    private PetOrder nightOrder(LocalDate checkin, int nights,
                                LocalDateTime delivery, LocalDateTime pickup) {
        PetOrder o = new PetOrder();
        o.setBilling_unit_wsh("night");
        o.setStart_date_wsh(checkin);
        o.setEnd_date_wsh(checkin.plusDays(nights));
        o.setQuantity_wsh(nights);
        o.setDelivery_time_wsh(delivery);
        o.setPickup_time_wsh(pickup);
        o.setStatus_wsh(OrderStatus.CONFIRMED);
        return o;
    }

    private PetOrder slotOrder(String unit, String delivery, String pickup) {
        PetOrder o = new PetOrder();
        o.setBilling_unit_wsh(unit);
        o.setDelivery_time_wsh(LocalDateTime.parse(delivery));
        o.setPickup_time_wsh(LocalDateTime.parse(pickup));
        o.setStatus_wsh(OrderStatus.CONFIRMED);
        return o;
    }

    // ============ night：宽限期占容量 ============

    @Test
    void nightIntervalIncludesGracePeriod() {
        // 1 夜：A=D 14:00，G=D+1 14:30；预约接走 14:00 时占用终点仍是 14:30
        PetOrder order = nightOrder(D, 1, D.atTime(14, 0), D.plusDays(1).atTime(14, 0));
        OrderOccupancy.Interval interval = OrderOccupancy.intervalOf(order, D.atTime(10, 0));

        assertEquals(D.atTime(14, 0), interval.start());
        assertEquals(D.plusDays(1).atTime(14, 30), interval.endExclusive());
    }

    @Test
    void nightLaterScheduledPickupExtendsInterval() {
        // 预约 16:00 接走：占用终点取 max(B, G) = 16:00
        PetOrder order = nightOrder(D, 1, D.atTime(14, 0), D.plusDays(1).atTime(16, 0));
        OrderOccupancy.Interval interval = OrderOccupancy.intervalOf(order, D.atTime(10, 0));
        assertEquals(D.plusDays(1).atTime(16, 0), interval.endExclusive());
    }

    @Test
    void newCheckinAtGraceStartConflictsButGraceEndIsLegal() {
        // AE15：上一单 1 夜，14:00 离店、14:30 免费截止
        PetOrder previous = nightOrder(D, 1, D.atTime(14, 0), D.plusDays(1).atTime(14, 0));
        OrderOccupancy.Interval previousInterval = OrderOccupancy.intervalOf(previous, D.atTime(10, 0));

        // 14:00 新单送达：与上一单的宽限期重叠 → 冲突
        OrderOccupancy.Interval at1400 = new OrderOccupancy.Interval(
                D.plusDays(1).atTime(14, 0), D.plusDays(2).atTime(14, 30));
        assertTrue(OrderOccupancy.overlaps(previousInterval, at1400));

        // 14:30 新单送达：首尾相接，不重叠
        OrderOccupancy.Interval at1430 = new OrderOccupancy.Interval(
                D.plusDays(1).atTime(14, 30), D.plusDays(2).atTime(14, 30));
        assertFalse(OrderOccupancy.overlaps(previousInterval, at1430));
    }

    @Test
    void completedOrderReleasesImmediately() {
        // 14:20 完成：不再参与占用，不必等到 14:30
        PetOrder order = nightOrder(D, 1, D.atTime(14, 0), D.plusDays(1).atTime(14, 0));
        order.setStatus_wsh(OrderStatus.COMPLETED);
        assertNull(OrderOccupancy.intervalOf(order, D.plusDays(1).atTime(14, 20)));
    }

    @Test
    void cancelledOrderDoesNotOccupy() {
        PetOrder order = nightOrder(D, 1, D.atTime(14, 0), D.plusDays(1).atTime(14, 0));
        order.setStatus_wsh(OrderStatus.CANCELLED);
        assertNull(OrderOccupancy.intervalOf(order, D.atTime(10, 0)));
    }

    @Test
    void inStoreOverdueOrderKeepsOccupyingAcrossDays() {
        // 6/4 14:30 应离店，但到 6/10 仍未完成：跨日仍占容量（终点开放）
        PetOrder order = nightOrder(D, 3, D.atTime(14, 0), D.plusDays(3).atTime(14, 30));
        order.setStatus_wsh(OrderStatus.IN_PROGRESS);

        OrderOccupancy.Interval interval = OrderOccupancy.intervalOf(order, LocalDateTime.of(2026, 6, 10, 9, 0));
        assertEquals(D.atTime(14, 0), interval.start());
        // 后续任意新单都被视为冲突
        OrderOccupancy.Interval future = new OrderOccupancy.Interval(
                LocalDateTime.of(2026, 6, 20, 14, 0), LocalDateTime.of(2026, 6, 21, 14, 30));
        assertTrue(OrderOccupancy.overlaps(interval, future));
    }

    @Test
    void notYetReceivedOrderDoesNotBecomeOpenEnded() {
        // CONFIRMED 只是预约：即使预约终点已过，也不应无限占用（实际入店后才会）
        PetOrder order = nightOrder(D, 1, D.atTime(14, 0), D.plusDays(1).atTime(14, 30));
        order.setStatus_wsh(OrderStatus.CONFIRMED);
        OrderOccupancy.Interval interval = OrderOccupancy.intervalOf(order, LocalDateTime.of(2026, 6, 10, 9, 0));
        assertEquals(D.plusDays(1).atTime(14, 30), interval.endExclusive());
    }

    // ============ 峰值并发 ============

    @Test
    void twoDisjointExistingOrdersAreNotCountedAsTwoSimultaneous() {
        // max=2 场景：两笔互不重叠的已有订单，与同一个长候选区间相交，不代表同时占 2 个名额
        OrderOccupancy.Interval a = new OrderOccupancy.Interval(
                LocalDateTime.of(2026, 6, 1, 10, 0), LocalDateTime.of(2026, 6, 1, 12, 0));
        OrderOccupancy.Interval b = new OrderOccupancy.Interval(
                LocalDateTime.of(2026, 6, 1, 14, 0), LocalDateTime.of(2026, 6, 1, 16, 0));
        OrderOccupancy.Interval candidate = new OrderOccupancy.Interval(
                LocalDateTime.of(2026, 6, 1, 11, 0), LocalDateTime.of(2026, 6, 1, 15, 0));

        assertEquals(2, OrderOccupancy.peakConcurrency(List.of(a, b), candidate));
    }

    @Test
    void threeOverlappingOrdersExceedCapacityTwo() {
        OrderOccupancy.Interval a = new OrderOccupancy.Interval(
                LocalDateTime.of(2026, 6, 1, 10, 0), LocalDateTime.of(2026, 6, 1, 18, 0));
        OrderOccupancy.Interval b = new OrderOccupancy.Interval(
                LocalDateTime.of(2026, 6, 1, 11, 0), LocalDateTime.of(2026, 6, 1, 18, 0));
        OrderOccupancy.Interval candidate = new OrderOccupancy.Interval(
                LocalDateTime.of(2026, 6, 1, 12, 0), LocalDateTime.of(2026, 6, 1, 18, 0));

        assertEquals(3, OrderOccupancy.peakConcurrency(List.of(a, b), candidate));
    }

    @Test
    void backToBackDoesNotCountAsSimultaneous() {
        OrderOccupancy.Interval a = new OrderOccupancy.Interval(
                LocalDateTime.of(2026, 6, 1, 10, 0), LocalDateTime.of(2026, 6, 1, 12, 0));
        OrderOccupancy.Interval candidate = new OrderOccupancy.Interval(
                LocalDateTime.of(2026, 6, 1, 12, 0), LocalDateTime.of(2026, 6, 1, 14, 0));

        assertEquals(1, OrderOccupancy.peakConcurrency(List.of(a), candidate));
    }

    // ============ session/hour 不附加 night 宽限 ============

    @Test
    void hourOrderEndsAtPickupWithoutGrace() {
        PetOrder order = slotOrder("hour", "2026-06-01T10:00:00", "2026-06-01T11:00:00");
        OrderOccupancy.Interval interval = OrderOccupancy.intervalOf(order, LocalDateTime.of(2026, 6, 1, 9, 0));
        assertEquals(LocalDateTime.of(2026, 6, 1, 11, 0), interval.endExclusive());
    }

    @Test
    void slotOrderAllowsBackToBack() {
        PetOrder existing = slotOrder("session", "2026-06-01T10:00:00", "2026-06-01T11:00:00");
        OrderOccupancy.Interval candidate = new OrderOccupancy.Interval(
                LocalDateTime.of(2026, 6, 1, 11, 0), LocalDateTime.of(2026, 6, 1, 12, 0));
        assertFalse(OrderOccupancy.overlaps(
                OrderOccupancy.intervalOf(existing, LocalDateTime.of(2026, 6, 1, 9, 0)), candidate));
    }

    @Test
    void slotOrderConflictsOnOneMinuteOverlap() {
        PetOrder existing = slotOrder("hour", "2026-06-01T10:00:00", "2026-06-01T12:00:00");
        OrderOccupancy.Interval candidate = new OrderOccupancy.Interval(
                LocalDateTime.of(2026, 6, 1, 11, 59), LocalDateTime.of(2026, 6, 1, 13, 0));
        assertTrue(OrderOccupancy.overlaps(
                OrderOccupancy.intervalOf(existing, LocalDateTime.of(2026, 6, 1, 9, 0)), candidate));
    }

    @Test
    void legacyOrderWithoutDatesNeverOccupies() {
        PetOrder legacy = new PetOrder();
        legacy.setStatus_wsh(OrderStatus.CONFIRMED);
        assertNull(OrderOccupancy.intervalOf(legacy, LocalDateTime.of(2026, 6, 1, 9, 0)));
    }
}
