package com.pet.system.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequestDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
    @JsonAlias("username")
    @Schema(description = "用户名")
    private String username_wsh;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6-100之间")
    @JsonAlias("password")
    @Schema(description = "密码")
    private String password_wsh;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @JsonAlias("phone")
    @Schema(description = "手机号")
    private String phone_wsh;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码格式不正确")
    @JsonAlias("captcha")
    @Schema(description = "验证码")
    private String captcha_wsh;

    @JsonAlias("nickname")
    @Schema(description = "昵称")
    private String nickname_wsh;

    @JsonAlias("email")
    @Schema(description = "邮箱")
    private String email_wsh;
}
