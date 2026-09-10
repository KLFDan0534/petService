package com.pet.boarding.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
public class MerchantCreateRequestDTO {
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

    @Schema(description = "营业执照")
    private String business_license_wsh;

    @Schema(description = "资质图片")
    private String qualification_image_wsh;

    @Schema(description = "是否接受未来预约: 0-关闭 1-开启(缺省默认开启)")
    private Integer future_booking_enabled_wsh;
}
