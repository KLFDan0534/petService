package com.pet.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 宠物分类实体
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Getter
@Setter
@TableName("category_wsh")
public class Category {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    private Long id_wsh;

    @JsonProperty("name_wsh")
    @NotBlank(message = "分类名称不能为空")
    @TableField(value = "name_wsh")
    private String name_wsh;

    @JsonProperty("parent_id_wsh")
    @TableField(value = "parent_id_wsh")
    private Long parent_id_wsh;

    @JsonProperty("sort_order_wsh")
    @TableField(value = "sort_order_wsh")
    private Integer sort_order_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    private LocalDateTime created_at_wsh;

    @JsonProperty("updated_at_wsh")
    @TableField(value = "updated_at_wsh", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updated_at_wsh;
}
