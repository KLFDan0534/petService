package com.pet.boarding.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 商家（Merchant）实体，映射 merchant_wsh 表。
 * <p>
 * 存储宠物寄养商家的基本信息、地理位置、审核状态、店铺营业模式与实时营业状态。
 * 商家是整个寄养服务平台的核心主体，旗下管理看护者（Keeper）和服务项目（ServiceItem）。
 * <p>
 * <b>状态流转：</b>
 * <ul>
 *   <li>MERCHANT_PENDING（待审核）→ MERCHANT_APPROVED（已通过）/ MERCHANT_REJECTED（已驳回）</li>
 *   <li>店铺营业模式：AUTO（按时间自动开关）| MANUAL_OPEN（手动开门）| MANUAL_CLOSED（手动关店）</li>
 *   <li>店铺状态：由 {@code com.pet.boarding.service.impl.MerchantServiceImpl#resolveStoreStatus} 实时计算</li>
 * </ul>
 *
 * @author: wsh
 */
@Getter
@Setter
@TableName("merchant_wsh")
@Schema(description = "商家实体")
public class Merchant {
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @TableField(value = "user_id_wsh")
    @Schema(description = "用户ID")
    private Long user_id_wsh;

    @TableField(value = "name_wsh")
    @Schema(description = "名称")
    private String name_wsh;

    @TableField(value = "phone_wsh")
    @Schema(description = "手机号")
    private String phone_wsh;

    @TableField(value = "address_wsh")
    @Schema(description = "地址")
    private String address_wsh;

    @TableField(value = "latitude_wsh")
    @Schema(description = "纬度")
    private BigDecimal latitude_wsh;

    @TableField(value = "longitude_wsh")
    @Schema(description = "经度")
    private BigDecimal longitude_wsh;

    @TableField(value = "description_wsh")
    @Schema(description = "描述")
    private String description_wsh;

    @TableField(value = "business_license_wsh")
    @Schema(description = "营业执照URL")
    private String business_license_wsh;

    @TableField(value = "rating_wsh")
    @Schema(description = "评分")
    private BigDecimal rating_wsh;

    @TableField(value = "status_wsh")
    @Schema(description = "状态")
    private Integer status_wsh;

    @TableField(value = "store_mode_wsh")
    @Schema(description = "店铺模式")
    private Integer store_mode_wsh;

    @TableField(value = "store_status_wsh")
    @Schema(description = "店铺状态")
    private Integer store_status_wsh;

    @TableField(value = "future_booking_enabled_wsh")
    @Schema(description = "是否接受未来预约: 0-关闭 1-开启(默认开启)")
    private Integer future_booking_enabled_wsh;

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

    @TableField(exist = false)
    @Schema(description = "距离(米)")
    private Double distance_wsh;
}
