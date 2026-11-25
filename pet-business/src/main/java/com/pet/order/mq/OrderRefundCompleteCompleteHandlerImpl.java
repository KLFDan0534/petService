package com.pet.order.mq;

import com.pet.common.RefundStatus;
import com.pet.customer.mapper.MqConsumeLogMapper;
import com.pet.marketing.service.CouponService;
import com.pet.membership.service.MembershipBenefitService;
import com.pet.mq.handler.OrderRefundCompleteHandler;
import com.pet.operation.entity.Notification;
import com.pet.operation.service.NotificationService;
import com.pet.order.entity.PetOrder;
import com.pet.order.entity.Refund;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.mapper.RefundMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

/**
 * 【业务模块】退款完成 MQ 消费者（实现）
 *
 * 业务作用：消费 order.refund 队列，处理「退款已完成」之后的后续动作。
 *
 * 调用链：
 * RefundServiceImpl.completeRefund()
 *   ↓ 更新 refund → completed、order → refunded 之后
 * MessageSender.sendOrderRefund(refundId)
 *   ↓
 * MessageListener.handleOrderRefund()
 *   ↓
 * 本类 handle(refundId)
 *
 * 注意事项：
 * - 消息载荷是「退款单 ID」的字符串形式，不是订单号、也不是订单 ID。
 * - 到达本类时退款单已经是 completed 状态，本类只做后续动作，不重复改状态。
 * - 本类由 pet-business 实现，接口在 pet-framework 的 com.pet.mq.handler，运行时由 Spring 注入。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderRefundCompleteCompleteHandlerImpl implements OrderRefundCompleteHandler {

    /** 消费台账里的消费者标识，与队列语义对齐 */
    private static final String CONSUMER = "refund-complete";

    private final RefundMapper refundMapper;
    private final MqConsumeLogMapper consumeLogMapper;
    private final OrderMapper orderMapper;
    private final NotificationService notificationService;
    private final CouponService couponService;
    private final MembershipBenefitService membershipBenefitService;


    /**
     * 处理退款完成事件
     *
     * 业务作用：退款完成后执行后续动作（通知用户、返还优惠券、写审计日志等）。
     * 业务规则：退款单不存在或状态不是 completed 时静默跳过；同一退款单的完成事件只处理一次。
     * 异常情况：后续处理失败仅记录日志并释放幂等占位，不向监听器抛异常。
     * 注意事项：本方法不抛异常——order.refund 没有死信队列，异常会被 nack 丢弃，消息直接消失。
     */
    @Override
    public void handle(String refundIdStr) {

        if (refundIdStr == null || refundIdStr.isBlank()) {
            log.warn("收到空的退款消息载荷");
            return;
        }

        Long refundId;
        try {
            refundId = Long.valueOf(refundIdStr.trim());
        } catch (NumberFormatException e) {
            log.warn("退款消息载荷不是合法 ID: {}", refundIdStr);
            return;
        }

        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            log.warn("退款单不存在，消息已跳过: refundId={}", refundId);
            return;
        }

        // 正常链路下 completeRefund 先把退款单更新为 completed 再发消息；
        // 若不是 completed，说明消息来自异常时序，不做后续处理。
        if (!RefundStatus.COMPLETED.equals(refund.getStatus_wsh())) {
            log.warn("退款单 {} 状态为 {}，非 completed，跳过后续处理", refundId, refund.getStatus_wsh());
            return;
        }

        PetOrder petOrder = orderMapper.selectById(refund.getOrder_id_wsh());
        if (petOrder == null){
            log.warn("订单不存在，消息已跳过: orderId={}", refund.getOrder_id_wsh());
            return;
        }

        String eventKey = refundId + ":completed";
        if (!tryClaim(eventKey)) {
            log.info("退款 {} 的完成事件已处理过，跳过重复消费", refundId);
            return;
        }

        Long userId = petOrder.getOwner_id_wsh();

        // 返券与通知是两件独立的事，必须各自兜住：
        // 返券失败不能让用户连退款通知都收不到，反之亦然。
        try {
            if (petOrder.getCoupon_id_wsh() != null) {
                couponService.returnCoupon(petOrder.getCoupon_id_wsh(), userId);
            }
        } catch (Exception e) {
            // order.refund 没有死信队列，消息失败即丢失，这里只能留 ERROR 日志供人工补偿
            log.error("退款 {} 返还优惠券失败，需人工补偿: orderId={}, couponId={}",
                    refundId, petOrder.getId_wsh(), petOrder.getCoupon_id_wsh(), e);
        }

        // 会员折扣对应的权益同样要释放，否则用户退款后额度仍被占用
        try {
            membershipBenefitService.releaseForOrder(petOrder.getId_wsh());
        } catch (Exception e) {
            log.error("退款 {} 释放会员权益失败，需人工补偿: orderId={}", refundId, petOrder.getId_wsh(), e);
        }

        try {
            // 发送消息通知用户 通知退款成功
            Notification notification = new Notification();
            notification.setUser_id_wsh(userId);
            notification.setTitle_wsh("退款完成通知");
            notification.setContent_wsh("您的退款「" + (refund.getReason_wsh() != null ? refund.getReason_wsh() : (refund.getOrder_no_wsh() != null ? refund.getOrder_no_wsh() : "")) + "」已完成，请登录网站查看详细信息。");
            notification.setType_wsh("order"); // order/payment/ticket/system/order_fulfillment
            notification.setRelated_id_wsh(refundId);
            notification.setIs_read_wsh(0);
            notification.setCreated_at_wsh(java.time.LocalDateTime.now());
            notification.setDeleted_wsh(0);
            notificationService.create(notification);
        } catch (Exception e) {
            releaseClaim(eventKey);
            log.warn("退款 {} 后续处理失败，已释放幂等占位以便重投", refundId, e);
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
}
