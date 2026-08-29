package com.pet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class AiChatRequestDTO {

    @Schema(description = "用户消息")
    @NotBlank(message = "消息不能为空")
    private String message_wsh;

    @Schema(description = "会话ID（可选，前端传入以关联历史记录）")
    private String session_id_wsh;

    @Schema(description = "历史对话（可选，最多携带最近10条）")
    private List<ChatTurn> history_wsh;

    @Data
    public static class ChatTurn {
        @Schema(description = "角色: user/assistant")
        private String role;
        @Schema(description = "消息内容")
        private String content;
    }
}
