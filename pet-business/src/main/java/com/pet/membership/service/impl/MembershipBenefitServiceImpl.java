package com.pet.membership.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.membership.dto.MembershipDiscountDTO;
import com.pet.membership.entity.MemberPlan;
import com.pet.membership.entity.MembershipBenefitUsage;
import com.pet.membership.entity.UserMembership;
import com.pet.membership.mapper.MemberPlanMapper;
import com.pet.membership.mapper.MembershipBenefitUsageMapper;
import com.pet.membership.mapper.UserMembershipMapper;
import com.pet.membership.service.MembershipBenefitService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MembershipBenefitServiceImpl implements MembershipBenefitService {
    private static final String STATUS_ACTIVE = "active";
    private static final String USAGE_LOCKED = "locked";
    private static final String USAGE_USED = "used";
    private static final String USAGE_RELEASED = "released";
    private static final String BENEFIT_DISCOUNT = "discount";
    private static final String BENEFIT_ORDER_DISCOUNT = "order_discount";
    private static final String BUSINESS_PET_ORDER = "pet_order";

    private final UserMembershipMapper userMembershipMapper;
    private final MemberPlanMapper memberPlanMapper;
    private final MembershipBenefitUsageMapper membershipBenefitUsageMapper;
    private final ObjectMapper objectMapper;

    public MembershipBenefitServiceImpl(UserMembershipMapper userMembershipMapper,
                                        MemberPlanMapper memberPlanMapper,
                                        MembershipBenefitUsageMapper membershipBenefitUsageMapper,
                                        ObjectMapper objectMapper) {
        this.userMembershipMapper = userMembershipMapper;
        this.memberPlanMapper = memberPlanMapper;
        this.membershipBenefitUsageMapper = membershipBenefitUsageMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public MembershipDiscountDTO previewOrderDiscount(Long userId, BigDecimal baseAmount) {
        BigDecimal amount = money(baseAmount);
        MembershipDiscountDTO empty = emptyResult(amount);
        if (userId == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return empty;
        }
        UserMembership membership = findActiveMembership(userId);
        if (membership == null) {
            return empty;
        }
        MemberPlan plan = membership.getPlan_id_wsh() == null ? null : memberPlanMapper.selectById(membership.getPlan_id_wsh());
        BigDecimal rate = normalizeRate(resolveDiscountRate(membership, plan));
        BigDecimal discount = amount.multiply(BigDecimal.ONE.subtract(rate)).setScale(2, RoundingMode.HALF_UP);
        if (discount.compareTo(amount) > 0) {
            discount = amount;
        }
        MembershipDiscountDTO dto = new MembershipDiscountDTO();
        dto.setEligible_wsh(true);
        dto.setMembership_id_wsh(membership.getId_wsh());
        dto.setPlan_id_wsh(membership.getPlan_id_wsh());
        dto.setPlan_code_wsh(membership.getPlan_code_wsh());
        dto.setPlan_name_wsh(resolvePlanName(membership, plan));
        dto.setLevel_wsh(membership.getLevel_wsh());
        dto.setDiscount_rate_wsh(rate);
        dto.setBase_amount_wsh(amount);
        dto.setMembership_discount_wsh(discount);
        dto.setFinal_amount_wsh(amount.subtract(discount).setScale(2, RoundingMode.HALF_UP));
        dto.setSnapshot_wsh(toSnapshot(dto));
        return dto;
    }

    @Override
    @Transactional
    public void lockForOrder(Long userId, MembershipDiscountDTO discount, Long orderId, String orderNo) {
        if (!hasDiscount(discount) || orderId == null) {
            return;
        }
        String requestId = requestId(orderId);
        MembershipBenefitUsage existing = findByRequestId(requestId);
        if (existing != null) {
            return;
        }
        MembershipBenefitUsage usage = new MembershipBenefitUsage();
        usage.setUser_id_wsh(userId);
        usage.setMembership_id_wsh(discount.getMembership_id_wsh());
        usage.setPlan_id_wsh(discount.getPlan_id_wsh());
        usage.setBenefit_type_wsh(BENEFIT_DISCOUNT);
        usage.setBenefit_code_wsh(BENEFIT_ORDER_DISCOUNT);
        usage.setBusiness_type_wsh(BUSINESS_PET_ORDER);
        usage.setBusiness_id_wsh(String.valueOf(orderId));
        usage.setAmount_wsh(money(discount.getMembership_discount_wsh()));
        usage.setQuantity_wsh(1);
        usage.setUsage_status_wsh(USAGE_LOCKED);
        usage.setRequest_id_wsh(requestId);
        usage.setUsage_snapshot_wsh(toUsageSnapshot(discount, orderId, orderNo));
        try {
            membershipBenefitUsageMapper.insert(usage);
        } catch (DuplicateKeyException ignored) {
        }
    }

    @Override
    @Transactional
    public void markUsedForOrder(Long orderId, String orderNo) {
        if (orderId == null) {
            return;
        }
        MembershipBenefitUsage update = new MembershipBenefitUsage();
        update.setUsage_status_wsh(USAGE_USED);
        update.setUsed_at_wsh(LocalDateTime.now());
        membershipBenefitUsageMapper.update(update, new LambdaUpdateWrapper<MembershipBenefitUsage>()
                .eq(MembershipBenefitUsage::getBusiness_type_wsh, BUSINESS_PET_ORDER)
                .eq(MembershipBenefitUsage::getBusiness_id_wsh, String.valueOf(orderId))
                .eq(MembershipBenefitUsage::getUsage_status_wsh, USAGE_LOCKED));
    }

    @Override
    @Transactional
    public void releaseForOrder(Long orderId) {
        if (orderId == null) {
            return;
        }
        MembershipBenefitUsage update = new MembershipBenefitUsage();
        update.setUsage_status_wsh(USAGE_RELEASED);
        membershipBenefitUsageMapper.update(update, new LambdaUpdateWrapper<MembershipBenefitUsage>()
                .eq(MembershipBenefitUsage::getBusiness_type_wsh, BUSINESS_PET_ORDER)
                .eq(MembershipBenefitUsage::getBusiness_id_wsh, String.valueOf(orderId))
                .in(MembershipBenefitUsage::getUsage_status_wsh, List.of(USAGE_LOCKED, USAGE_USED)));
    }

    @Override
    public List<MembershipBenefitUsage> listUsageByUser(Long userId) {
        return membershipBenefitUsageMapper.selectList(new LambdaQueryWrapper<MembershipBenefitUsage>()
                .eq(MembershipBenefitUsage::getUser_id_wsh, userId)
                .orderByDesc(MembershipBenefitUsage::getCreated_at_wsh)
                .orderByDesc(MembershipBenefitUsage::getId_wsh));
    }

    @Override
    public List<MembershipBenefitUsage> listUsageForAdmin(Long userId) {
        return membershipBenefitUsageMapper.selectList(new LambdaQueryWrapper<MembershipBenefitUsage>()
                .eq(userId != null, MembershipBenefitUsage::getUser_id_wsh, userId)
                .orderByDesc(MembershipBenefitUsage::getCreated_at_wsh)
                .orderByDesc(MembershipBenefitUsage::getId_wsh));
    }

    private UserMembership findActiveMembership(Long userId) {
        return userMembershipMapper.selectOne(new LambdaQueryWrapper<UserMembership>()
                .eq(UserMembership::getUser_id_wsh, userId)
                .eq(UserMembership::getStatus_wsh, STATUS_ACTIVE)
                .gt(UserMembership::getExpires_at_wsh, LocalDateTime.now())
                .last("LIMIT 1"));
    }

    private MembershipBenefitUsage findByRequestId(String requestId) {
        return membershipBenefitUsageMapper.selectOne(new LambdaQueryWrapper<MembershipBenefitUsage>()
                .eq(MembershipBenefitUsage::getRequest_id_wsh, requestId)
                .last("LIMIT 1"));
    }

    private boolean hasDiscount(MembershipDiscountDTO discount) {
        return discount != null
                && Boolean.TRUE.equals(discount.getEligible_wsh())
                && discount.getMembership_id_wsh() != null
                && money(discount.getMembership_discount_wsh()).compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal resolveDiscountRate(UserMembership membership, MemberPlan plan) {
        BigDecimal snapshotRate = snapshotDecimal(membership.getBenefit_snapshot_wsh(), "discount_rate_wsh");
        if (snapshotRate != null) {
            return snapshotRate;
        }
        return plan != null ? plan.getDiscount_rate_wsh() : BigDecimal.ONE;
    }

    private String resolvePlanName(UserMembership membership, MemberPlan plan) {
        String snapshotName = snapshotText(membership.getBenefit_snapshot_wsh(), "plan_name_wsh");
        if (snapshotName != null) {
            return snapshotName;
        }
        return plan != null ? plan.getName_wsh() : membership.getPlan_code_wsh();
    }

    private BigDecimal snapshotDecimal(String snapshot, String field) {
        if (snapshot == null || snapshot.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(snapshot).get(field);
            return node != null && node.isNumber() ? node.decimalValue() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String snapshotText(String snapshot, String field) {
        if (snapshot == null || snapshot.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(snapshot).get(field);
            return node != null && !node.isNull() ? node.asText() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String toSnapshot(MembershipDiscountDTO dto) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("membership_id_wsh", dto.getMembership_id_wsh());
        data.put("plan_id_wsh", dto.getPlan_id_wsh());
        data.put("plan_code_wsh", dto.getPlan_code_wsh());
        data.put("plan_name_wsh", dto.getPlan_name_wsh());
        data.put("level_wsh", dto.getLevel_wsh());
        data.put("discount_rate_wsh", dto.getDiscount_rate_wsh());
        data.put("base_amount_wsh", dto.getBase_amount_wsh());
        data.put("membership_discount_wsh", dto.getMembership_discount_wsh());
        data.put("final_amount_wsh", dto.getFinal_amount_wsh());
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String toUsageSnapshot(MembershipDiscountDTO discount, Long orderId, String orderNo) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("order_id_wsh", orderId);
        data.put("order_no_wsh", orderNo);
        data.put("benefit_wsh", discount.getSnapshot_wsh());
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    private MembershipDiscountDTO emptyResult(BigDecimal amount) {
        MembershipDiscountDTO dto = new MembershipDiscountDTO();
        dto.setEligible_wsh(false);
        dto.setDiscount_rate_wsh(BigDecimal.ONE);
        dto.setBase_amount_wsh(amount);
        dto.setMembership_discount_wsh(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        dto.setFinal_amount_wsh(amount);
        dto.setSnapshot_wsh("{}");
        return dto;
    }

    private BigDecimal normalizeRate(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String requestId(Long orderId) {
        return "membership_discount:order:" + orderId;
    }
}
