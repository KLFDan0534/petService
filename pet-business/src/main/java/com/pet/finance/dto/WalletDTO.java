package com.pet.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WalletDTO {
    @Schema(description = "钱包ID")
    private Long id_wsh;
    @Schema(description = "用户ID")
    private Long user_id_wsh;
    @Schema(description = "钱包余额")
    private BigDecimal balance_wsh;
    @Schema(description = "冻结金额")
    private BigDecimal frozen_amount_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}