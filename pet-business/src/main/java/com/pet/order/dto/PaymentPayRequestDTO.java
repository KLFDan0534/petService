package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PaymentPayRequestDTO {
    @Schema(description = "支付单号")
    private String pay_no_wsh;

    /**
     * 【读取支付单号】
     *
     * 业务作用：返回去除首尾空白后的支付单号。
     *
     * 调用场景：PaymentController.pay 校验并取出支付单号后转交 PaymentService.pay。
     *
     * 调用链：PaymentController.pay → PaymentPayRequestDTO.getPay_no_wsh → PaymentService.pay。
     *
     * 数据处理：null 原样返回 null，否则返回 trim 结果。
     *
     * 业务规则：空白字符一律剔除，避免按单号精确匹配时查不到记录。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：手写 getter 覆盖了 Lombok @Data 生成的默认实现，二者签名一致，不要重复生成。
     */
    public String getPay_no_wsh() {
        return pay_no_wsh == null ? null : pay_no_wsh.trim();
    }
}
