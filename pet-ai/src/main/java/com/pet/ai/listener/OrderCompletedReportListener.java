package com.pet.ai.listener;

import com.pet.ai.service.AiReportService;
import com.pet.order.entity.PetOrder;
import com.pet.order.event.OrderCompletedEvent;
import com.pet.order.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class OrderCompletedReportListener {

    private final AiReportService aiReportService;
    private final OrderMapper orderMapper;

    public OrderCompletedReportListener(AiReportService aiReportService, OrderMapper orderMapper) {
        this.aiReportService = aiReportService;
        this.orderMapper = orderMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        if (event == null || event.orderId() == null) {
            return;
        }
        PetOrder order = orderMapper.selectById(event.orderId());
        if (order == null || Integer.valueOf(1).equals(order.getFinal_report_generated_wsh())) {
            return;
        }
        try {
            aiReportService.generateBoardingReport(event.petId(), event.keeperId(), event.orderId());
            order.setFinal_report_generated_wsh(1);
            orderMapper.updateById(order);
        } catch (Exception e) {
            log.warn("为订单 {} 生成最终报告失败", event.orderId(), e);
        }
    }
}
