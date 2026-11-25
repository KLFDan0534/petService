package com.pet.order.mq;

import com.pet.config.RabbitMQConfig;
import com.pet.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 订单支付超时事件的 RabbitMQ 监听器。
 * 当从死信队列收到支付超时消息后，如果订单在允许的时间窗口内仍未支付，
 * 则取消该待支付订单。
 */
@Component
@Slf4j
public class OrderPaymentTimeoutListener {

    private final OrderService orderService;

    /**
     * 业务作用：注入 OrderService，供超时消息处理时执行订单取消。
     * 注意事项：类级 Javadoc 写在 @Component/@Slf4j 之后，不符合注解在前、注释在上的常规写法，但不影响编译。
     */
    public OrderPaymentTimeoutListener(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 处理指定订单的支付超时事件。
     * 如果订单仍处于待支付状态，则尝试取消该订单。
     * 处理成功时确认消息，处理失败时否定确认消息。
     *
     * @param orderNo 订单号
     * @param message AMQP 消息
     * @param channel RabbitMQ 通道
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
                // TODO 处理 nack 失败的情况, 最后的 false 表示不重新入队,但是会导致改订单无限未支付状态
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception nackError) {
                log.warn("支付超时消息 nack 失败: {}", orderNo, nackError);
            }
        }
    }
}
