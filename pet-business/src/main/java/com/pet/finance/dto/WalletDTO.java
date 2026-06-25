package com.pet.finance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WalletDTO {
    private Long id_wsh;
    private Long user_id_wsh;
    private BigDecimal balance_wsh;
    private BigDecimal frozen_amount_wsh;
    private LocalDateTime created_at_wsh;
}