package com.pet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 智能下单 confirm 阶段请求：凭方案 token 落单，可选自动支付。
 */
@Data
public class AgentConfirmRequestDTO {

    @NotBlank(message = "方案 token 不能为空")
    @Schema(description = "plan 阶段返回的方案 token")
    private String plan_token_wsh;

    @Schema(description = "是否自动支付")
    private Boolean auto_pay_wsh;

    @Schema(description = "支付密码（auto_pay 为 true 时必填）")
    private String payment_password_wsh;
}