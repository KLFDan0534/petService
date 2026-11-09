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

    /**
     * 【构造方法】注入订单 Mapper 与通知服务
     *
     * 业务作用：由 Spring 注入 OrderMapper 与 NotificationService，供订单完成事件处理使用。
     *
     * 调用场景：Spring 容器启动创建该监听器 Bean 时调用。
     *
     * 调用链：Spring 容器 → 本构造方法 → 两个 final 字段赋值。
     *
     * 数据处理：仅字段赋值。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：依赖缺失时由 Spring 抛出 Bean 创建异常。
     *
     * 注意事项：依赖为 final 字段。
     */
    public OrderFeedbackNotifier(OrderMapper orderMapper, NotificationService notificationService) {
        this.orderMapper = orderMapper;
        this.notificationService = notificationService;
    }

    /**
     * 【订单完成反馈提醒】事务提交后给订单主人发一条反馈引导通知
     *
     * 业务作用：订单进入 COMPLETED 后通知宠物主去评价或投诉，保证「每个订单都有反馈」闭环。
     *
     * 调用场景：订单完成事务提交后，由 Spring 事务事件机制自动触发（@TransactionalEventListener AFTER_COMMIT）。
     *
     * 调用链：completeOrder / updateOrderStatus → publishOrderCompletedEvent → 本方法 → orderMapper.selectById / notificationService.create → SSE 推送。
     *
     * 数据处理：用 event.orderId() 查订单，取 owner_id_wsh 与 order_no_wsh，写入 notification（user_id_wsh、title_wsh="服务已完成，期待您的反馈"、content_wsh 含订单号、type_wsh=order_feedback、related_id_wsh=订单ID、is_read_wsh=0）。
     *
     * 业务规则：订单不存在或订单无宠物主时直接返回，不创建通知。
     *
     * 状态影响：写 notification 表一条记录，并由 NotificationService 触发 SSE 推送。
     *
     * 异常情况：内部 catch Exception 仅 log.warn，任何失败都不影响订单完成主流程。
     *
     * 注意事项：AFTER_COMMIT 阶段若 notificationService.create 自身带事务，其事务不会与订单事务合并；同一订单若重复发布事件会产生重复通知，本方法未做幂等判断。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCompleted(OrderCompletedEvent event) {
        try {
            PetOrder order = orderMapper.selectById(event.orderId());
            // 订单不存在或没有宠物主时跳过
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
        // 通知失败只记日志，不影响订单完成主流程
        } catch (Exception e) {
            log.warn("订单完成反馈通知发送失败，订单ID: {}", event.orderId(), e);
        }
    }
}
