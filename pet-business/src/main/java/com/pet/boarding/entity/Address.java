package com.pet.boarding.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户地址（Address）实体，映射 address_wsh 表。
 * <p>
 * 存储用户的收货/联系地址，支持多地址管理和默认地址设置。
 * 每个地址包含标签（如"家"、"公司"）、联系人、联系电话、地理坐标等信息。
 * 一个用户可以有多个地址，但最多只有一个默认地址。
 *
 * @author: wsh
 */
@Getter
@Setter
@TableName("address_wsh")
@Schema(description = "地址实体")
public class Address {
    @TableId(type = IdType.AUTO, value = "id_wsh")
    @Schema(description = "ID")
    private Long id_wsh;

    @TableField(value = "user_id_wsh")
    @Schema(description = "用户ID")
    private Long user_id_wsh;

    @TableField(value = "label_wsh")
    @Schema(description = "标签")
    private String label_wsh;

    @TableField(value = "name_wsh")
    @Schema(description = "名称")
    private String name_wsh;

    @TableField(value = "phone_wsh")
    @Schema(description = "手机号")
    private String phone_wsh;

    @TableField(value = "address_wsh")
    @Schema(description = "地址")
    private String address_wsh;

    @TableField(value = "detail_wsh")
    @Schema(description = "详细地址")
    private String detail_wsh;

    @TableField(value = "latitude_wsh")
    @Schema(description = "纬度")
    private BigDecimal latitude_wsh;

    @TableField(value = "longitude_wsh")
    @Schema(description = "经度")
    private BigDecimal longitude_wsh;

    @TableField(value = "is_default_wsh")
    @Schema(description = "是否默认")
    private Integer is_default_wsh;

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
