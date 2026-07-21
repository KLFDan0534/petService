package com.pet.membership.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.common.BusinessException;
import com.pet.finance.service.AccountingService;
import com.pet.membership.dto.MembershipOrderCreateRequestDTO;
import com.pet.membership.dto.MembershipOrderDTO;
import com.pet.membership.entity.MemberPlan;
import com.pet.membership.entity.MembershipEvent;
import com.pet.membership.entity.MembershipOrder;
import com.pet.membership.entity.UserMembership;
import com.pet.membership.mapper.MemberPlanMapper;
import com.pet.membership.mapper.MembershipEventMapper;
import com.pet.membership.mapper.MembershipOrderMapper;
import com.pet.membership.mapper.UserMembershipMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipOrderServiceImplTest {

    @Mock private MembershipOrderMapper membershipOrderMapper;
    @Mock private MemberPlanMapper memberPlanMapper;
    @Mock private UserMembershipMapper userMembershipMapper;
    @Mock private MembershipEventMapper membershipEventMapper;
    @Mock private AccountingService accountingService;

    private MembershipOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MembershipOrderServiceImpl(
                membershipOrderMapper,
                memberPlanMapper,
                userMembershipMapper,
                membershipEventMapper,
                accountingService,
                new ObjectMapper());
    }

    @Test
    void createOrderCreatesPendingOrderFromActivePlan() {
        when(membershipOrderMapper.selectOne(any())).thenReturn(null);
        when(memberPlanMapper.selectById(8L)).thenReturn(activePlan());
        when(userMembershipMapper.selectOne(any())).thenReturn(null);
        when(membershipOrderMapper.insert(any(MembershipOrder.class))).thenAnswer(invocation -> {
            MembershipOrder order = invocation.getArgument(0);
            order.setId_wsh(101L);
            return 1;
        });

        MembershipOrderCreateRequestDTO request = request(8L, " req-001 ");
        request.setPay_method_wsh(" balance ");
        MembershipOrderDTO dto = service.createOrder(7L, request);

        ArgumentCaptor<MembershipOrder> orderCaptor = ArgumentCaptor.forClass(MembershipOrder.class);
        verify(membershipOrderMapper).insert(orderCaptor.capture());
        MembershipOrder saved = orderCaptor.getValue();
        assertEquals(7L, saved.getUser_id_wsh());
        assertEquals(8L, saved.getPlan_id_wsh());
        assertEquals("GOLD_MONTH", saved.getPlan_code_wsh());
        assertEquals(new BigDecimal("20.00"), saved.getAmount_wsh());
        assertEquals("balance", saved.getPay_method_wsh());
        assertEquals("pending", saved.getStatus_wsh());
        assertEquals("req-001", saved.getRequest_id_wsh());
        assertNotNull(saved.getOrder_no_wsh());
        assertEquals(saved.getMembership_start_at_wsh().plusDays(30), saved.getMembership_end_at_wsh());
        assertEquals("Gold Month", dto.getPlan_name_wsh());
        assertEquals(2, dto.getLevel_wsh());

        ArgumentCaptor<MembershipEvent> eventCaptor = ArgumentCaptor.forClass(MembershipEvent.class);
        verify(membershipEventMapper).insert(eventCaptor.capture());
        assertEquals("order_created", eventCaptor.getValue().getEvent_type_wsh());
        assertEquals(101L, eventCaptor.getValue().getMembership_order_id_wsh());
    }

    @Test
    void createOrderRejectsMockPayMethod() {
        when(membershipOrderMapper.selectOne(any())).thenReturn(null);

        MembershipOrderCreateRequestDTO request = request(8L, "req-mock");
        request.setPay_method_wsh("mock");

        assertThrows(BusinessException.class, () -> service.createOrder(7L, request));

        verify(membershipOrderMapper, never()).insert(any(MembershipOrder.class));
        verifyNoInteractions(memberPlanMapper, userMembershipMapper, membershipEventMapper, accountingService);
    }

    @Test
    void createOrderReturnsExistingOrderForSameRequestIdAndUser() {
        MembershipOrder existing = existingOrder(7L, "pending");
        existing.setRequest_id_wsh("req-dup");
        when(membershipOrderMapper.selectOne(any())).thenReturn(existing);

        MembershipOrderDTO dto = service.createOrder(7L, request(8L, "req-dup"));

        assertEquals(existing.getOrder_no_wsh(), dto.getOrder_no_wsh());
        verify(membershipOrderMapper, never()).insert(any(MembershipOrder.class));
        verifyNoInteractions(memberPlanMapper, userMembershipMapper, membershipEventMapper);
    }

    @Test
    void createOrderRejectsRequestIdOwnedByAnotherUser() {
        MembershipOrder existing = existingOrder(9L, "pending");
        existing.setRequest_id_wsh("req-dup");
        when(membershipOrderMapper.selectOne(any())).thenReturn(existing);

        assertThrows(BusinessException.class, () -> service.createOrder(7L, request(8L, "req-dup")));

        verify(membershipOrderMapper, never()).insert(any(MembershipOrder.class));
        verifyNoInteractions(memberPlanMapper, userMembershipMapper, membershipEventMapper);
    }

    @Test
    void createOrderRejectsDisabledPlan() {
        MemberPlan plan = activePlan();
        plan.setStatus_wsh(0);
        when(membershipOrderMapper.selectOne(any())).thenReturn(null);
        when(memberPlanMapper.selectById(8L)).thenReturn(plan);

        assertThrows(BusinessException.class, () -> service.createOrder(7L, request(8L, "req-disabled")));

        verify(membershipOrderMapper, never()).insert(any(MembershipOrder.class));
        verifyNoInteractions(userMembershipMapper, membershipEventMapper);
    }

    @Test
    void createOrderStartsAfterCurrentActiveMembershipExpiry() {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(12).withNano(0);
        UserMembership membership = new UserMembership();
        membership.setExpires_at_wsh(expiresAt);
        when(membershipOrderMapper.selectOne(any())).thenReturn(null);
        when(memberPlanMapper.selectOne(any())).thenReturn(activePlan());
        when(userMembershipMapper.selectOne(any())).thenReturn(membership);
        when(membershipOrderMapper.insert(any(MembershipOrder.class))).thenAnswer(invocation -> {
            MembershipOrder order = invocation.getArgument(0);
            order.setId_wsh(102L);
            return 1;
        });

        MembershipOrderCreateRequestDTO request = request(null, "req-renew");
        request.setPlan_code_wsh(" gold_month ");
        service.createOrder(7L, request);

        ArgumentCaptor<MembershipOrder> orderCaptor = ArgumentCaptor.forClass(MembershipOrder.class);
        verify(membershipOrderMapper).insert(orderCaptor.capture());
        MembershipOrder saved = orderCaptor.getValue();
        assertEquals(expiresAt, saved.getMembership_start_at_wsh());
        assertEquals(expiresAt.plusDays(30), saved.getMembership_end_at_wsh());
    }

    @Test
    void cancelPendingOrderUpdatesStatusAndWritesEvent() {
        MembershipOrder existing = existingOrder(7L, "pending");
        when(membershipOrderMapper.selectOne(any())).thenReturn(existing);
        when(membershipOrderMapper.update(any(MembershipOrder.class), any())).thenReturn(1);

        MembershipOrderDTO dto = service.cancelPendingOrder(7L, existing.getOrder_no_wsh());

        assertEquals("cancelled", dto.getStatus_wsh());
        ArgumentCaptor<MembershipOrder> updateCaptor = ArgumentCaptor.forClass(MembershipOrder.class);
        verify(membershipOrderMapper).update(updateCaptor.capture(), any());
        assertEquals("cancelled", updateCaptor.getValue().getStatus_wsh());

        ArgumentCaptor<MembershipEvent> eventCaptor = ArgumentCaptor.forClass(MembershipEvent.class);
        verify(membershipEventMapper).insert(eventCaptor.capture());
        assertEquals("order_cancelled", eventCaptor.getValue().getEvent_type_wsh());
    }

    @Test
    void cancelOrderRejectsNonPendingStatus() {
        MembershipOrder existing = existingOrder(7L, "paid");
        when(membershipOrderMapper.selectOne(any())).thenReturn(existing);

        assertThrows(BusinessException.class, () -> service.cancelPendingOrder(7L, existing.getOrder_no_wsh()));

        verify(membershipOrderMapper, never()).update(any(MembershipOrder.class), any());
        verifyNoInteractions(membershipEventMapper);
    }

    @Test
    void cancelOrderRejectsAnotherUserOrder() {
        MembershipOrder existing = existingOrder(9L, "pending");
        when(membershipOrderMapper.selectOne(any())).thenReturn(existing);

        assertThrows(BusinessException.class, () -> service.cancelPendingOrder(7L, existing.getOrder_no_wsh()));

        verify(membershipOrderMapper, never()).update(any(MembershipOrder.class), any());
        verifyNoInteractions(membershipEventMapper);
    }

    @Test
    void listOrdersForAdminRejectsUnsupportedStatus() {
        assertThrows(BusinessException.class, () -> service.listOrdersForAdmin("finished"));

        verify(membershipOrderMapper, never()).selectList(any());
    }

    @Test
    void payPendingOrderActivatesMembership() {
        MembershipOrder existing = existingOrder(7L, "pending");
        when(membershipOrderMapper.selectOne(any())).thenReturn(existing);
        when(membershipOrderMapper.update(any(MembershipOrder.class), any())).thenReturn(1);
        when(accountingService.systemUserId()).thenReturn(0L);
        when(userMembershipMapper.selectOne(any())).thenReturn(null);
        when(memberPlanMapper.selectById(8L)).thenReturn(activePlan());
        when(userMembershipMapper.insert(any(UserMembership.class))).thenAnswer(invocation -> {
            UserMembership membership = invocation.getArgument(0);
            membership.setId_wsh(201L);
            return 1;
        });

        MembershipOrderDTO dto = service.payOrder(7L, existing.getOrder_no_wsh());

        assertEquals("paid", dto.getStatus_wsh());
        ArgumentCaptor<MembershipOrder> orderCaptor = ArgumentCaptor.forClass(MembershipOrder.class);
        verify(membershipOrderMapper).update(orderCaptor.capture(), any());
        assertEquals("paid", orderCaptor.getValue().getStatus_wsh());

        ArgumentCaptor<UserMembership> membershipCaptor = ArgumentCaptor.forClass(UserMembership.class);
        verify(userMembershipMapper).insert(membershipCaptor.capture());
        UserMembership saved = membershipCaptor.getValue();
        assertEquals(7L, saved.getUser_id_wsh());
        assertEquals(8L, saved.getPlan_id_wsh());
        assertEquals("GOLD_MONTH", saved.getPlan_code_wsh());
        assertEquals("active", saved.getStatus_wsh());
        assertEquals(existing.getId_wsh(), saved.getLast_order_id_wsh());
        assertEquals(existing.getMembership_start_at_wsh(), saved.getStarted_at_wsh());
        assertEquals(existing.getMembership_end_at_wsh(), saved.getExpires_at_wsh());

        ArgumentCaptor<MembershipEvent> eventCaptor = ArgumentCaptor.forClass(MembershipEvent.class);
        verify(membershipEventMapper, org.mockito.Mockito.times(2)).insert(eventCaptor.capture());
        assertEquals("order_paid", eventCaptor.getAllValues().get(0).getEvent_type_wsh());
        assertEquals("membership_activated", eventCaptor.getAllValues().get(1).getEvent_type_wsh());
        verify(accountingService).debit(eq(7L), eq(new BigDecimal("20.00")), eq("membership_payment"), eq(null),
                eq("membership_order"), eq("100"), eq("membership-payment:100:owner"), any());
        verify(accountingService).credit(eq(0L), eq(new BigDecimal("20.00")), eq("membership_payment"), eq(null),
                eq("membership_order"), eq("100"), eq("membership-payment:100:system"), any());
    }

    @Test
    void userPayRejectsExternalMembershipPaymentMethod() {
        MembershipOrder existing = existingOrder(7L, "pending");
        existing.setPay_method_wsh("wechat");
        when(membershipOrderMapper.selectOne(any())).thenReturn(existing);

        assertThrows(BusinessException.class, () -> service.payOrder(7L, existing.getOrder_no_wsh()));

        verify(membershipOrderMapper, never()).update(any(MembershipOrder.class), any());
        verifyNoInteractions(accountingService, userMembershipMapper, memberPlanMapper, membershipEventMapper);
    }

    @Test
    void payPaidOrderDoesNotExtendMembershipAgain() {
        MembershipOrder existing = existingOrder(7L, "paid");
        UserMembership membership = activeMembership(7L, existing.getId_wsh(), LocalDateTime.now().plusDays(20));
        when(membershipOrderMapper.selectOne(any())).thenReturn(existing);
        when(userMembershipMapper.selectOne(any())).thenReturn(membership);

        MembershipOrderDTO dto = service.payOrder(7L, existing.getOrder_no_wsh());

        assertEquals("paid", dto.getStatus_wsh());
        verify(membershipOrderMapper, never()).update(any(MembershipOrder.class), any());
        verify(userMembershipMapper, never()).insert(any(UserMembership.class));
        verify(userMembershipMapper, never()).updateById(any(UserMembership.class));
        verify(membershipEventMapper, never()).insert(any(MembershipEvent.class));
        verify(memberPlanMapper, never()).selectById(anyLong());
    }

    @Test
    void payPendingOrderRenewsFromCurrentActiveExpiry() {
        MembershipOrder existing = existingOrder(7L, "pending");
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(12).withNano(0);
        UserMembership membership = activeMembership(7L, 90L, expiresAt);
        when(membershipOrderMapper.selectOne(any())).thenReturn(existing);
        when(membershipOrderMapper.update(any(MembershipOrder.class), any())).thenReturn(1);
        when(userMembershipMapper.selectOne(any())).thenReturn(membership);
        when(memberPlanMapper.selectById(8L)).thenReturn(activePlan());

        service.payOrder(7L, existing.getOrder_no_wsh());

        ArgumentCaptor<UserMembership> membershipCaptor = ArgumentCaptor.forClass(UserMembership.class);
        verify(userMembershipMapper).updateById(membershipCaptor.capture());
        UserMembership saved = membershipCaptor.getValue();
        assertEquals(expiresAt, saved.getStarted_at_wsh());
        assertEquals(expiresAt.plusDays(30), saved.getExpires_at_wsh());
        assertEquals(existing.getId_wsh(), saved.getLast_order_id_wsh());

        ArgumentCaptor<MembershipOrder> orderCaptor = ArgumentCaptor.forClass(MembershipOrder.class);
        verify(membershipOrderMapper).updateById(orderCaptor.capture());
        assertEquals(expiresAt, orderCaptor.getValue().getMembership_start_at_wsh());
        assertEquals(expiresAt.plusDays(30), orderCaptor.getValue().getMembership_end_at_wsh());
    }

    @Test
    void concurrentPayOrdersForSameUserSerializesMembershipActivation() throws Exception {
        MembershipOrder first = existingOrder(7L, "pending");
        first.setId_wsh(100L);
        first.setOrder_no_wsh("MO-CONCURRENT-1");
        MembershipOrder second = existingOrder(7L, "pending");
        second.setId_wsh(101L);
        second.setOrder_no_wsh("MO-CONCURRENT-2");
        ConcurrentLinkedQueue<MembershipOrder> orders = new ConcurrentLinkedQueue<>();
        orders.add(first);
        orders.add(second);

        AtomicInteger activeActivations = new AtomicInteger();
        AtomicInteger maxActiveActivations = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        when(membershipOrderMapper.selectOne(any())).thenAnswer(invocation -> orders.poll());
        when(membershipOrderMapper.update(any(MembershipOrder.class), any())).thenReturn(1);
        when(memberPlanMapper.selectById(8L)).thenReturn(activePlan());
        when(userMembershipMapper.selectOne(any())).thenAnswer(invocation -> {
            int current = activeActivations.incrementAndGet();
            maxActiveActivations.accumulateAndGet(current, Math::max);
            Thread.sleep(30);
            activeActivations.decrementAndGet();
            return null;
        });
        when(userMembershipMapper.insert(any(UserMembership.class))).thenReturn(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> firstPay = executor.submit(() -> {
                await(start);
                service.payOrder(7L, "MO-CONCURRENT-1");
            });
            Future<?> secondPay = executor.submit(() -> {
                await(start);
                service.payOrder(7L, "MO-CONCURRENT-2");
            });
            start.countDown();
            firstPay.get(2, TimeUnit.SECONDS);
            secondPay.get(2, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, maxActiveActivations.get());
    }

    private MembershipOrderCreateRequestDTO request(Long planId, String requestId) {
        MembershipOrderCreateRequestDTO request = new MembershipOrderCreateRequestDTO();
        request.setPlan_id_wsh(planId);
        request.setRequest_id_wsh(requestId);
        return request;
    }

    private MemberPlan activePlan() {
        MemberPlan plan = new MemberPlan();
        plan.setId_wsh(8L);
        plan.setCode_wsh("GOLD_MONTH");
        plan.setName_wsh("Gold Month");
        plan.setLevel_wsh(2);
        plan.setPrice_wsh(new BigDecimal("19.995"));
        plan.setDuration_days_wsh(30);
        plan.setDiscount_rate_wsh(new BigDecimal("0.90"));
        plan.setStatus_wsh(1);
        return plan;
    }

    private MembershipOrder existingOrder(Long userId, String status) {
        MembershipOrder order = new MembershipOrder();
        order.setId_wsh(100L);
        order.setOrder_no_wsh("MO202607050001");
        order.setUser_id_wsh(userId);
        order.setPlan_id_wsh(8L);
        order.setPlan_code_wsh("GOLD_MONTH");
        order.setAmount_wsh(new BigDecimal("20.00"));
        order.setPay_method_wsh("balance");
        order.setStatus_wsh(status);
        order.setRequest_id_wsh("req-existing");
        order.setMembership_start_at_wsh(LocalDateTime.now());
        order.setMembership_end_at_wsh(LocalDateTime.now().plusDays(30));
        order.setPlan_snapshot_wsh("{\"plan_name_wsh\":\"Gold Month\",\"level_wsh\":2}");
        return order;
    }

    private UserMembership activeMembership(Long userId, Long lastOrderId, LocalDateTime expiresAt) {
        UserMembership membership = new UserMembership();
        membership.setId_wsh(200L);
        membership.setUser_id_wsh(userId);
        membership.setPlan_id_wsh(8L);
        membership.setPlan_code_wsh("GOLD_MONTH");
        membership.setLevel_wsh(2);
        membership.setStatus_wsh("active");
        membership.setStarted_at_wsh(LocalDateTime.now().minusDays(10));
        membership.setExpires_at_wsh(expiresAt);
        membership.setLast_order_id_wsh(lastOrderId);
        return membership;
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
