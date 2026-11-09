package com.pet.order.pricing;

/**
 * 【支付用途】
 *
 * 业务作用：
 * 同一张订单可以有两笔独立支付：下单时的原预付，以及实际晚接产生的补费。
 * 用用途把两者分开，避免原支付单与补费单互相覆盖（原支付接口只能看到 order）。
 *
 * 约定：
 * - 值写入 payment_wsh.purpose_wsh，历史数据默认为 order。
 * - 补费单唯一性由订单行锁 + pet_order_wsh.overtime_payment_id_wsh 保证，
 *   不加「订单 + 用途」全表唯一约束（会破坏原支付的重建/重试策略）。
 */
public final class PaymentPurpose {

    /** 下单预付（订单 final_amount） */
    public static final String ORDER = "order";

    /** 实际晚接补费（按夜订单接走时结算） */
    public static final String NIGHT_OVERTIME = "night_overtime";

    private PaymentPurpose() {
    }
}
