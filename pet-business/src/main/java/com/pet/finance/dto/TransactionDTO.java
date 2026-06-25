package com.pet.finance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionDTO {
    private Long id_wsh;
    private Long wallet_id_wsh;
    private Long user_id_wsh;
    private String type_wsh;
    private BigDecimal amount_wsh;
    private BigDecimal balance_after_wsh;
    private Long order_id_wsh;
    private String description_wsh;
    private LocalDateTime created_at_wsh;
}