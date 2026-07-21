package com.pet.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleCountDTO {
    @Schema(description = "角色编码")
    private String roleCode;
    @Schema(description = "角色数量")
    private long count;
}
