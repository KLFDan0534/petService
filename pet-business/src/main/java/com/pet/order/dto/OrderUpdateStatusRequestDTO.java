package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderUpdateStatusRequestDTO {
    @Schema(description = "订单状态")
    @NotBlank(message = "状态不能为空")
    @JsonAlias("status")
    private String status_wsh;
}
