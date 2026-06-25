package com.pet.customer.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("rating_wsh")
public class Rating {
    @JsonProperty("id_wsh")
    @TableId(type = IdType.AUTO, value = "id_wsh")
    private Long id_wsh;

    @JsonProperty("order_id_wsh")
    @JsonAlias({"orderId", "order_id"})
    @TableField(value = "order_id_wsh")
    private Long order_id_wsh;

    @JsonProperty("user_id_wsh")
    @TableField(value = "user_id_wsh")
    private Long user_id_wsh;

    @JsonProperty("target_id_wsh")
    @JsonAlias({"targetId", "target_id"})
    @TableField(value = "target_id_wsh")
    private Long target_id_wsh;

    @JsonProperty("target_type_wsh")
    @JsonAlias({"targetType", "target_type"})
    @TableField(value = "target_type_wsh")
    private String target_type_wsh;

    @JsonProperty("score_wsh")
    @JsonAlias({"score"})
    @TableField(value = "score_wsh")
    private Integer score_wsh;

    @JsonProperty("content_wsh")
    @JsonAlias({"content"})
    @TableField(value = "content_wsh")
    private String content_wsh;

    @JsonProperty("images_wsh")
    @JsonAlias({"images", "imageUrls", "image_urls"})
    @TableField(value = "images_wsh")
    private String images_wsh;

    @JsonProperty("reply_wsh")
    @JsonAlias({"reply"})
    @TableField(value = "reply_wsh")
    private String reply_wsh;

    @JsonProperty("reply_at_wsh")
    @JsonAlias({"replyAt", "reply_at"})
    @TableField(value = "reply_at_wsh")
    private LocalDateTime reply_at_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    private LocalDateTime created_at_wsh;

    @JsonGetter("id")
    public Long getId() {
        return id_wsh;
    }

    @JsonGetter("orderId")
    public Long getOrderId() {
        return order_id_wsh;
    }

    @JsonGetter("userId")
    public Long getUserId() {
        return user_id_wsh;
    }

    @JsonGetter("targetId")
    public Long getTargetId() {
        return target_id_wsh;
    }

    @JsonGetter("targetType")
    public String getTargetType() {
        return target_type_wsh;
    }

    @JsonGetter("score")
    public Integer getScore() {
        return score_wsh;
    }

    @JsonGetter("content")
    public String getContent() {
        return content_wsh;
    }

    @JsonGetter("images")
    public String getImages() {
        return images_wsh;
    }

    @JsonGetter("reply")
    public String getReply() {
        return reply_wsh;
    }

    @JsonGetter("replyAt")
    public LocalDateTime getReplyAt() {
        return reply_at_wsh;
    }

    @JsonGetter("createdAt")
    public LocalDateTime getCreatedAt() {
        return created_at_wsh;
    }
}
