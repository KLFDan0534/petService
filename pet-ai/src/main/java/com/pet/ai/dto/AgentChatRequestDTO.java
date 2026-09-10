package com.pet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AgentChatRequestDTO {

    @NotBlank(message = "消息不能为空")
    @Schema(description = "用户输入的消息")
        private String question_wsh;

    @Schema(description = "历史记录列表")
    private List<Map<String, String>> history_wsh;
}
