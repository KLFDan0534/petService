package com.pet.order.service;

import com.pet.order.dto.PaymentDTO;
import com.pet.order.entity.Payment;
import java.util.List;

public interface PaymentService {
    /**
     * 根据订单编号获取支付记录
     * @param orderNo 订单编号
     * @return 支付记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Payment getByOrderNo(Long ownerId, String orderNo);
    /**
     * 根据用户ID获取支付记录列表
     * @param userId 用户ID
     * @return 支付记录列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Payment> listByUser(Long userId);
    /**
     * 根据订单ID创建支付
     * @param ownerId 主人ID
     * @param orderId 订单ID
     * @param method 支付方式
     * @return 创建的支付记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Payment createPaymentByOrderId(Long ownerId, Long orderId, String method);
    /**
     * 根据订单编号创建支付
     * @param ownerId 主人ID
     * @param orderNo 订单编号
     * @param method 支付方式
     * @return 创建的支付记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Payment createPayment(Long ownerId, String orderNo, String method);
    /**
     * 执行支付
     * @param payNo 支付编号
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void pay(Long userId, String payNo);

    PaymentDTO toDTO(Payment entity);
}

