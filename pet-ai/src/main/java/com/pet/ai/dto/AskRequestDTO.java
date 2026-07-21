package com.pet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AskRequestDTO {

    @NotBlank(message = "问题不能为空")
    @Schema(description = "问题")
    private String question_wsh;

    @Schema(description = "宠物档案")
    private String pet_profile_wsh;
}
