package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款数据传输对象
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class RefundDTO {
    @Schema(description = "退款ID")
    private Long id_wsh;
    @Schema(description = "订单ID")
    private Long order_id_wsh;
    @Schema(description = "订单号")
    private String order_no_wsh;
    @Schema(description = "退款金额")
    private BigDecimal amount_wsh;
    @Schema(description = "退款原因")
    private String reason_wsh;
    @Schema(description = "退款状态")
    private String status_wsh;
    @Schema(description = "退款前订单状态")
    private String order_status_before_refund_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}
