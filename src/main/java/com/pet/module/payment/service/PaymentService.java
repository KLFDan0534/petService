package com.pet.module.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.module.order.entity.PetOrder;
import com.pet.module.order.mapper.OrderMapper;
import com.pet.module.payment.entity.Payment;
import com.pet.module.payment.mapper.PaymentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;

    public PaymentService(PaymentMapper paymentMapper, OrderMapper orderMapper) {
        this.paymentMapper = paymentMapper;
        this.orderMapper = orderMapper;
    }

    public Payment getByOrderNo(String orderNo) {
        return paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getOrderNo, orderNo));
    }

    public List<Payment> listByUser(Long userId) {
        return paymentMapper.selectList(
                new LambdaQueryWrapper<Payment>()
                        .inSql(Payment::getOrderId,
                                "SELECT id FROM pet_order WHERE owner_id = " + userId)
                        .orderByDesc(Payment::getCreatedAt));
    }

    @Transactional
    public Payment createPayment(Long ownerId, String orderNo, String method) {
        PetOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getOrderNo, orderNo)
                        .eq(PetOrder::getOwnerId, ownerId));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"pending".equals(order.getStatus())) {
            throw new BusinessException("当前订单状态不可支付");
        }
        Payment existing = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderNo, orderNo)
                        .eq(Payment::getStatus, "pending"));
        if (existing != null) {
            return existing;
        }
        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setOrderNo(orderNo);
        payment.setPayNo("PAY" + UUID.randomUUID().toString().substring(0, 16).toUpperCase());
        payment.setAmount(order.getFinalAmount());
        payment.setMethod(method);
        payment.setStatus("pending");
        paymentMapper.insert(payment);
        return payment;
    }

    @Transactional
    public void pay(String payNo) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getPayNo, payNo));
        if (payment == null) {
            throw new BusinessException("支付记录不存在");
        }
        if (!"pending".equals(payment.getStatus())) {
            throw new BusinessException("已支付");
        }
        payment.setStatus("success");
        payment.setPaidAt(LocalDateTime.now());
        paymentMapper.updateById(payment);

        PetOrder order = orderMapper.selectById(payment.getOrderId());
        if (order != null) {
            order.setStatus("paid");
            orderMapper.updateById(order);
        }
    }
}
