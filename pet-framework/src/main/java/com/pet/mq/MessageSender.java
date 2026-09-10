package com.pet.mq;

import com.pet.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 【RabbitMQ 消息生产者】
 *
 * 业务作用：
 * 统一的消息发送入口，所有需要异步处理的核心业务事件都通过此类发送到 pet.direct 交换机。
 * 生产者模块（OrderService、ComplaintService 等）在关键业务节点调用此服务发送消息。
 *
 * 消息路由概览：
 * ┌──────────────────────────┬──────────────────────────────┬──────────────────────────┐
 * │ 方法                     │ Routing Key                  │ 消费者                    │
 * ├──────────────────────────┼──────────────────────────────┼──────────────────────────┤
 * │ sendOrderCreate          │ order.create                 │ MessageListener          │
 * │ sendOrderCancel          │ order.cancel                 │ MessageListener          │
 * │ sendOrderPaymentTimeout  │ order.payment.timeout.delay  │ 延迟队列 → payment.timeout│
 * │ sendOrderAcceptTimeout   │ order.accept.timeout.delay   │ 延迟队列 → accept.timeout │
 * │ sendOrderRefund          │ order.refund                 │ MessageListener          │
 * │ sendMessage              │ message.send                 │ MessageListener          │
 * │ sendAiReport             │ ai.report                    │ OrderCompletedReportListener│
 * │ sendComplaintProcess     │ complaint.process            │ MessageListener          │
 * └──────────────────────────┴──────────────────────────────┴──────────────────────────┘
 *
 * 注意事项：
 * - 所有消息通过 DirectExchange(pet.direct) 发送
 * - 超时相关消息使用 TTL 延迟队列，到期后自动转入实际处理队列
 * - AI 报告消息由单独的 OrderCompletedReportListener 消费
 */
@Component
public class MessageSender {

    private static final Logger log = LoggerFactory.getLogger(MessageSender.class);
    private final RabbitTemplate rabbitTemplate;

    public MessageSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 【订单创建事件】
     *
     * 业务作用：
     * 订单创建成功后发送异步通知。
     *
     * 触发时机：
     * OrderService.createOrder() 执行成功后。
     *
     * 消费者：
     * MessageListener.handleOrderCreate() — 当前仅记录日志确认消息。
     *
     * @param orderNo 订单号
     */
    public void sendOrderCreate(String orderNo) {
        log.info("发送订单创建事件: {}", orderNo);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_DIRECT,
                RabbitMQConfig.QUEUE_ORDER_CREATE, orderNo);
    }

    /**
     * 【订单取消事件】
     *
     * 业务作用：
     * 订单取消时发送异步通知（用户取消/系统自动取消）。
     *
     * 触发时机：
     * OrderService.cancelOrder() 或自动超时取消时。
     *
     * 消费者：
     * MessageListener.handleOrderCancel() — 当前仅记录日志确认消息。
     *
     * @param orderNo 订单号
     */
    public void sendOrderCancel(String orderNo) {
        log.info("发送订单取消事件: {}", orderNo);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_DIRECT,
                RabbitMQConfig.QUEUE_ORDER_CANCEL, orderNo);
    }

    /**
     * 【订单支付超时事件（延迟消息）】
     *
     * 业务作用：
     * 订单创建后发送延迟消息，若在 TTL（15分钟）内未支付则自动取消订单。
     *
     * 触发时机：
     * 订单创建成功后立即发送到延迟队列。
     *
     * 消息流：
     * sendOrderPaymentTimeout
     *   ↓ queue: order.payment.timeout.delay (TTL=15min)
     *   ↓ DLX → pet.direct
     *   ↓ queue: order.payment.timeout
     *   ↓ OrderService.cancelPendingOrderIfPaymentTimeout()
     *
     * @param orderNo 订单号
     */
    public void sendOrderPaymentTimeout(String orderNo) {
        log.info("发送订单支付超时事件: {}", orderNo);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_DIRECT,
                RabbitMQConfig.QUEUE_ORDER_PAYMENT_TIMEOUT_DELAY, orderNo);
    }

    /**
     * 【订单接单超时事件（延迟消息）】
     *
     * 业务作用：
     * 用户支付后发送延迟消息，若在 TTL（30分钟）内商家/看护者未接单则自动确认接单。
     *
     * 触发时机：
     * 订单支付成功后立即发送到延迟队列。
     *
     * 消息流：
     * sendOrderAcceptTimeout
     *   ↓ queue: order.accept.timeout.delay (TTL=30min)
     *   ↓ DLX → pet.direct
     *   ↓ queue: order.accept.timeout
     *   ↓ OrderService.autoAcceptPaidOrderIfTimeout()
     *
     * @param orderNo 订单号
     */
    public void sendOrderAcceptTimeout(String orderNo) {
        log.info("发送订单接单超时事件: {}", orderNo);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_DIRECT,
                RabbitMQConfig.QUEUE_ORDER_ACCEPT_TIMEOUT_DELAY, orderNo);
    }

    /**
     * 【订单退款事件】
     *
     * 业务作用：
     * 订单退款处理时发送异步通知。
     *
     * 触发时机：
     * 退款申请通过后。
     *
     * 消费者：
     * MessageListener.handleOrderRefund() — 当前仅记录日志确认消息。
     *
     * @param orderNo 订单号
     */
    public void sendOrderRefund(String orderNo) {
        log.info("发送订单退款事件: {}", orderNo);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_DIRECT,
                RabbitMQConfig.QUEUE_ORDER_REFUND, orderNo);
    }

    /**
     * 【消息发送事件】
     *
     * 业务作用：
     * 聊天消息/系统通知发送时异步处理。
     *
     * 触发时机：
     * ChatService 发送消息时。
     *
     * 消费者：
     * MessageListener.handleMessageSend() — 当前仅记录日志确认消息。
     *
     * @param payload 消息内容负载
     */
    public void sendMessage(String payload) {
        log.info("发送消息事件: {}", payload);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_DIRECT,
                RabbitMQConfig.QUEUE_MESSAGE_SEND, payload);
    }

    /**
     * 【AI 报告生成事件】
     *
     * 业务作用：
     * 订单完成后触发 AI 报告生成，由独立的 AI 消费者处理。
     *
     * 触发时机：
     * OrderFulfillmentService.completeOrder() 订单完成时。
     *
     * 消费者：
     * OrderCompletedReportListener.handleAiReport() — 调用 AiReportService 生成分析报告。
     *
     * 消息载荷：{"orderId": 123}
     *
     * @param payload JSON 格式的负载
     */
    public void sendAiReport(String payload) {
        log.info("发送 AI 报告事件: {}", payload);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_DIRECT,
                RabbitMQConfig.QUEUE_AI_REPORT, payload);
    }

    /**
     * 【投诉处理事件】
     *
     * 业务作用：
     * 投诉审核通过后触发异步处理（通知投诉双方处理结果等）。
     *
     * 触发时机：
     * ComplaintService.approve() 审核通过时。
     *
     * 消费者：
     * MessageListener.handleComplaintProcess() → ComplaintProcessHandler.handle()
     *
     * @param complaintId 投诉记录 ID
     */
    public void sendComplaintProcess(Long complaintId) {
        log.info("发送投诉处理事件: {}", complaintId);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_DIRECT,
                RabbitMQConfig.QUEUE_COMPLAINT_PROCESS, complaintId.toString());
    }
}
