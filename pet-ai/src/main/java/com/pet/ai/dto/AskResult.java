package com.pet.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AskResult {
    private String question;
    private String answer;
    @JsonProperty("reply_wsh")
    private String reply_wsh;

    public AskResult() {}
    public AskResult(String question, String answer) {
        this.question = question;
        this.answer = answer;
        this.reply_wsh = answer;
    }
}
