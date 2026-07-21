package com.pet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgentChatResponseDTO {
    @Schema(description = "鍥炵瓟鍐呭")
    private String reply;
    @Schema(description = "瀵硅瘽ID")
    private String conversationId;
    @Schema(description = "缁撴潫鍘熷洜")
    private String finishReason;
}
