package com.pet.membership.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.common.BusinessException;
import com.pet.membership.dto.UserMembershipDTO;
import com.pet.membership.entity.MemberPlan;
import com.pet.membership.entity.UserMembership;
import com.pet.membership.mapper.MemberPlanMapper;
import com.pet.membership.mapper.UserMembershipMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipServiceImplTest {

    @Mock private UserMembershipMapper userMembershipMapper;
    @Mock private MemberPlanMapper memberPlanMapper;

    private MembershipServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MembershipServiceImpl(userMembershipMapper, memberPlanMapper, new ObjectMapper());
    }

    @Test
    void getCurrentMembershipReturnsInactiveWhenUserHasNoMembership() {
        when(userMembershipMapper.selectOne(any())).thenReturn(null);

        UserMembershipDTO dto = service.getCurrentMembership(7L);

        assertEquals(7L, dto.getUser_id_wsh());
        assertEquals("inactive", dto.getStatus_wsh());
        assertFalse(dto.getActive_wsh());
        assertEquals(0L, dto.getRemaining_days_wsh());
        assertEquals(BigDecimal.ONE, dto.getDiscount_rate_wsh());
    }

    @Test
    void getCurrentMembershipRefreshesExpiredActiveMembership() {
        UserMembership membership = activeMembership();
        membership.setExpires_at_wsh(LocalDateTime.now().minusMinutes(1));
        when(userMembershipMapper.selectOne(any())).thenReturn(membership);
        when(memberPlanMapper.selectById(8L)).thenReturn(activePlan());

        UserMembershipDTO dto = service.getCurrentMembership(7L);

        assertEquals("expired", dto.getStatus_wsh());
        assertFalse(dto.getActive_wsh());
        ArgumentCaptor<UserMembership> captor = ArgumentCaptor.forClass(UserMembership.class);
        verify(userMembershipMapper).update(captor.capture(), any());
        assertEquals("expired", captor.getValue().getStatus_wsh());
    }

    @Test
    void listMembershipsForAdminRejectsUnsupportedStatus() {
        assertThrows(BusinessException.class, () -> service.listMembershipsForAdmin("paid"));

        verify(userMembershipMapper, never()).selectList(any());
    }

    private UserMembership activeMembership() {
        UserMembership membership = new UserMembership();
        membership.setId_wsh(200L);
        membership.setUser_id_wsh(7L);
        membership.setPlan_id_wsh(8L);
        membership.setPlan_code_wsh("GOLD_MONTH");
        membership.setLevel_wsh(2);
        membership.setStatus_wsh("active");
        membership.setStarted_at_wsh(LocalDateTime.now().minusDays(30));
        membership.setExpires_at_wsh(LocalDateTime.now().plusDays(10));
        membership.setBenefit_snapshot_wsh("{\"plan_name_wsh\":\"Snapshot Gold\",\"discount_rate_wsh\":0.80}");
        return membership;
    }

    private MemberPlan activePlan() {
        MemberPlan plan = new MemberPlan();
        plan.setId_wsh(8L);
        plan.setName_wsh("Gold Month");
        plan.setDiscount_rate_wsh(new BigDecimal("0.90"));
        return plan;
    }
}
