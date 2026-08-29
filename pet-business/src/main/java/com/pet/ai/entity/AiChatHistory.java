package com.pet.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AI 智能客服聊天记录实体，映射 ai_chat_history_wsh 表。
 * <p>按用户+会话分组持久化每次问答，前端可加载历史对话。</p>
 */
@Getter
@Setter
@TableName("ai_chat_history_wsh")
@Schema(description = "AI 聊天记录实体")
public class AiChatHistory {

    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "记录ID")
    private Long id_wsh;

    @JsonProperty("user_id_wsh")
    @TableField(value = "user_id_wsh")
    @Schema(description = "用户ID")
    private Long user_id_wsh;

    @JsonProperty("session_id_wsh")
    @TableField(value = "session_id_wsh")
    @Schema(description = "会话ID (UUID)")
    private String session_id_wsh;

    @JsonProperty("role_wsh")
    @TableField(value = "role_wsh")
    @Schema(description = "角色: user / assistant")
    private String role_wsh;

    @JsonProperty("content_wsh")
    @TableField(value = "content_wsh")
    @Schema(description = "消息内容")
    private String content_wsh;

    @JsonProperty("sources_wsh")
    @TableField(value = "sources_wsh")
    @Schema(description = "参考文档标题，逗号分隔")
    private String sources_wsh;

    @JsonProperty("need_human_wsh")
    @TableField(value = "need_human_wsh")
    @Schema(description = "是否需要转人工")
    private Boolean need_human_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    @Schema(description = "逻辑删除")
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}
