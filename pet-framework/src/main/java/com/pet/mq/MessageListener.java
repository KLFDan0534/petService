package com.pet.mq;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ message listener that handles messages from six queues:
 * order.create, order.cancel, order.refund, message.send, ai.report,
 * and complaint.process. Each handler acknowledges or negatively
 * acknowledges the message on the channel.
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Component
public class MessageListener {

    private static final Logger log = LoggerFactory.getLogger(MessageListener.class);

    /**
     * Handles order creation messages from the order.create queue.
     * @param orderNo the order number
     * @param message the AMQP message
     * @param channel the RabbitMQ channel
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @RabbitListener(queues = "order.create")
    public void handleOrderCreate(String orderNo, Message message, Channel channel) {
        try {
            log.info("Order created: {}", orderNo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("Failed to process order create: {}", orderNo, e);
            try { channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false); } catch (Exception ignored) {}
        }
    }

    /**
     * Handles order cancellation messages from the order.cancel queue.
     * @param orderNo the order number
     * @param message the AMQP message
     * @param channel the RabbitMQ channel
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @RabbitListener(queues = "order.cancel")
    public void handleOrderCancel(String orderNo, Message message, Channel channel) {
        try {
            log.info("Order cancelled: {}", orderNo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("Failed to process order cancel: {}", orderNo, e);
            try { channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false); } catch (Exception ignored) {}
        }
    }

    /**
     * Handles order refund messages from the order.refund queue.
     * @param orderNo the order number
     * @param message the AMQP message
     * @param channel the RabbitMQ channel
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @RabbitListener(queues = "order.refund")
    public void handleOrderRefund(String orderNo, Message message, Channel channel) {
        try {
            log.info("Order refund: {}", orderNo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("Failed to process order refund: {}", orderNo, e);
            try { channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false); } catch (Exception ignored) {}
        }
    }

    /**
     * Handles message send events from the message.send queue.
     * @param payload the message payload
     * @param message the AMQP message
     * @param channel the RabbitMQ channel
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @RabbitListener(queues = "message.send")
    public void handleMessageSend(String payload, Message message, Channel channel) {
        try {
            log.info("Message send: {}", payload);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("Failed to process message send: {}", payload, e);
            try { channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false); } catch (Exception ignored) {}
        }
    }

    /**
     * Handles AI report messages from the ai.report queue.
     * @param payload the report payload
     * @param message the AMQP message
     * @param channel the RabbitMQ channel
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @RabbitListener(queues = "ai.report")
    public void handleAiReport(String payload, Message message, Channel channel) {
        try {
            log.info("AI report generated: {}", payload);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("Failed to process AI report: {}", payload, e);
            try { channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false); } catch (Exception ignored) {}
        }
    }

    /**
     * Handles complaint processing messages from the complaint.process queue.
     * @param complaintId the complaint identifier
     * @param message the AMQP message
     * @param channel the RabbitMQ channel
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @RabbitListener(queues = "complaint.process")
    public void handleComplaintProcess(String complaintId, Message message, Channel channel) {
        try {
            log.info("Complaint process: {}", complaintId);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("Failed to process complaint: {}", complaintId, e);
            try { channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false); } catch (Exception ignored) {}
        }
    }
}
