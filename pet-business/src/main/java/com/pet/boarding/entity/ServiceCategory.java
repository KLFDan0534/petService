package com.pet.boarding.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 服务分类（ServiceCategory）实体，映射 service_category_wsh 表。
 * <p>
 * 采用树形结构，支持多级分类（通过 parent_id 关联父分类）。
 * 用于对商家提供的宠物服务项目进行归类，如"洗澡"、"美容"、"寄养"等一级分类下可以设立子分类。
 * 分类有启用/禁用状态，启用的分类才能被服务项目引用。
 *
 * @author: wsh
 */
@Getter
@Setter
@TableName("service_category_wsh")
@Schema(description = "服务分类实体")
public class ServiceCategory {
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @Schema(description = "父分类ID")
    private Long parent_id_wsh;

    @Schema(description = "名称")
    private String name_wsh;

    @Schema(description = "编码")
    private String code_wsh;

    @TableField(value = "sort_wsh")
    @Schema(description = "排序")
    private Integer sort_order_wsh;

    @Schema(description = "状态")
    private Integer status_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;
}
