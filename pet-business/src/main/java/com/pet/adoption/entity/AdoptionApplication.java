package com.pet.adoption.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 领养申请实体
 * 映射数据库表 adoption_application_wsh，存储领养申请及审核信息
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Getter
@Setter
@TableName("adoption_application_wsh")
public class AdoptionApplication {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    private Long id_wsh;

    @JsonProperty("user_id_wsh")
    @TableField(value = "user_id_wsh")
    private Long user_id_wsh;

    @JsonProperty("pet_id_wsh")
    @TableField(value = "pet_id_wsh")
    private Long pet_id_wsh;

    @JsonProperty("merchant_id_wsh")
    @TableField(value = "merchant_id_wsh")
    private Long merchant_id_wsh;

    @JsonProperty("applicant_name_wsh")
    @NotBlank(message = "Applicant name cannot be empty")
    @TableField(value = "applicant_name_wsh")
    private String applicant_name_wsh;

    @JsonProperty("applicant_phone_wsh")
    @NotBlank(message = "Contact phone cannot be empty")
    @TableField(value = "applicant_phone_wsh")
    private String applicant_phone_wsh;

    @JsonProperty("applicant_address_wsh")
    @NotBlank(message = "Residence address cannot be empty")
    @TableField(value = "applicant_address_wsh")
    private String applicant_address_wsh;

    @JsonProperty("housing_type_wsh")
    @TableField(value = "housing_type_wsh")
    private String housing_type_wsh;

    @JsonProperty("has_yard_wsh")
    @TableField(value = "has_yard_wsh")
    private Integer has_yard_wsh;

    @JsonProperty("family_members_wsh")
    @TableField(value = "family_members_wsh")
    private String family_members_wsh;

    @JsonProperty("pet_experience_wsh")
    @TableField(value = "pet_experience_wsh")
    private String pet_experience_wsh;

    @JsonProperty("reason_wsh")
    @NotBlank(message = "Adoption reason cannot be empty")
    @TableField(value = "reason_wsh")
    private String reason_wsh;

    @JsonProperty("economic_condition_wsh")
    @TableField(value = "economic_condition_wsh")
    private String economic_condition_wsh;

    @JsonProperty("agree_visit_wsh")
    @TableField(value = "agree_visit_wsh")
    private Integer agree_visit_wsh;

    @JsonProperty("merchant_status_wsh")
    @TableField(value = "merchant_status_wsh")
    private String merchant_status_wsh;

    @JsonProperty("merchant_remark_wsh")
    @TableField(value = "merchant_remark_wsh")
    private String merchant_remark_wsh;

    @JsonProperty("admin_status_wsh")
    @TableField(value = "admin_status_wsh")
    private String admin_status_wsh;

    @JsonProperty("admin_remark_wsh")
    @TableField(value = "admin_remark_wsh")
    private String admin_remark_wsh;

    @JsonProperty("status_wsh")
    @TableField(value = "status_wsh")
    private String status_wsh;

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
