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

/**
 * 看护者实体
 * 映射数据库表 keeper_wsh，存储看护者基本信息及服务数据
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Getter
@Setter
@TableName("keeper_wsh")
public class Keeper {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    private Long id_wsh;

    @JsonProperty("merchant_id_wsh")
    @JsonAlias({"merchantId", "merchant_id"})
    @TableField(value = "merchant_id_wsh")
    private Long merchant_id_wsh;

    @JsonProperty("user_id_wsh")
    @JsonAlias({"userId", "user_id"})
    @TableField(value = "user_id_wsh")
    private Long user_id_wsh;

    @JsonProperty("name_wsh")
    @JsonAlias({"name"})
    @TableField(value = "name_wsh")
    private String name_wsh;

    @JsonProperty("phone_wsh")
    @JsonAlias({"phone"})
    @TableField(value = "phone_wsh")
    private String phone_wsh;

    @JsonProperty("avatar_wsh")
    @JsonAlias({"avatar"})
    @TableField(value = "avatar_wsh")
    private String avatar_wsh;

    @JsonProperty("experience_years_wsh")
    @JsonAlias({"experienceYears", "experience_years"})
    @TableField(value = "experience_years_wsh")
    private Integer experience_years_wsh;

    @JsonProperty("rating_wsh")
    @TableField(value = "rating_wsh")
    private BigDecimal rating_wsh;

    @JsonProperty("completion_rate_wsh")
    @TableField(value = "completion_rate_wsh")
    private BigDecimal completion_rate_wsh;

    @JsonProperty("complaint_rate_wsh")
    @TableField(value = "complaint_rate_wsh")
    private BigDecimal complaint_rate_wsh;

    @JsonProperty("price_per_day_wsh")
    @JsonAlias({"pricePerDay", "price_per_day"})
    @TableField(value = "price_per_day_wsh")
    private BigDecimal price_per_day_wsh;

    @JsonProperty("max_pets_wsh")
    @JsonAlias({"maxPets", "max_pets"})
    @TableField(value = "max_pets_wsh")
    private Integer max_pets_wsh;

    @JsonProperty("current_pets_wsh")
    @JsonAlias({"currentPets", "current_pets"})
    @TableField(value = "current_pets_wsh")
    private Integer current_pets_wsh;

    @JsonProperty("bio_wsh")
    @JsonAlias({"bio"})
    @TableField(value = "bio_wsh")
    private String bio_wsh;

    @JsonProperty("status_wsh")
    @JsonAlias({"status"})
    @TableField(value = "status_wsh")
    private Integer status_wsh;

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
