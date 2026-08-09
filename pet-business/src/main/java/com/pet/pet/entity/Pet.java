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
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @JsonProperty("owner_id_wsh")
    @JsonAlias({"ownerId", "owner_id"})
    @TableField(value = "owner_id_wsh")
    @Schema(description = "宠物主用户ID")
    private Long owner_id_wsh;

    @JsonProperty("name_wsh")
    @JsonAlias({"name"})
    @NotBlank(message = "�1�7�1�7�1�7�1�7�1�7�1�7�1�7�0�8�1�7�1�7�1�7�0�2�1�7�1�7")
    @TableField(value = "name_wsh")
    @Schema(description = "名称")
    private String name_wsh;

    @JsonProperty("type_wsh")
    @JsonAlias({"type"})
    @NotBlank(message = "�1�7�1�7�1�7�1�7类�1�7�0�0�1�7�1�7�1�7�0�2�1�7�1�7")
    @TableField(value = "type_wsh")
    @Schema(description = "类型")
    private String type_wsh;

    @JsonProperty("breed_wsh")
    @JsonAlias({"breed"})
    @TableField(value = "breed_wsh")
    @Schema(description = "品种")
    private String breed_wsh;

    @JsonProperty("age_wsh")
    @JsonAlias({"age"})
    @Positive(message = "�1�7�1�7龄�1�7�1�7�1�7�1�7�0�2�1�7�1�7�1�7�1�7")
    @TableField(value = "age_wsh")
    @Schema(description = "年龄")
    private Integer age_wsh;

    @JsonProperty("weight_wsh")
    @JsonAlias({"weight"})
    @Positive(message = "�1�7�1�7�1�7�1�9�1�7�1�7�1�7�0�2�1�7�1�7�1�7�1�7")
    @TableField(value = "weight_wsh")
    @Schema(description = "体重(kg)")
    private BigDecimal weight_wsh;

    @JsonProperty("gender_wsh")
    @JsonAlias({"gender"})
    @TableField(value = "gender_wsh")
    @Schema(description = "性别")
    private Integer gender_wsh;

    @JsonProperty("sterilized_wsh")
    @JsonAlias({"sterilized"})
    @TableField(value = "sterilized_wsh")
    @Schema(description = "是否绝育")
    private Integer sterilized_wsh;

    @JsonProperty("vaccinated_wsh")
    @JsonAlias({"vaccinated"})
    @TableField(value = "vaccinated_wsh")
    @Schema(description = "是否已接种疫苗")
    private Integer vaccinated_wsh;

    @JsonProperty("avatar_wsh")
    @JsonAlias({"avatar"})
    @TableField(value = "avatar_wsh")
    @Schema(description = "头像URL")
    private String avatar_wsh;

    @JsonProperty("description_wsh")
    @JsonAlias({"description"})
    @TableField(value = "description_wsh")
    @Schema(description = "描述")
    private String description_wsh;

    @JsonProperty("allergies_wsh")
    @JsonAlias({"allergies"})
    @TableField(value = "allergies_wsh")
    @Schema(description = "过敏信息")
    private String allergies_wsh;

    @JsonProperty("habits_wsh")
    @JsonAlias({"habits"})
    @TableField(value = "habits_wsh")
    @Schema(description = "生活习惯")
    private String habits_wsh;

    @JsonProperty("owner_name_wsh")
    @TableField(exist = false)
    @Schema(description = "宠物主姓名")
    private String owner_name_wsh;

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

    /**
     * 获取宠物 ID（JSON 别名 "id"）。
     *
     * @return 宠物 ID
     */
    @JsonGetter("id")
    public Long getId() {
        return id_wsh;
    }

    /**
     * 获取宠物主人用户 ID（JSON 别名 "ownerId"）。
     *
     * @return 主人用户 ID
     */
    @JsonGetter("ownerId")
    public Long getOwnerId() {
        return owner_id_wsh;
    }

    /**
     * 获取宠物名称（JSON 别名 "name"）。
     *
     * @return 宠物名称
     */
    @JsonGetter("name")
    public String getName() {
        return name_wsh;
    }

    /**
     * 获取宠物类型（狗/猫/其他）（JSON 别名 "type"）。
     *
     * @return 宠物类型
     */
    @JsonGetter("type")
    public String getType() {
        return type_wsh;
    }

    /**
     * 获取宠物品种（JSON 别名 "breed"）。
     *
     * @return 品种
     */
    @JsonGetter("breed")
    public String getBreed() {
        return breed_wsh;
    }

    /**
     * 获取宠物年龄（JSON 别名 "age"）。
     *
     * @return 年龄
     */
    @JsonGetter("age")
    public Integer getAge() {
        return age_wsh;
    }

    /**
     * 获取宠物体重（kg）（JSON 别名 "weight"）。
     *
     * @return 体重
     */
    @JsonGetter("weight")
    public BigDecimal getWeight() {
        return weight_wsh;
    }

    /**
     * 获取宠物性别（JSON 别名 "gender"）。
     *
     * @return 性别编码
     */
    @JsonGetter("gender")
    public Integer getGender() {
        return gender_wsh;
    }

    /**
     * 获取绝育状态（JSON 别名 "sterilized"）。
     *
     * @return 绝育状态（1 是 / 0 否）
     */
    @JsonGetter("sterilized")
    public Integer getSterilized() {
        return sterilized_wsh;
    }

    /**
     * 获取疫苗接种状态（JSON 别名 "vaccinated"）。
     *
     * @return 疫苗接种状态（1 是 / 0 否）
     */
    @JsonGetter("vaccinated")
    public Integer getVaccinated() {
        return vaccinated_wsh;
    }

    /**
     * 获取宠物头像 URL（JSON 别名 "avatar"）。
     *
     * @return 头像 URL
     */
    @JsonGetter("avatar")
    public String getAvatar() {
        return avatar_wsh;
    }

    /**
     * 获取宠物描述（JSON 别名 "description"）。
     *
     * @return 描述文本
     */
    @JsonGetter("description")
    public String getDescription() {
        return description_wsh;
    }

    /**
     * 获取宠物过敏信息（JSON 别名 "allergies"）。
     *
     * @return 过敏信息
     */
    @JsonGetter("allergies")
    public String getAllergies() {
        return allergies_wsh;
    }

    /**
     * 获取宠物生活习惯（JSON 别名 "habits"）。
     *
     * @return 生活习惯
     */
    @JsonGetter("habits")
    public String getHabits() {
        return habits_wsh;
    }

    /**
     * 获取宠物主人姓名（JSON 别名 "ownerName"）。
     * 此字段为冗余展示字段，非数据库列。
     *
     * @return 主人姓名
     */
    @JsonGetter("ownerName")
    public String getOwnerName() {
        return owner_name_wsh;
    }
}
