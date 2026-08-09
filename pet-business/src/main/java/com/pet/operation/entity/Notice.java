package com.pet.operation.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 系统公告实体，支持普通公告（notice）和其他自定义类型。
 * <p>
 * 普通公告支持多种投递方式（弹窗 popup / 通知 notification / 广播 broadcast），
 * 通过 delivery_type_wsh 字段以逗号分隔存储。可设置排序权重、图片和链接。
 */
@Getter
@Setter
@TableName("notice_wsh")
@Schema(description = "公告实体")
public class Notice {
    @JsonProperty("id_wsh")
    @TableId(type = IdType.AUTO, value = "id_wsh")
    @Schema(description = "ID")
    private Long id_wsh;

    @JsonProperty("title_wsh")
    @JsonAlias("title")
    @TableField(value = "title_wsh")
    @Schema(description = "标题")
    private String title_wsh;

    @JsonProperty("content_wsh")
    @JsonAlias("content")
    @TableField(value = "content_wsh")
    @Schema(description = "内容")
    private String content_wsh;

    @JsonProperty("type_wsh")
    @JsonAlias("type")
    @TableField(value = "type_wsh")
    @Schema(description = "类型")
    private String type_wsh;

    @JsonProperty("delivery_type_wsh")
    @JsonAlias({"deliveryType", "delivery_type"})
    @TableField(value = "delivery_type_wsh")
    @Schema(description = "投递方式")
    private String delivery_type_wsh;

    @JsonProperty("image_url_wsh")
    @JsonAlias({"imageUrl", "image_url"})
    @TableField(value = "image_url_wsh")
    @Schema(description = "图片URL")
    private String image_url_wsh;

    @JsonProperty("link_url_wsh")
    @JsonAlias({"linkUrl", "link_url"})
    @TableField(value = "link_url_wsh")
    @Schema(description = "链接URL")
    private String link_url_wsh;

    @JsonProperty("sort_order_wsh")
    @JsonAlias({"sortOrder", "sort_order"})
    @TableField(value = "sort_order_wsh")
    @Schema(description = "排序")
    private Integer sort_order_wsh;

    @JsonProperty("status_wsh")
    @JsonAlias("status")
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

    @JsonProperty("id")
    public Long getId() {
        return id_wsh;
    }

    @JsonProperty("title")
    public String getTitle() {
        return title_wsh;
    }

    @JsonProperty("content")
    public String getContent() {
        return content_wsh;
    }

    @JsonProperty("type")
    public String getType() {
        return type_wsh;
    }

    @JsonProperty("imageUrl")
    public String getImageUrl() {
        return image_url_wsh;
    }

    @JsonProperty("linkUrl")
    public String getLinkUrl() {
        return link_url_wsh;
    }

    @JsonProperty("sortOrder")
    public Integer getSortOrder() {
        return sort_order_wsh;
    }

    @JsonProperty("status")
    public Integer getStatus() {
        return status_wsh;
    }

    @JsonProperty("createdAt")
    public LocalDateTime getCreatedAt() {
        return created_at_wsh;
    }

    @JsonProperty("updatedAt")
    public LocalDateTime getUpdatedAt() {
        return updated_at_wsh;
    }
}
