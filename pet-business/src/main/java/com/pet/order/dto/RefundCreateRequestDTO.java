package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundCreateRequestDTO {
    @Schema(description = "订单ID")
    private Long order_id_wsh;

    @Schema(description = "退款原因")
    private String reason_wsh;
}
