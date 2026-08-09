package com.pet.membership.service;

import com.pet.membership.dto.MembershipDiscountDTO;
import com.pet.membership.entity.MembershipBenefitUsage;

import java.math.BigDecimal;
import java.util.List;

public interface MembershipBenefitService {

    /**
     * Previews the membership discount applicable to a given order amount.
     * Calculates the discount rate, discount amount, and final amount for the user's
     * current active membership. Returns an ineligible result if the user has no
     * active membership or the amount is invalid.
     *
     * @param userId      the user ID
     * @param baseAmount  the original order amount before discount
     * @return the discount preview DTO with eligibility and calculated values
     */
    MembershipDiscountDTO previewOrderDiscount(Long userId, BigDecimal baseAmount);

    /**
     * Locks the membership benefit for a specific order, creating a usage record
     * in "locked" status. This prevents the same discount from being consumed by
     * multiple orders. Idempotent — safe to call multiple times for the same order.
     *
     * @param userId   the user ID
     * @param discount the discount details from preview
     * @param orderId  the order ID
     * @param orderNo  the order number
     */
    void lockForOrder(Long userId, MembershipDiscountDTO discount, Long orderId, String orderNo);

    /**
     * Marks the membership benefit as used for the given completed order.
     * Transitions the usage record from "locked" to "used" status.
     *
     * @param orderId the order ID
     * @param orderNo the order number
     */
    void markUsedForOrder(Long orderId, String orderNo);

    /**
     * Releases (cancels) the membership benefit lock for a cancelled or failed order.
     * Transitions the usage record to "released" status so the benefit can be reused.
     *
     * @param orderId the order ID
     */
    void releaseForOrder(Long orderId);

    /**
     * Lists benefit usage records for the given user, newest first.
     *
     * @param userId the user ID
     * @return list of benefit usage records
     */
    List<MembershipBenefitUsage> listUsageByUser(Long userId);

    /**
     * Lists benefit usage records for admin management, optionally filtered by user.
     *
     * @param userId optional user ID filter; null returns all records
     * @return list of benefit usage records
     */
    List<MembershipBenefitUsage> listUsageForAdmin(Long userId);
}
