package com.pet.boarding.dto;

import lombok.Data;
import java.time.LocalTime;

/**
 * 营业时间数据传输对象
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class BusinessHoursDTO {
    private Long id_wsh;
    private Long merchant_id_wsh;
    private Integer day_of_week_wsh;
    private LocalTime open_time_wsh;
    private LocalTime close_time_wsh;
    private Integer is_closed_wsh;
}