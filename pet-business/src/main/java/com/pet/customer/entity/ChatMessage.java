package com.pet.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@TableName("chat_message_wsh")
@Schema(description = "聊天消息实体")
public class ChatMessage {
    @JsonProperty("id_wsh")
    @TableId(type = IdType.AUTO, value = "id_wsh")
    @Schema(description = "ID")
    private Long id_wsh;

    @JsonProperty("from_user_id_wsh")
    @JsonAlias({"fromUserId", "from_user_id"})
    @TableField(value = "from_user_id_wsh")
    @Schema(description = "发送方用户ID")
    private Long from_user_id_wsh;

    @JsonProperty("to_user_id_wsh")
    @JsonAlias({"toUserId", "to_user_id"})
    @TableField(value = "to_user_id_wsh")
    @Schema(description = "接收方用户ID")
    private Long to_user_id_wsh;

    @JsonProperty("order_id_wsh")
    @JsonAlias({"orderId", "order_id"})
    @TableField(value = "order_id_wsh")
    @Schema(description = "订单ID")
    private Long order_id_wsh;

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

    @JsonProperty("file_url_wsh")
    @JsonAlias({"fileUrl", "file_url"})
    @TableField(value = "file_url_wsh")
    @Schema(description = "文件URL")
    private String file_url_wsh;

    @JsonProperty("read_wsh")
    @JsonAlias("read")
    @TableField(value = "`read_wsh`")
    @Schema(description = "是否已读")
    private Integer read_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @JsonGetter("fromUserId")
    public Long getFromUserId() {
        return from_user_id_wsh;
    }

    @JsonGetter("toUserId")
    public Long getToUserId() {
        return to_user_id_wsh;
    }

    @JsonGetter("orderId")
    public Long getOrderId() {
        return order_id_wsh;
    }

    @JsonGetter("fileUrl")
    public String getFileUrl() {
        return file_url_wsh;
    }

    @JsonGetter("read")
    public Integer getRead() {
        return read_wsh;
    }
}
