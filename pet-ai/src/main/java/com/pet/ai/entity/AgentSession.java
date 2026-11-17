package com.pet.ai.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 智能下单会话状态实体，映射 agent_session_wsh 表。
 * <p>
 * 一行 = 一个「已经说过话」的会话。结构化状态全部收在 {@code state_json_wsh}，
 * 对话消息仍存在 {@code ai_chat_history_wsh}（两者按同一个 session_id 关联）。
 * <p>
 * 空会话不入库：本表的插入只发生在首条消息处理成功之后。
 */
@Getter
@Setter
@TableName("agent_session_wsh")
@Schema(description = "智能下单会话状态")
public class AgentSession {

    /** 会话ID：与 ai_chat_history_wsh.session_id_wsh 同值，由 AiChatHistoryService.newSessionId() 生成 */
    @TableId(value = "session_id_wsh", type = IdType.INPUT)
    @Schema(description = "会话ID")
    private String session_id_wsh;

    @TableField(value = "user_id_wsh")
    @Schema(description = "用户ID")
    private Long user_id_wsh;

    /** 首条用户输入截断，供历史会话列表直接展示，避免联查聊天表 */
    @TableField(value = "title_wsh")
    @Schema(description = "会话标题")
    private String title_wsh;

    /** AgentSessionState 的 JSON 序列化结果 */
    @TableField(value = "state_json_wsh")
    @Schema(description = "结构化会话状态 JSON")
    private String state_json_wsh;

    /** COLLECTING / AWAIT_CONFIRM / CONFIRMED，与 state 内的 status 同步冗余，用于 CAS 幂等 */
    @TableField(value = "status_wsh")
    @Schema(description = "会话状态")
    private String status_wsh;

    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @TableField(value = "updated_at_wsh")
    @Schema(description = "最后活动时间")
    private LocalDateTime updated_at_wsh;
}
