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

    @Override
    public List<Refund> listAll() {
        log.info("调用 listAll()");
        return refundMapper.selectList(
                new LambdaQueryWrapper<Refund>().orderByDesc(Refund::getCreated_at_wsh));
    }

    @Override
    public List<Refund> listByOwner(Long ownerId) {
        log.info("调用 listByOwner()");
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

    @Transactional
    @Override
    public Refund createRefundByOrderId(Long ownerId, Long orderId, String reason) {
        log.info("调用 createRefundByOrderId()");
        PetOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException(404, "订单不存在");
        if (!ownerId.equals(order.getOwner_id_wsh())) throw new BusinessException(403, "无权操作此订单");
        return createRefund(ownerId, order.getOrder_no_wsh(), reason);
    }

    @Transactional
    @Override
    public Refund createRefund(Long ownerId, String orderNo, String reason) {
        log.info("调用 createRefund()");
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

    @Transactional
    @Override
    public void approveRefund(Long refundId) {
        log.info("调用 approveRefund()");
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

    @Transactional
    @Override
    public void completeRefund(Long refundId) {
        log.info("调用 completeRefund()");
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

    @Transactional
    @Override
    public void rejectRefund(Long refundId) {
        log.info("调用 rejectRefund()");
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

    private Refund getById(Long refundId) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) throw new BusinessException(404, "退款申请不存在");
        return refund;
    }

    private String resolveStatusBeforeRefund(Refund refund) {
        String status = refund.getOrder_status_before_refund_wsh();
        return REFUNDABLE_ORDER_STATUSES.contains(status) ? status : OrderStatus.PAID;
    }

    private BigDecimal defaultMoney(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
