package com.pet.boarding.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 商家营业时间（BusinessHours）实体，映射 business_hours_wsh 表。
 * <p>
 * 存储商家一周每天的营业时间配置，支持设置营业时段（开门/关门时间）和全天休息标记。
 * 营业时间配置在自动模式下（MODE_AUTO）决定了店铺的实时营业状态。
 * 如果某天标记为休息（is_closed = 1），则当天店铺自动关闭。
 * 支持跨天营业（如 22:00 开门到次日 02:00 关门）。
 *
 * @author: wsh
 */
@Getter
@Setter
@TableName("business_hours_wsh")
@Schema(description = "营业时间实体")
public class BusinessHours {
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @TableField(value = "merchant_id_wsh")
    @Schema(description = "商家ID")
    private Long merchant_id_wsh;

    @TableField(value = "day_of_week_wsh")
    @Schema(description = "星期几")
    private Integer day_of_week_wsh;

    @TableField(value = "open_time_wsh")
    @Schema(description = "营业开始时间")
    private LocalTime open_time_wsh;

    @TableField(value = "close_time_wsh")
    @Schema(description = "营业结束时间")
    private LocalTime close_time_wsh;

    @TableField(value = "is_closed_wsh")
    @Schema(description = "是否休息")
    private Integer is_closed_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @TableField(value = "updated_at_wsh", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;
}
