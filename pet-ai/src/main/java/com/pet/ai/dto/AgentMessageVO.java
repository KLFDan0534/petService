package com.pet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 会话中的一条消息（用于刷新页面时恢复聊天记录）。
 */
@Getter
@Setter
public class AgentMessageVO {

    @Schema(description = "user / assistant")
    private String role_wsh;

    @Schema(description = "消息内容")
    private String content_wsh;

    @Schema(description = "消息时间")
    private LocalDateTime created_at_wsh;
}
