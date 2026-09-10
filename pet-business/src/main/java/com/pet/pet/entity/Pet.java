package com.pet.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 宠物实体，映射数据库表 pet_wsh。
 * 包含宠物的基础信息（名称、类型、品种、年龄、体重、性别）、
 * 健康状态（绝育、疫苗）、外观（头像）和个性化信息（描述、过敏、习惯）。
 * 通过 owner_id 关联宠物主人（User 表），支持逻辑删除。
 *
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Getter
@Setter
@TableName("pet_wsh")
@Schema(description = "宠物实体")
public class Pet {
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @TableField(value = "owner_id_wsh")
    @Schema(description = "宠物主用户ID")
    private Long owner_id_wsh;

    @NotBlank(message = "宠物名称不能为空")
    @TableField(value = "name_wsh")
    @Schema(description = "名称")
    private String name_wsh;

    @NotBlank(message = "宠物类型不能为空")
    @TableField(value = "type_wsh")
    @Schema(description = "类型")
    private String type_wsh;

    @TableField(value = "breed_wsh")
    @Schema(description = "品种")
    private String breed_wsh;

    @Positive(message = "年龄必须为正数")
    @TableField(value = "age_wsh")
    @Schema(description = "年龄")
    private Integer age_wsh;

    @Positive(message = "体重必须为正数")
    @TableField(value = "weight_wsh")
    @Schema(description = "体重(kg)")
    private BigDecimal weight_wsh;

    @TableField(value = "gender_wsh")
    @Schema(description = "性别")
    private Integer gender_wsh;

    @TableField(value = "sterilized_wsh")
    @Schema(description = "是否绝育")
    private Integer sterilized_wsh;

    @TableField(value = "vaccinated_wsh")
    @Schema(description = "是否已接种疫苗")
    private Integer vaccinated_wsh;

    @TableField(value = "avatar_wsh")
    @Schema(description = "头像URL")
    private String avatar_wsh;

    @TableField(value = "description_wsh")
    @Schema(description = "描述")
    private String description_wsh;

    @TableField(value = "allergies_wsh")
    @Schema(description = "过敏信息")
    private String allergies_wsh;

    @TableField(value = "habits_wsh")
    @Schema(description = "生活习惯")
    private String habits_wsh;

    @TableField(exist = false)
    @Schema(description = "宠物主姓名")
    private String owner_name_wsh;

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
