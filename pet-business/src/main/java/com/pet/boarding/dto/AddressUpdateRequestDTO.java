package com.pet.boarding.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
public class AddressUpdateRequestDTO {
    @Schema(description = "地址标签")
    private String label_wsh;

    @Schema(description = "联系人姓名")
    private String name_wsh;

    @Schema(description = "联系电话")
    private String phone_wsh;

    @Schema(description = "地址")
    private String address_wsh;

    @Schema(description = "详细地址")
    private String detail_wsh;

    @Schema(description = "纬度")
    private BigDecimal latitude_wsh;

    @Schema(description = "经度")
    private BigDecimal longitude_wsh;

    @Schema(description = "是否默认")
    private Integer is_default_wsh;
}
