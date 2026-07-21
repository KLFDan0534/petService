package com.pet.boarding.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 地址实体
 * 映射数据库表 address_wsh，存储用户地址信息
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Getter
@Setter
@TableName("address_wsh")
@Schema(description = "地址实体")
public class Address {
    @JsonProperty("id_wsh")
    @TableId(type = IdType.AUTO, value = "id_wsh")
    @Schema(description = "ID")
    private Long id_wsh;

    @JsonProperty("user_id_wsh")
    @TableField(value = "user_id_wsh")
    @Schema(description = "用户ID")
    private Long user_id_wsh;

    @JsonProperty("label_wsh")
    @TableField(value = "label_wsh")
    @Schema(description = "标签")
    private String label_wsh;

    @JsonProperty("name_wsh")
    @TableField(value = "name_wsh")
    @Schema(description = "名称")
    private String name_wsh;

    @JsonProperty("phone_wsh")
    @TableField(value = "phone_wsh")
    @Schema(description = "手机号")
    private String phone_wsh;

    @JsonProperty("address_wsh")
    @TableField(value = "address_wsh")
    @Schema(description = "地址")
    private String address_wsh;

    @JsonProperty("detail_wsh")
    @TableField(value = "detail_wsh")
    @Schema(description = "详细地址")
    private String detail_wsh;

    @JsonProperty("latitude_wsh")
    @TableField(value = "latitude_wsh")
    @Schema(description = "纬度")
    private BigDecimal latitude_wsh;

    @JsonProperty("longitude_wsh")
    @TableField(value = "longitude_wsh")
    @Schema(description = "经度")
    private BigDecimal longitude_wsh;

    @JsonProperty("is_default_wsh")
    @TableField(value = "is_default_wsh")
    @Schema(description = "是否默认")
    private Integer is_default_wsh;

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
