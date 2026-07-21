package com.pet.boarding.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
public class MerchantUpdateRequestDTO {
    @Schema(description = "商家名称")
    private String name_wsh;

    @Schema(description = "手机号")
    private String phone_wsh;

    @Schema(description = "地址")
    private String address_wsh;

    @Schema(description = "纬度")
    private BigDecimal latitude_wsh;

    @Schema(description = "经度")
    private BigDecimal longitude_wsh;

    @Schema(description = "描述")
    private String description_wsh;
}
