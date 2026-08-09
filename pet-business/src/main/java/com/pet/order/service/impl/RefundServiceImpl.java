package com.pet.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pet.common.BusinessException;
import com.pet.common.OrderStatus;
import com.pet.common.RefundStatus;
import com.pet.finance.service.AccountingService;
import com.pet.order.dto.RefundDTO;
import com.pet.order.entity.PetOrder;
import com.pet.order.entity.Refund;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.mapper.RefundMapper;
import com.pet.order.service.RefundService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link RefundService} for refund processing.
 * <p>
 * Refund lifecycle: PENDING → APPROVED → COMPLETED (or PENDING → REJECTED).
 * Only orders in certain statuses (PAID through IN_PROGRESS) are refundable.
 * Completed refunds transfer funds from the system account back to the owner
 * and reverse platform subsidies. Rejected refunds restore the order to its
 * pre-refund status.
 */
@Service
@Slf4j
public class RefundServiceImpl implements RefundService {

    private static final Set<String> REFUNDABLE_ORDER_STATUSES = Set.of(
            OrderStatus.PAID,
            OrderStatus.CONFIRMED,
            OrderStatus.DELIVERED,
            OrderStatus.RECEIVED,
            OrderStatus.IN_PROGRESS);

    private final RefundMapper refundMapper;
    private final OrderMapper orderMapper;
    private final AccountingService accountingService;

    public RefundServiceImpl(RefundMapper refundMapper, OrderMapper orderMapper, AccountingService accountingService) {
        this.refundMapper = refundMapper;
        this.orderMapper = orderMapper;
        this.accountingService = accountingService;
    }

    /**
     * 【查询全部退款记录（实现）】
     *
     * 业务作用：
     * 按创建时间降序查询所有退款记录。
     *
     * @return 退款记录列表
     */
    @Override
    public List<Refund> listAll() {
        log.info("Query all refunds");
        return refundMapper.selectList(
                new LambdaQueryWrapper<Refund>().orderByDesc(Refund::getCreated_at_wsh));
    }

    /**
     * 【查询主人退款记录（实现）】
     *
     * 业务作用：
     * 通过主人订单ID批量查询关联退款记录。
     *
     * @param ownerId 主人ID
     * @return 退款记录列表，按创建时间降序
     */
    @Override
    public List<Refund> listByOwner(Long ownerId) {
        log.info("Query refunds for owner: {}", ownerId);
        List<PetOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getOwner_id_wsh, ownerId)
                        .select(PetOrder::getId_wsh));
        if (orders.isEmpty()) return List.of();
        List<Long> orderIds = orders.stream().map(PetOrder::getId_wsh).collect(Collectors.toList());
        return refundMapper.selectList(
                new LambdaQueryWrapper<Refund>()
                        .in(Refund::getOrder_id_wsh, orderIds)
                        .orderByDesc(Refund::getCreated_at_wsh));
    }

    /**
     * 【根据订单ID创建退款（实现）】
     *
     * 业务作用：
     * 校验订单归属后委托createRefund创建退款申请。
     *
     * @param ownerId 主人ID
     * @param orderId 订单ID
     * @param reason  退款原因
     * @return 退款记录
     */
    @Transactional
    @Override
    public Refund createRefundByOrderId(Long ownerId, Long orderId, String reason) {
        log.info("Create refund for order: {}", orderId);
        PetOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException(404, "订单不存在");
        if (!ownerId.equals(order.getOwner_id_wsh())) throw new BusinessException(403, "无权操作此订单");
        return createRefund(ownerId, order.getOrder_no_wsh(), reason);
    }

    /**
     * 【创建退款申请（实现）】
     *
     * 业务作用：
     * 核心退款创建逻辑：校验可退款状态 → 防止重复 → 标记订单REFUNDING → 保存退款前状态 → insert退款记录。
     *
     * @param ownerId 主人ID
     * @param orderNo 订单号
     * @param reason  退款原因
     * @return 退款记录
     */
    @Transactional
    @Override
    public Refund createRefund(Long ownerId, String orderNo, String reason) {
        log.info("Create refund for order: {}", orderNo);
        PetOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getOrder_no_wsh, orderNo)
                        .eq(PetOrder::getOwner_id_wsh, ownerId));
        if (order == null) throw new BusinessException(404, "订单不存在");
        String statusBeforeRefund = order.getStatus_wsh();
        if (!REFUNDABLE_ORDER_STATUSES.contains(statusBeforeRefund)) {
            throw new BusinessException(400, "当前订单状态不可退款");
        }
        Refund existing = refundMapper.selectOne(
                new LambdaQueryWrapper<Refund>()
                        .eq(Refund::getOrder_no_wsh, orderNo)
                        .in(Refund::getStatus_wsh, RefundStatus.PENDING, RefundStatus.APPROVED)
                        .last("LIMIT 1"));
        if (existing != null) throw new BusinessException(400, "已有退款申请在处理中");

        PetOrder update = new PetOrder();
        update.setStatus_wsh(OrderStatus.REFUNDING);
        int updated = orderMapper.update(update, new LambdaUpdateWrapper<PetOrder>()
                .eq(PetOrder::getId_wsh, order.getId_wsh())
                .eq(PetOrder::getStatus_wsh, statusBeforeRefund));
        if (updated == 0) {
            throw new BusinessException(400, "Order status changed, cannot create refund");
        }
        order.setStatus_wsh(OrderStatus.REFUNDING);

        Refund refund = new Refund();
        refund.setOrder_id_wsh(order.getId_wsh());
        refund.setOrder_no_wsh(orderNo);
        refund.setAmount_wsh(order.getFinal_amount_wsh());
        refund.setReason_wsh(reason);
        refund.setStatus_wsh(RefundStatus.PENDING);
        refund.setOrder_status_before_refund_wsh(statusBeforeRefund);
        refundMapper.insert(refund);
        return refund;
    }

    /**
     * Approves a pending refund application. Uses optimistic locking to ensure
     * the refund is still in PENDING status before transitioning to APPROVED.
     * <p>
     * <b>State transition:</b> Refund PENDING → APPROVED
     *
     * @param refundId the refund ID to approve
     * @throws BusinessException if refund not found or not in PENDING status
     */
    /**
     * 【审核通过退款（实现）】
     *
     * 业务作用：
     * 使用乐观锁将PENDING状态的退款更新为APPROVED，等待执行退款转账。
     *
     * @param refundId 退款ID
     */
    @Transactional
    @Override
    public void approveRefund(Long refundId) {
        log.info("Approve refund: {}", refundId);
        Refund refund = getById(refundId);
        if (!RefundStatus.PENDING.equals(refund.getStatus_wsh())) {
            throw new BusinessException(400, "当前状态不可审核");
        }
        Refund update = new Refund();
        update.setStatus_wsh(RefundStatus.APPROVED);
        int updated = refundMapper.update(update, new LambdaUpdateWrapper<Refund>()
                .eq(Refund::getId_wsh, refund.getId_wsh())
                .eq(Refund::getStatus_wsh, RefundStatus.PENDING));
        if (updated == 0) {
            throw new BusinessException(400, "Refund status changed, cannot approve");
        }
        refund.setStatus_wsh(RefundStatus.APPROVED);
    }

    /**
     * 【完成退款执行（实现）】
     *
     * 业务作用：
     * 执行退款转账：系统→主人转账退款金额 + 回冲平台补贴 + 标记退款COMPLETED + 标记订单REFUNDED。
     *
     * 调用链：
     * RefundService.completeRefund()
     * ↓
     * 查询退款 → 校验状态/订单 → accountingService.transfer(退款)
     * → accountingService.debit(回冲补贴) → 乐观锁更新退款COMPLETED → 乐观锁更新订单REFUNDED
     *
     * @param refundId 退款ID
     */
    @Transactional
    @Override
    public void completeRefund(Long refundId) {
        log.info("Complete refund: {}", refundId);
        Refund refund = getById(refundId);
        if (RefundStatus.COMPLETED.equals(refund.getStatus_wsh())) return;
        if (!RefundStatus.APPROVED.equals(refund.getStatus_wsh())) {
            throw new BusinessException(400, "当前状态不可完成退款");
        }
        PetOrder order = orderMapper.selectById(refund.getOrder_id_wsh());
        if (order == null) throw new BusinessException(404, "订单不存在");
        if (OrderStatus.COMPLETED.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "订单已完成结算，暂不支持退款");
        }

        if (!OrderStatus.REFUNDING.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "Order is not in refunding status");
        }

        BigDecimal refundAmount = defaultMoney(refund.getAmount_wsh());
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            accountingService.transfer(accountingService.systemUserId(), order.getOwner_id_wsh(),
                    refundAmount, "refund", order.getId_wsh(),
                    "refund", String.valueOf(refund.getId_wsh()),
                    "refund:complete:" + refund.getId_wsh(),
                    "订单退款 - " + order.getOrder_no_wsh());
        }

        BigDecimal subsidy = defaultMoney(order.getPlatform_subsidy_wsh());
        if (subsidy.compareTo(BigDecimal.ZERO) > 0) {
            accountingService.debit(accountingService.systemUserId(), subsidy, "coupon_subsidy_refund", order.getId_wsh(),
                    "coupon", String.valueOf(order.getCoupon_id_wsh()),
                    "refund:coupon-subsidy:" + refund.getId_wsh(),
                    "退款回冲平台优惠券补贴 - " + order.getOrder_no_wsh());
        }

        Refund refundUpdate = new Refund();
        refundUpdate.setStatus_wsh(RefundStatus.COMPLETED);
        int refundUpdated = refundMapper.update(refundUpdate, new LambdaUpdateWrapper<Refund>()
                .eq(Refund::getId_wsh, refund.getId_wsh())
                .eq(Refund::getStatus_wsh, RefundStatus.APPROVED));
        if (refundUpdated == 0) {
            throw new BusinessException(400, "Refund status changed, cannot complete");
        }
        PetOrder orderUpdate = new PetOrder();
        orderUpdate.setStatus_wsh(OrderStatus.REFUNDED);
        int orderUpdated = orderMapper.update(orderUpdate, new LambdaUpdateWrapper<PetOrder>()
                .eq(PetOrder::getId_wsh, order.getId_wsh())
                .eq(PetOrder::getStatus_wsh, OrderStatus.REFUNDING));
        if (orderUpdated == 0) {
            throw new BusinessException(400, "Order status changed, cannot complete refund");
        }
        refund.setStatus_wsh(RefundStatus.COMPLETED);
        order.setStatus_wsh(OrderStatus.REFUNDED);
    }

    /**
     * 【驳回退款申请（实现）】
     *
     * 业务作用：
     * 驳回退款申请，恢复订单状态到退款前的状态（order_status_before_refund）。
     *
     * 调用链：
     * RefundService.rejectRefund()
     * ↓
     * 查询退款 → 乐观锁更新退款REJECTED → 查询订单 → 恢复订单为退款前状态
     *
     * @param refundId 退款ID
     */
    @Transactional
    @Override
    public void rejectRefund(Long refundId) {
        log.info("Reject refund: {}", refundId);
        Refund refund = getById(refundId);
        if (!RefundStatus.PENDING.equals(refund.getStatus_wsh())) {
            throw new BusinessException(400, "当前状态不可驳回");
        }
        Refund update = new Refund();
        update.setStatus_wsh(RefundStatus.REJECTED);
        int updated = refundMapper.update(update, new LambdaUpdateWrapper<Refund>()
                .eq(Refund::getId_wsh, refund.getId_wsh())
                .eq(Refund::getStatus_wsh, RefundStatus.PENDING));
        if (updated == 0) {
            throw new BusinessException(400, "Refund status changed, cannot reject");
        }
        refund.setStatus_wsh(RefundStatus.REJECTED);

        PetOrder order = orderMapper.selectById(refund.getOrder_id_wsh());
        if (order != null) {
            String restoredStatus = resolveStatusBeforeRefund(refund);
            PetOrder orderUpdate = new PetOrder();
            orderUpdate.setStatus_wsh(restoredStatus);
            int orderUpdated = orderMapper.update(orderUpdate, new LambdaUpdateWrapper<PetOrder>()
                    .eq(PetOrder::getId_wsh, order.getId_wsh())
                    .eq(PetOrder::getStatus_wsh, OrderStatus.REFUNDING));
            if (orderUpdated == 0) {
                throw new BusinessException(400, "Order status changed, cannot reject refund");
            }
            order.setStatus_wsh(restoredStatus);
        }
    }

    /**
     * Converts a Refund entity to a RefundDTO with all relevant fields.
     *
     * @param entity the Refund entity, may be null
     * @return the corresponding RefundDTO, or null if input is null
     */
    @Override
    public RefundDTO toDTO(Refund entity) {
        if (entity == null) return null;
        RefundDTO dto = new RefundDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setOrder_id_wsh(entity.getOrder_id_wsh());
        dto.setOrder_no_wsh(entity.getOrder_no_wsh());
        dto.setAmount_wsh(entity.getAmount_wsh());
        dto.setReason_wsh(entity.getReason_wsh());
        dto.setStatus_wsh(entity.getStatus_wsh());
        dto.setOrder_status_before_refund_wsh(entity.getOrder_status_before_refund_wsh());
        dto.setCreated_at_wsh(entity.getCreated_at_wsh());
        return dto;
    }

    /**
     * Retrieves a refund by ID, throwing an exception if not found.
     *
     * @param refundId the refund ID
     * @return the Refund entity
     * @throws BusinessException if not found (404)
     */
    private Refund getById(Long refundId) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) throw new BusinessException(404, "退款申请不存在");
        return refund;
    }

    /**
     * Resolves the order status to restore after a refund is rejected.
     * Uses the saved {@code order_status_before_refund} if it's a valid
     * refundable status, otherwise falls back to PAID.
     *
     * @param refund the refund entity with the saved pre-refund status
     * @return the status to restore on the order
     */
    private String resolveStatusBeforeRefund(Refund refund) {
        String status = refund.getOrder_status_before_refund_wsh();
        return REFUNDABLE_ORDER_STATUSES.contains(status) ? status : OrderStatus.PAID;
    }

    private BigDecimal defaultMoney(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
