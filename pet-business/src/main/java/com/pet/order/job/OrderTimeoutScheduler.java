package com.pet.order.job;

import com.pet.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时兜底调度器。
 *
 * 业务作用：
 * 订单有两条超时链路——支付超时（15 分钟）与接单超时（30 分钟）。
 * 两条链路的主通道都是 RabbitMQ 延迟消息，本调度器每 60 秒扫一次作为兜底：
 * MQ 消息丢失、消费失败或被 Nack 丢弃时，订单仍然能被推进。
 *
 * 为什么两个任务放在一个类：
 * 原来 OrderAcceptTimeoutScheduler 与 OrderPaymentTimeoutScheduler 两个类
 * 除方法名外完全相同（各 57 行）。合并后依赖只注入一次，两个方法各自
 * try/catch，一个失败不影响另一个。
 */
@Component
@Slf4j
public class OrderTimeoutScheduler {

    private final OrderService orderService;

    public OrderTimeoutScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 支付超时：pending 超过 15 分钟仍未付款 → 取消订单并释放优惠券/会员权益。
     */
    @Scheduled(fixedDelay = 60000)
    public void cancelTimeoutPendingOrders() {
        try {
            int cancelled = orderService.cancelPaymentTimeoutOrders();
            if (cancelled > 0) {
                log.info("支付超时调度器已取消 {} 笔待支付订单", cancelled);
            }
        } catch (Exception e) {
            log.warn("支付超时调度器执行失败", e);
        }
    }

    /**
     * 接单超时：paid 超过 30 分钟仍未被看护者接单 → 自动确认，进入待接收。
     */
    @Scheduled(fixedDelay = 60000)
    public void acceptTimeoutPaidOrders() {
        try {
            int accepted = orderService.autoAcceptPaidOrdersIfTimeout();
            if (accepted > 0) {
                log.info("接单超时调度器已自动接单 {} 笔已支付订单", accepted);
            }
        } catch (Exception e) {
            log.warn("接单超时调度器执行失败", e);
        }
    }
}
