package com.pet.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for RabbitMQ.
 * Defines a direct exchange, six durable queues (order.create, order.cancel,
 * order.refund, message.send, ai.report, complaint.process), and their bindings.
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_DIRECT = "pet.direct";
    public static final String QUEUE_ORDER_CREATE = "order.create";
    public static final String QUEUE_ORDER_CANCEL = "order.cancel";
    public static final String QUEUE_ORDER_REFUND = "order.refund";
    public static final String QUEUE_MESSAGE_SEND = "message.send";
    public static final String QUEUE_AI_REPORT = "ai.report";
    public static final String QUEUE_COMPLAINT_PROCESS = "complaint.process";

    /**
     * Creates the pet.direct direct exchange.
     * @return the DirectExchange instance
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(EXCHANGE_DIRECT);
    }

    /**
     * Creates the order.create durable queue.
     * @return the Queue instance
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public Queue orderCreateQueue() { return new Queue(QUEUE_ORDER_CREATE, true); }

    /**
     * Creates the order.cancel durable queue.
     * @return the Queue instance
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public Queue orderCancelQueue() { return new Queue(QUEUE_ORDER_CANCEL, true); }

    /**
     * Creates the order.refund durable queue.
     * @return the Queue instance
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public Queue orderRefundQueue() { return new Queue(QUEUE_ORDER_REFUND, true); }

    /**
     * Creates the message.send durable queue.
     * @return the Queue instance
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public Queue messageSendQueue() { return new Queue(QUEUE_MESSAGE_SEND, true); }

    /**
     * Creates the ai.report durable queue.
     * @return the Queue instance
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public Queue aiReportQueue() { return new Queue(QUEUE_AI_REPORT, true); }

    /**
     * Creates the complaint.process durable queue.
     * @return the Queue instance
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public Queue complaintProcessQueue() { return new Queue(QUEUE_COMPLAINT_PROCESS, true); }

    /**
     * Binds the order.create queue to the direct exchange with order.create routing key.
     * @return the Binding instance
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public Binding orderCreateBinding() {
        return BindingBuilder.bind(orderCreateQueue()).to(directExchange()).with(QUEUE_ORDER_CREATE);
    }

    /**
     * Binds the order.cancel queue to the direct exchange with order.cancel routing key.
     * @return the Binding instance
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public Binding orderCancelBinding() {
        return BindingBuilder.bind(orderCancelQueue()).to(directExchange()).with(QUEUE_ORDER_CANCEL);
    }

    /**
     * Binds the order.refund queue to the direct exchange with order.refund routing key.
     * @return the Binding instance
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public Binding orderRefundBinding() {
        return BindingBuilder.bind(orderRefundQueue()).to(directExchange()).with(QUEUE_ORDER_REFUND);
    }

    /**
     * Binds the message.send queue to the direct exchange with message.send routing key.
     * @return the Binding instance
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public Binding messageSendBinding() {
        return BindingBuilder.bind(messageSendQueue()).to(directExchange()).with(QUEUE_MESSAGE_SEND);
    }

    /**
     * Binds the ai.report queue to the direct exchange with ai.report routing key.
     * @return the Binding instance
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public Binding aiReportBinding() {
        return BindingBuilder.bind(aiReportQueue()).to(directExchange()).with(QUEUE_AI_REPORT);
    }

    /**
     * Binds the complaint.process queue to the direct exchange with complaint.process routing key.
     * @return the Binding instance
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public Binding complaintProcessBinding() {
        return BindingBuilder.bind(complaintProcessQueue()).to(directExchange()).with(QUEUE_COMPLAINT_PROCESS);
    }
}
