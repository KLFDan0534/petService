package com.pet.order.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 小费数据传输对象
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class TipDTO {
    private Long id_wsh;
    private Long order_id_wsh;
    private Long from_user_id_wsh;
    private Long to_user_id_wsh;
    private BigDecimal amount_wsh;
    private String message_wsh;
    private LocalDateTime created_at_wsh;
}