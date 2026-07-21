package com.pet.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateEmailRequestDTO {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    @Schema(description = "邮箱")
    private String email_wsh;
}
