package com.pet.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class UserCouponDTO {
    @Schema(description = "用户优惠券ID")
    private Long id_wsh;
    @Schema(description = "优惠券模板ID")
    private Long template_id_wsh;
    @Schema(description = "用户ID")
    private Long user_id_wsh;
    @Schema(description = "使用状态")
    private String status_wsh;
    @Schema(description = "来源")
    private String source_wsh;
    @Schema(description = "使用订单ID")
    private Long order_id_wsh;
    @Schema(description = "使用订单编号")
    private String order_no_wsh;
    @Schema(description = "抵扣金额")
    private BigDecimal discount_amount_wsh;
    @Schema(description = "过期时间")
    private LocalDateTime expire_at_wsh;
    @Schema(description = "领取时间")
    private LocalDateTime created_at_wsh;

    @Schema(description = "优惠券名称")
    private String name_wsh;
    @Schema(description = "优惠券类型")
    private String type_wsh;
    @Schema(description = "使用门槛金额")
    private BigDecimal threshold_amount_wsh;
    @Schema(description = "模板折扣金额")
    private BigDecimal discount_amount_template_wsh;
    @Schema(description = "折扣率")
    private BigDecimal discount_rate_wsh;
    @Schema(description = "最大折扣金额")
    private BigDecimal max_discount_amount_wsh;
    @Schema(description = "适用范围类型")
    private String scope_type_wsh;
    @Schema(description = "商家ID")
    private Long merchant_id_wsh;
}
