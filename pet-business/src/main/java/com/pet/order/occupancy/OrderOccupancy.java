package com.pet.order.occupancy;

import com.pet.common.BookingUnit;
import com.pet.common.OrderStatus;
import com.pet.order.entity.PetOrder;
import com.pet.order.pricing.OrderPricing;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 【订单占用区间小型工具】
 *
 * 业务作用：
 * 把「一张订单在什么时间段占用了宠物/看护者容量」收敛成唯一实现，
 * 供下单冲突校验、keeper 容量校验、可用性日历三处共用，避免再写第二套解释。
 *
 * 区间口径（全部为半开区间 [start, endExclusive)，首尾相接不算重叠）：
 * - night：预约占用 = [A, max(B, G))，G 是标准离店锚点 + 30 分钟免费宽限；
 *   宽限期真正免费，但仍占容量。
 * - session/hour：沿用 [A, B) 服务预约区间，不附加 night 的宽限或离店锚点。
 * - 旧 day（U10 移除）：[start_date 00:00, end_date+1 00:00)。
 * - 已完成订单不再参与占用（提前完成立即释放，不必等原截止时间）。
 * - 已接收/服务中订单若已过预约终点仍未完成，保守地视为终点开放，持续占用到完成。
 *
 * 容量判定：
 * 上限比较「任一时刻同时在养数」，用「开始 +1 / 结束 −1」事件扫描求峰值；
 * 同一时刻先处理结束再处理开始，避免首尾相接被误判为同时占用。
 */
public final class OrderOccupancy {

    /** 在店状态：宠物已经在看护者处，未完成前一直占容量。 */
    private static final Set<String> IN_STORE_STATUSES = Set.of(
            OrderStatus.RECEIVED, OrderStatus.IN_PROGRESS);

    /** 已接收但逾期未完成时的开放终点：不预测还会晚接多久，直接视为终点开放。 */
    private static final LocalDateTime OPEN_END = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    private OrderOccupancy() {
    }

    /** 占用区间（半开）。 */
    public record Interval(LocalDateTime start, LocalDateTime endExclusive) {
        public boolean isValid() {
            return start != null && endExclusive != null && start.isBefore(endExclusive);
        }
    }

    /**
     * 计算订单的占用区间（使用当前时间判断是否逾期未完成）。
     *
     * @param order 订单
     * @return 占用区间；订单已完成或区间无法确定时返回 null（不参与占用）
     */
    public static Interval intervalOf(PetOrder order) {
        return intervalOf(order, LocalDateTime.now());
    }

    /**
     * 计算订单的占用区间。
     *
     * @param order 订单
     * @param now   当前时间（测试可注入，避免依赖系统时钟）
     * @return 占用区间；订单已完成/已取消或区间无法确定时返回 null
     */
    public static Interval intervalOf(PetOrder order, LocalDateTime now) {
        if (order == null) {
            return null;
        }
        String status = order.getStatus_wsh();
        if (OrderStatus.COMPLETED.equals(status)
                || OrderStatus.CANCELLED.equals(status)
                || OrderStatus.REFUNDED.equals(status)) {
            return null;
        }
        Interval interval = scheduledInterval(order);
        if (interval == null) {
            return null;
        }
        // 已在店但预约终点已过：保守地持续占用到完成
        if (status != null
                && IN_STORE_STATUSES.contains(status)
                && now != null
                && !interval.endExclusive().isAfter(now)) {
            return new Interval(interval.start(), OPEN_END);
        }
        return interval;
    }

    /** 按预约事实推导区间，不考虑是否已在店。 */
    private static Interval scheduledInterval(PetOrder order) {
        String unit = BookingUnit.normalize(order.getBilling_unit_wsh());
        if (BookingUnit.NIGHT.equals(unit)) {
            return nightInterval(order);
        }
        String mode = BookingUnit.bookingMode(unit);
        boolean handoverMode = BookingUnit.MODE_SLOT.equals(mode) || BookingUnit.MODE_INTERVAL.equals(mode);
        if (handoverMode && order.getDelivery_time_wsh() != null && order.getPickup_time_wsh() != null) {
            return new Interval(order.getDelivery_time_wsh(), order.getPickup_time_wsh());
        }
        // 旧 day / 历史缺时刻数据：按日期区间，含离店日
        if (order.getStart_date_wsh() == null || order.getEnd_date_wsh() == null) {
            return null;
        }
        return new Interval(order.getStart_date_wsh().atStartOfDay(),
                order.getEnd_date_wsh().plusDays(1).atStartOfDay());
    }

    /**
     * 按夜占用：[A, max(B, G))。
     * <p>
     * G 只依赖预订夜数 N（标准离店锚点 + 30 分钟），提前加收的一夜不顺延离店，
     * 因此也不会延长占用终点。
     */
    private static Interval nightInterval(PetOrder order) {
        if (order.getStart_date_wsh() == null) {
            return null;
        }
        Integer nights = order.getQuantity_wsh();
        LocalDateTime start = order.getDelivery_time_wsh() != null
                ? order.getDelivery_time_wsh()
                : OrderPricing.checkinAnchor(order.getStart_date_wsh());
        LocalDateTime end;
        if (nights != null && nights >= 1) {
            LocalDateTime grace = OrderPricing.freeCheckoutUntil(order.getStart_date_wsh(), nights);
            LocalDateTime scheduledPickup = order.getPickup_time_wsh();
            end = scheduledPickup == null || grace.isAfter(scheduledPickup) ? grace : scheduledPickup;
        } else {
            end = order.getPickup_time_wsh();
        }
        if (end == null) {
            return null;
        }
        return new Interval(start, end);
    }

    /** 两个半开区间是否重叠（首尾相接不重叠）。 */
    public static boolean overlaps(Interval a, Interval b) {
        if (a == null || b == null || !a.isValid() || !b.isValid()) {
            return false;
        }
        return a.start().isBefore(b.endExclusive()) && b.start().isBefore(a.endExclusive());
    }

    /**
     * 计算「候选区间 + 已有区间」在任一时刻的同时占用峰值。
     * <p>
     * 不能把「与候选区间相交的已有订单数」直接相加：两笔互不重叠的已有订单
     * 与同一个长候选区间相交，并不代表它们同时占用了 2 个名额。
     *
     * @param existing  已有订单的占用区间（null 元素会被忽略）
     * @param candidate 候选订单的占用区间
     * @return 峰值并发数（候选区间自身计入）
     */
    public static int peakConcurrency(List<Interval> existing, Interval candidate) {
        if (candidate == null || !candidate.isValid()) {
            return 0;
        }
        List<Event> events = new ArrayList<>();
        if (existing != null) {
            for (Interval interval : existing) {
                if (interval != null && interval.isValid()) {
                    events.add(new Event(interval.start(), 1));
                    events.add(new Event(interval.endExclusive(), -1));
                }
            }
        }
        events.add(new Event(candidate.start(), 1));
        events.add(new Event(candidate.endExclusive(), -1));
        // 同一时刻先处理结束（-1）再处理开始（+1）：首尾相接不算同时占用
        events.sort(Comparator.comparing(Event::at).thenComparingInt(Event::delta));
        int current = 0;
        int peak = 0;
        for (Event event : events) {
            current += event.delta();
            peak = Math.max(peak, current);
        }
        return peak;
    }

    private record Event(LocalDateTime at, int delta) {
    }
}
