package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderCancelRequestDTO {
    @Schema(description = "订单ID")
    @JsonAlias({"orderId", "order_id", "id_wsh", "id"})
    private Long order_id_wsh;

    @Schema(description = "订单号")
    @JsonAlias({"orderNo", "order_no"})
    private String order_no_wsh;
}
