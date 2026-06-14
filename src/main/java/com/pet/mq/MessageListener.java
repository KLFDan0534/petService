package com.pet.mq;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MessageListener {

    private static final Logger log = LoggerFactory.getLogger(MessageListener.class);

    @RabbitListener(queues = "order.create")
    public void handleOrderCreate(String orderNo, Message message, Channel channel) {
        try {
            log.info("Order created: {}", orderNo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("Failed to process order create: {}", orderNo, e);
        }
    }

    @RabbitListener(queues = "order.cancel")
    public void handleOrderCancel(String orderNo, Message message, Channel channel) {
        try {
            log.info("Order cancelled: {}", orderNo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("Failed to process order cancel: {}", orderNo, e);
        }
    }

    @RabbitListener(queues = "order.refund")
    public void handleOrderRefund(String orderNo, Message message, Channel channel) {
        try {
            log.info("Order refund: {}", orderNo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("Failed to process order refund: {}", orderNo, e);
        }
    }

    @RabbitListener(queues = "message.send")
    public void handleMessageSend(String payload, Message message, Channel channel) {
        try {
            log.info("Message send: {}", payload);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("Failed to process message send: {}", payload, e);
        }
    }

    @RabbitListener(queues = "ai.report")
    public void handleAiReport(String payload, Message message, Channel channel) {
        try {
            log.info("AI report generated: {}", payload);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("Failed to process AI report: {}", payload, e);
        }
    }

    @RabbitListener(queues = "complaint.process")
    public void handleComplaintProcess(String complaintId, Message message, Channel channel) {
        try {
            log.info("Complaint process: {}", complaintId);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("Failed to process complaint: {}", complaintId, e);
        }
    }
}
