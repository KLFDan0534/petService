package com.pet.module.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_DIRECT = "pet.direct";
    public static final String QUEUE_ORDER_CREATE = "order.create";
    public static final String QUEUE_ORDER_CANCEL = "order.cancel";
    public static final String QUEUE_ORDER_REFUND = "order.refund";
    public static final String QUEUE_MESSAGE_SEND = "message.send";
    public static final String QUEUE_AI_REPORT = "ai.report";
    public static final String QUEUE_COMPLAINT_PROCESS = "complaint.process";

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(EXCHANGE_DIRECT);
    }

    @Bean
    public Queue orderCreateQueue() { return new Queue(QUEUE_ORDER_CREATE, true); }

    @Bean
    public Queue orderCancelQueue() { return new Queue(QUEUE_ORDER_CANCEL, true); }

    @Bean
    public Queue orderRefundQueue() { return new Queue(QUEUE_ORDER_REFUND, true); }

    @Bean
    public Queue messageSendQueue() { return new Queue(QUEUE_MESSAGE_SEND, true); }

    @Bean
    public Queue aiReportQueue() { return new Queue(QUEUE_AI_REPORT, true); }

    @Bean
    public Queue complaintProcessQueue() { return new Queue(QUEUE_COMPLAINT_PROCESS, true); }

    @Bean
    public Binding orderCreateBinding() {
        return BindingBuilder.bind(orderCreateQueue()).to(directExchange()).with(QUEUE_ORDER_CREATE);
    }

    @Bean
    public Binding orderCancelBinding() {
        return BindingBuilder.bind(orderCancelQueue()).to(directExchange()).with(QUEUE_ORDER_CANCEL);
    }

    @Bean
    public Binding orderRefundBinding() {
        return BindingBuilder.bind(orderRefundQueue()).to(directExchange()).with(QUEUE_ORDER_REFUND);
    }

    @Bean
    public Binding messageSendBinding() {
        return BindingBuilder.bind(messageSendQueue()).to(directExchange()).with(QUEUE_MESSAGE_SEND);
    }

    @Bean
    public Binding aiReportBinding() {
        return BindingBuilder.bind(aiReportQueue()).to(directExchange()).with(QUEUE_AI_REPORT);
    }

    @Bean
    public Binding complaintProcessBinding() {
        return BindingBuilder.bind(complaintProcessQueue()).to(directExchange()).with(QUEUE_COMPLAINT_PROCESS);
    }
}
