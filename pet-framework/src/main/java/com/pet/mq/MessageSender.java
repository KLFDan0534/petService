package com.pet.mq;

import com.pet.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ message sender that publishes events to the pet.direct exchange
 * with routing keys corresponding to order.create, order.cancel, order.refund,
 * message.send, ai.report, and complaint.process.
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Component
public class MessageSender {

    private static final Logger log = LoggerFactory.getLogger(MessageSender.class);
    private final RabbitTemplate rabbitTemplate;

    /**
     * Constructs a MessageSender with the given RabbitTemplate.
     * @param rabbitTemplate the RabbitTemplate used to send messages
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    public MessageSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Sends an order creation event to the order.create queue.
     * @param orderNo the order number
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    public void sendOrderCreate(String orderNo) {
        log.info("Sending order create event: {}", orderNo);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_DIRECT,
                RabbitMQConfig.QUEUE_ORDER_CREATE, orderNo);
    }

    /**
     * Sends an order cancellation event to the order.cancel queue.
     * @param orderNo the order number
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    public void sendOrderCancel(String orderNo) {
        log.info("Sending order cancel event: {}", orderNo);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_DIRECT,
                RabbitMQConfig.QUEUE_ORDER_CANCEL, orderNo);
    }

    public void sendOrderPaymentTimeout(String orderNo) {
        log.info("Sending order payment timeout event: {}", orderNo);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_DIRECT,
                RabbitMQConfig.QUEUE_ORDER_PAYMENT_TIMEOUT_DELAY, orderNo);
    }

    public void sendOrderAcceptTimeout(String orderNo) {
        log.info("Sending order accept timeout event: {}", orderNo);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_DIRECT,
                RabbitMQConfig.QUEUE_ORDER_ACCEPT_TIMEOUT_DELAY, orderNo);
    }

    /**
     * Sends an order refund event to the order.refund queue.
     * @param orderNo the order number
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    public void sendOrderRefund(String orderNo) {
        log.info("Sending order refund event: {}", orderNo);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_DIRECT,
                RabbitMQConfig.QUEUE_ORDER_REFUND, orderNo);
    }

    /**
     * Sends a message event to the message.send queue.
     * @param payload the message payload
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    public void sendMessage(String payload) {
        log.info("Sending message event: {}", payload);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_DIRECT,
                RabbitMQConfig.QUEUE_MESSAGE_SEND, payload);
    }

    /**
     * Sends an AI report event to the ai.report queue.
     * @param payload the report payload
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    public void sendAiReport(String payload) {
        log.info("Sending AI report event: {}", payload);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_DIRECT,
                RabbitMQConfig.QUEUE_AI_REPORT, payload);
    }

    /**
     * Sends a complaint processing event to the complaint.process queue.
     * @param complaintId the complaint identifier
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    public void sendComplaintProcess(Long complaintId) {
        log.info("Sending complaint process event: {}", complaintId);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_DIRECT,
                RabbitMQConfig.QUEUE_COMPLAINT_PROCESS, complaintId.toString());
    }
}
