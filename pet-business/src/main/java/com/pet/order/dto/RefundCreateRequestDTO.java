package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundCreateRequestDTO {
    @Schema(description = "订单ID")
    @NotNull(message = "订单ID不能为空")
    private Long order_id_wsh;

    /**
     * 退款原因：商家审核退款时唯一的判断依据，不允许为空。
     * 前端把「原因 + 补充说明」拼成一句话提交，这里限长与 refund_wsh.reason_wsh 的 TEXT 列对齐。
     */
    @Schema(description = "退款原因")
    @NotBlank(message = "请填写退款原因")
    @Size(max = 500, message = "退款原因不能超过 500 个字符")
    private String reason_wsh;
}
