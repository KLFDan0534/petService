package com.pet.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 聊天消息实体，记录用户之间或用户与商家/照看者之间的即时通讯消息。
 * <p>支持文本、图片、视频、文件四种消息类型，包含已读未读标记和关联订单维度。</p>
 */
@Getter
@Setter
@TableName("chat_message_wsh")
@Schema(description = "聊天消息实体")
public class ChatMessage {
    @TableId(type = IdType.AUTO, value = "id_wsh")
    @Schema(description = "ID")
    private Long id_wsh;

    @TableField(value = "from_user_id_wsh")
    @Schema(description = "发送方用户ID")
    private Long from_user_id_wsh;

    @TableField(value = "to_user_id_wsh")
    @Schema(description = "接收方用户ID")
    private Long to_user_id_wsh;

    @TableField(value = "order_id_wsh")
    @Schema(description = "订单ID")
    private Long order_id_wsh;

    @TableField(value = "content_wsh")
    @Schema(description = "内容")
    private String content_wsh;

    @TableField(value = "type_wsh")
    @Schema(description = "类型")
    private String type_wsh;

    @TableField(value = "file_url_wsh")
    @Schema(description = "文件URL")
    private String file_url_wsh;

    @TableField(value = "`read_wsh`")
    @Schema(description = "是否已读")
    private Integer read_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}
