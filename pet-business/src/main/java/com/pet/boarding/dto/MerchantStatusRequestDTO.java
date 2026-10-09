package com.pet.boarding.dto;

import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
public class MerchantStatusRequestDTO {
    @Schema(description = "商家审核状态：0待审核 1已通过 2已拒绝")
    private Integer status_wsh;
}
