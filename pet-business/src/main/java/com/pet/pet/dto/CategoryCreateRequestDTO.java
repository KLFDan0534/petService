package com.pet.pet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryCreateRequestDTO {
    @Schema(description = "分类名称")
    @NotBlank(message = "分类名称不能为空")
    private String name_wsh;
    @Schema(description = "父分类ID")
    private Long parent_id_wsh;
    @Schema(description = "排序值")
    private Integer sort_order_wsh;
}
