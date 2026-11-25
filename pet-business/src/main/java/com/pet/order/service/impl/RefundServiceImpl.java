package com.pet.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.BusinessException;
import com.pet.common.OrderStatus;
import com.pet.common.PageResult;
import com.pet.common.RefundStatus;
import com.pet.config.RabbitMQConfig;
import com.pet.finance.service.AccountingService;
import com.pet.mq.MessageSender;
import com.pet.operation.entity.Notification;
import com.pet.operation.service.NotificationService;
import com.pet.order.dto.RefundDTO;
import com.pet.order.entity.PetOrder;
import com.pet.order.entity.Refund;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.mapper.RefundMapper;
import com.pet.order.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
@RequiredArgsConstructor
@Service
@Slf4j
public class RefundServiceImpl implements RefundService {

    /**
     * 可申请全额退款的状态：只允许「已支付/已确认」且尚未接收宠物。
     * <p>
     * 宠物一旦被接收（RECEIVED/IN_PROGRESS）就存在实际交接与照护，全额退款需要
     * 另一套交接/结算流程，本期不实现，因此直接拒绝而不是留下无合法结单路径的订单。
     */
    private static final Set<String> REFUNDABLE_ORDER_STATUSES = Set.of(
            OrderStatus.PAID,
            OrderStatus.CONFIRMED);

    private final RefundMapper refundMapper;
    private final OrderMapper orderMapper;
    private final AccountingService accountingService;
    private final MessageSender messageSender;
    private final NotificationService notificationService;

/*        public RefundServiceImpl(RefundMapper refundMapper, OrderMapper orderMapper, AccountingService accountingService) {
        this.refundMapper = refundMapper;
        this.orderMapper = orderMapper;
        this.accountingService = accountingService;
    }*/

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
        log.info("查询所有退款");
        return refundMapper.selectList(
                new LambdaQueryWrapper<Refund>().orderByDesc(Refund::getCreated_at_wsh));
    }

    /**
     * 业务作用：分页查询全部退款记录，供后台审核列表使用。
     * 业务规则：按创建时间倒序；size 越界（<1 或 >100）回落到 20。
     * 注意事项：与 listAll 共用同一套排序口径。
     */
    @Override
    public PageResult<Refund> pageAll(Integer page, Integer size) {
        int current = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1 || size > 100) ? 20 : size;

        Page<Refund> result = refundMapper.selectPage(
                new Page<>(current, pageSize),
                new LambdaQueryWrapper<Refund>().orderByDesc(Refund::getCreated_at_wsh));

        PageResult<Refund> pageResult = new PageResult<>();
        pageResult.setList(result.getRecords());
        pageResult.setTotal(result.getTotal());
        pageResult.setPage(current);
        pageResult.setSize(pageSize);
        pageResult.setPages(result.getPages());
        return pageResult;
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
        log.info("查询所属用户的退款记录: {}", ownerId);
        List<PetOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getOwner_id_wsh, ownerId)
                        .select(PetOrder::getId_wsh));
        // 该用户没有订单，无需再查退款
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
        log.info("为订单创建退款: {}", orderId);
        // 退款原因是商家审核的唯一依据，空白原因直接拒绝（不依赖前端是否校验）
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isEmpty()) {
            throw new BusinessException(400, "请填写退款原因");
        }
        PetOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException(404, "订单不存在");
        if (!ownerId.equals(order.getOwner_id_wsh())) throw new BusinessException(403, "无权操作此订单");
        return createRefund(ownerId, order.getOrder_no_wsh(), normalizedReason);
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
        log.info("为订单创建退款: {}", orderNo);
        // 先加订单行锁再重读状态：与「确认接收」并发时只能一方进入
        PetOrder order = orderMapper.selectByOrderNoForUpdate(orderNo);
        if (order == null || !ownerId.equals(order.getOwner_id_wsh())) {
            throw new BusinessException(404, "订单不存在");
        }
        String statusBeforeRefund = order.getStatus_wsh();
        // 仅支付后、接收前的订单允许全额退款
        if (!REFUNDABLE_ORDER_STATUSES.contains(statusBeforeRefund)) {
            throw new BusinessException(400, "当前订单状态不可退款");
        }
        if (order.getReceived_at_wsh() != null) {
            throw new BusinessException(400, "宠物已接收，不支持全额退款");
        }
        if (order.getCheckout_confirmed_at_wsh() != null) {
            throw new BusinessException(400, "接走已确认，不支持全额退款");
        }
        // 已有待处理或已通过的退款时拒绝重复申请
        Refund existing = refundMapper.selectOne(
                new LambdaQueryWrapper<Refund>()
                        .eq(Refund::getOrder_no_wsh, orderNo)
                        .in(Refund::getStatus_wsh, RefundStatus.PENDING, RefundStatus.APPROVED)
                        .last("LIMIT 1"));
        if (existing != null) throw new BusinessException(400, "已有退款申请在处理中");

        PetOrder update = new PetOrder();
        update.setStatus_wsh(OrderStatus.REFUNDING);
        // 带状态条件更新，避免并发重复置为退款中
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
     * 【审核通过退款（实现）】
     * 业务作用：
     * 使用乐观锁将PENDING状态的退款更新为APPROVED，等待执行退款转账。
     *
     * @param refundId 退款ID
     */
    @Transactional
    @Override
    public void approveRefund(Long refundId) {
        log.info("批准退款: {}", refundId);
        Refund refund = getById(refundId);
        if (!RefundStatus.PENDING.equals(refund.getStatus_wsh())) {
            throw new BusinessException(400, "当前状态不可审核");
        }
        Refund update = new Refund();
        update.setStatus_wsh(RefundStatus.APPROVED);
        // 乐观锁更新，状态已被改动则拒绝
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
     * 执行退款转账：系统→主人转账退款金额 + 回冲平台补贴 + 标记退款COMPLETED + 标记订单REFUNDED + 发送MQ消息通知用户。
     *
     * 调用链：
     * RefundService.completeRefund()
     * ↓
     * 查询退款 → 校验状态/订单 → accountingService.transfer(退款)
     * → accountingService.debit(回冲补贴) → 乐观锁更新退款COMPLETED → 乐观锁更新订单REFUNDED → 发送MQ消息通知用户
     *
     * @param refundId 退款ID
     */
    @Transactional
    @Override
    public void completeRefund(Long refundId) {
        log.info("完成退款: {}", refundId);
        Refund refund = getById(refundId);
        if (RefundStatus.COMPLETED.equals(refund.getStatus_wsh())) return;
        if (!RefundStatus.APPROVED.equals(refund.getStatus_wsh())) {
            throw new BusinessException(400, "当前状态不可完成退款");
        }
        PetOrder order = orderMapper.selectById(refund.getOrder_id_wsh());
        if (order == null) throw new BusinessException(404, "订单不存在");
        // 订单已完成结算，不再走退款
        if (OrderStatus.COMPLETED.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "订单已完成结算，暂不支持退款");
        }

        if (!OrderStatus.REFUNDING.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "订单已经处于退款状态中");
        }


        // 退款金额口径：refund.amount_wsh 在创建退款单时就已写入 order.final_amount_wsh，
        // 而 final_amount = settlement_amount - coupon_discount - membership_discount，
        // 即「用户实付」，这里不能再减 coupon_discount 或 platform_subsidy，
        // 否则会二次扣减，导致用户少拿钱（platform_subsidy 本身就包含 coupon_discount）。
        BigDecimal refundAmount = defaultMoney(refund.getAmount_wsh());

        // 平台补贴是平台在支付时额外注入系统暂存账户的资金（PaymentServiceImpl 的 coupon_subsidy 入账），
        // 退款时必须单独撤回，不能从用户退款额里扣——那是用户实付之外的钱。
        BigDecimal subsidy = defaultMoney(order.getPlatform_subsidy_wsh());
        if (subsidy.compareTo(BigDecimal.ZERO) > 0) {
            accountingService.debit(accountingService.systemUserId(), subsidy, "coupon_subsidy_refund", order.getId_wsh(),
                "coupon", String.valueOf(order.getCoupon_id_wsh()),
                "refund:coupon-subsidy:" + refund.getId_wsh(),
                "退款回冲平台优惠券补贴 - " + order.getOrder_no_wsh(), null);
        }

        // 金额大于零才真正执行转账
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            accountingService.transfer(accountingService.systemUserId(), order.getOwner_id_wsh(),
                    refundAmount, "refund", order.getId_wsh(),
                    "refund", String.valueOf(refund.getId_wsh()),
                    "refund:complete:" + refund.getId_wsh(),
                    "订单退款 - " + order.getOrder_no_wsh());
        }


        // 更新入库，标记退款为COMPLETED
        Refund refundUpdate = new Refund();
        refundUpdate.setStatus_wsh(RefundStatus.COMPLETED);
        int refundUpdated = refundMapper.update(refundUpdate, new LambdaUpdateWrapper<Refund>()
                .eq(Refund::getId_wsh, refund.getId_wsh())
                .eq(Refund::getStatus_wsh, RefundStatus.APPROVED));
        if (refundUpdated == 0) {
            throw new BusinessException(400, "退款状态已经改变,但无法完成退款");
        }
        // 更新入库，标记订单为REFUNDED
        PetOrder orderUpdate = new PetOrder();
        orderUpdate.setStatus_wsh(OrderStatus.REFUNDED);
        int orderUpdated = orderMapper.update(orderUpdate, new LambdaUpdateWrapper<PetOrder>()
                .eq(PetOrder::getId_wsh, order.getId_wsh())
                .eq(PetOrder::getStatus_wsh, OrderStatus.REFUNDING));
        if (orderUpdated == 0) {
            throw new BusinessException(400, "订单状态已经改变,但无法完成退款");
        }
        refund.setStatus_wsh(RefundStatus.COMPLETED);
        order.setStatus_wsh(OrderStatus.REFUNDED);

        log.info("退款完成: {},开始发送mq消息", refund.getId_wsh());

        Runnable task = () -> {
            try {
                messageSender.sendOrderRefund(String.valueOf(refund.getId_wsh()));
            } catch (Exception e) {
                log.warn("发送订单退款消息失败: refundId={}", refundId, e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
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
    public void rejectRefund(Long refundId, String reason) {
        log.info("拒绝退款: {}", refundId);
        // 驳回原因是用户唯一能看到的解释，空白原因直接拒绝，不依赖前端校验
        String rejectReason = reason == null ? "" : reason.trim();
        if (rejectReason.isEmpty()) {
            throw new BusinessException(400, "请填写驳回原因");
        }
        Refund refund = getById(refundId);
        if (!RefundStatus.PENDING.equals(refund.getStatus_wsh())) {
            throw new BusinessException(400, "当前状态不可驳回");
        }
        Refund update = new Refund();
        update.setStatus_wsh(RefundStatus.REJECTED);
        update.setReject_reason_wsh(rejectReason);
        int updated = refundMapper.update(update, new LambdaUpdateWrapper<Refund>()
                .eq(Refund::getId_wsh, refund.getId_wsh())
                .eq(Refund::getStatus_wsh, RefundStatus.PENDING));
        if (updated == 0) {
            throw new BusinessException(400, "Refund status changed, cannot reject");
        }
        refund.setStatus_wsh(RefundStatus.REJECTED);
        refund.setReject_reason_wsh(rejectReason);

        PetOrder order = orderMapper.selectById(refund.getOrder_id_wsh());
        // 订单仍存在时恢复退款前的状态
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

        // 驳回是终态，用户只能通过站内信得知结果与原因；通知失败不影响驳回本身
        notifyRejected(refund, order, rejectReason);
    }

    /**
     * 驳回后退款单已进入终态，用户不会再去订单详情等结果，
     * 所以必须主动发一条站内信说明「被驳回 + 原因」，否则用户只看到状态从「退款中」跳回原状态。
     * <p>
     * 通知属于副作用：任何异常只记 warn，不向上抛，避免把已经落库的驳回回滚掉。
     */
    private void notifyRejected(Refund refund, PetOrder order, String rejectReason) {
        Long userId = order == null ? null : order.getOwner_id_wsh();
        if (userId == null) {
            log.warn("退款 {} 驳回通知跳过：订单或主人不存在", refund.getId_wsh());
            return;
        }
        Runnable task = () -> {
            try {
                Notification notification = new Notification();
                notification.setUser_id_wsh(userId);
                notification.setTitle_wsh("退款申请被驳回");
                notification.setContent_wsh("订单 " + refund.getOrder_no_wsh() + " 的退款申请未通过审核。驳回原因：" + rejectReason
                        + "。如有疑问，可在订单详情页联系客服。");
                notification.setType_wsh("order");
                notification.setRelated_id_wsh(refund.getId_wsh());
                notification.setIs_read_wsh(0);
                notificationService.create(notification);
            } catch (Exception e) {
                log.warn("发送退款驳回通知失败: refundId={}, userId={}", refund.getId_wsh(), userId, e);
            }
        };
        // 事务提交后再发，避免驳回回滚却已推送通知
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
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
        dto.setReject_reason_wsh(entity.getReject_reason_wsh());
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

    /**
     * 业务作用：把可能为 null 的金额统一按 0 处理，避免空指针与比较异常。
     * 数据处理：amount 为 null 返回 BigDecimal.ZERO，否则原样返回。
     * 注意事项：仅做 null 兜底，不做精度与正负校验。
     */
    private BigDecimal defaultMoney(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
