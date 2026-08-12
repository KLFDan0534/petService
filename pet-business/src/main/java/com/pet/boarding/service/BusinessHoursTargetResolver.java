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

    private boolean isRestDay(BusinessHours h) {
        return h.getIs_closed_wsh() != null && h.getIs_closed_wsh() == 1;
    }

    private int previousDayOfWeek(int dayOfWeek) {
        return dayOfWeek == 1 ? 7 : dayOfWeek - 1;
    }

    private List<BusinessHours> hoursFor(List<BusinessHours> hours, int dayOfWeek) {
        return hours.stream().filter(h -> dayOfWeek == h.getDay_of_week_wsh()).toList();
    }
}