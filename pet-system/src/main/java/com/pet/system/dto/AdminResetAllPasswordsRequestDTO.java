package com.pet.system.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminResetAllPasswordsRequestDTO {

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 72, message = "密码长度必须为6到72个字符")
    @JsonAlias("password")
    @Schema(description = "所有用户的新登录密码")
    private String password_wsh;
}
