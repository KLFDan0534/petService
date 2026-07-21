package com.pet.system.dto;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UpdateUserStatusRequestDTO {
    @NotNull(message = "状态不能为空")
    @Schema(description = "用户状态")
    private Integer status_wsh;
}
