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
 * 看护者（Keeper）实体，映射 keeper_wsh 表。
 * <p>
 * 看护者是实际提供宠物寄养服务的个体，隶属于某个商家（Merchant）。
 * 每个看护者与一个用户账号关联，系统通过角色（KEEPER）控制操作权限。
 * <p>
 * <b>状态流转：</b>
 * <ul>
 *   <li>入驻申请：KEEPER_PENDING（待审核）→ KEEPER_ACTIVE（已通过）/ KEEPER_REJECTED（已驳回）</li>
 *   <li>在职状态：KEEPER_ACTIVE（在线）↔ KEEPER_OFFLINE（离线）| KEEPER_BUSY（忙碌）</li>
 *   <li>离职状态：KEEPER_RESIGNED（辞职）/ KEEPER_TERMINATED（解雇）</li>
 * </ul>
 *
 * @author: wsh
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

    /**
     * 获取看护者 ID，JSON 序列化时输出为 "id"。
     *
     * @return 看护者 ID
     */
    @JsonGetter("id")
    public Long getId() {
        return id_wsh;
    }

    /**
     * 获取所属商家 ID，JSON 序列化时输出为 "merchantId"。
     *
     * @return 商家 ID
     */
    @JsonGetter("merchantId")
    public Long getMerchantId() {
        return merchant_id_wsh;
    }

    /**
     * 获取关联的用户 ID，JSON 序列化时输出为 "userId"。
     *
     * @return 用户 ID
     */
    @JsonGetter("userId")
    public Long getUserId() {
        return user_id_wsh;
    }

    /**
     * 获取看护者姓名，JSON 序列化时输出为 "name"。
     *
     * @return 姓名
     */
    @JsonGetter("name")
    public String getName() {
        return name_wsh;
    }

    /**
     * 获取看护者联系电话，JSON 序列化时输出为 "phone"。
     *
     * @return 联系电话
     */
    @JsonGetter("phone")
    public String getPhone() {
        return phone_wsh;
    }

    /**
     * 获取看护者头像 URL，JSON 序列化时输出为 "avatar"。
     *
     * @return 头像 URL
     */
    @JsonGetter("avatar")
    public String getAvatar() {
        return avatar_wsh;
    }

    /**
     * 获取从业经验年数，JSON 序列化时输出为 "experienceYears"。
     *
     * @return 经验年数
     */
    @JsonGetter("experienceYears")
    public Integer getExperienceYears() {
        return experience_years_wsh;
    }

    /**
     * 获取评分，JSON 序列化时输出为 "rating"。
     *
     * @return 评分（例如 5.0）
     */
    @JsonGetter("rating")
    public BigDecimal getRating() {
        return rating_wsh;
    }

    /**
     * 获取订单完成率，JSON 序列化时输出为 "completionRate"。
     *
     * @return 完成率（百分比或小数）
     */
    @JsonGetter("completionRate")
    public BigDecimal getCompletionRate() {
        return completion_rate_wsh;
    }

    /**
     * 获取投诉率，JSON 序列化时输出为 "complaintRate"。
     *
     * @return 投诉率（百分比或小数）
     */
    @JsonGetter("complaintRate")
    public BigDecimal getComplaintRate() {
        return complaint_rate_wsh;
    }

    /**
     * 获取每日服务价格，JSON 序列化时输出为 "pricePerDay"。
     *
     * @return 每日价格
     */
    @JsonGetter("pricePerDay")
    public BigDecimal getPricePerDay() {
        return price_per_day_wsh;
    }

    /**
     * 获取最大可接待宠物数量，JSON 序列化时输出为 "maxPets"。
     *
     * @return 最大宠物数
     */
    @JsonGetter("maxPets")
    public Integer getMaxPets() {
        return max_pets_wsh;
    }

    /**
     * 获取当前已接待的宠物数量，JSON 序列化时输出为 "currentPets"。
     *
     * @return 当前宠物数
     */
    @JsonGetter("currentPets")
    public Integer getCurrentPets() {
        return current_pets_wsh;
    }

    /**
     * 获取个人简介，JSON 序列化时输出为 "bio"。
     *
     * @return 个人简介文本
     */
    @JsonGetter("bio")
    public String getBio() {
        return bio_wsh;
    }

    /**
     * 获取看护者状态，JSON 序列化时输出为 "status"。
     * <p>
     * 状态值参考 {@link com.pet.common.StatusCode} 中的 KEEPER_* 常量。
     *
     * @return 状态码
     */
    @JsonGetter("status")
    public Integer getStatus() {
        return status_wsh;
    }
}
