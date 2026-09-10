package com.pet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgentChatResponseDTO {
    @Schema(description = "回答内容")
    private String reply;
    @Schema(description = "对话ID")
    private String conversationId;
    @Schema(description = "结束原因")
    private String finishReason;
}
