package com.pet.membership.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "Member plan")
public class MemberPlanDTO {
    private Long id_wsh;
    private String code_wsh;
    private String name_wsh;
    private Integer level_wsh;
    private BigDecimal price_wsh;
    private Integer duration_days_wsh;
    private BigDecimal discount_rate_wsh;
    private String monthly_coupon_config_wsh;
    private String benefit_config_wsh;
    private Integer status_wsh;
    private Integer sort_order_wsh;
    private String remark_wsh;
    private LocalDateTime created_at_wsh;
    private LocalDateTime updated_at_wsh;
}
