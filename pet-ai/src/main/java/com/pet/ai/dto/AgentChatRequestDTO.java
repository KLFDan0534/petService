package com.pet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AgentChatRequestDTO {

    @NotBlank(message = "娑堟伅涓嶈兘涓虹┖")
    @Schema(description = "鐢ㄦ埛杈撳叆鐨勬秷鎭?")
        private String question_wsh;

    @Schema(description = "鍘嗗彶璁板綍鍒楄〃")
    private List<Map<String, String>> history_wsh;
}
