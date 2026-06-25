package com.pet.boarding.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 营业时间实体
 * 映射数据库表 business_hours_wsh，存储商家每日营业时间信息
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Getter
@Setter
@TableName("business_hours_wsh")
public class BusinessHours {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    private Long id_wsh;

    @JsonProperty("merchant_id_wsh")
    @TableField(value = "merchant_id_wsh")
    private Long merchant_id_wsh;

    @JsonProperty("day_of_week_wsh")
    @TableField(value = "day_of_week_wsh")
    private Integer day_of_week_wsh;

    @JsonProperty("open_time_wsh")
    @TableField(value = "open_time_wsh")
    private LocalTime open_time_wsh;

    @JsonProperty("close_time_wsh")
    @TableField(value = "close_time_wsh")
    private LocalTime close_time_wsh;

    @JsonProperty("is_closed_wsh")
    @TableField(value = "is_closed_wsh")
    private Integer is_closed_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    private LocalDateTime created_at_wsh;

    @JsonProperty("updated_at_wsh")
    @TableField(value = "updated_at_wsh", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updated_at_wsh;
}
