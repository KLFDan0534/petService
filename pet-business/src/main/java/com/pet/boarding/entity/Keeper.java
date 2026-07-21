package com.pet.boarding.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 看护者实体
 * 映射数据库表 keeper_wsh，存储看护者基本信息及服务数据
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Getter
@Setter
@TableName("keeper_wsh")
@Schema(description = "看护者实体")
public class Keeper {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @JsonProperty("merchant_id_wsh")
    @JsonAlias({"merchantId", "merchant_id"})
    @TableField(value = "merchant_id_wsh")
    @Schema(description = "商家ID")
    private Long merchant_id_wsh;

    @JsonProperty("user_id_wsh")
    @JsonAlias({"userId", "user_id"})
    @TableField(value = "user_id_wsh")
    @Schema(description = "用户ID")
    private Long user_id_wsh;

    @JsonProperty("name_wsh")
    @JsonAlias({"name"})
    @TableField(value = "name_wsh")
    @Schema(description = "名称")
    private String name_wsh;

    @JsonProperty("phone_wsh")
    @JsonAlias({"phone"})
    @TableField(value = "phone_wsh")
    @Schema(description = "手机号")
    private String phone_wsh;

    @JsonProperty("avatar_wsh")
    @JsonAlias({"avatar"})
    @TableField(value = "avatar_wsh")
    @Schema(description = "头像URL")
    private String avatar_wsh;

    @JsonProperty("experience_years_wsh")
    @JsonAlias({"experienceYears", "experience_years"})
    @TableField(value = "experience_years_wsh")
    @Schema(description = "从业经验年数")
    private Integer experience_years_wsh;

    @JsonProperty("rating_wsh")
    @TableField(value = "rating_wsh")
    @Schema(description = "评分")
    private BigDecimal rating_wsh;

    @JsonProperty("completion_rate_wsh")
    @TableField(value = "completion_rate_wsh")
    @Schema(description = "完成率")
    private BigDecimal completion_rate_wsh;

    @JsonProperty("complaint_rate_wsh")
    @TableField(value = "complaint_rate_wsh")
    @Schema(description = "投诉率")
    private BigDecimal complaint_rate_wsh;

    @JsonProperty("price_per_day_wsh")
    @JsonAlias({"pricePerDay", "price_per_day"})
    @TableField(value = "price_per_day_wsh")
    @Schema(description = "每日价格")
    private BigDecimal price_per_day_wsh;

    @JsonProperty("max_pets_wsh")
    @JsonAlias({"maxPets", "max_pets"})
    @TableField(value = "max_pets_wsh")
    @Schema(description = "最大可接待宠物数")
    private Integer max_pets_wsh;

    @JsonProperty("current_pets_wsh")
    @JsonAlias({"currentPets", "current_pets"})
    @TableField(value = "current_pets_wsh")
    @Schema(description = "当前已接待宠物数")
    private Integer current_pets_wsh;

    @JsonProperty("bio_wsh")
    @JsonAlias({"bio"})
    @TableField(value = "bio_wsh")
    @Schema(description = "个人简介")
    private String bio_wsh;

    @JsonProperty("status_wsh")
    @JsonAlias({"status"})
    @TableField(value = "status_wsh")
    @Schema(description = "状态")
    private Integer status_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @JsonProperty("updated_at_wsh")
    @TableField(value = "updated_at_wsh", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;

    @JsonGetter("id")
    public Long getId() {
        return id_wsh;
    }

    @JsonGetter("merchantId")
    public Long getMerchantId() {
        return merchant_id_wsh;
    }

    @JsonGetter("userId")
    public Long getUserId() {
        return user_id_wsh;
    }

    @JsonGetter("name")
    public String getName() {
        return name_wsh;
    }

    @JsonGetter("phone")
    public String getPhone() {
        return phone_wsh;
    }

    @JsonGetter("avatar")
    public String getAvatar() {
        return avatar_wsh;
    }

    @JsonGetter("experienceYears")
    public Integer getExperienceYears() {
        return experience_years_wsh;
    }

    @JsonGetter("rating")
    public BigDecimal getRating() {
        return rating_wsh;
    }

    @JsonGetter("completionRate")
    public BigDecimal getCompletionRate() {
        return completion_rate_wsh;
    }

    @JsonGetter("complaintRate")
    public BigDecimal getComplaintRate() {
        return complaint_rate_wsh;
    }

    @JsonGetter("pricePerDay")
    public BigDecimal getPricePerDay() {
        return price_per_day_wsh;
    }

    @JsonGetter("maxPets")
    public Integer getMaxPets() {
        return max_pets_wsh;
    }

    @JsonGetter("currentPets")
    public Integer getCurrentPets() {
        return current_pets_wsh;
    }

    @JsonGetter("bio")
    public String getBio() {
        return bio_wsh;
    }

    @JsonGetter("status")
    public Integer getStatus() {
        return status_wsh;
    }
}
