package com.pet.system.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class RefreshTokenRequestDTO {
    @NotBlank(message = "刷新令牌不能为空")
    @Schema(description = "刷新令牌")
    private String refresh_token_wsh;
}
