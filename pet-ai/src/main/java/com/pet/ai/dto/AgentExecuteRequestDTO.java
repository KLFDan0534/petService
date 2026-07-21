package com.pet.ai.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AgentExecuteRequestDTO {

    @JsonAlias("input")
    @NotBlank(message = "输入内容不能为空")
    @Size(max = 500, message = "输入内容过长，请控制在 500 字符以内")
    @Schema(description = "输入内容")
    private String input_wsh;

    @JsonAlias("latitude")
    @Schema(description = "纬度")
    private Double latitude_wsh;

    @JsonAlias("longitude")
    @Schema(description = "经度")
    private Double longitude_wsh;

    @JsonAlias("autoPay")
    @Schema(description = "是否自动支付")
    private Boolean auto_pay_wsh;

    @JsonAlias("paymentPassword")
    @Schema(description = "支付密码")
    private String payment_password_wsh;
}
