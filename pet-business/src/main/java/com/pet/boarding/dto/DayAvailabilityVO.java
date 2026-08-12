package com.pet.boarding.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * U2: 单日可预约性视图对象。
 *
 * <p>{@code reason_code_wsh} 在不可预约时说明原因，可取值：
 * <ul>
 *   <li>{@code MERCHANT_REST_DAY}：商家休息日</li>
 *   <li>{@code KEEPER_ON_LEAVE}：看护员当天请假</li>
 *   <li>{@code CAPACITY_EXCEEDED}：看护员当天容量已满</li>
 * </ul>
 */
@Getter
@Setter
public class DayAvailabilityVO {

    /** 目标日期 */
    private LocalDate date_wsh;

    /** 当天是否可预约 */
    private Boolean bookable_wsh;

    /** 不可预约原因码（可预约时为 null） */
    private String reason_code_wsh;

    /** 当天营业窗口（不可预约时为空列表） */
    private List<AvailabilityWindowVO> windows_wsh;
}