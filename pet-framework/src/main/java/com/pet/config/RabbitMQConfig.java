package com.pet.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 * 定义直连交换机、六个持久化队列（order.create、order.cancel、
 * order.refund、message.send、ai.report、complaint.process）以及它们的绑定关系
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_DIRECT = "pet.direct";
    public static final String QUEUE_ORDER_CREATE = "order.create";
    public static final String QUEUE_ORDER_CANCEL = "order.cancel";
    public static final String QUEUE_ORDER_PAYMENT_TIMEOUT_DELAY = "order.payment.timeout.delay";
    public static final String QUEUE_ORDER_PAYMENT_TIMEOUT = "order.payment.timeout";
    public static final String QUEUE_ORDER_ACCEPT_TIMEOUT_DELAY = "order.accept.timeout.delay";
    public static final String QUEUE_ORDER_ACCEPT_TIMEOUT = "order.accept.timeout";
    public static final String QUEUE_ORDER_REFUND = "order.refund";
    public static final String QUEUE_MESSAGE_SEND = "message.send";
    public static final String QUEUE_AI_REPORT = "ai.report";
    public static final String QUEUE_NOTICE_NOTIFICATION = "notice.notification";
    public static final String QUEUE_COMPLAINT_PROCESS = "complaint.process";
    public static final int ORDER_PAYMENT_TIMEOUT_TTL_MS = 15 * 60 * 1000;
    public static final int ORDER_ACCEPT_TIMEOUT_TTL_MS = 30 * 60 * 1000;

    /**
     * 创建pet.direct直连交换机
     * @return 直连交换机实例
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(EXCHANGE_DIRECT);
    }

    /**
     * 创建order.create持久化队列
     * @return 队列实例
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public Queue orderCreateQueue() { return new Queue(QUEUE_ORDER_CREATE, true); }

    /**
     * 创建order.cancel持久化队列
     * @return 队列实例
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public Queue orderCancelQueue() { return new Queue(QUEUE_ORDER_CANCEL, true); }

    /**
     * 创建order.payment.timeout.delay延迟队列，消息TTL为15分钟
     * 过期后的消息会通过死信转发到同一交换机下的order.payment.timeout队列
     *
     * @return 配置完成的队列实例
     */
    @Bean
    public Queue orderPaymentTimeoutDelayQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_PAYMENT_TIMEOUT_DELAY)
            .ttl(ORDER_PAYMENT_TIMEOUT_TTL_MS)
            .deadLetterExchange(EXCHANGE_DIRECT)
            .deadLetterRoutingKey(QUEUE_ORDER_PAYMENT_TIMEOUT)
            .build();
    }

    /**
     * 创建order.payment.timeout持久化队列，用于接收延迟队列TTL过期后转发过来的死信消息
     *
     * @return 队列实例
     */
    @Bean
    public Queue orderPaymentTimeoutQueue() { return new Queue(QUEUE_ORDER_PAYMENT_TIMEOUT, true); }

    /**
     * 创建order.accept.timeout.delay延迟队列，消息TTL为30分钟
     * 过期后的消息会通过死信转发到同一交换机下的order.accept.timeout队列
     *
     * @return 配置完成的队列实例
     */
    @Bean
    public Queue orderAcceptTimeoutDelayQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_ACCEPT_TIMEOUT_DELAY)
            .ttl(ORDER_ACCEPT_TIMEOUT_TTL_MS)
            .deadLetterExchange(EXCHANGE_DIRECT)
            .deadLetterRoutingKey(QUEUE_ORDER_ACCEPT_TIMEOUT)
            .build();
    }

    /**
     * 创建order.accept.timeout持久化队列，用于接收接单延迟队列TTL过期后转发过来的死信消息
     *
     * @return 队列实例
     */
    @Bean
    public Queue orderAcceptTimeoutQueue() { return new Queue(QUEUE_ORDER_ACCEPT_TIMEOUT, true); }

    /**
     * 创建order.refund持久化队列
     * @return 队列实例
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public Queue orderRefundQueue() { return new Queue(QUEUE_ORDER_REFUND, true); }

    /**
     * 创建message.send持久化队列
     * @return 队列实例
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public Queue messageSendQueue() { return new Queue(QUEUE_MESSAGE_SEND, true); }

    /**
     * 创建ai.report持久化队列
     * @return 队列实例
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public Queue aiReportQueue() { return new Queue(QUEUE_AI_REPORT, true); }

    /**
     * 创建notice.notification持久化队列
     * @return 队列实例
     * @author: wsh
     * @date: 2026/10/07 21:44
     */
    @Bean
    public Queue noticeNotificationQueue() { return new Queue(QUEUE_NOTICE_NOTIFICATION, true); }

    /**
     * 创建complaint.process持久化队列
     * @return 队列实例
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public Queue complaintProcessQueue() { return new Queue(QUEUE_COMPLAINT_PROCESS, true); }

    /**
     * 将order.create队列绑定到直连交换机，路由key为order.create
     * @return 绑定关系实例
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public Binding orderCreateBinding() {
        return BindingBuilder.bind(orderCreateQueue()).to(directExchange()).with(QUEUE_ORDER_CREATE);
    }

    /**
     * 将order.cancel队列绑定到直连交换机，路由key为order.cancel
     * @return 绑定关系实例
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public Binding orderCancelBinding() {
        return BindingBuilder.bind(orderCancelQueue()).to(directExchange()).with(QUEUE_ORDER_CANCEL);
    }

    /**
     * 将order.payment.timeout.delay延迟队列绑定到直连交换机
     * 路由key为order.payment.timeout.delay
     *
     * @return 绑定关系实例
     */
    @Bean
    public Binding orderPaymentTimeoutDelayBinding() {
        return BindingBuilder.bind(orderPaymentTimeoutDelayQueue()).to(directExchange()).with(QUEUE_ORDER_PAYMENT_TIMEOUT_DELAY);
    }

    /**
     * 将order.payment.timeout队列绑定到直连交换机
     * 路由key为order.payment.timeout
     *
     * @return 绑定关系实例
     */
    @Bean
    public Binding orderPaymentTimeoutBinding() {
        return BindingBuilder.bind(orderPaymentTimeoutQueue()).to(directExchange()).with(QUEUE_ORDER_PAYMENT_TIMEOUT);
    }

    /**
     * 将order.accept.timeout.delay延迟队列绑定到直连交换机
     * 路由key为order.accept.timeout.delay
     *
     * @return 绑定关系实例
     */
    @Bean
    public Binding orderAcceptTimeoutDelayBinding() {
        return BindingBuilder.bind(orderAcceptTimeoutDelayQueue()).to(directExchange()).with(QUEUE_ORDER_ACCEPT_TIMEOUT_DELAY);
    }

    /**
     * 将order.accept.timeout队列绑定到直连交换机
     * 路由key为order.accept.timeout
     *
     * @return 绑定关系实例
     */
    @Bean
    public Binding orderAcceptTimeoutBinding() {
        return BindingBuilder.bind(orderAcceptTimeoutQueue()).to(directExchange()).with(QUEUE_ORDER_ACCEPT_TIMEOUT);
    }

    /**
     * 将order.refund队列绑定到直连交换机，路由key为order.refund
     * @return 绑定关系实例
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public Binding orderRefundBinding() {
        return BindingBuilder.bind(orderRefundQueue()).to(directExchange()).with(QUEUE_ORDER_REFUND);
    }

    /**
     * 将message.send队列绑定到直连交换机，路由key为message.send
     * @return 绑定关系实例
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public Binding messageSendBinding() {
        return BindingBuilder.bind(messageSendQueue()).to(directExchange()).with(QUEUE_MESSAGE_SEND);
    }

    /**
     * 将ai.report队列绑定到直连交换机，路由key为ai.report
     * @return 绑定关系实例
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public Binding aiReportBinding() {
        return BindingBuilder.bind(aiReportQueue()).to(directExchange()).with(QUEUE_AI_REPORT);
    }

    /**
     * 将notice.notification队列绑定到直连交换机，路由key为notice.notification
     * @return 绑定关系实例
     * @author: wsh
     * @date: 2026/10/08 16:22
     */
    @Bean
    public Binding noticeNotificationBinding() {
        return BindingBuilder.bind(noticeNotificationQueue()).to(directExchange()).with(QUEUE_NOTICE_NOTIFICATION);
    }

    /**
     * 将complaint.process队列绑定到直连交换机，路由key为complaint.process
     * @return 绑定关系实例
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public Binding complaintProcessBinding() {
        return BindingBuilder.bind(complaintProcessQueue()).to(directExchange()).with(QUEUE_COMPLAINT_PROCESS);
    }
}
