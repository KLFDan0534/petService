package com.pet.order.listener;

import com.pet.operation.entity.Notification;
import com.pet.operation.service.NotificationService;
import com.pet.order.entity.PetOrder;
import com.pet.order.event.OrderCompletedEvent;
import com.pet.order.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 订单完成反馈提醒监听器。
 * <p>
 * 业务作用：订单进入 COMPLETED 后（无论走 completeOrder 还是手动状态修正），
 * 在事务提交后给订单主人发送一条站内通知，引导用户完成评价或投诉反馈，
 * 保证“每个订单都有反馈”闭环。
 * <p>
 * 调用链：completeOrder()/updateOrderStatus() → publishOrderCompletedEvent()
 * → 本监听器 afterCommit → 查订单 → notificationService.create() → SSE 推送。
 * <p>
 * 异常处理：任何异常只记录日志，不影响订单完成主流程。
 */
@Component
@Slf4j
public class OrderFeedbackNotifier {

    private final OrderMapper orderMapper;
    private final NotificationService notificationService;

    public OrderFeedbackNotifier(OrderMapper orderMapper, NotificationService notificationService) {
        this.orderMapper = orderMapper;
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCompleted(OrderCompletedEvent event) {
        try {
            PetOrder order = orderMapper.selectById(event.orderId());
            if (order == null || order.getOwner_id_wsh() == null) {
                return;
            }
            Notification notification = new Notification();
            notification.setUser_id_wsh(order.getOwner_id_wsh());
            notification.setTitle_wsh("服务已完成，期待您的反馈");
            notification.setContent_wsh("订单「" + order.getOrder_no_wsh() + "」已顺利完成，欢迎评价本次服务；如有问题可随时投诉反馈，我们会尽快处理。");
            notification.setType_wsh("order_feedback");
            notification.setRelated_id_wsh(order.getId_wsh());
            notification.setIs_read_wsh(0);
            notificationService.create(notification);
        } catch (Exception e) {
            log.warn("订单完成反馈通知发送失败，订单ID: {}", event.orderId(), e);
        }
    }
}
