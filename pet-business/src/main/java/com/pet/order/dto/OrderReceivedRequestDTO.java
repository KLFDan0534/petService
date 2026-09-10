package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderReceivedRequestDTO {
    @Schema(description = "订单号")
    @NotBlank(message = "订单号不能为空")
    private String order_no_wsh;

    @Schema(description = "交接码")
    @NotBlank(message = "交接码不能为空")
    private String handover_code_wsh;

    @Schema(description = "接收地址")
    private String received_address_wsh;

    @Schema(description = "接收纬度")
    private BigDecimal received_latitude_wsh;

    @Schema(description = "接收经度")
    private BigDecimal received_longitude_wsh;

    @Schema(description = "接收精度")
    private BigDecimal received_accuracy_wsh;
}
