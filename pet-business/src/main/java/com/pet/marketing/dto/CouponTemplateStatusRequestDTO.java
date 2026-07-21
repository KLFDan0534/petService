package com.pet.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CouponTemplateStatusRequestDTO {
    @Schema(description = "状态")
    private Integer status_wsh;
}
