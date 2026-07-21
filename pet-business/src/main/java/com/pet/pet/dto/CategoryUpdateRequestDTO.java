package com.pet.pet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CategoryUpdateRequestDTO {
    @Schema(description = "分类名称")
    private String name_wsh;
    @Schema(description = "父分类ID")
    private Long parent_id_wsh;
    @Schema(description = "排序值")
    private Integer sort_order_wsh;
}
