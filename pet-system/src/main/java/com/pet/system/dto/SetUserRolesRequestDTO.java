package com.pet.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class SetUserRolesRequestDTO {
    @NotNull(message = "角色ID列表不能为空")
    @Schema(description = "角色ID列表")
    private List<Long> role_ids_wsh;
}
