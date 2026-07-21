package com.pet.boarding.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
public class KeeperAttendanceDTO {
    @Schema(description = "考勤记录ID")
    private Long id_wsh;
    @Schema(description = "看护者ID")
    private Long keeper_id_wsh;
    @Schema(description = "商家ID")
    private Long merchant_id_wsh;
    @Schema(description = "看护者名称")
    private String keeper_name_wsh;
    @Schema(description = "商家名称")
    private String merchant_name_wsh;
    @Schema(description = "签到时间")
    private LocalDateTime check_in_at_wsh;
    @Schema(description = "签到纬度")
    private BigDecimal check_in_latitude_wsh;
    @Schema(description = "签到经度")
    private BigDecimal check_in_longitude_wsh;
    @Schema(description = "签到地址")
    private String check_in_address_wsh;
    @Schema(description = "签到精度")
    private BigDecimal check_in_accuracy_wsh;
    @Schema(description = "签到距离(米)")
    private BigDecimal check_in_distance_wsh;
    @Schema(description = "签退时间")
    private LocalDateTime check_out_at_wsh;
    @Schema(description = "签退纬度")
    private BigDecimal check_out_latitude_wsh;
    @Schema(description = "签退经度")
    private BigDecimal check_out_longitude_wsh;
    @Schema(description = "签退地址")
    private String check_out_address_wsh;
    @Schema(description = "签退精度")
    private BigDecimal check_out_accuracy_wsh;
    @Schema(description = "签退距离(米)")
    private BigDecimal check_out_distance_wsh;
    @Schema(description = "签到半径(米)")
    private Integer radius_meters_wsh;
    @Schema(description = "是否在岗")
    private Boolean on_duty_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;
}
