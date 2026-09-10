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
 * RabbitMQ listener for order accept timeout events.
 * When an accept timeout message is received from the dead-letter queue,
 * automatically accepts (confirms) the paid order if the merchant has not
 * responded within the allowed window.
 */
public class OrderAcceptTimeoutListener {

    private final OrderService orderService;

    public OrderAcceptTimeoutListener(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Handles an accept timeout event for the given order.
     * Auto-accepts the order if it is in paid status and unaccepted.
     * Acknowledges the message on success, negatively acknowledges on failure.
     *
     * @param orderNo the order number
     * @param message the AMQP message
     * @param channel the RabbitMQ channel
     */
    /**
     * 【处理接单超时消息】
     *
     * 业务作用：
     * 消费接单超时延迟队列（QUEUE_ORDER_ACCEPT_TIMEOUT），在支付后指定时间窗口内
     * 看护者未手动接单时自动接单。
     *
     * 消费队列：
     * RabbitMQConfig.QUEUE_ORDER_ACCEPT_TIMEOUT
     *
     * 触发时机：
     * 订单支付成功后，PaymentServiceImpl.scheduleAcceptTimeoutCheck()发送延迟消息，
     * 到达TTL后投递到本队列。
     *
     * 调用链：
     * MQ Broker
     * ↓
     * handleAcceptTimeout(orderNo, message, channel)
     * ↓
     * orderService.autoAcceptPaidOrderIfTimeout(orderNo) → basicAck/Nack
     *
     * 数据处理：
     * 手动ACK模式，处理成功basicAck，异常时basicNack(requeue=false)防止死循环。
     *
     * 业务规则：
     * 1. 自动接单成功记录info日志，忽略时记录debug日志
     * 2. 消费异常时记录error日志并Nack，不重试
     *
     * @param orderNo 订单编号（消息体）
     * @param message AMQP消息
     * @param channel AMQP通道
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_ACCEPT_TIMEOUT)
    public void handleAcceptTimeout(String orderNo, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            boolean accepted = orderService.autoAcceptPaidOrderIfTimeout(orderNo);
            if (accepted) {
                log.info("接单超时消息已自动接单: {}", orderNo);
            } else {
                log.debug("忽略接单超时消息, 订单未支付: {}", orderNo);
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理接单超时消息失败: {}", orderNo, e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception nackError) {
                log.warn("接单超时消息 nack 失败: {}", orderNo, nackError);
            }
        }
    }
}
