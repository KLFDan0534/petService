package com.pet.membership.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Member plan create request")
public class MemberPlanCreateRequestDTO {
    @NotBlank(message = "Plan code cannot be empty")
    @Schema(description = "Stable plan code")
    private String code_wsh;

    @NotBlank(message = "Plan name cannot be empty")
    @Schema(description = "Plan name")
    private String name_wsh;

    @NotNull(message = "Plan level cannot be empty")
    @Min(value = 1, message = "Plan level must be greater than zero")
    @Schema(description = "Plan level")
    private Integer level_wsh;

    @NotNull(message = "Plan price cannot be empty")
    @DecimalMin(value = "0.00", message = "Plan price cannot be negative")
    @Schema(description = "Plan price")
    private BigDecimal price_wsh;

    @NotNull(message = "Plan duration cannot be empty")
    @Min(value = 1, message = "Plan duration must be greater than zero")
    @Schema(description = "Plan duration in days")
    private Integer duration_days_wsh;

    @Schema(description = "Order discount rate, 1.00 means no discount")
    private BigDecimal discount_rate_wsh;

    @Schema(description = "Monthly coupon config JSON")
    private String monthly_coupon_config_wsh;

    @Schema(description = "Benefit config JSON")
    private String benefit_config_wsh;

    @Schema(description = "0 disabled, 1 enabled")
    private Integer status_wsh;

    @Schema(description = "Sort order")
    private Integer sort_order_wsh;

    @Schema(description = "Remark")
    private String remark_wsh;
}
