package com.pet.boarding.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务项目实体
 * 映射数据库表 pet_service_wsh，存储商家提供的宠物服务项目信息
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Getter
@Setter
@TableName("pet_service_wsh")
public class ServiceItem {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    private Long id_wsh;

    @JsonProperty("merchant_id_wsh")
    @TableField(value = "merchant_id_wsh")
    private Long merchant_id_wsh;

    @JsonProperty("name_wsh")
    @TableField(value = "name_wsh")
    private String name_wsh;

    @JsonProperty("type_wsh")
    @TableField(value = "type_wsh")
    private String type_wsh;

    @JsonProperty("description_wsh")
    @TableField(value = "description_wsh")
    private String description_wsh;

    @JsonProperty("price_wsh")
    @TableField(value = "price_wsh")
    private BigDecimal price_wsh;

    @JsonProperty("unit_wsh")
    @TableField(value = "unit_wsh")
    private String unit_wsh;

    @JsonProperty("images_wsh")
    @TableField(value = "images_wsh")
    private String images_wsh;

    @JsonProperty("status_wsh")
    @TableField(value = "status_wsh")
    private Integer status_wsh;

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
