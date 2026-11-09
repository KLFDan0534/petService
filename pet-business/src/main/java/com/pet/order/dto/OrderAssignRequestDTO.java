package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderAssignRequestDTO {

    @Schema(description = "订单号")
    @NotBlank(message = "orderNo cannot be empty")
    private String order_no_wsh;

    @Schema(description = "目标看护者ID（必须是订单所属商家名下的看护者）")
    @NotNull(message = "keeperId cannot be empty")
    private Long keeper_id_wsh;
}
