package com.pet.operation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 收藏目标类型 DTO，用于返回支持的收藏类型编码和显示名称
 */
@Data
public class FavoriteTargetTypeDTO {
    @Schema(description = "目标类型编码")
    private String code_wsh;
    @Schema(description = "目标类型名称")
    private String label_wsh;
}
