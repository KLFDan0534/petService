package com.pet.customer.mq;

import com.pet.common.mq.ComplaintProcessHandler;
import com.pet.customer.entity.Complaint;
import com.pet.customer.mapper.ComplaintMapper;
import com.pet.operation.entity.Notification;
import com.pet.operation.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 【业务模块】投诉处理 MQ 消费者（实现）
 * 业务作用：监听 RabbitMQ complaint.process 队列，处理投诉解决事件。
 * 当投诉处理完成后，创建站内通知告知投诉人处理结果。
 */
@Component
public class ComplaintProcessHandlerImpl implements ComplaintProcessHandler {

    private static final Logger log = LoggerFactory.getLogger(ComplaintProcessHandlerImpl.class);

    private final ComplaintMapper complaintMapper;
    private final NotificationService notificationService;

    public ComplaintProcessHandlerImpl(ComplaintMapper complaintMapper,
                                       NotificationService notificationService) {
        this.complaintMapper = complaintMapper;
        this.notificationService = notificationService;
    }

    /**
     * 【业务名称】处理投诉事件
     * 业务作用：处理投诉记录，向投诉人发送站内通知。
     * 调用场景：RabbitMQ 异步消费投诉处理消息。
     * 调用链：handle() → selectById() → create() 通知。
     * 数据处理：查询投诉记录 → 创建站内通知写入。
     * 业务规则：投诉ID为空或投诉不存在时静默跳过；投诉无主人时跳过。
     * 状态影响：新增一条站内通知记录。
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

        if (complaint.getOwner_id_wsh() == null) {
            log.warn("投诉 {} 没有所有者, 跳过通知", complaintId);
            return;
        }

        try {
            Notification notification = new Notification();
            notification.setUser_id_wsh(complaint.getOwner_id_wsh());
            notification.setTitle_wsh("Complaint processed");
            notification.setContent_wsh("Complaint \"" + complaint.getTitle_wsh()
                    + "\" has been " + complaint.getStatus_wsh()
                    + (complaint.getResult_wsh() != null ? ": " + complaint.getResult_wsh() : ""));
            notification.setType_wsh("complaint");
            notification.setRelated_id_wsh(complaintId);
            notification.setIs_read_wsh(0);
            notificationService.create(notification);
            log.info("投诉 {} 的通知已发送", complaintId);
        } catch (Exception e) {
            log.warn("为投诉 {} 发送通知失败", complaintId, e);
        }
    }
}
