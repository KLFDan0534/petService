package com.pet.boarding.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 营业时间实体
 * 映射数据库表 business_hours_wsh，存储商家每日营业时间信息
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Getter
@Setter
@TableName("business_hours_wsh")
@Schema(description = "营业时间实体")
public class BusinessHours {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @JsonProperty("merchant_id_wsh")
    @TableField(value = "merchant_id_wsh")
    @Schema(description = "商家ID")
    private Long merchant_id_wsh;

    @JsonProperty("day_of_week_wsh")
    @TableField(value = "day_of_week_wsh")
    @Schema(description = "星期几")
    private Integer day_of_week_wsh;

    @JsonProperty("open_time_wsh")
    @TableField(value = "open_time_wsh")
    @Schema(description = "营业开始时间")
    private LocalTime open_time_wsh;

    @JsonProperty("close_time_wsh")
    @TableField(value = "close_time_wsh")
    @Schema(description = "营业结束时间")
    private LocalTime close_time_wsh;

    @JsonProperty("is_closed_wsh")
    @TableField(value = "is_closed_wsh")
    @Schema(description = "是否休息")
    private Integer is_closed_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @JsonProperty("updated_at_wsh")
    @TableField(value = "updated_at_wsh", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;
}
