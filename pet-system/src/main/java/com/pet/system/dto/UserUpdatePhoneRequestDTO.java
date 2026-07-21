package com.pet.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdatePhoneRequestDTO {
    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number")
    @Schema(description = "手机号")
    private String phone_wsh;
}
