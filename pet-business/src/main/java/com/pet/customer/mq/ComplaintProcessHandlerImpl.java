package com.pet.customer.mq;

import com.pet.mq.handler.ComplaintProcessHandler;
import com.pet.customer.entity.Complaint;
import com.pet.customer.mapper.ComplaintMapper;
import com.pet.customer.mapper.MqConsumeLogMapper;
import com.pet.operation.entity.Notification;
import com.pet.operation.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

/**
 * 【业务模块】投诉处理 MQ 消费者（实现）
 * 业务作用：监听 RabbitMQ complaint.process 队列，处理投诉解决事件。
 * 当投诉处理完成后，创建站内通知告知投诉人处理结果。
 */
@Slf4j
@Component
public class ComplaintProcessHandlerImpl implements ComplaintProcessHandler {

    /** 消费台账里的消费者标识，与队列语义对齐 */
    private static final String CONSUMER = "complaint-process";

    private final ComplaintMapper complaintMapper;
    private final NotificationService notificationService;
    private final MqConsumeLogMapper consumeLogMapper;

    /**
     * 【构造方法】注入投诉 Mapper 与通知服务
     *
     * 业务作用：由 Spring 注入 ComplaintMapper 与 NotificationService，供消费投诉处理消息时查询投诉并发送通知。
     * 异常情况：依赖缺失时由 Spring 抛出 Bean 创建异常。
     * 注意事项：依赖为 final 字段；本类未使用 @Slf4j，而是手工声明 LoggerFactory 日志对象。
     */
    public ComplaintProcessHandlerImpl(ComplaintMapper complaintMapper,
                                       NotificationService notificationService,
                                       MqConsumeLogMapper consumeLogMapper) {
        this.complaintMapper = complaintMapper;
        this.notificationService = notificationService;
        this.consumeLogMapper = consumeLogMapper;
    }

    /**
     * 处理投诉事件
     *
     * 业务作用：处理投诉记录，向投诉人发送站内通知。
     * 业务规则：投诉ID为空或投诉不存在时静默跳过；投诉无主人时跳过。
     * 异常情况：通知创建失败仅记录日志，不影响主流程。
     * 注意事项：异步处理，不抛异常。
     */
    @Override
    public void handle(Long complaintId) {
        if (complaintId == null) {
            log.warn("ComplaintProcessHandler 收到空的 complaintId");
            return;
        }

        Complaint complaint = complaintMapper.selectById(complaintId);
        if (complaint == null) {
            log.warn("未找到投诉: {}, 消息已跳过", complaintId);
            return;
        }

        log.info("处理投诉 {} (状态={}, 结果={})",
                complaintId, complaint.getStatus_wsh(), complaint.getResult_wsh());

        // 投诉无主人则跳过通知
        if (complaint.getOwner_id_wsh() == null) {
            log.warn("投诉 {} 没有所有者, 跳过通知", complaintId);
            return;
        }

        // 幂等闸门：同一条投诉的同一个终态只允许发一次通知。
        // 事件键用「投诉ID + 终态」，而不是单纯的投诉ID —— 万一将来允许一条投诉多次流转，
        // 不同终态仍然各发一条，不会被误判成重复。
        String eventKey = complaintId + ":" + complaint.getStatus_wsh();
        if (!tryClaim(eventKey)) {
            log.info("投诉 {} 状态 {} 的通知已发送过，跳过重复消费", complaintId, complaint.getStatus_wsh());
            return;
        }

        // 通知失败只记日志，不影响消息消费；同时释放台账，让下次投递还能重试
        try {
            Notification notification = new Notification();
            notification.setUser_id_wsh(complaint.getOwner_id_wsh());
            notification.setTitle_wsh("投诉处理通知");
            notification.setContent_wsh("您的投诉「" + complaint.getTitle_wsh() + "」"
                    + statusText(complaint.getStatus_wsh())
                    + (complaint.getResult_wsh() != null ? "，处理结果：" + complaint.getResult_wsh() : "。"));
            notification.setType_wsh("complaint");
            notification.setRelated_id_wsh(complaintId);
            notification.setIs_read_wsh(0);
            notificationService.create(notification);
            log.info("投诉 {} 的通知已发送", complaintId);
        } catch (Exception e) {
            releaseClaim(eventKey);
            log.warn("为投诉 {} 发送通知失败，已释放幂等占位以便重投", complaintId, e);
        }
    }

    /**
     * 抢占幂等占位。
     *
     * @param eventKey 业务事件键
     * @return true 表示首次处理；false 表示已处理过
     */
    private boolean tryClaim(String eventKey) {
        try {
            consumeLogMapper.insertClaim(CONSUMER, eventKey);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /**
     * 释放幂等占位（业务处理失败时调用，避免事件被永久判为已处理）。
     *
     * @param eventKey 业务事件键
     */
    private void releaseClaim(String eventKey) {
        try {
            consumeLogMapper.releaseClaim(CONSUMER, eventKey);
        } catch (Exception e) {
            log.warn("释放消费台账失败: consumer={}, eventKey={}", CONSUMER, eventKey, e);
        }
    }

    /** 状态转用户可读文案；未知状态回退为「已更新」，不把内部枚举暴露给用户。 */
    private String statusText(String status) {
        if (status == null) {
            return "已更新";
        }
        return switch (status) {
            case "resolved" -> "已处理完成";
            case "rejected" -> "已驳回";
            case "processing" -> "正在处理中";
            case "pending" -> "已受理";
            default -> "已更新";
        };
    }
}
