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
 * 商家（Merchant）实体，映射 merchant_wsh 表。
 * <p>
 * 存储宠物寄养商家的基本信息、地理位置、审核状态、店铺营业模式与实时营业状态。
 * 商家是整个寄养服务平台的核心主体，旗下管理看护者（Keeper）和服务项目（ServiceItem）。
 * <p>
 * <b>状态流转：</b>
 * <ul>
 *   <li>MERCHANT_PENDING（待审核）→ MERCHANT_APPROVED（已通过）/ MERCHANT_REJECTED（已驳回）</li>
 *   <li>店铺营业模式：AUTO（按时间自动开关）| MANUAL_OPEN（手动开门）| MANUAL_CLOSED（手动关店）</li>
 *   <li>店铺状态：由 {@code com.pet.boarding.service.impl.MerchantServiceImpl#resolveStoreStatus} 实时计算</li>
 * </ul>
 *
 * @author: wsh
 */
@Getter
@Setter
@TableName("merchant_wsh")
@Schema(description = "商家实体")
public class Merchant {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @JsonProperty("user_id_wsh")
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

    @JsonProperty("address_wsh")
    @JsonAlias({"address"})
    @TableField(value = "address_wsh")
    @Schema(description = "地址")
    private String address_wsh;

    @JsonProperty("latitude_wsh")
    @JsonAlias({"latitude", "lat"})
    @TableField(value = "latitude_wsh")
    @Schema(description = "纬度")
    private BigDecimal latitude_wsh;

    @JsonProperty("longitude_wsh")
    @JsonAlias({"longitude", "lng"})
    @TableField(value = "longitude_wsh")
    @Schema(description = "经度")
    private BigDecimal longitude_wsh;

    @JsonProperty("description_wsh")
    @JsonAlias({"description"})
    @TableField(value = "description_wsh")
    @Schema(description = "描述")
    private String description_wsh;

    @JsonProperty("business_license_wsh")
    @JsonAlias({"businessLicense", "business_license"})
    @TableField(value = "business_license_wsh")
    @Schema(description = "营业执照URL")
    private String business_license_wsh;

    @JsonProperty("rating_wsh")
    @TableField(value = "rating_wsh")
    @Schema(description = "评分")
    private BigDecimal rating_wsh;

    @JsonProperty("status_wsh")
    @TableField(value = "status_wsh")
    @Schema(description = "状态")
    private Integer status_wsh;

    @JsonProperty("store_mode_wsh")
    @JsonAlias({"storeMode", "store_mode"})
    @TableField(value = "store_mode_wsh")
    @Schema(description = "店铺模式")
    private Integer store_mode_wsh;

@JsonProperty("store_status_wsh")
    @JsonAlias({"storeStatus", "store_status"})
    @TableField(value = "store_status_wsh")
    @Schema(description = "店铺状态")
    private Integer store_status_wsh;

    @JsonProperty("future_booking_enabled_wsh")
    @JsonAlias({"futureBookingEnabled", "future_booking_enabled"})
    @TableField(value = "future_booking_enabled_wsh")
    @Schema(description = "是否接受未来预约: 0-关闭 1-开启(默认开启)")
    private Integer future_booking_enabled_wsh;

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

    @JsonProperty("distance_wsh")
    @TableField(exist = false)
    @Schema(description = "距离(米)")
    private Double distance_wsh;

    /**
     * 获取商家 ID，JSON 序列化时输出为 "id"。
     *
     * @return 商家 ID
     */
    @JsonGetter("id")
    public Long getId() {
        return id_wsh;
    }

    /**
     * 获取商家所属用户 ID，JSON 序列化时输出为 "userId"。
     *
     * @return 用户 ID
     */
    @JsonGetter("userId")
    public Long getUserId() {
        return user_id_wsh;
    }

    /**
     * 获取商家名称，JSON 序列化时输出为 "name"。
     *
     * @return 商家名称
     */
    @JsonGetter("name")
    public String getName() {
        return name_wsh;
    }

    /**
     * 获取商家联系电话，JSON 序列化时输出为 "phone"。
     *
     * @return 联系电话
     */
    @JsonGetter("phone")
    public String getPhone() {
        return phone_wsh;
    }

    /**
     * 获取商家地址文本，JSON 序列化时输出为 "address"。
     *
     * @return 地址文本
     */
    @JsonGetter("address")
    public String getAddress() {
        return address_wsh;
    }

    /**
     * 获取商家纬度坐标，JSON 序列化时输出为 "latitude"。
     *
     * @return 纬度值
     */
    @JsonGetter("latitude")
    public BigDecimal getLatitude() {
        return latitude_wsh;
    }

    /**
     * 获取商家经度坐标，JSON 序列化时输出为 "longitude"。
     *
     * @return 经度值
     */
    @JsonGetter("longitude")
    public BigDecimal getLongitude() {
        return longitude_wsh;
    }

    /**
     * 获取商家描述信息，JSON 序列化时输出为 "description"。
     *
     * @return 描述文本
     */
    @JsonGetter("description")
    public String getDescription() {
        return description_wsh;
    }

    /**
     * 获取商家营业执照图片 URL，JSON 序列化时输出为 "businessLicense"。
     *
     * @return 营业执照图片 URL
     */
    @JsonGetter("businessLicense")
    public String getBusinessLicense() {
        return business_license_wsh;
    }

    /**
     * 获取商家审核状态，JSON 序列化时输出为 "status"。
     * <p>
     * 状态值参考 {@link com.pet.common.StatusCode} 中的 MERCHANT_* 常量。
     *
     * @return 审核状态码
     */
    @JsonGetter("status")
    public Integer getStatus() {
        return status_wsh;
    }

    /**
     * 获取与用户的距离（非持久化字段），JSON 序列化时输出为 "distance"。
     * <p>
     * 该字段仅在 {@link com.pet.boarding.mapper.MerchantMapper#searchNearby} 查询时动态计算填充，
     * 数据库表中不存在此字段。
     *
     * @return 距离（公里）
     */
    @JsonGetter("distance")
    public Double getDistance() {
        return distance_wsh;
    }
}
