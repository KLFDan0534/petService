package com.pet.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RealNameReviewRequestDTO {
    @Schema(description = "审核备注")
    private String remark_wsh;
}
