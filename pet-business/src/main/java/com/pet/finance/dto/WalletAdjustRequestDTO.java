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

    /**
     * 【获取规范化后的调整模式】
     *
     * 业务作用：返回管理员调整余额的模式标识，对空值与大小写做兜底处理。
     *
     * 调用场景：WalletController.adjustBalance() 分流 set/add/subtract 三种调整方式。
     *
     * 调用链：WalletController.adjustBalance() → getMode_wsh() → 分支调用 AccountingService 的 setBalanceByAdmin/credit/debit。
     *
     * 数据处理：mode_wsh 为 null 或空白时返回 "set"，否则 trim 后转小写。
     *
     * 业务规则：默认模式为 set（直接设置余额）。
     *
     * 状态影响：无。
     *
     * 注意事项：字段上的 @Schema 说明写的是 set/amount，而控制器实际分支用的是 set/add/subtract，文档与实现不一致；本 getter 为手写实现，Lombok @Getter 检测到同名方法后不再生成，实际以本方法为准。
     */
    public String getMode_wsh() {
        return mode_wsh == null || mode_wsh.isBlank() ? "set" : mode_wsh.trim().toLowerCase();
    }
}
