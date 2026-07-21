package com.pet.marketing.service;

import com.pet.marketing.dto.CouponDiscountResult;
import com.pet.marketing.dto.CouponGrantRequestDTO;
import com.pet.marketing.dto.CouponQuoteDTO;
import com.pet.marketing.dto.CouponQuoteRequestDTO;
import com.pet.marketing.dto.CouponTemplateCreateRequestDTO;
import com.pet.marketing.dto.CouponTemplateDTO;
import com.pet.marketing.dto.UserCouponDTO;
import com.pet.marketing.entity.CouponTemplate;
import com.pet.marketing.entity.UserCoupon;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {
    List<CouponTemplateDTO> listTemplates(boolean activeOnly);

    CouponTemplate createTemplate(Long adminId, CouponTemplateCreateRequestDTO request);

    CouponTemplate updateTemplateStatus(Long templateId, Integer status);

    int grantToUser(Long adminId, Long templateId, CouponGrantRequestDTO request);

    int grantToAllUsers(Long adminId, Long templateId, CouponGrantRequestDTO request);

    int grantByCondition(Long adminId, Long templateId, CouponGrantRequestDTO request);

    UserCoupon claim(Long userId, Long templateId);

    List<UserCouponDTO> listMyCoupons(Long userId);

    List<UserCouponDTO> listAvailableCoupons(Long userId, Long merchantId, BigDecimal orderAmount);

    CouponQuoteDTO quote(Long userId, CouponQuoteRequestDTO request);

    CouponDiscountResult previewForOrder(Long userId,
                                         Long userCouponId,
                                         BigDecimal totalAmount,
                                         BigDecimal longStayDiscount,
                                         Long merchantId,
                                         Long serviceId);

    void lockForOrder(Long userId, Long userCouponId, Long orderId, String orderNo, BigDecimal discountAmount);

    void markUsedForOrder(Long orderId, String orderNo);

    void releaseForOrder(Long orderId);
}
