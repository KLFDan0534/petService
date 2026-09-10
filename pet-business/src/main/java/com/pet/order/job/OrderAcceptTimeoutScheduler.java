package com.pet.order.job;

import com.pet.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
/**
 * Scheduled task that periodically auto-accepts paid orders whose
 * merchant acceptance window has expired. Runs every 60 seconds as a
 * fallback mechanism alongside the RabbitMQ delayed message approach.
 */
public class OrderAcceptTimeoutScheduler {

    private final OrderService orderService;

    public OrderAcceptTimeoutScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Scheduled method that auto-accepts all paid orders that have exceeded
     * the accept timeout threshold. Logs the count of accepted orders.
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
