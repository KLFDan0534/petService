package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PaymentCreateRequestDTO {
    @Schema(description = "订单ID")
    private Long order_id_wsh;

    @Schema(description = "订单号")
    private String order_no_wsh;

    @Schema(description = "支付方式：balance/wechat/alipay；用户主动支付仅支持balance")
    private String method_wsh = "balance";

    public String getOrder_no_wsh() {
        return order_no_wsh == null ? null : order_no_wsh.trim();
    }

    public String getMethod_wsh() {
        return method_wsh == null || method_wsh.isBlank() ? "balance" : method_wsh.trim();
    }
}
