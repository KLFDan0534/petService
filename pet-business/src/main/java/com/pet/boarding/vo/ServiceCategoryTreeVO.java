package com.pet.boarding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ServiceCategoryTreeVO {
    @Schema(description = "分类ID")
    private Long id_wsh;
    @Schema(description = "父分类ID")
    private Long parent_id_wsh;
    @Schema(description = "分类名称")
    private String name_wsh;
    @Schema(description = "分类编码")
    private String code_wsh;
    @Schema(description = "排序序号")
    private Integer sort_order_wsh;
    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status_wsh;
    @Schema(description = "子分类列表")
    private List<ServiceCategoryTreeVO> children = new ArrayList<>();
}
