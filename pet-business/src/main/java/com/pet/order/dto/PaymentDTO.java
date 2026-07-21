package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付数据传输对象
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class PaymentDTO {
    @Schema(description = "支付ID")
    private Long id_wsh;
    @Schema(description = "订单ID")
    private Long order_id_wsh;
    @Schema(description = "订单号")
    private String order_no_wsh;
    @Schema(description = "支付单号")
    private String pay_no_wsh;
    @Schema(description = "支付金额")
    private BigDecimal amount_wsh;
    @Schema(description = "支付方式")
    private String method_wsh;
    @Schema(description = "支付状态")
    private String status_wsh;
    @Schema(description = "支付时间")
    private LocalDateTime paid_at_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @JsonProperty("id")
    public Long getId() { return id_wsh; }

    @JsonProperty("orderId")
    public Long getOrderId() { return order_id_wsh; }

    @JsonProperty("orderNo")
    public String getOrderNo() { return order_no_wsh; }

    @JsonProperty("payNo")
    public String getPayNo() { return pay_no_wsh; }

    @JsonProperty("amount")
    public BigDecimal getAmount() { return amount_wsh; }

    @JsonProperty("method")
    public String getMethod() { return method_wsh; }

    @JsonProperty("status")
    public String getStatus() { return status_wsh; }
}
