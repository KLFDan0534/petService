package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderProcessRequestDTO {
    @Schema(description = "订单号")
    @NotBlank(message = "orderNo cannot be empty")
    private String order_no_wsh;
}
