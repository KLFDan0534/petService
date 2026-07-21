package com.pet.membership.service;

import com.pet.membership.dto.MembershipOrderCreateRequestDTO;
import com.pet.membership.dto.MembershipOrderDTO;
import com.pet.membership.entity.MembershipOrder;

import java.util.List;

public interface MembershipOrderService {
    MembershipOrderDTO createOrder(Long userId, MembershipOrderCreateRequestDTO request);

    List<MembershipOrderDTO> listMyOrders(Long userId);

    List<MembershipOrderDTO> listOrdersForAdmin(String status);

    MembershipOrderDTO getMyOrder(Long userId, String orderNo);

    MembershipOrderDTO cancelPendingOrder(Long userId, String orderNo);

    MembershipOrderDTO payOrder(Long userId, String orderNo);

    MembershipOrderDTO confirmPaidForAdmin(Long operatorId, String orderNo);

    MembershipOrderDTO toDTO(MembershipOrder order);
}
