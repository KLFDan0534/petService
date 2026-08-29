package com.pet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class AiChatResponseDTO {
    @Schema(description = "AI 回复内容")
    private String reply_wsh;

    @Schema(description = "是否需要转人工服务")
    private boolean need_human_wsh;

    @Schema(description = "命中的知识库文档标题")
    private List<String> sources_wsh;

    @Schema(description = "会话ID（前端用于关联历史记录）")
    private String session_id_wsh;
}
