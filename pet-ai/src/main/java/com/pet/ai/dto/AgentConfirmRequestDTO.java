package com.pet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 智能下单 confirm 阶段请求：凭会话ID落单，可选自动支付。
 * <p>
 * 一个会话只能下一单：服务端用 status 的 CAS 占用会话，重复点击会被挡住。
 */
@Data
public class AgentConfirmRequestDTO {

    @NotBlank(message = "会话ID不能为空")
    @Schema(description = "会话ID（message 接口返回）")
    private String session_id_wsh;

    @Schema(description = "是否自动支付")
    private Boolean auto_pay_wsh;

    @Schema(description = "支付密码（auto_pay 为 true 时必填）")
    private String payment_password_wsh;
}