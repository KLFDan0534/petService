package com.pet.boarding.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 看护者考勤签到（KeeperAttendance）实体，映射 keeper_attendance_wsh 表。
 * <p>
 * 记录看护者每天上下班的签到/签退信息，包含签到/签退的时间、地理位置（经纬度）、
 * 地址文本、定位精度、与商家的距离等。
 * 签到/签退时系统会校验看护者是否在商家设定的打卡半径范围内（地理围栏）。
 * 一条记录代表一个完整的班次：check_in_at（签到时间）→ check_out_at（签退时间）。
 * check_out_at 为 null 表示当前班次尚未结束（在岗）。
 */
@Getter
@Setter
@TableName("keeper_attendance_wsh")
@Schema(description = "看护者签到实体")
public class KeeperAttendance {
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @Schema(description = "看护者ID")
    private Long keeper_id_wsh;

    @Schema(description = "商家ID")
    private Long merchant_id_wsh;

    @Schema(description = "签到时间")
    private LocalDateTime check_in_at_wsh;

    @Schema(description = "签到纬度")
    private BigDecimal check_in_latitude_wsh;

    @Schema(description = "签到经度")
    private BigDecimal check_in_longitude_wsh;

    @Schema(description = "签到地址")
    private String check_in_address_wsh;

    @Schema(description = "签到精度(米)")
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

    @Schema(description = "签退精度(米)")
    private BigDecimal check_out_accuracy_wsh;

    @Schema(description = "签退距离(米)")
    private BigDecimal check_out_distance_wsh;

    @Schema(description = "签到半径(米)")
    private Integer radius_meters_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;
}
