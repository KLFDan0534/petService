package com.pet.pet.dto;

import lombok.Data;
import java.util.List;

/**
 * 分类数据传输对象
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class CategoryDTO {
    private Long id_wsh;
    private String name_wsh;
    private Long parent_id_wsh;
    private Integer sort_order_wsh;
    private List<CategoryDTO> children;
}