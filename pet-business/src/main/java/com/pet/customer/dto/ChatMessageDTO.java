package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonGetter;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageDTO {
    @Schema(description = "消息ID")
    private Long id_wsh;
    @Schema(description = "发送者用户ID")
    private Long from_user_id_wsh;
    @Schema(description = "接收者用户ID")
    private Long to_user_id_wsh;
    @Schema(description = "订单ID")
    private Long order_id_wsh;
    @Schema(description = "消息内容")
    private String content_wsh;
    @Schema(description = "消息类型")
    private String type_wsh;
    @Schema(description = "文件URL")
    private String file_url_wsh;
    @Schema(description = "是否已读")
    private Integer read_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @JsonGetter("id")
    public Long getId() {
        return id_wsh;
    }

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

    @JsonGetter("content")
    public String getContent() {
        return content_wsh;
    }

    @JsonGetter("type")
    public String getType() {
        return type_wsh;
    }

    @JsonGetter("fileUrl")
    public String getFileUrl() {
        return file_url_wsh;
    }

    @JsonGetter("read")
    public Integer getRead() {
        return read_wsh;
    }

    @JsonGetter("is_read_wsh")
    public Boolean getIsReadWsh() {
        return read_wsh != null && read_wsh == 1;
    }

    @JsonGetter("createdAt")
    public LocalDateTime getCreatedAt() {
        return created_at_wsh;
    }
}
