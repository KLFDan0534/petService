package com.pet.membership.service.impl;

import com.pet.common.BusinessException;
import com.pet.membership.dto.MemberPlanCreateRequestDTO;
import com.pet.membership.entity.MemberPlan;
import com.pet.membership.mapper.MemberPlanMapper;
import com.pet.membership.mapper.MembershipOrderMapper;
import com.pet.membership.mapper.UserMembershipMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberPlanServiceImplTest {

    @Mock private MemberPlanMapper memberPlanMapper;
    @Mock private UserMembershipMapper userMembershipMapper;
    @Mock private MembershipOrderMapper membershipOrderMapper;

    private MemberPlanServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MemberPlanServiceImpl(
                memberPlanMapper,
                userMembershipMapper,
                membershipOrderMapper,
                new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    void createPlanNormalizesCodeMoneyRateAndJson() {
        when(memberPlanMapper.selectCount(any())).thenReturn(0L);

        MemberPlanCreateRequestDTO request = validCreateRequest();
        request.setCode_wsh(" gold_month ");
        request.setPrice_wsh(new BigDecimal("19.995"));
        request.setDiscount_rate_wsh(new BigDecimal("0.955"));
        request.setBenefit_config_wsh("{\"discount\":true}");

        service.createPlan(request);

        ArgumentCaptor<MemberPlan> captor = ArgumentCaptor.forClass(MemberPlan.class);
        verify(memberPlanMapper).insert(captor.capture());
        MemberPlan saved = captor.getValue();
        assertEquals("GOLD_MONTH", saved.getCode_wsh());
        assertEquals(new BigDecimal("20.00"), saved.getPrice_wsh());
        assertEquals(new BigDecimal("0.96"), saved.getDiscount_rate_wsh());
        assertEquals("{\"discount\":true}", saved.getBenefit_config_wsh());
        assertEquals(1, saved.getStatus_wsh());
    }

    @Test
    void createPlanRejectsDuplicateCodeBeforeInsert() {
        when(memberPlanMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.createPlan(validCreateRequest()));

        verify(memberPlanMapper, never()).insert(any(MemberPlan.class));
    }

    @Test
    void createPlanRejectsBrokenJsonConfig() {
        when(memberPlanMapper.selectCount(any())).thenReturn(0L);
        MemberPlanCreateRequestDTO request = validCreateRequest();
        request.setMonthly_coupon_config_wsh("{bad-json");

        assertThrows(BusinessException.class, () -> service.createPlan(request));

        verify(memberPlanMapper, never()).insert(any(MemberPlan.class));
    }

    @Test
    void deletePlanRejectsPlanWithMembershipHistory() {
        MemberPlan existing = new MemberPlan();
        existing.setId_wsh(9L);
        when(memberPlanMapper.selectById(9L)).thenReturn(existing);
        when(userMembershipMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.deletePlan(9L));

        verify(memberPlanMapper, never()).deleteById(9L);
        verify(membershipOrderMapper, never()).selectCount(any());
    }

    @Test
    void updateStatusRejectsUnsupportedStatus() {
        MemberPlan existing = new MemberPlan();
        existing.setId_wsh(9L);
        when(memberPlanMapper.selectById(9L)).thenReturn(existing);

        assertThrows(BusinessException.class, () -> service.updateStatus(9L, 2));

        verify(memberPlanMapper, never()).updateById(any(MemberPlan.class));
    }

    private MemberPlanCreateRequestDTO validCreateRequest() {
        MemberPlanCreateRequestDTO request = new MemberPlanCreateRequestDTO();
        request.setCode_wsh("GOLD");
        request.setName_wsh("Gold");
        request.setLevel_wsh(1);
        request.setPrice_wsh(new BigDecimal("19.90"));
        request.setDuration_days_wsh(30);
        return request;
    }
}
