package com.pet.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WalletAdjustRequestDTO {
    @Schema(description = "用户ID")
    private Long user_id_wsh;
    @Schema(description = "调整模式（set/amount）")
    private String mode_wsh = "set";
    @Schema(description = "设置余额（mode=set时使用）")
    private BigDecimal balance_wsh;
    @Schema(description = "调整金额（mode=amount时使用）")
    private BigDecimal amount_wsh;
    @Schema(description = "调整备注")
    private String remark_wsh;
    @Schema(description = "请求ID（幂等）")
    private String request_id_wsh;

    public String getMode_wsh() {
        return mode_wsh == null || mode_wsh.isBlank() ? "set" : mode_wsh.trim().toLowerCase();
    }
}
