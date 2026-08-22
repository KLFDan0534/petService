package com.pet.customer.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 投诉沟通消息实体，映射 complaint_message_wsh 表。
 * <p>用于用户与客服在处理投诉过程中的往来消息。</p>
 */
@Getter
@Setter
@TableName("complaint_message_wsh")
@Schema(description = "投诉沟通消息实体")
public class ComplaintMessage {

    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @JsonProperty("complaint_id_wsh")
    @TableField(value = "complaint_id_wsh")
    @Schema(description = "投诉ID")
    private Long complaint_id_wsh;

    @JsonProperty("from_user_id_wsh")
    @TableField(value = "from_user_id_wsh")
    @Schema(description = "发送人ID")
    private Long from_user_id_wsh;

    @JsonProperty("to_user_id_wsh")
    @TableField(value = "to_user_id_wsh")
    @Schema(description = "接收人ID")
    private Long to_user_id_wsh;

    @JsonProperty("content_wsh")
    @TableField(value = "content_wsh")
    @Schema(description = "消息内容")
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
