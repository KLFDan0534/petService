package com.pet.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 宠物实体
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Getter
@Setter
@TableName("pet_wsh")
public class Pet {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    private Long id_wsh;

    @JsonProperty("owner_id_wsh")
    @JsonAlias({"ownerId", "owner_id"})
    @TableField(value = "owner_id_wsh")
    private Long owner_id_wsh;

    @JsonProperty("name_wsh")
    @JsonAlias({"name"})
    @NotBlank(message = "�1�7�1�7�1�7�1�7�1�7�1�7�1�7�0�8�1�7�1�7�1�7�0�2�1�7�1�7")
    @TableField(value = "name_wsh")
    private String name_wsh;

    @JsonProperty("type_wsh")
    @JsonAlias({"type"})
    @NotBlank(message = "�1�7�1�7�1�7�1�7类�1�7�0�0�1�7�1�7�1�7�0�2�1�7�1�7")
    @TableField(value = "type_wsh")
    private String type_wsh;

    @JsonProperty("breed_wsh")
    @JsonAlias({"breed"})
    @TableField(value = "breed_wsh")
    private String breed_wsh;

    @JsonProperty("age_wsh")
    @JsonAlias({"age"})
    @Positive(message = "�1�7�1�7龄�1�7�1�7�1�7�1�7�0�2�1�7�1�7�1�7�1�7")
    @TableField(value = "age_wsh")
    private Integer age_wsh;

    @JsonProperty("weight_wsh")
    @JsonAlias({"weight"})
    @Positive(message = "�1�7�1�7�1�7�1�9�1�7�1�7�1�7�0�2�1�7�1�7�1�7�1�7")
    @TableField(value = "weight_wsh")
    private BigDecimal weight_wsh;

    @JsonProperty("gender_wsh")
    @JsonAlias({"gender"})
    @TableField(value = "gender_wsh")
    private Integer gender_wsh;

    @JsonProperty("sterilized_wsh")
    @JsonAlias({"sterilized"})
    @TableField(value = "sterilized_wsh")
    private Integer sterilized_wsh;

    @JsonProperty("vaccinated_wsh")
    @JsonAlias({"vaccinated"})
    @TableField(value = "vaccinated_wsh")
    private Integer vaccinated_wsh;

    @JsonProperty("avatar_wsh")
    @JsonAlias({"avatar"})
    @TableField(value = "avatar_wsh")
    private String avatar_wsh;

    @JsonProperty("description_wsh")
    @JsonAlias({"description"})
    @TableField(value = "description_wsh")
    private String description_wsh;

    @JsonProperty("allergies_wsh")
    @JsonAlias({"allergies"})
    @TableField(value = "allergies_wsh")
    private String allergies_wsh;

    @JsonProperty("habits_wsh")
    @JsonAlias({"habits"})
    @TableField(value = "habits_wsh")
    private String habits_wsh;

    @JsonProperty("owner_name_wsh")
    @TableField(exist = false)
    private String owner_name_wsh;

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

    @JsonGetter("ownerId")
    public Long getOwnerId() {
        return owner_id_wsh;
    }

    @JsonGetter("name")
    public String getName() {
        return name_wsh;
    }

    @JsonGetter("type")
    public String getType() {
        return type_wsh;
    }

    @JsonGetter("breed")
    public String getBreed() {
        return breed_wsh;
    }

    @JsonGetter("age")
    public Integer getAge() {
        return age_wsh;
    }

    @JsonGetter("weight")
    public BigDecimal getWeight() {
        return weight_wsh;
    }

    @JsonGetter("gender")
    public Integer getGender() {
        return gender_wsh;
    }

    @JsonGetter("sterilized")
    public Integer getSterilized() {
        return sterilized_wsh;
    }

    @JsonGetter("vaccinated")
    public Integer getVaccinated() {
        return vaccinated_wsh;
    }

    @JsonGetter("avatar")
    public String getAvatar() {
        return avatar_wsh;
    }

    @JsonGetter("description")
    public String getDescription() {
        return description_wsh;
    }

    @JsonGetter("allergies")
    public String getAllergies() {
        return allergies_wsh;
    }

    @JsonGetter("habits")
    public String getHabits() {
        return habits_wsh;
    }

    @JsonGetter("ownerName")
    public String getOwnerName() {
        return owner_name_wsh;
    }
}
