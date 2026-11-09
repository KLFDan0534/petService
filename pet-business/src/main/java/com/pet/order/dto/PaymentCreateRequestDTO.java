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

    /**
     * 【获取订单号（去空白）】
     *
     * 业务作用：返回 trim 后的订单号，避免首尾空格导致按订单号查询失败。
     *
     * 调用场景：支付接口从请求体读取订单号时。
     *
     * 调用链：支付接口 → getOrder_no_wsh() → 订单查询逻辑。
     *
     * 数据处理：null 返回 null，否则返回 trim 后的值。
     *
     * 业务规则：不做空串转 null 的处理。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：类上有 @Data，本方法显式覆盖了 Lombok 生成的同名 getter。
     */
    public String getOrder_no_wsh() {
        return order_no_wsh == null ? null : order_no_wsh.trim();
    }

    /**
     * 【获取支付方式（缺省余额）】
     *
     * 业务作用：返回 trim 后的支付方式，空值统一缺省为 balance。
     *
     * 调用场景：支付接口从请求体读取支付方式时。
     *
     * 调用链：支付接口 → getMethod_wsh() → 支付方式校验与扣款分支。
     *
     * 数据处理：null 或空白返回 "balance"，否则返回 trim 后的值。
     *
     * 业务规则：字段默认值本身就是 balance，本方法再兜底一次。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：只做 trim 与缺省，不校验是否属于合法渠道，合法性由服务层判断。
     */
    public String getMethod_wsh() {
        return method_wsh == null || method_wsh.isBlank() ? "balance" : method_wsh.trim();
    }
}
