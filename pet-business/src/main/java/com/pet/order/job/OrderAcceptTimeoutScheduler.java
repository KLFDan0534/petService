package com.pet.order.job;

import com.pet.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderAcceptTimeoutScheduler {

    private final OrderService orderService;

    public OrderAcceptTimeoutScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

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
