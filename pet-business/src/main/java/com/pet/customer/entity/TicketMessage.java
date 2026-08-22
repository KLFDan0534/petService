package com.pet.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 工单消息实体，记录工单处理过程中用户与处理人之间的留言交流。
 */
@Getter
@Setter
@TableName("ticket_message_wsh")
@Schema(description = "工单消息实体")
public class TicketMessage {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @JsonProperty("ticket_id_wsh")
    @TableField(value = "ticket_id_wsh")
    @Schema(description = "工单ID")
    private Long ticket_id_wsh;

    @JsonProperty("user_id_wsh")
    @TableField(value = "user_id_wsh")
    @Schema(description = "用户ID")
    private Long user_id_wsh;

    @JsonProperty("content_wsh")
    @TableField(value = "content_wsh")
    @Schema(description = "内容")
    private String content_wsh;

    @JsonProperty("file_url_wsh")
    @TableField(value = "file_url_wsh")
    @Schema(description = "图片附件URL")
    private String file_url_wsh;

    @JsonProperty("is_read_wsh")
    @TableField(value = "is_read_wsh")
    @Schema(description = "是否已读")
    private Integer is_read_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}
