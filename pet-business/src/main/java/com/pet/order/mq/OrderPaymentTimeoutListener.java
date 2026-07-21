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
public class OrderPaymentTimeoutListener {

    private final OrderService orderService;

    public OrderPaymentTimeoutListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_PAYMENT_TIMEOUT)
    public void handlePaymentTimeout(String orderNo, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            boolean cancelled = orderService.cancelPendingOrderIfPaymentTimeout(orderNo);
            if (cancelled) {
                log.info("Payment timeout message cancelled order: {}", orderNo);
            } else {
                log.debug("Payment timeout message ignored, order is not timeout pending: {}", orderNo);
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to process payment timeout message: {}", orderNo, e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception nackError) {
                log.warn("Failed to nack payment timeout message: {}", orderNo, nackError);
            }
        }
    }
}
