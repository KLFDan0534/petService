package com.pet.operation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContentReviewReviewRequestDTO {
    @Schema(description = "审核备注")
    private String remark_wsh;
}
