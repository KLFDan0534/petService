package com.pet.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class RoleVO {
    @Schema(description = "角色ID")
    private Long id_wsh;
    @Schema(description = "角色名称")
    private String name_wsh;
    @Schema(description = "角色编码")
    private String code_wsh;
    @Schema(description = "角色描述")
    private String description_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
    @Schema(description = "用户数量")
    private Integer user_count_wsh;
}
