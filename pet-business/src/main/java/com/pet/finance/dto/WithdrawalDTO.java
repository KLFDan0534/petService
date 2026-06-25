package com.pet.finance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WithdrawalDTO {
    private Long id_wsh;
    private Long user_id_wsh;
    private BigDecimal amount_wsh;
    private BigDecimal fee_wsh;
    private BigDecimal actual_amount_wsh;
    private String bank_name_wsh;
    private String bank_card_wsh;
    private String account_name_wsh;
    private String status_wsh;
    private String remark_wsh;
    private LocalDateTime created_at_wsh;
}