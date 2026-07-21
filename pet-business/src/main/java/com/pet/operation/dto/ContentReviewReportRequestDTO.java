package com.pet.operation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContentReviewReportRequestDTO {
    @Schema(description = "目标类型")
    @NotBlank(message = "目标类型不能为空")
    private String target_type_wsh;

    @Schema(description = "目标ID")
    @NotNull(message = "目标ID不能为空")
    private Long target_id_wsh;

    @Schema(description = "举报原因")
    @NotBlank(message = "举报原因不能为空")
    private String reason_wsh;
}
