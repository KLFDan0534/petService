package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TipCreateRequestDTO {

    @Schema(description = "订单ID")
    @NotNull(message = "订单ID不能为空")
    private Long order_id_wsh;

    @Schema(description = "接收用户ID")
    private Long to_user_id_wsh;

    @Schema(description = "小费金额")
    @NotNull(message = "小费金额不能为空")
    private BigDecimal amount_wsh;

    @Schema(description = "小费留言")
    private String message_wsh;
}
