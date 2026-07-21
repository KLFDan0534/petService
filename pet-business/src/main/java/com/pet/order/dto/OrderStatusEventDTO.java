package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderStatusEventDTO {
    @Schema(description = "订单ID")
    private Long order_id_wsh;
    @Schema(description = "订单号")
    private String order_no_wsh;
    @Schema(description = "订单状态")
    private String status_wsh;
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;
}
