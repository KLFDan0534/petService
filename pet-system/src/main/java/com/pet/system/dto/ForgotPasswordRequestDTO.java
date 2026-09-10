package com.pet.system.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ForgotPasswordRequestDTO {
    @NotBlank(message = "邮箱不能为空")
    @Schema(description = "邮箱")
    private String email_wsh;

    @Schema(description = "Cloudflare Turnstile 人机校验 token")
    private String turnstileToken;
}
