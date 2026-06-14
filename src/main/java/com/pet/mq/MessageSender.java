package com.pet.mq;

import com.pet.module.rabbitmq.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessageSender {

    private static final Logger log = LoggerFactory.getLogger(MessageSender.class);
    private final RabbitTemplate rabbitTemplate;

    public MessageSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendOrderCreate(String orderNo) {
        log.info("Sending order create event: {}", orderNo);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_DIRECT,
                RabbitMQConfig.QUEUE_ORDER_CREATE, orderNo);
    }

    public void sendOrderCancel(String orderNo) {
        log.info("Sending order cancel event: {}", orderNo);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_DIRECT,
                RabbitMQConfig.QUEUE_ORDER_CANCEL, orderNo);
    }

    public void sendOrderRefund(String orderNo) {
        log.info("Sending order refund event: {}", orderNo);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_DIRECT,
                RabbitMQConfig.QUEUE_ORDER_REFUND, orderNo);
    }

    public void sendMessage(String payload) {
        log.info("Sending message event: {}", payload);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_DIRECT,
                RabbitMQConfig.QUEUE_MESSAGE_SEND, payload);
    }

    public void sendAiReport(String payload) {
        log.info("Sending AI report event: {}", payload);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_DIRECT,
                RabbitMQConfig.QUEUE_AI_REPORT, payload);
    }

    public void sendComplaintProcess(Long complaintId) {
        log.info("Sending complaint process event: {}", complaintId);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_DIRECT,
                RabbitMQConfig.QUEUE_COMPLAINT_PROCESS, complaintId.toString());
    }
}
