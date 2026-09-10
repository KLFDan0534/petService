package com.pet.boarding.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
public class AttendanceCheckRequestDTO {
    @Schema(description = "纬度")
    @NotNull(message = "纬度不能为空")
    private BigDecimal latitude_wsh;

    @Schema(description = "经度")
    @NotNull(message = "经度不能为空")
    private BigDecimal longitude_wsh;

    @Schema(description = "地址")
    private String address_wsh;

    @Schema(description = "定位精度")
    private BigDecimal accuracy_wsh;

    @Schema(description = "定位来源")
    private String location_source_wsh;
}
