package com.pet.order.service;

import com.pet.order.dto.RefundDTO;
import com.pet.order.entity.Refund;
import java.util.List;

public interface RefundService {
    /**
     * 获取所有退款列表
     * @return 退款列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Refund> listAll();
    /**
     * 根据主人ID获取退款列表
     * @param ownerId 主人ID
     * @return 退款列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Refund> listByOwner(Long ownerId);
    /**
     * 根据订单ID创建退款申请
     * @param ownerId 主人ID
     * @param orderId 订单ID
     * @param reason 退款原因
     * @return 创建的退款记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Refund createRefundByOrderId(Long ownerId, Long orderId, String reason);
    /**
     * 根据订单编号创建退款申请
     * @param ownerId 主人ID
     * @param orderNo 订单编号
     * @param reason 退款原因
     * @return 创建的退款记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Refund createRefund(Long ownerId, String orderNo, String reason);
    /**
     * 审批通过退款
     * @param refundId 退款ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void approveRefund(Long refundId);
    /**
     * 完成退款
     * @param refundId 退款ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void completeRefund(Long refundId);
    /**
     * 驳回退款申请
     * @param refundId 退款ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void rejectRefund(Long refundId);

    RefundDTO toDTO(Refund entity);
}

