package com.pet.order.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.common.OrderStatus;
import com.pet.order.entity.Payment;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.mapper.PaymentMapper;
import com.pet.order.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 支付服务实现类
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;

    public PaymentServiceImpl(PaymentMapper paymentMapper, OrderMapper orderMapper) {
        this.paymentMapper = paymentMapper;
        this.orderMapper = orderMapper;
    }

    /**
     * 根据订单编号获取支付记录
     * @param orderNo 订单编号
     * @return 支付记录
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Override
    public Payment getByOrderNo(String orderNo) {
        log.info("调用 getByOrderNo()");
        return paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getOrder_no_wsh, orderNo));
    }

    /**
     * 根据用户ID获取支付记录列表
     * @param userId 用户ID
     * @return 支付记录列表
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Override
    public List<Payment> listByUser(Long userId) {
        log.info("调用 listByUser()");
        List<PetOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getOwner_id_wsh, userId)
                        .select(PetOrder::getId_wsh));
        if (orders.isEmpty()) return List.of();
        List<Long> orderIds = orders.stream().map(PetOrder::getId_wsh).collect(Collectors.toList());
        return paymentMapper.selectList(
                new LambdaQueryWrapper<Payment>()
                        .in(Payment::getOrder_id_wsh, orderIds)
                        .orderByDesc(Payment::getCreated_at_wsh));
    }

    /**
     * 根据订单ID创建支付
     * @param ownerId 主人ID
     * @param orderId 订单ID
     * @param method 支付方式
     * @return 创建的支付记录
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Transactional
    @Override
    public Payment createPaymentByOrderId(Long ownerId, Long orderId, String method) {
        log.info("调用 createPaymentByOrderId()");
        PetOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getOwner_id_wsh().equals(ownerId)) throw new BusinessException("无权操作此订单");
        return createPayment(ownerId, order.getOrder_no_wsh(), method);
    }

    /**
     * 根据订单编号创建支付
     * @param ownerId 主人ID
     * @param orderNo 订单编号
     * @param method 支付方式
     * @return 创建的支付记录
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Transactional
    @Override
    public Payment createPayment(Long ownerId, String orderNo, String method) {
        log.info("调用 createPayment()");
        PetOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getOrder_no_wsh, orderNo)
                        .eq(PetOrder::getOwner_id_wsh, ownerId));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (OrderStatus.PAID.equals(order.getStatus_wsh())) {
            throw new BusinessException("订单已支付");
        }
        if (!OrderStatus.PENDING.equals(order.getStatus_wsh())) {
            throw new BusinessException("当前订单状态不可支付");
        }
        Payment existing = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrder_no_wsh, orderNo)
                        .eq(Payment::getStatus_wsh, "pending"));
        if (existing != null) {
            return existing;
        }
        Payment payment = new Payment();
        payment.setOrder_id_wsh(order.getId_wsh());
        payment.setOrder_no_wsh(orderNo);
        payment.setPay_no_wsh("PAY" + UUID.randomUUID().toString().substring(0, 16).toUpperCase());
        payment.setAmount_wsh(order.getFinal_amount_wsh());
        payment.setMethod_wsh(method);
        payment.setStatus_wsh("pending");
        paymentMapper.insert(payment);
        return payment;
    }

    /**
     * 执行支付
     * @param payNo 支付编号
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Transactional
    @Override
    public void pay(String payNo) {
        log.info("调用 pay()");
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getPay_no_wsh, payNo));
        if (payment == null) {
            throw new BusinessException("支付记录不存在");
        }
        if (!"pending".equals(payment.getStatus_wsh())) {
            throw new BusinessException("已支付");
        }
        payment.setStatus_wsh("success");
        payment.setPaid_at_wsh(LocalDateTime.now());
        paymentMapper.updateById(payment);

        PetOrder order = orderMapper.selectById(payment.getOrder_id_wsh());
        if (order != null) {
            order.setStatus_wsh(OrderStatus.PAID);
            orderMapper.updateById(order);
        }
    }
}
