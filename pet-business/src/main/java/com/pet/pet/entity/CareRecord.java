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
 * 护理记录实体，映射数据库表 care_record_wsh。
 * 记录宠物在寄养服务过程中的护理操作（喂养、遛狗、喂药等）。
 * 关联订单、宠物和看护者，包含护理类型、内容描述和图片。
 * 是后续生成护理报告和结算的重要依据。
 *
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

    /**
     * 获取图片 URL 列表（JSON 别名 "images"），多个图片以逗号分隔。
     *
     * @return 图片 URL 字符串
     */
    @JsonGetter("images")
    public String getImages() {
        return images_wsh;
    }

    /**
     * 获取护理记录 ID（JSON 别名 "id"）。
     *
     * @return 记录 ID
     */
    @JsonGetter("id")
    public Long getId() {
        return id_wsh;
    }

    /**
     * 获取关联订单 ID（JSON 别名 "orderId"）。
     *
     * @return 订单 ID
     */
    @JsonGetter("orderId")
    public Long getOrderId() {
        return order_id_wsh;
    }

    /**
     * 获取关联宠物 ID（JSON 别名 "petId"）。
     *
     * @return 宠物 ID
     */
    @JsonGetter("petId")
    public Long getPetId() {
        return pet_id_wsh;
    }

    /**
     * 获取执行看护者 ID（JSON 别名 "keeperId"）。
     *
     * @return 看护者 ID
     */
    @JsonGetter("keeperId")
    public Long getKeeperId() {
        return keeper_id_wsh;
    }

    /**
     * 获取护理类型（喂食/遛狗/喂药等）（JSON 别名 "type"）。
     *
     * @return 护理类型
     */
    @JsonGetter("type")
    public String getType() {
        return type_wsh;
    }

    /**
     * 获取护理内容描述（JSON 别名 "content"）。
     *
     * @return 详细内容
     */
    @JsonGetter("content")
    public String getContent() {
        return content_wsh;
    }

    /**
     * 获取护理发生时间（JSON 别名 "recordTime"）。
     *
     * @return 护理时间
     */
    @JsonGetter("recordTime")
    public LocalDateTime getRecordTime() {
        return record_time_wsh;
    }
}
