package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderReceivedRequestDTO {
    @Schema(description = "订单号")
    @NotBlank(message = "订单号不能为空")
    @JsonAlias({"orderNo", "order_no"})
    private String order_no_wsh;

    @Schema(description = "交接码")
    @NotBlank(message = "交接码不能为空")
    @JsonAlias({"handoverCode", "handover_code", "receiveCode", "receive_code"})
    private String handover_code_wsh;

    @Schema(description = "接收地址")
    @JsonAlias({"receivedAddress", "received_address"})
    private String received_address_wsh;

    @Schema(description = "接收纬度")
    @JsonAlias({"receivedLatitude", "received_latitude"})
    private BigDecimal received_latitude_wsh;

    @Schema(description = "接收经度")
    @JsonAlias({"receivedLongitude", "received_longitude"})
    private BigDecimal received_longitude_wsh;

    @Schema(description = "接收精度")
    @JsonAlias({"receivedAccuracy", "received_accuracy"})
    private BigDecimal received_accuracy_wsh;
}
