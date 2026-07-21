package com.pet.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CouponQuoteRequestDTO {
    @Schema(description = "用户优惠券ID")
    private Long user_coupon_id_wsh;

    @Schema(description = "商家ID")
    private Long merchant_id_wsh;

    @Schema(description = "服务ID")
    private Long service_id_wsh;

    @Schema(description = "订单总金额")
    private BigDecimal total_amount_wsh;

    @Schema(description = "长住优惠金额")
    private BigDecimal long_stay_discount_wsh;
}
