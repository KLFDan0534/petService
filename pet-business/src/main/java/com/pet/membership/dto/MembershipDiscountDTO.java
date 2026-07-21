package com.pet.membership.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "会员订单折扣结果")
public class MembershipDiscountDTO {
    private Boolean eligible_wsh;
    private Long membership_id_wsh;
    private Long plan_id_wsh;
    private String plan_code_wsh;
    private String plan_name_wsh;
    private Integer level_wsh;
    private BigDecimal discount_rate_wsh;
    private BigDecimal base_amount_wsh;
    private BigDecimal membership_discount_wsh;
    private BigDecimal final_amount_wsh;
    private String snapshot_wsh;
}
