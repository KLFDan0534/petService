package com.pet.order.dto;

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
    private Long id_wsh;
    private Long order_id_wsh;
    private String order_no_wsh;
    private BigDecimal amount_wsh;
    private String reason_wsh;
    private String status_wsh;
    private LocalDateTime created_at_wsh;
}