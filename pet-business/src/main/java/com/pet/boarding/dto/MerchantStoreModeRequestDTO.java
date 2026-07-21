package com.pet.boarding.dto;

import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
public class MerchantStoreModeRequestDTO {
    @Schema(description = "店铺模式")
    private Integer store_mode_wsh;
}
