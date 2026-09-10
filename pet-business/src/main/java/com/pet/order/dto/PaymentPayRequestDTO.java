package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PaymentPayRequestDTO {
    @Schema(description = "支付单号")
    private String pay_no_wsh;

    public String getPay_no_wsh() {
        return pay_no_wsh == null ? null : pay_no_wsh.trim();
    }
}
