package com.pet.system.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDTO {
    @NotBlank(message = "Username is required")
    @JsonAlias("username")
    @Schema(description = "用户名")
    private String username_wsh;

    @NotBlank(message = "Password is required")
    @JsonAlias("password")
    @Schema(description = "密码")
    private String password_wsh;
}
