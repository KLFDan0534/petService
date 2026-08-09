package com.pet.operation.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户通知实体，每个通知归属于单个用户。
 * <p>
 * 通知可由系统公告推送、订单状态变更等事件触发创建，
 * 创建后通过 SSE {@link NotificationBroadcaster} 实时推送给用户。
 */
@Getter
@Setter
@TableName("notification_wsh")
@Schema(description = "通知实体")
public class Notification {
    @JsonProperty("id_wsh")
    @TableId(type = IdType.AUTO, value = "id_wsh")
    @Schema(description = "ID")
    private Long id_wsh;

    @JsonProperty("user_id_wsh")
    @TableField(value = "user_id_wsh")
    @Schema(description = "用户ID")
    private Long user_id_wsh;

    @JsonProperty("title_wsh")
    @TableField(value = "title_wsh")
    @Schema(description = "标题")
    private String title_wsh;

    @JsonProperty("content_wsh")
    @TableField(value = "content_wsh")
    @Schema(description = "内容")
    private String content_wsh;

    @JsonProperty("type_wsh")
    @TableField(value = "type_wsh")
    @Schema(description = "类型")
    private String type_wsh;

    @JsonProperty("is_read_wsh")
    @TableField(value = "is_read_wsh")
    @Schema(description = "是否已读")
    private Integer is_read_wsh;

    @JsonProperty("related_id_wsh")
    @TableField(value = "related_id_wsh")
    @Schema(description = "关联ID")
    private Long related_id_wsh;

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
