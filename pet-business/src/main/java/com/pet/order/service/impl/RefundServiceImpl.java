package com.pet.order.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.common.OrderStatus;
import com.pet.common.RefundStatus;
import com.pet.order.entity.PetOrder;
import com.pet.order.entity.Refund;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.mapper.RefundMapper;
import com.pet.order.service.RefundService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 退款服务实现类
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Service
@Slf4j
public class RefundServiceImpl implements RefundService {

    private final RefundMapper refundMapper;
    private final OrderMapper orderMapper;

    public RefundServiceImpl(RefundMapper refundMapper, OrderMapper orderMapper) {
        this.refundMapper = refundMapper;
        this.orderMapper = orderMapper;
    }

    /**
     * 获取所有退款列表
     * @return 退款列表
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Override
    public List<Refund> listAll() {
        log.info("调用 listAll()");
        return refundMapper.selectList(
                new LambdaQueryWrapper<Refund>()
                        .orderByDesc(Refund::getCreated_at_wsh));
    }

    /**
     * 根据主人ID获取退款列表
     * @param ownerId 主人ID
     * @return 退款列表
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
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

    /**
     * 根据订单ID创建退款申请
     * @param ownerId 主人ID
     * @param orderId 订单ID
     * @param reason 退款原因
     * @return 创建的退款记录
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Transactional
    @Override
    public Refund createRefundByOrderId(Long ownerId, Long orderId, String reason) {
        log.info("调用 createRefundByOrderId()");
        PetOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getOwner_id_wsh().equals(ownerId)) throw new BusinessException("无权操作此订单");
        return createRefund(ownerId, order.getOrder_no_wsh(), reason);
    }

    /**
     * 根据订单编号创建退款申请
     * @param ownerId 主人ID
     * @param orderNo 订单编号
     * @param reason 退款原因
     * @return 创建的退款记录
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Transactional
    @Override
    public Refund createRefund(Long ownerId, String orderNo, String reason) {
        log.info("调用 createRefund()");
        PetOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getOrder_no_wsh, orderNo)
                        .eq(PetOrder::getOwner_id_wsh, ownerId));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!OrderStatus.PAID.equals(order.getStatus_wsh()) && !OrderStatus.IN_PROGRESS.equals(order.getStatus_wsh())) {
            throw new BusinessException("当前订单状态不可退款");
        }
        Refund existing = refundMapper.selectOne(
                new LambdaQueryWrapper<Refund>()
                        .eq(Refund::getOrder_no_wsh, orderNo)
                        .in(Refund::getStatus_wsh, RefundStatus.PENDING, RefundStatus.APPROVED));
        if (existing != null) {
            throw new BusinessException("已有退款申请在处理中");
        }
        order.setStatus_wsh("refunding");
        orderMapper.updateById(order);

        Refund refund = new Refund();
        refund.setOrder_id_wsh(order.getId_wsh());
        refund.setOrder_no_wsh(orderNo);
        refund.setAmount_wsh(order.getFinal_amount_wsh());
        refund.setReason_wsh(reason);
        refund.setStatus_wsh(RefundStatus.PENDING);
        refundMapper.insert(refund);
        return refund;
    }

    /**
     * 审批通过退款
     * @param refundId 退款ID
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Transactional
    @Override
    public void approveRefund(Long refundId) {
        log.info("调用 approveRefund()");
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException("退款申请不存在");
        }
        if (!RefundStatus.PENDING.equals(refund.getStatus_wsh())) {
            throw new BusinessException("当前状态不可审核");
        }
        refund.setStatus_wsh(RefundStatus.APPROVED);
        refundMapper.updateById(refund);
    }

    /**
     * 完成退款
     * @param refundId 退款ID
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Transactional
    @Override
    public void completeRefund(Long refundId) {
        log.info("调用 completeRefund()");
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException("退款申请不存在");
        }
        if (!RefundStatus.APPROVED.equals(refund.getStatus_wsh())) {
            throw new BusinessException("当前状态不可完成退款");
        }
        refund.setStatus_wsh(RefundStatus.COMPLETED);
        refundMapper.updateById(refund);

        PetOrder order = orderMapper.selectById(refund.getOrder_id_wsh());
        if (order != null) {
            order.setStatus_wsh("refunded");
            orderMapper.updateById(order);
        }
    }

    /**
     * 驳回退款申请
     * @param refundId 退款ID
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Transactional
    @Override
    public void rejectRefund(Long refundId) {
        log.info("调用 rejectRefund()");
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException("退款申请不存在");
        }
        if (!RefundStatus.PENDING.equals(refund.getStatus_wsh())) {
            throw new BusinessException("当前状态不可驳回");
        }
        refund.setStatus_wsh(RefundStatus.REJECTED);
        refundMapper.updateById(refund);

        PetOrder order = orderMapper.selectById(refund.getOrder_id_wsh());
        if (order != null) {
            order.setStatus_wsh(OrderStatus.PAID);
            orderMapper.updateById(order);
        }
    }
}
