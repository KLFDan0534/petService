package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderDeliveredRequestDTO {
    @Schema(description = "订单号")
    @NotBlank(message = "订单号不能为空")
    private String order_no_wsh;

    @Schema(description = "送达地址")
    private String delivered_address_wsh;

    @Schema(description = "送达纬度")
    private BigDecimal delivered_latitude_wsh;

    @Schema(description = "送达经度")
    private BigDecimal delivered_longitude_wsh;

    @Schema(description = "送达精度")
    private BigDecimal delivered_accuracy_wsh;
}
