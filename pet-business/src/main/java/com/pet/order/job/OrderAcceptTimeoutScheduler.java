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
                log.info("Accept timeout scheduler auto accepted {} paid orders", accepted);
            }
        } catch (Exception e) {
            log.warn("Accept timeout scheduler failed", e);
        }
    }
}
