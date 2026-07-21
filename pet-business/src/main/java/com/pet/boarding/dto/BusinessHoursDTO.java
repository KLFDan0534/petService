package com.pet.boarding.dto;

import lombok.Data;
import java.time.LocalTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 营业时间数据传输对象
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class BusinessHoursDTO {
    @Schema(description = "营业时间ID")
    private Long id_wsh;
    @Schema(description = "商家ID")
    private Long merchant_id_wsh;
    @Schema(description = "星期几")
    private Integer day_of_week_wsh;
    @Schema(description = "营业开始时间")
    private LocalTime open_time_wsh;
    @Schema(description = "营业结束时间")
    private LocalTime close_time_wsh;
    @Schema(description = "是否休息")
    private Integer is_closed_wsh;
}