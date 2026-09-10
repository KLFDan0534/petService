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
/**
 * RabbitMQ listener for order payment timeout events.
 * When a payment timeout message is received from the dead-letter queue,
 * cancels the pending order if it has not been paid within the allowed window.
 */
public class OrderPaymentTimeoutListener {

    private final OrderService orderService;

    public OrderPaymentTimeoutListener(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Handles a payment timeout event for the given order.
     * Attempts to cancel the order if it is still in pending status.
     * Acknowledges the message on success, negatively acknowledges on failure.
     *
     * @param orderNo the order number
     * @param message the AMQP message
     * @param channel the RabbitMQ channel
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_PAYMENT_TIMEOUT)
    public void handlePaymentTimeout(String orderNo, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            boolean cancelled = orderService.cancelPendingOrderIfPaymentTimeout(orderNo);
            if (cancelled) {
                log.info("支付超时消息已取消订单: {}", orderNo);
            } else {
                log.debug("忽略支付超时消息, 订单不处于待超时状态: {}", orderNo);
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理支付超时消息失败: {}", orderNo, e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception nackError) {
                log.warn("支付超时消息 nack 失败: {}", orderNo, nackError);
            }
        }
    }
}
