package com.pet.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WithdrawalDTO {
    @Schema(description = "提现记录ID")
    private Long id_wsh;
    @Schema(description = "用户ID")
    private Long user_id_wsh;
    @Schema(description = "提现金额")
    private BigDecimal amount_wsh;
    @Schema(description = "手续费")
    private BigDecimal fee_wsh;
    @Schema(description = "实际到账金额")
    private BigDecimal actual_amount_wsh;
    @Schema(description = "银行名称")
    private String bank_name_wsh;
    @Schema(description = "银行卡号")
    private String bank_card_wsh;
    @Schema(description = "账户名称")
    private String account_name_wsh;
    @Schema(description = "提现状态")
    private String status_wsh;
    @Schema(description = "备注")
    private String remark_wsh;
    @Schema(description = "申请时间")
    private LocalDateTime created_at_wsh;
}