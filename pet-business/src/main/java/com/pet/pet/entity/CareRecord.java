package com.pet.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 护理记录实体
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Getter
@Setter
@TableName("care_record_wsh")
@Schema(description = "护理记录实体")
public class CareRecord {
    @JsonProperty("id_wsh")
    @TableId(type = IdType.AUTO, value = "id_wsh")
    @Schema(description = "ID")
    private Long id_wsh;

    @JsonProperty("order_id_wsh")
    @JsonAlias({"orderId", "order_id"})
    @TableField(value = "order_id_wsh")
    @Schema(description = "订单ID")
    private Long order_id_wsh;

    @JsonProperty("pet_id_wsh")
    @JsonAlias({"petId", "pet_id"})
    @TableField(value = "pet_id_wsh")
    @Schema(description = "宠物ID")
    private Long pet_id_wsh;

    @JsonProperty("keeper_id_wsh")
    @JsonAlias({"keeperId", "keeper_id"})
    @TableField(value = "keeper_id_wsh")
    @Schema(description = "看护者ID")
    private Long keeper_id_wsh;

    @JsonProperty("type_wsh")
    @JsonAlias({"type", "record_type_wsh", "recordType"})
    @TableField(value = "type_wsh")
    @Schema(description = "类型")
    private String type_wsh;

    @JsonProperty("content_wsh")
    @JsonAlias("content")
    @TableField(value = "content_wsh")
    @Schema(description = "内容")
    private String content_wsh;

    @JsonProperty("images_wsh")
    @JsonAlias({"images", "image_urls_wsh", "imageUrls"})
    @TableField(value = "images_wsh")
    @Schema(description = "图片URL(逗号分隔)")
    private String images_wsh;

    @JsonProperty("record_time_wsh")
    @JsonAlias({"recordTime", "record_time", "happened_at_wsh", "happenedAt"})
    @TableField(value = "record_time_wsh")
    @Schema(description = "护理记录时间")
    private LocalDateTime record_time_wsh;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @JsonGetter("images")
    public String getImages() {
        return images_wsh;
    }

    @JsonGetter("id")
    public Long getId() {
        return id_wsh;
    }

    @JsonGetter("orderId")
    public Long getOrderId() {
        return order_id_wsh;
    }

    @JsonGetter("petId")
    public Long getPetId() {
        return pet_id_wsh;
    }

    @JsonGetter("keeperId")
    public Long getKeeperId() {
        return keeper_id_wsh;
    }

    @JsonGetter("type")
    public String getType() {
        return type_wsh;
    }

    @JsonGetter("content")
    public String getContent() {
        return content_wsh;
    }

    @JsonGetter("recordTime")
    public LocalDateTime getRecordTime() {
        return record_time_wsh;
    }
}
