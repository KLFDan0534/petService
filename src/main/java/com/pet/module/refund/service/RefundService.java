package com.pet.module.refund.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.module.order.entity.PetOrder;
import com.pet.module.order.mapper.OrderMapper;
import com.pet.module.refund.entity.Refund;
import com.pet.module.refund.mapper.RefundMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RefundService {

    private final RefundMapper refundMapper;
    private final OrderMapper orderMapper;

    public RefundService(RefundMapper refundMapper, OrderMapper orderMapper) {
        this.refundMapper = refundMapper;
        this.orderMapper = orderMapper;
    }

    public List<Refund> listByOwner(Long ownerId) {
        List<PetOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getOwnerId, ownerId)
                        .select(PetOrder::getId));
        if (orders.isEmpty()) return List.of();
        List<Long> orderIds = orders.stream().map(PetOrder::getId).collect(Collectors.toList());
        return refundMapper.selectList(
                new LambdaQueryWrapper<Refund>()
                        .in(Refund::getOrderId, orderIds)
                        .orderByDesc(Refund::getCreatedAt));
    }

    @Transactional
    public Refund createRefund(Long ownerId, String orderNo, String reason) {
        PetOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getOrderNo, orderNo)
                        .eq(PetOrder::getOwnerId, ownerId));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"paid".equals(order.getStatus()) && !"in_progress".equals(order.getStatus())) {
            throw new BusinessException("当前订单状态不可退款");
        }
        Refund existing = refundMapper.selectOne(
                new LambdaQueryWrapper<Refund>()
                        .eq(Refund::getOrderNo, orderNo)
                        .in(Refund::getStatus, "pending", "approved"));
        if (existing != null) {
            throw new BusinessException("已有退款申请在处理中");
        }
        order.setStatus("refunding");
        orderMapper.updateById(order);

        Refund refund = new Refund();
        refund.setOrderId(order.getId());
        refund.setOrderNo(orderNo);
        refund.setAmount(order.getFinalAmount());
        refund.setReason(reason);
        refund.setStatus("pending");
        refundMapper.insert(refund);
        return refund;
    }

    @Transactional
    public void approveRefund(Long refundId) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException("退款申请不存在");
        }
        if (!"pending".equals(refund.getStatus())) {
            throw new BusinessException("当前状态不可审批");
        }
        refund.setStatus("approved");
        refundMapper.updateById(refund);
    }

    @Transactional
    public void completeRefund(Long refundId) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException("退款申请不存在");
        }
        if (!"approved".equals(refund.getStatus())) {
            throw new BusinessException("当前状态不可完成退款");
        }
        refund.setStatus("completed");
        refundMapper.updateById(refund);

        PetOrder order = orderMapper.selectById(refund.getOrderId());
        if (order != null) {
            order.setStatus("refunded");
            orderMapper.updateById(order);
        }
    }

    @Transactional
    public void rejectRefund(Long refundId) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException("退款申请不存在");
        }
        if (!"pending".equals(refund.getStatus())) {
            throw new BusinessException("当前状态不可驳回");
        }
        refund.setStatus("rejected");
        refundMapper.updateById(refund);

        PetOrder order = orderMapper.selectById(refund.getOrderId());
        if (order != null) {
            order.setStatus("paid");
            orderMapper.updateById(order);
        }
    }
}
