package com.pet.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pet.common.BusinessException;
import com.pet.common.OrderStatus;
import com.pet.finance.service.AccountingService;
import com.pet.marketing.service.CouponService;
import com.pet.membership.service.MembershipBenefitService;
import com.pet.mq.MessageSender;
import com.pet.order.entity.Payment;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.mapper.PaymentMapper;
import com.pet.order.service.OrderService;
import com.pet.order.service.OrderStatusBroadcaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 订单余额支付链路单元测试。
 * 覆盖：余额充足支付成功→订单PAID、余额不足拒扣、重复支付幂等、越权、外部支付方式拒绝。
 */
@ExtendWith(MockitoExtension.class)
class PaymentBalancePayTest {

    @Mock private PaymentMapper paymentMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderStatusBroadcaster orderStatusBroadcaster;
    @Mock private AccountingService accountingService;
    @Mock private CouponService couponService;
    @Mock private MembershipBenefitService membershipBenefitService;
    @Mock private MessageSender messageSender;
    @Mock private OrderService orderService;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(paymentMapper, orderMapper, orderStatusBroadcaster,
                accountingService, couponService, membershipBenefitService, messageSender, orderService);
    }

    private Payment pendingPayment(long id, long orderId, String payNo, BigDecimal amount, String method) {
        Payment p = new Payment();
        p.setId_wsh(id);
        p.setOrder_id_wsh(orderId);
        p.setPay_no_wsh(payNo);
        p.setAmount_wsh(amount);
        p.setMethod_wsh(method);
        p.setStatus_wsh("pending");
        return p;
    }

    private PetOrder pendingOrder(long id, long ownerId, String orderNo, BigDecimal amount, BigDecimal subsidy) {
        PetOrder o = new PetOrder();
        o.setId_wsh(id);
        o.setOwner_id_wsh(ownerId);
        o.setOrder_no_wsh(orderNo);
        o.setFinal_amount_wsh(amount);
        o.setPlatform_subsidy_wsh(subsidy);
        o.setStatus_wsh(OrderStatus.PENDING);
        o.setCreated_at_wsh(LocalDateTime.now());
        return o;
    }

    @Test
    void balanceSufficientDebitsOnceAndMarksOrderPaid() {
        when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(pendingPayment(1L, 1L, "PAY1", new BigDecimal("100.00"), "balance"));
        PetOrder order = pendingOrder(1L, 7L, "ORD1", new BigDecimal("100.00"), new BigDecimal("10.00"));
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(paymentMapper.update(any(Payment.class), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(accountingService.systemUserId()).thenReturn(1L);
        when(orderMapper.update(any(PetOrder.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

        paymentService.pay(7L, "PAY1");

        verify(accountingService, times(1)).debit(eq(7L), eq(new BigDecimal("100.00")), eq("payment"), eq(1L), any(), any(), any(), any());
        verify(accountingService, times(1)).credit(eq(1L), eq(new BigDecimal("100.00")), eq("payment"), eq(1L), any(), any(), any(), any());
        verify(accountingService, times(1)).credit(eq(1L), eq(new BigDecimal("10.00")), eq("coupon_subsidy"), eq(1L), any(), any(), any(), any());
        verify(orderMapper).update(any(PetOrder.class), any(LambdaUpdateWrapper.class));
        assertEquals(OrderStatus.PAID, order.getStatus_wsh());
        verify(couponService).markUsedForOrder(eq(1L), eq("ORD1"));
        verify(membershipBenefitService).markUsedForOrder(eq(1L), eq("ORD1"));
    }

    @Test
    void balanceInsufficientRollsBackAndNoOrderStateChange() {
        when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(pendingPayment(1L, 1L, "PAY1", new BigDecimal("100.00"), "balance"));
        when(orderMapper.selectById(1L)).thenReturn(pendingOrder(1L, 7L, "ORD1", new BigDecimal("100.00"), BigDecimal.ZERO));
        when(paymentMapper.update(any(Payment.class), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(accountingService.debit(eq(7L), any(), eq("payment"), eq(1L), any(), any(), any(), any()))
                .thenThrow(new BusinessException(400, "余额不足"));

        assertThrows(BusinessException.class, () -> paymentService.pay(7L, "PAY1"));

        verify(orderMapper, never()).update(any(PetOrder.class), any(LambdaUpdateWrapper.class));
        verify(couponService, never()).markUsedForOrder(anyLong(), any());
    }

    @Test
    void duplicatePaymentIsIdempotentAndNoSecondDeduction() {
        Payment paid = pendingPayment(1L, 1L, "PAY1", new BigDecimal("100.00"), "balance");
        paid.setStatus_wsh("success");
        when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paid);
        when(orderMapper.selectById(1L)).thenReturn(pendingOrder(1L, 7L, "ORD1", new BigDecimal("100.00"), BigDecimal.ZERO));

        paymentService.pay(7L, "PAY1");

        verify(accountingService, never()).debit(any(), any(), any(), any(), any(), any(), any(), any());
        verify(orderMapper, never()).update(any(PetOrder.class), any(LambdaUpdateWrapper.class));
    }

    @Test
    void cannotPayAnotherUsersOrder() {
        when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(pendingPayment(1L, 1L, "PAY1", new BigDecimal("100.00"), "balance"));
        when(orderMapper.selectById(1L)).thenReturn(pendingOrder(1L, 99L, "ORD1", new BigDecimal("100.00"), BigDecimal.ZERO));

        assertThrows(BusinessException.class, () -> paymentService.pay(7L, "PAY1"));

        verify(accountingService, never()).debit(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void externalMethodRequiresPlatformConfirmation() {
        when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(pendingPayment(1L, 1L, "PAY1", new BigDecimal("100.00"), "wechat"));
        when(orderMapper.selectById(1L)).thenReturn(pendingOrder(1L, 7L, "ORD1", new BigDecimal("100.00"), BigDecimal.ZERO));

        assertThrows(BusinessException.class, () -> paymentService.pay(7L, "PAY1"));

        verify(accountingService, never()).debit(any(), any(), any(), any(), any(), any(), any(), any());
    }
}