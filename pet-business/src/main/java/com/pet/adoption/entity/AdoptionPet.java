package com.pet.adoption.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 领养宠物实体
 * 映射数据库表 adoption_pet_wsh，存储待领养宠物的详细信息
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Getter
@Setter
@TableName("adoption_pet_wsh")
public class AdoptionPet {
    @TableId(value = "id_wsh", type = IdType.AUTO)
    private Long id_wsh;

    @TableField(value = "merchant_id_wsh")
    private Long merchant_id_wsh;

    @TableField(value = "name_wsh")
    @NotBlank(message = "Pet name is required")
    private String name_wsh;

    @TableField(value = "type_wsh")
    private String type_wsh;

    @TableField(value = "breed_wsh")
    private String breed_wsh;

    @TableField(value = "age_wsh")
    @Positive(message = "Age must be positive")
    private Integer age_wsh;

    @TableField(value = "gender_wsh")
    private String gender_wsh;

    @TableField(value = "weight_wsh")
    private BigDecimal weight_wsh;

    @TableField(value = "color_wsh")
    private String color_wsh;

    @TableField(value = "health_status_wsh")
    private String health_status_wsh;

    @TableField(value = "vaccinated_wsh")
    private Integer vaccinated_wsh;

    @TableField(value = "sterilized_wsh")
    private Integer sterilized_wsh;

    @TableField(value = "personality_wsh")
    private String personality_wsh;

    @TableField(value = "story_wsh")
    private String story_wsh;

    @TableField(value = "adoption_requirements_wsh")
    private String adoption_requirements_wsh;

    @TableField(value = "adoption_fee_wsh")
    private BigDecimal adoption_fee_wsh;

    @TableField(value = "cover_image_wsh")
    private String cover_image_wsh;

    @TableField(value = "images_wsh")
    private String images_wsh;

    @TableField(value = "status_wsh")
    private String status_wsh;

    @TableLogic
    @com.fasterxml.jackson.annotation.JsonIgnore
    @TableField(value = "deleted_wsh")
    private Integer deleted_wsh;

    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    private LocalDateTime created_at_wsh;

    @TableField(value = "updated_at_wsh", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updated_at_wsh;
}
