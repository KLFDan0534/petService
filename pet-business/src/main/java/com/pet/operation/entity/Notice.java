package com.pet.operation.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("notice_wsh")
public class Notice {
    @JsonProperty("id_wsh")
    @TableId(type = IdType.AUTO, value = "id_wsh")
    private Long id_wsh;

    @JsonProperty("title_wsh")
    @JsonAlias("title")
    @TableField(value = "title_wsh")
    private String title_wsh;

    @JsonProperty("content_wsh")
    @JsonAlias("content")
    @TableField(value = "content_wsh")
    private String content_wsh;

    @JsonProperty("type_wsh")
    @JsonAlias("type")
    @TableField(value = "type_wsh")
    private String type_wsh;

    @JsonProperty("image_url_wsh")
    @JsonAlias({"imageUrl", "image_url"})
    @TableField(value = "image_url_wsh")
    private String image_url_wsh;

    @JsonProperty("link_url_wsh")
    @JsonAlias({"linkUrl", "link_url"})
    @TableField(value = "link_url_wsh")
    private String link_url_wsh;

    @JsonProperty("sort_order_wsh")
    @JsonAlias({"sortOrder", "sort_order"})
    @TableField(value = "sort_order_wsh")
    private Integer sort_order_wsh;

    @JsonProperty("status_wsh")
    @JsonAlias("status")
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
