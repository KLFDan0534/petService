package com.pet.order.job;

import com.pet.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
/**
 * Scheduled task that periodically cancels pending orders whose payment
 * window has expired. Runs every 60 seconds as a fallback mechanism
 * alongside the RabbitMQ delayed message approach.
 */
public class OrderPaymentTimeoutScheduler {

    private final OrderService orderService;

    public OrderPaymentTimeoutScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Scheduled method that cancels all pending orders that have exceeded
     * the payment timeout threshold. Logs the count of cancelled orders.
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
}
