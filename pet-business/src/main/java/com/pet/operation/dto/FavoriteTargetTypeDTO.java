package com.pet.operation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class FavoriteTargetTypeDTO {
    @Schema(description = "目标类型编码")
    private String code_wsh;
    @Schema(description = "目标类型名称")
    private String label_wsh;
}
