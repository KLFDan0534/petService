package com.pet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RagAskRequestDTO {
    @Schema(description = "用户问题")
    @NotBlank(message = "问题不能为空")
    private String question_wsh;
}
