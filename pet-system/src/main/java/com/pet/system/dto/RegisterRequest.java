package com.pet.system.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @jakarta.validation.constraints.Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
    @JsonAlias("username")
    @JsonProperty("username_wsh")
    private String username_wsh;

    @NotBlank(message = "密码不能为空")
    @jakarta.validation.constraints.Size(min = 6, max = 100, message = "密码长度必须在6-100之间")
    @JsonAlias("password")
    @JsonProperty("password_wsh")
    private String password_wsh;

    @JsonAlias("nickname")
    @JsonProperty("nickname_wsh")
    private String nickname_wsh;

    @JsonAlias("phone")
    @JsonProperty("phone_wsh")
    private String phone_wsh;

    @JsonAlias("email")
    @JsonProperty("email_wsh")
    private String email_wsh;
}
