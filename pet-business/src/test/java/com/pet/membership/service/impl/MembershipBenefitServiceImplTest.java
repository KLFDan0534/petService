package com.pet.membership.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.membership.dto.MembershipDiscountDTO;
import com.pet.membership.entity.MemberPlan;
import com.pet.membership.entity.MembershipBenefitUsage;
import com.pet.membership.entity.UserMembership;
import com.pet.membership.mapper.MemberPlanMapper;
import com.pet.membership.mapper.MembershipBenefitUsageMapper;
import com.pet.membership.mapper.UserMembershipMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipBenefitServiceImplTest {

    @Mock private UserMembershipMapper userMembershipMapper;
    @Mock private MemberPlanMapper memberPlanMapper;
    @Mock private MembershipBenefitUsageMapper membershipBenefitUsageMapper;

    private MembershipBenefitServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MembershipBenefitServiceImpl(
                userMembershipMapper,
                memberPlanMapper,
                membershipBenefitUsageMapper,
                new ObjectMapper());
    }

    @Test
    void previewOrderDiscountReturnsEmptyWhenUserHasNoActiveMembership() {
        when(userMembershipMapper.selectOne(any())).thenReturn(null);

        MembershipDiscountDTO dto = service.previewOrderDiscount(7L, new BigDecimal("100.00"));

        assertFalse(dto.getEligible_wsh());
        assertEquals(new BigDecimal("0.00"), dto.getMembership_discount_wsh());
        assertEquals(new BigDecimal("100.00"), dto.getFinal_amount_wsh());
        verify(memberPlanMapper, never()).selectById(any());
    }

    @Test
    void previewOrderDiscountUsesMembershipSnapshotRateBeforeCurrentPlanRate() {
        UserMembership membership = activeMembership();
        membership.setBenefit_snapshot_wsh("{\"plan_name_wsh\":\"Snapshot Gold\",\"discount_rate_wsh\":0.80}");
        MemberPlan plan = activePlan(new BigDecimal("0.90"));
        when(userMembershipMapper.selectOne(any())).thenReturn(membership);
        when(memberPlanMapper.selectById(8L)).thenReturn(plan);

        MembershipDiscountDTO dto = service.previewOrderDiscount(7L, new BigDecimal("123.45"));

        assertTrue(dto.getEligible_wsh());
        assertEquals("Snapshot Gold", dto.getPlan_name_wsh());
        assertEquals(new BigDecimal("0.80"), dto.getDiscount_rate_wsh());
        assertEquals(new BigDecimal("24.69"), dto.getMembership_discount_wsh());
        assertEquals(new BigDecimal("98.76"), dto.getFinal_amount_wsh());
    }

    @Test
    void previewOrderDiscountIgnoresInvalidDiscountRate() {
        UserMembership membership = activeMembership();
        MemberPlan plan = activePlan(new BigDecimal("1.50"));
        when(userMembershipMapper.selectOne(any())).thenReturn(membership);
        when(memberPlanMapper.selectById(8L)).thenReturn(plan);

        MembershipDiscountDTO dto = service.previewOrderDiscount(7L, new BigDecimal("88.00"));

        assertTrue(dto.getEligible_wsh());
        assertEquals(new BigDecimal("1.00"), dto.getDiscount_rate_wsh());
        assertEquals(new BigDecimal("0.00"), dto.getMembership_discount_wsh());
        assertEquals(new BigDecimal("88.00"), dto.getFinal_amount_wsh());
    }

    @Test
    void lockForOrderWritesUsageOncePerRequestId() {
        MembershipDiscountDTO discount = discount();
        when(membershipBenefitUsageMapper.selectOne(any())).thenReturn(null);

        service.lockForOrder(7L, discount, 99L, "ORD-99");

        ArgumentCaptor<MembershipBenefitUsage> captor = ArgumentCaptor.forClass(MembershipBenefitUsage.class);
        verify(membershipBenefitUsageMapper).insert(captor.capture());
        MembershipBenefitUsage saved = captor.getValue();
        assertEquals(7L, saved.getUser_id_wsh());
        assertEquals(200L, saved.getMembership_id_wsh());
        assertEquals("discount", saved.getBenefit_type_wsh());
        assertEquals("order_discount", saved.getBenefit_code_wsh());
        assertEquals("pet_order", saved.getBusiness_type_wsh());
        assertEquals("99", saved.getBusiness_id_wsh());
        assertEquals(new BigDecimal("12.00"), saved.getAmount_wsh());
        assertEquals("locked", saved.getUsage_status_wsh());
        assertEquals("membership_discount:order:99", saved.getRequest_id_wsh());
    }

    @Test
    void lockForOrderSkipsExistingUsageForSameOrder() {
        when(membershipBenefitUsageMapper.selectOne(any())).thenReturn(new MembershipBenefitUsage());

        service.lockForOrder(7L, discount(), 99L, "ORD-99");

        verify(membershipBenefitUsageMapper, never()).insert(any(MembershipBenefitUsage.class));
    }

    @Test
    void markUsedAndReleaseUpdateOnlyOrderBenefitRows() {
        service.markUsedForOrder(99L, "ORD-99");
        service.releaseForOrder(99L);

        ArgumentCaptor<MembershipBenefitUsage> captor = ArgumentCaptor.forClass(MembershipBenefitUsage.class);
        verify(membershipBenefitUsageMapper, org.mockito.Mockito.times(2)).update(captor.capture(), any());
        List<MembershipBenefitUsage> updates = captor.getAllValues();
        assertEquals("used", updates.get(0).getUsage_status_wsh());
        assertEquals("released", updates.get(1).getUsage_status_wsh());
    }

    private MembershipDiscountDTO discount() {
        MembershipDiscountDTO dto = new MembershipDiscountDTO();
        dto.setEligible_wsh(true);
        dto.setMembership_id_wsh(200L);
        dto.setPlan_id_wsh(8L);
        dto.setPlan_code_wsh("GOLD_MONTH");
        dto.setPlan_name_wsh("Gold Month");
        dto.setLevel_wsh(2);
        dto.setDiscount_rate_wsh(new BigDecimal("0.90"));
        dto.setBase_amount_wsh(new BigDecimal("120.00"));
        dto.setMembership_discount_wsh(new BigDecimal("12.00"));
        dto.setFinal_amount_wsh(new BigDecimal("108.00"));
        dto.setSnapshot_wsh("{}");
        return dto;
    }

    private UserMembership activeMembership() {
        UserMembership membership = new UserMembership();
        membership.setId_wsh(200L);
        membership.setUser_id_wsh(7L);
        membership.setPlan_id_wsh(8L);
        membership.setPlan_code_wsh("GOLD_MONTH");
        membership.setLevel_wsh(2);
        membership.setStatus_wsh("active");
        membership.setExpires_at_wsh(LocalDateTime.now().plusDays(10));
        return membership;
    }

    private MemberPlan activePlan(BigDecimal rate) {
        MemberPlan plan = new MemberPlan();
        plan.setId_wsh(8L);
        plan.setCode_wsh("GOLD_MONTH");
        plan.setName_wsh("Gold Month");
        plan.setLevel_wsh(2);
        plan.setDiscount_rate_wsh(rate);
        return plan;
    }
}
