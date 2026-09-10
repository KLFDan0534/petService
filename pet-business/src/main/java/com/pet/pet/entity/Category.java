package com.pet.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 宠物分类实体，映射数据库表 category_wsh。
 * 支持多级树形分类结构（通过 parent_id 自关联），
 * 用于对宠物服务项目进行层级归类，sort_order 控制同级展示顺序。
 *
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Getter
@Setter
@TableName("category_wsh")
@Schema(description = "宠物分类实体")
public class Category {
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @NotBlank(message = "分类名称不能为空")
    @TableField(value = "name_wsh")
    @Schema(description = "名称")
    private String name_wsh;

    @TableField(value = "parent_id_wsh")
    @Schema(description = "父分类ID")
    private Long parent_id_wsh;

    @TableField(value = "sort_order_wsh")
    @Schema(description = "排序")
    private Integer sort_order_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @TableField(value = "updated_at_wsh", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;
}
