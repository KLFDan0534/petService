package com.pet.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CouponQuoteDTO {
    @Schema(description = "用户优惠券ID")
    private Long user_coupon_id_wsh;
    @Schema(description = "优惠券模板ID")
    private Long template_id_wsh;
    @Schema(description = "优惠券名称")
    private String coupon_name_wsh;
    @Schema(description = "订单总金额")
    private BigDecimal total_amount_wsh;
    @Schema(description = "长住优惠金额")
    private BigDecimal long_stay_discount_wsh;
    @Schema(description = "优惠券抵扣金额")
    private BigDecimal coupon_discount_wsh;
    @Schema(description = "平台补贴金额")
    private BigDecimal platform_subsidy_wsh;
    @Schema(description = "结算金额")
    private BigDecimal settlement_amount_wsh;
    @Schema(description = "最终金额")
    private BigDecimal final_amount_wsh;
    @Schema(description = "出资方")
    private String funding_party_wsh;
}
