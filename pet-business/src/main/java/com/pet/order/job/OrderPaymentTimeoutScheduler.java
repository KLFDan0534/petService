package com.pet.order.job;

import com.pet.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderPaymentTimeoutScheduler {

    private final OrderService orderService;

    public OrderPaymentTimeoutScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedDelay = 60000)
    public void cancelTimeoutPendingOrders() {
        try {
            int cancelled = orderService.cancelPaymentTimeoutOrders();
            if (cancelled > 0) {
                log.info("Payment timeout scheduler cancelled {} pending orders", cancelled);
            }
        } catch (Exception e) {
            log.warn("Payment timeout scheduler failed", e);
        }
    }
}
