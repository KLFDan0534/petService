package com.pet.order.dto;

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
    private Long id_wsh;
    private Long order_id_wsh;
    private String order_no_wsh;
    private String pay_no_wsh;
    private BigDecimal amount_wsh;
    private String method_wsh;
    private String status_wsh;
    private LocalDateTime paid_at_wsh;
    private LocalDateTime created_at_wsh;
}