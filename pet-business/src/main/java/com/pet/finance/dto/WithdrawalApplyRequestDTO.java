package com.pet.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class WithdrawalApplyRequestDTO {
    @Schema(description = "提现金额")
    private BigDecimal amount_wsh;

    @Schema(description = "银行名称")
    private String bank_name_wsh;

    @Schema(description = "银行卡号")
    private String bank_card_wsh;

    @Schema(description = "账户名称")
    private String account_name_wsh;
}
