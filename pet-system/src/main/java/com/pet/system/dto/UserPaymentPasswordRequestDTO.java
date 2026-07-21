package com.pet.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPaymentPasswordRequestDTO {
    @NotBlank(message = "Payment password is required")
    @Pattern(regexp = "^\\d{6}$", message = "Payment password must be 6 digits")
    @Schema(description = "支付密码")
    private String payment_password_wsh;
}
