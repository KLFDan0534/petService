package com.pet.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterCaptchaVO {
    @Schema(description = "手机号")
    private String phone_wsh;
    @Schema(description = "验证码")
    private String captcha_wsh;
    @Schema(description = "过期时间（秒）")
    private Integer expires_in_seconds_wsh;
}

