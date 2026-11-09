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
import com.pet.order.pricing.PaymentPurpose;
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

    // ============ 晚接补费（night_overtime） ============

    private Payment overtimePayment(long id, long orderId, String payNo, BigDecimal amount, String status) {
        Payment p = new Payment();
        p.setId_wsh(id);
        p.setOrder_id_wsh(orderId);
        p.setPay_no_wsh(payNo);
        p.setAmount_wsh(amount);
        p.setMethod_wsh("balance");
        p.setPurpose_wsh(PaymentPurpose.NIGHT_OVERTIME);
        p.setStatus_wsh(status);
        return p;
    }

    /** 三天前下单、已确认接走且待补费的订单（订单早已超过 15 分钟支付窗口）。 */
    private PetOrder overtimeOrder(long id, long ownerId, BigDecimal overtime) {
        PetOrder o = new PetOrder();
        o.setId_wsh(id);
        o.setOwner_id_wsh(ownerId);
        o.setOrder_no_wsh("ORD-OT");
        o.setFinal_amount_wsh(new BigDecimal("450.00"));
        o.setTotal_amount_wsh(new BigDecimal("450.00"));
        o.setSettlement_amount_wsh(new BigDecimal("450.00"));
        o.setOvertime_hours_wsh(2);
        o.setOvertime_amount_wsh(overtime);
        o.setOvertime_payment_id_wsh(9L);
        o.setCheckout_confirmed_at_wsh(LocalDateTime.now().minusHours(1));
        o.setStatus_wsh(OrderStatus.IN_PROGRESS);
        o.setCreated_at_wsh(LocalDateTime.now().minusDays(3));
        return o;
    }

    @Test
    void overtimePaymentDebitsOnceAndCompletesOrderEvenLongAfterCreation() {
        when(paymentMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(overtimePayment(9L, 1L, "PAY-OT", new BigDecimal("12.50"), "pending"));
        PetOrder order = overtimeOrder(1L, 7L, new BigDecimal("12.50"));
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderMapper.selectByIdForUpdate(1L)).thenReturn(order);
        when(paymentMapper.update(any(Payment.class), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(accountingService.systemUserId()).thenReturn(1L);

        paymentService.pay(7L, "PAY-OT");

        verify(accountingService, times(1)).debit(eq(7L), eq(new BigDecimal("12.50")), eq("payment"), eq(1L), any(), any(), any(), any());
        verify(accountingService, times(1)).credit(eq(1L), eq(new BigDecimal("12.50")), eq("payment"), eq(1L), any(), any(), any(), any());
        // 补费不重跑补贴/券/会员
        verify(accountingService, never()).credit(any(), any(), eq("coupon_subsidy"), any(), any(), any(), any(), any());
        verify(couponService, never()).markUsedForOrder(anyLong(), any());
        verify(membershipBenefitService, never()).markUsedForOrder(anyLong(), any());
        // 累计金额与完成交给 OrderService 的同事务回调
        verify(orderService, times(1)).applyOvertimePayment(1L, 9L);
        // 补费不推进订单 pending→paid
        verify(orderMapper, never()).update(any(PetOrder.class), any(LambdaUpdateWrapper.class));
    }

    @Test
    void overtimeInsufficientBalanceRollsBackButKeepsFrozenCheckout() {
        when(paymentMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(overtimePayment(9L, 1L, "PAY-OT", new BigDecimal("12.50"), "pending"));
        PetOrder order = overtimeOrder(1L, 7L, new BigDecimal("12.50"));
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderMapper.selectByIdForUpdate(1L)).thenReturn(order);
        when(paymentMapper.update(any(Payment.class), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(accountingService.debit(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new BusinessException(400, "余额不足"));

        assertThrows(BusinessException.class, () -> paymentService.pay(7L, "PAY-OT"));

        // 事务回滚：不完成、不结算；接走时间与补费单是先前独立提交的，不会被撤销
        verify(orderService, never()).applyOvertimePayment(anyLong(), anyLong());
        assertEquals(OrderStatus.IN_PROGRESS, order.getStatus_wsh());
        assertEquals(new BigDecimal("12.50"), order.getOvertime_amount_wsh());
    }

    @Test
    void duplicateOvertimePaymentIsIdempotent() {
        when(paymentMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(overtimePayment(9L, 1L, "PAY-OT", new BigDecimal("12.50"), "success"));
        PetOrder order = overtimeOrder(1L, 7L, new BigDecimal("12.50"));
        order.setStatus_wsh(OrderStatus.COMPLETED);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderMapper.selectByIdForUpdate(1L)).thenReturn(order);

        paymentService.pay(7L, "PAY-OT");

        verify(accountingService, never()).debit(any(), any(), any(), any(), any(), any(), any(), any());
        verify(orderService, never()).applyOvertimePayment(anyLong(), anyLong());
    }

    @Test
    void keeperCannotPayOvertimeForOwner() {
        when(paymentMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(overtimePayment(9L, 1L, "PAY-OT", new BigDecimal("12.50"), "pending"));
        when(orderMapper.selectById(1L)).thenReturn(overtimeOrder(1L, 7L, new BigDecimal("12.50")));

        // AE13：本单 keeper（非主人）支付同一 pay_no 必须 403
        BusinessException ex = assertThrows(BusinessException.class, () -> paymentService.pay(30L, "PAY-OT"));
        assertEquals(403, ex.getCode());
        verify(accountingService, never()).debit(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void overtimePaymentMustMatchOrderBinding() {
        when(paymentMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(overtimePayment(10L, 1L, "PAY-OT", new BigDecimal("12.50"), "pending"));
        PetOrder order = overtimeOrder(1L, 7L, new BigDecimal("12.50"));
        // 订单绑定的是 9L，支付单是 10L → 关联不一致必须拒绝
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderMapper.selectByIdForUpdate(1L)).thenReturn(order);

        assertThrows(BusinessException.class, () -> paymentService.pay(7L, "PAY-OT"));
        verify(accountingService, never()).debit(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void overtimeAmountMismatchRejected() {
        when(paymentMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(overtimePayment(9L, 1L, "PAY-OT", new BigDecimal("99.00"), "pending"));
        PetOrder order = overtimeOrder(1L, 7L, new BigDecimal("12.50"));
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderMapper.selectByIdForUpdate(1L)).thenReturn(order);

        assertThrows(BusinessException.class, () -> paymentService.pay(7L, "PAY-OT"));
        verify(accountingService, never()).debit(any(), any(), any(), any(), any(), any(), any(), any());
    }
}
