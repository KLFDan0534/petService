package com.pet.boarding.service;

import com.pet.boarding.entity.BusinessHours;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 目标日期营业时段解析器。
 * <p>
 * 与实时营业状态 {@code store_status_wsh} 解耦：本解析器只回答“某个交接时间点是否落在商家
 * 该目标日期的营业时段内”，用于未来预约的送达/接回时间校验，不触碰看护员在线状态或店铺实时状态。
 * <p>
 * 语义约定（Asia/Shanghai 项目时区）：
 * <ul>
 *   <li>营业时段使用半开区间 {@code [open, close)}：送达/接回恰好等于 close 视为不在时段内。</li>
 *   <li>跨午夜时段（如周一 22:00 - 周二 02:00）归属到起始日：目标时刻 2026-08-11 01:00 属于周一时段，
 *       不能误读为周二时段。</li>
 *   <li>{@code is_closed_wsh}=1 的时段当天休息，不参与匹配。</li>
 *   <li>{@code open == close} 视为全天营业（与实时 {@code isWithinRange} 的既有语义一致）。</li>
 * </ul>
 */
@Component
public class BusinessHoursTargetResolver {

    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    /**
     * 单个营业窗口（半开区间 {@code [start, end)}），窗口不跨天，跨午夜时段按天拆分。
     */
    public record BusinessWindow(LocalDateTime start, LocalDateTime end) {
    }

    /**
     * 计算目标日期当天的营业窗口列表（用于可预约性槽位生成）。
     * <p>
     * 语义与 {@link #isWithinBusinessHours} 完全一致：
     * <ul>
     *   <li>当天时段：普通时段 {@code [open, close)}；跨午夜时段当天部分 {@code [open, 24:00)}；
     *       {@code open == close} 视为全天 {@code [00:00, 次日 00:00)}。</li>
     *   <li>前一日跨午夜时段的次日清晨部分 {@code [00:00, close)} 计入当天。</li>
     *   <li>休息日（{@code is_closed_wsh}=1）不产生窗口。</li>
     *   <li>重叠/相接的窗口合并为单一窗口。</li>
     * </ul>
     *
     * @param hours 商家全部营业时段配置（含休息日标记）
     * @param date  目标日期
     * @return 合并后的当天营业窗口列表；配置为空时返回空列表
     */
    public List<BusinessWindow> windowsForDate(List<BusinessHours> hours, LocalDate date) {
        if (hours == null || hours.isEmpty() || date == null) {
            return List.of();
        }
        int dayOfWeek = date.getDayOfWeek().getValue();
        List<BusinessWindow> candidates = new ArrayList<>();

        // 1. 目标日当天的时段
        for (BusinessHours h : hoursFor(hours, dayOfWeek)) {
            if (isRestDay(h)) continue;
            LocalTime open = h.getOpen_time_wsh();
            LocalTime close = h.getClose_time_wsh();
            if (open == null || close == null) continue;
            if (open.equals(close)) {
                candidates.add(new BusinessWindow(date.atStartOfDay(), date.plusDays(1).atStartOfDay()));
            } else if (open.isBefore(close)) {
                candidates.add(new BusinessWindow(date.atTime(open), date.atTime(close)));
            } else {
                candidates.add(new BusinessWindow(date.atTime(open), date.plusDays(1).atStartOfDay()));
            }
        }

        // 2. 前一日跨午夜时段覆盖次日清晨 [00:00, close)
        for (BusinessHours h : hoursFor(hours, previousDayOfWeek(dayOfWeek))) {
            if (isRestDay(h)) continue;
            LocalTime open = h.getOpen_time_wsh();
            LocalTime close = h.getClose_time_wsh();
            if (open == null || close == null) continue;
            if (open.isAfter(close)) {
                candidates.add(new BusinessWindow(date.atStartOfDay(), date.atTime(close)));
            }
        }

        return merge(candidates);
    }

    /**
     * 【合并营业窗口】把重叠或相接的窗口合并为单一窗口
     *
     * 业务作用：windowsForDate 收集到当天与前一日的候选窗口后，合并为互不重叠的有序窗口列表，供槽位生成使用。
     *
     * 调用场景：windowsForDate 在返回结果前调用。
     *
     * 调用链：windowsForDate → 本方法 → 返回合并后的 List<BusinessWindow>。
     *
     * 数据处理：按 start 升序排序后线性扫描，若当前窗口 start 不晚于上一窗口 end 则合并（end 取较大值），否则追加为新窗口。
     *
     * 业务规则：相接（start == last.end）也算重叠并合并；窗口数 <= 1 时原样返回。
     *
     * 状态影响：无，返回新列表，不修改入参。
     *
     * 异常情况：无。
     *
     * 注意事项：相接窗口会被合并，因此 [00:00,08:00) 与 [08:00,18:00) 会合成一个 [00:00,18:00)，槽位生成时不会在 08:00 处断开。
     */
    private List<BusinessWindow> merge(List<BusinessWindow> windows) {
        if (windows.size() <= 1) {
            return windows;
        }
        List<BusinessWindow> sorted = new ArrayList<>(windows);
        sorted.sort(Comparator.comparing(BusinessWindow::start));
        List<BusinessWindow> merged = new ArrayList<>();
        for (BusinessWindow w : sorted) {
            if (merged.isEmpty()) {
                merged.add(w);
                continue;
            }
            BusinessWindow last = merged.get(merged.size() - 1);
            if (!w.start().isAfter(last.end())) {
                if (w.end().isAfter(last.end())) {
                    merged.set(merged.size() - 1, new BusinessWindow(last.start(), w.end()));
                }
            } else {
                merged.add(w);
            }
        }
        return merged;
    }

    /**
     * @param hours 商家全部营业时段配置（含休息日标记）
     * @param target 待校验的目标时间点（Asia/Shanghai 墙钟时间）
     * @return target 是否落在营业时段 {@code [open, close)} 内
     */
    public boolean isWithinBusinessHours(List<BusinessHours> hours, LocalDateTime target) {
        if (hours == null || hours.isEmpty() || target == null) {
            return false;
        }
        LocalDate date = target.toLocalDate();
        LocalTime time = target.toLocalTime();
        int dayOfWeek = date.getDayOfWeek().getValue();

        // 1. 目标日当天的时段：普通时段 [open, close)；跨午夜时段当天部分 [open, 24:00)
        for (BusinessHours h : hoursFor(hours, dayOfWeek)) {
            if (isRestDay(h)) continue;
            LocalTime open = h.getOpen_time_wsh();
            LocalTime close = h.getClose_time_wsh();
            if (open == null || close == null) continue;
            if (open.equals(close)) return true;
            if (open.isBefore(close)) {
                if (!time.isBefore(open) && time.isBefore(close)) return true;
            } else {
                if (!time.isBefore(open)) return true;
            }
        }

        // 2. 前一日跨午夜时段覆盖次日清晨 [00:00, close)
        for (BusinessHours h : hoursFor(hours, previousDayOfWeek(dayOfWeek))) {
            if (isRestDay(h)) continue;
            LocalTime open = h.getOpen_time_wsh();
            LocalTime close = h.getClose_time_wsh();
            if (open == null || close == null) continue;
            if (open.isAfter(close) && time.isBefore(close)) return true;
        }
        return false;
    }

    /**
     * 【休息日判定】判断某条营业时段配置是否为休息日
     *
     * 业务作用：过滤掉 is_closed_wsh=1 的配置，使其不参与窗口生成与时刻匹配。
     *
     * 调用场景：windowsForDate 与 isWithinBusinessHours 的两个循环中调用。
     *
     * 调用链：windowsForDate / isWithinBusinessHours → 本方法。
     *
     * 数据处理：读取 is_closed_wsh，非空且等于 1 返回 true。
     *
     * 业务规则：is_closed_wsh 为 null 时视为非休息日。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：使用 Integer 拆箱比较，已先判 null 避免空指针；取值只认 1，其它非 0 值都按营业处理。
     */
    private boolean isRestDay(BusinessHours h) {
        return h.getIs_closed_wsh() != null && h.getIs_closed_wsh() == 1;
    }

    /**
     * 【取前一天的星期值】把 1-7 的星期值回退一天并处理跨周
     *
     * 业务作用：为了把前一日跨午夜时段覆盖到当天的清晨部分。
     *
     * 调用场景：windowsForDate 与 isWithinBusinessHours 中调用。
     *
     * 调用链：windowsForDate / isWithinBusinessHours → 本方法 → hoursFor。
     *
     * 数据处理：输入 dayOfWeek（1=周一…7=周日），周一返回 7，其它返回 dayOfWeek-1。
     *
     * 业务规则：使用 ISO 星期编号，1 为周一。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：依赖 day_of_week_wsh 字段同样使用 ISO 编号（1-7），若数据存 0-6 会导致匹配错位。
     */
    private int previousDayOfWeek(int dayOfWeek) {
        return dayOfWeek == 1 ? 7 : dayOfWeek - 1;
    }

    /**
     * 【按星期筛选时段】取出指定星期的全部营业时段配置
     *
     * 业务作用：从商家全部营业时段中挑出目标星期的记录。
     *
     * 调用场景：windowsForDate 与 isWithinBusinessHours 中调用。
     *
     * 调用链：windowsForDate / isWithinBusinessHours → 本方法 → Stream.filter。
     *
     * 数据处理：按 day_of_week_wsh 等于入参星期值过滤，返回列表。
     *
     * 业务规则：只按星期匹配，不判断休息日标记（由 isRestDay 另行判断）。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：day_of_week_wsh 为 null 时 `dayOfWeek == null` 拆箱比较会抛 NullPointerException；同一星期存在多条配置时全部返回，由调用方逐条处理。
     */
    private List<BusinessHours> hoursFor(List<BusinessHours> hours, int dayOfWeek) {
        return hours.stream().filter(h -> dayOfWeek == h.getDay_of_week_wsh()).toList();
    }
}