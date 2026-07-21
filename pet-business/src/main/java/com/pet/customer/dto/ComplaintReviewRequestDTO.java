package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ComplaintReviewRequestDTO {
    @Schema(description = "处理结果")
    private String result_wsh;
}
