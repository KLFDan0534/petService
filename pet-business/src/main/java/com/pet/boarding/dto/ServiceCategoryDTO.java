package com.pet.boarding.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class ServiceCategoryDTO {
    @Schema(description = "分类ID")
    private Long id_wsh;
    @Schema(description = "父分类ID")
    private Long parent_id_wsh;
    @Schema(description = "分类名称")
    private String name_wsh;
    @Schema(description = "分类编码")
    private String code_wsh;
    @Schema(description = "排序")
    private Integer sort_order_wsh;
    @Schema(description = "状态")
    private Integer status_wsh;
}
