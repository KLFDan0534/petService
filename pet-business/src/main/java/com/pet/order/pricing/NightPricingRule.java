package com.pet.order.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 【按夜计价规则（从不可变快照读出的冻结值）】
 *
 * 业务作用：
 * 实际晚接补费只允许读取下单时冻结的夜价与锚点，不允许回查当前服务价。
 * 商家后续改价不影响原单（AE17）。
 *
 * @param nightPrice           冻结的夜价 P
 * @param bookedNights         预订夜数 N
 * @param standardCheckoutAt   标准离店锚点 O
 * @param freeCheckoutUntil    免费接走截止 G
 */
public record NightPricingRule(
        BigDecimal nightPrice,
        int bookedNights,
        LocalDateTime standardCheckoutAt,
        LocalDateTime freeCheckoutUntil) {
}
