package com.pet.marketing.entity;

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

@Getter
@Setter
@TableName("coupon_usage_wsh")
@Schema(description = "优惠券使用记录实体")
public class CouponUsage {
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @Schema(description = "用户优惠券ID")
    private Long user_coupon_id_wsh;

    @Schema(description = "优惠券模板ID")
    private Long template_id_wsh;

    @Schema(description = "用户ID")
    private Long user_id_wsh;

    @Schema(description = "订单ID")
    private Long order_id_wsh;

    @Schema(description = "订单号")
    private String order_no_wsh;

    @Schema(description = "优惠金额")
    private BigDecimal discount_amount_wsh;

    @Schema(description = "承担方")
    private String funding_party_wsh;

    @Schema(description = "状态")
    private String status_wsh;

    @JsonIgnore
    @TableLogic
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}
