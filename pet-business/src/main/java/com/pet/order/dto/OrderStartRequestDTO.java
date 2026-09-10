package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderStartRequestDTO {
    @Schema(description = "订单号")
    @NotBlank(message = "订单号不能为空")
    private String order_no_wsh;

    @Schema(description = "开始服务照片")
    private String start_photo_wsh;
}
