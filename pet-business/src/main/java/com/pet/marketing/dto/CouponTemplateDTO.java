package com.pet.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class CouponTemplateDTO {
    @Schema(description = "优惠券模板ID")
    private Long id_wsh;
    @Schema(description = "优惠券名称")
    private String name_wsh;
    @Schema(description = "优惠券类型")
    private String type_wsh;
    @Schema(description = "使用门槛金额")
    private BigDecimal threshold_amount_wsh;
    @Schema(description = "折扣金额")
    private BigDecimal discount_amount_wsh;
    @Schema(description = "折扣率")
    private BigDecimal discount_rate_wsh;
    @Schema(description = "最大折扣金额")
    private BigDecimal max_discount_amount_wsh;
    @Schema(description = "发放总数量")
    private Integer total_quantity_wsh;
    @Schema(description = "已发放数量")
    private Integer issued_quantity_wsh;
    @Schema(description = "每人限领数量")
    private Integer per_user_limit_wsh;
    @Schema(description = "有效期开始时间")
    private LocalDateTime valid_from_wsh;
    @Schema(description = "有效期结束时间")
    private LocalDateTime valid_to_wsh;
    @Schema(description = "状态")
    private Integer status_wsh;
    @Schema(description = "适用范围类型")
    private String scope_type_wsh;
    @Schema(description = "商家ID")
    private Long merchant_id_wsh;
    @Schema(description = "创建人ID")
    private Long created_by_wsh;
    @Schema(description = "备注")
    private String remark_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;
}
