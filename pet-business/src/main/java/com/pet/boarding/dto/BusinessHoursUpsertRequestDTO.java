package com.pet.boarding.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
public class BusinessHoursUpsertRequestDTO {
    @Schema(description = "星期几")
    private Integer day_of_week_wsh;

    @Schema(description = "营业开始时间")
    private LocalTime open_time_wsh;

    @Schema(description = "营业结束时间")
    private LocalTime close_time_wsh;

    @Schema(description = "是否休息")
    private Integer is_closed_wsh;
}
