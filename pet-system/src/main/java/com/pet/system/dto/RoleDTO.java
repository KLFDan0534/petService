package com.pet.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class RoleDTO {
    @Schema(description = "角色ID")
    private Long id_wsh;
    @Schema(description = "角色名称")
    private String name_wsh;
    @Schema(description = "角色编码")
    private String code_wsh;
    @Schema(description = "角色描述")
    private String description_wsh;
}