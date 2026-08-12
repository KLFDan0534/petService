package com.pet.boarding.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * U2: 单个营业窗口视图对象（半开区间 [start_wsh, end_wsh)）。
 *
 * <p>窗口不跨天：跨午夜时段按天拆分（如周一 22:00-02:00 拆为
 * 周一 [22:00, 24:00) 与周二 [00:00, 02:00) 两个窗口）。
 * {@code slots_wsh} 为窗口内按槽位粒度生成的可用起始时间（ISO 本地日期时间），
 * 最后一个槽位保证不超过窗口结束边界。
 */
@Getter
@Setter
public class AvailabilityWindowVO {

    /** 窗口开始时间（Asia/Shanghai 墙钟时间） */
    private LocalDateTime start_wsh;

    /** 窗口结束时间（不包含） */
    private LocalDateTime end_wsh;

    /** 可用起始槽位列表 */
    private List<String> slots_wsh;
}