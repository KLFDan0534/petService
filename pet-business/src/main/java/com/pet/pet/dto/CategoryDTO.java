package com.pet.pet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/**
 * 分类数据传输对象
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class CategoryDTO {
    @Schema(description = "分类ID")
    private Long id_wsh;
    @Schema(description = "分类名称")
    private String name_wsh;
    @Schema(description = "父分类ID")
    private Long parent_id_wsh;
    @Schema(description = "排序值")
    private Integer sort_order_wsh;
    @Schema(description = "子分类列表")
    private List<CategoryDTO> children;
}