package com.pet.order.mq;

import com.pet.config.RabbitMQConfig;
import com.pet.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderAcceptTimeoutListener {

    private final OrderService orderService;

    public OrderAcceptTimeoutListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_ACCEPT_TIMEOUT)
    public void handleAcceptTimeout(String orderNo, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            boolean accepted = orderService.autoAcceptPaidOrderIfTimeout(orderNo);
            if (accepted) {
                log.info("Accept timeout message auto accepted order: {}", orderNo);
            } else {
                log.debug("Accept timeout message ignored, order is not paid: {}", orderNo);
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to process accept timeout message: {}", orderNo, e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception nackError) {
                log.warn("Failed to nack accept timeout message: {}", orderNo, nackError);
            }
        }
    }
}
