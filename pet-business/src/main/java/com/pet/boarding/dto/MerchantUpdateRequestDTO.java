package com.pet.boarding.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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

    @Schema(description = "是否接受未来预约: 0-关闭 1-开启(仅商家本人或管理员可修改)")
    @JsonAlias("futureBookingEnabled")
    private Integer future_booking_enabled_wsh;
}
