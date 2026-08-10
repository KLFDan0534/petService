package com.pet.boarding.service;

import com.pet.boarding.entity.BusinessHours;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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