package com.pet.membership.service;

import com.pet.membership.dto.MembershipDiscountDTO;
import com.pet.membership.entity.MembershipBenefitUsage;

import java.math.BigDecimal;
import java.util.List;

public interface MembershipBenefitService {
    MembershipDiscountDTO previewOrderDiscount(Long userId, BigDecimal baseAmount);

    void lockForOrder(Long userId, MembershipDiscountDTO discount, Long orderId, String orderNo);

    void markUsedForOrder(Long orderId, String orderNo);

    void releaseForOrder(Long orderId);

    List<MembershipBenefitUsage> listUsageByUser(Long userId);

    List<MembershipBenefitUsage> listUsageForAdmin(Long userId);
}
