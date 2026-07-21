package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderDeliveredRequestDTO {
    @Schema(description = "订单号")
    @NotBlank(message = "订单号不能为空")
    @JsonAlias({"orderNo", "order_no"})
    private String order_no_wsh;

    @Schema(description = "送达地址")
    @JsonAlias({"deliveredAddress", "delivered_address"})
    private String delivered_address_wsh;

    @Schema(description = "送达纬度")
    @JsonAlias({"deliveredLatitude", "delivered_latitude"})
    private BigDecimal delivered_latitude_wsh;

    @Schema(description = "送达经度")
    @JsonAlias({"deliveredLongitude", "delivered_longitude"})
    private BigDecimal delivered_longitude_wsh;

    @Schema(description = "送达精度")
    @JsonAlias({"deliveredAccuracy", "delivered_accuracy"})
    private BigDecimal delivered_accuracy_wsh;
}
