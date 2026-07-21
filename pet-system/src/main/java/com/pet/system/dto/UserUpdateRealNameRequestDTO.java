package com.pet.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRealNameRequestDTO {
    @NotBlank(message = "Real name is required")
    @Schema(description = "真实姓名")
    private String real_name_wsh;

    @NotBlank(message = "ID card number is required")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$", message = "Invalid ID card number")
    @Schema(description = "身份证号")
    private String id_card_no_wsh;
}
