package com.pet.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AskResult {
    @Schema(description = "问题")
    private String question;
    @Schema(description = "答案")
    private String answer;
    @Schema(description = "回复")
        private String reply_wsh;

    public AskResult() {}
    public AskResult(String question, String answer) {
        this.question = question;
        this.answer = answer;
        this.reply_wsh = answer;
    }
}
