package com.pet.mq;

import com.pet.common.mq.ComplaintProcessHandler;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 【RabbitMQ 消息消费者】
 *
 * 业务作用：
 * 统一的消息消费入口，监听 pet.direct 交换机下的多个队列。
 * 收到消息后执行业务逻辑（当前主要为日志记录），
 * 处理完成后手动确认（basicAck）或拒绝（basicNack）消息。
 *
 * 监听的队列：
 * - order.create       : 订单创建事件
 * - order.cancel       : 订单取消事件
 * - order.refund       : 订单退款事件
 * - message.send       : 消息发送事件
 * - ai.report          : AI 报告生成事件（由 OrderCompletedReportListener 消费）
 * - complaint.process  : 投诉处理完成事件
 *
 * 说明：
 * 当前大部分监听器仅做日志记录和消息确认（ACK），
 * 后续可在对应 handle 方法中扩展实际业务逻辑。
 * AI 报告事件有独立的消费者（OrderCompletedReportListener），
 * 此处为备用的日志记录消费者。
 *
 * 异常处理：
 * 所有 handle 方法在捕获异常后调用 basicNack（requeue=false）
 * 将消息丢弃而非重新入队，避免死循环。
 */
@Component
public class MessageListener {

    private static final Logger log = LoggerFactory.getLogger(MessageListener.class);

    @Autowired(required = false)
    private ComplaintProcessHandler complaintProcessHandler;

    /**
     * 【订单创建事件处理】
     *
     * 生产者：
     * MessageSender.sendOrderCreate()
     *   触发时机：OrderService.createOrder() 成功后
     *
     * 当前行为：
     * 记录日志并确认消息。
     *
     * @param orderNo 订单号
     * @param message AMQP 消息体
     * @param channel RabbitMQ 信道
     */
    @RabbitListener(queues = "order.create")
    public void handleOrderCreate(String orderNo, Message message, Channel channel) {
        try {
            log.info("订单已创建: {}", orderNo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("处理订单创建失败: {}", orderNo, e);
            try { channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false); } catch (Exception ignored) {}
        }
    }

    /**
     * 【订单取消事件处理】
     *
     * 生产者：
     * MessageSender.sendOrderCancel()
     *   触发时机：OrderService.cancelOrder() 或超时自动取消时
     *
     * 当前行为：
     * 记录日志并确认消息。
     *
     * @param orderNo 订单号
     * @param message AMQP 消息体
     * @param channel RabbitMQ 信道
     */
    @RabbitListener(queues = "order.cancel")
    public void handleOrderCancel(String orderNo, Message message, Channel channel) {
        try {
            log.info("订单已取消: {}", orderNo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("处理订单取消失败: {}", orderNo, e);
            try { channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false); } catch (Exception ignored) {}
        }
    }

    /**
     * 【订单退款事件处理】
     *
     * 生产者：
     * MessageSender.sendOrderRefund()
     *   触发时机：退款申请审核通过时
     *
     * 当前行为：
     * 记录日志并确认消息。
     *
     * @param orderNo 订单号
     * @param message AMQP 消息体
     * @param channel RabbitMQ 信道
     */
    @RabbitListener(queues = "order.refund")
    public void handleOrderRefund(String orderNo, Message message, Channel channel) {
        try {
            log.info("订单退款: {}", orderNo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("处理订单退款失败: {}", orderNo, e);
            try { channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false); } catch (Exception ignored) {}
        }
    }

    /**
     * 【消息发送事件处理】
     *
     * 生产者：
     * MessageSender.sendMessage()
     *   触发时机：聊天消息/系统通知发送时
     *
     * 当前行为：
     * 记录日志并确认消息。
     *
     * @param payload 消息内容
     * @param message AMQP 消息体
     * @param channel RabbitMQ 信道
     */
    @RabbitListener(queues = "message.send")
    public void handleMessageSend(String payload, Message message, Channel channel) {
        try {
            log.info("消息发送: {}", payload);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("处理消息发送失败: {}", payload, e);
            try { channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false); } catch (Exception ignored) {}
        }
    }

    /**
     * 【AI 报告事件处理（备用的日志消费者）】
     *
     * 说明：
     * 此消费者仅记录日志。AI 报告的真正业务处理由
     * OrderCompletedReportListener 独立消费。
     *
     * 生产者：
     * MessageSender.sendAiReport()
     *   触发时机：订单完成时
     *
     * @param payload JSON 负载
     * @param message AMQP 消息体
     * @param channel RabbitMQ 信道
     */
    @RabbitListener(queues = "ai.report")
    public void handleAiReport(String payload, Message message, Channel channel) {
        try {
            log.info("AI 报告已生成: {}", payload);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("处理 AI 报告失败: {}", payload, e);
            try { channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false); } catch (Exception ignored) {}
        }
    }

    /**
     * 【投诉处理事件】
     *
     * 生产者：
     * MessageSender.sendComplaintProcess()
     *   触发时机：投诉审核通过时
     *
     * 业务流：
     * handleComplaintProcess()
     *   ↓
     * ComplaintProcessHandler.handle(complaintId)
     *   ↓ (pet-business 模块实现)
     * 通知投诉双方处理结果
     *
     * @param complaintId 投诉 ID（字符串形式）
     * @param message AMQP 消息体
     * @param channel RabbitMQ 信道
     */
    @RabbitListener(queues = "complaint.process")
    public void handleComplaintProcess(String complaintId, Message message, Channel channel) {
        try {
            log.info("投诉处理: {}", complaintId);
            if (complaintProcessHandler != null) {
                complaintProcessHandler.handle(Long.valueOf(complaintId));
            } else {
                log.warn("上下文中没有可用的 ComplaintProcessHandler");
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("处理投诉失败: {}", complaintId, e);
            try { channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false); } catch (Exception ignored) {}
        }
    }
}
