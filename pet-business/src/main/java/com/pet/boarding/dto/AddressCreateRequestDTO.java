package com.pet.boarding.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
public class AddressCreateRequestDTO {
    @Schema(description = "地址标签")
    @NotBlank
    private String label_wsh;

    @Schema(description = "联系人姓名")
    @NotBlank
    private String name_wsh;

    @Schema(description = "联系电话")
    @NotBlank
    private String phone_wsh;

    @Schema(description = "地址")
    @NotBlank
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
