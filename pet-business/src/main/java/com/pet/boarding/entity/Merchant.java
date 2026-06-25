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
 * 商家实体
 * 映射数据库表 merchant_wsh，存储商家基本信息及地理位置
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Getter
@Setter
@TableName("merchant_wsh")
public class Merchant {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    private Long id_wsh;

    @JsonProperty("user_id_wsh")
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

    @JsonProperty("address_wsh")
    @JsonAlias({"address"})
    @TableField(value = "address_wsh")
    private String address_wsh;

    @JsonProperty("latitude_wsh")
    @JsonAlias({"latitude", "lat"})
    @TableField(value = "latitude_wsh")
    private BigDecimal latitude_wsh;

    @JsonProperty("longitude_wsh")
    @JsonAlias({"longitude", "lng"})
    @TableField(value = "longitude_wsh")
    private BigDecimal longitude_wsh;

    @JsonProperty("description_wsh")
    @JsonAlias({"description"})
    @TableField(value = "description_wsh")
    private String description_wsh;

    @JsonProperty("business_license_wsh")
    @JsonAlias({"businessLicense", "business_license"})
    @TableField(value = "business_license_wsh")
    private String business_license_wsh;

    @JsonProperty("rating_wsh")
    @TableField(value = "rating_wsh")
    private BigDecimal rating_wsh;

    @JsonProperty("status_wsh")
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

    @JsonProperty("distance_wsh")
    @TableField(exist = false)
    private Double distance_wsh;

    @JsonGetter("id")
    public Long getId() {
        return id_wsh;
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

    @JsonGetter("address")
    public String getAddress() {
        return address_wsh;
    }

    @JsonGetter("latitude")
    public BigDecimal getLatitude() {
        return latitude_wsh;
    }

    @JsonGetter("longitude")
    public BigDecimal getLongitude() {
        return longitude_wsh;
    }

    @JsonGetter("description")
    public String getDescription() {
        return description_wsh;
    }

    @JsonGetter("businessLicense")
    public String getBusinessLicense() {
        return business_license_wsh;
    }

    @JsonGetter("status")
    public Integer getStatus() {
        return status_wsh;
    }

    @JsonGetter("distance")
    public Double getDistance() {
        return distance_wsh;
    }
}
