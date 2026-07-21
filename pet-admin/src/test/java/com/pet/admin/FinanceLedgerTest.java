package com.pet.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.mapper.ServiceItemMapper;
import com.pet.boarding.service.KeeperAttendanceService;
import com.pet.boarding.service.KeeperLeaveService;
import com.pet.boarding.service.MerchantService;
import com.pet.common.BusinessException;
import com.pet.common.OrderStatus;
import com.pet.common.RefundStatus;
import com.pet.common.StatusCode;
import com.pet.finance.controller.WalletController;
import com.pet.finance.dto.WalletAdjustRequestDTO;
import com.pet.finance.entity.Wallet;
import com.pet.finance.service.AccountingService;
import com.pet.finance.service.WalletService;
import com.pet.marketing.service.CouponService;
import com.pet.membership.service.MembershipBenefitService;
import com.pet.mq.MessageSender;
import com.pet.order.entity.Payment;
import com.pet.order.entity.PetOrder;
import com.pet.order.entity.Refund;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.mapper.PaymentMapper;
import com.pet.order.mapper.RefundMapper;
import com.pet.order.service.OrderService;
import com.pet.order.service.OrderSnapshotService;
import com.pet.order.service.OrderStatusBroadcaster;
import com.pet.order.service.impl.OrderServiceImpl;
import com.pet.order.service.impl.PaymentServiceImpl;
import com.pet.order.service.impl.RefundServiceImpl;
import com.pet.pet.mapper.PetMapper;
import com.pet.qualification.service.QualificationService;
import com.pet.security.JwtAuthenticationToken;
import com.pet.system.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceLedgerTest {

    private static final Long OWNER_ID = 10L;
    private static final Long ORDER_ID = 20L;
    private static final String ORDER_NO = "ORD-FIN-001";

    @Mock private PaymentMapper paymentMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderStatusBroadcaster orderStatusBroadcaster;
    @Mock private AccountingService accountingService;
    @Mock private CouponService couponService;
    @Mock private MembershipBenefitService membershipBenefitService;
    @Mock private MessageSender messageSender;
    @Mock private OrderService orderService;
    @Mock private RefundMapper refundMapper;
    @Mock private WalletService walletService;
    @Mock private PetMapper petMapper;
    @Mock private KeeperMapper keeperMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private MerchantService merchantService;
    @Mock private ServiceItemMapper serviceItemMapper;
    @Mock private UserMapper userMapper;
    @Mock private OrderSnapshotService orderSnapshotService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private QualificationService qualificationService;
    @Mock private KeeperAttendanceService keeperAttendanceService;
    @Mock private KeeperLeaveService keeperLeaveService;

    private PaymentServiceImpl paymentService;
    private RefundServiceImpl refundService;
    private OrderServiceImpl orderStatusService;
    private WalletController walletController;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                paymentMapper,
                orderMapper,
                orderStatusBroadcaster,
                accountingService,
                couponService,
                membershipBenefitService,
                messageSender,
                orderService);
        refundService = new RefundServiceImpl(refundMapper, orderMapper, accountingService);
        orderStatusService = new OrderServiceImpl(
                orderMapper,
                paymentMapper,
                petMapper,
                keeperMapper,
                merchantMapper,
                merchantService,
                serviceItemMapper,
                userMapper,
                orderSnapshotService,
                eventPublisher,
                qualificationService,
                orderStatusBroadcaster,
                messageSender,
                accountingService,
                keeperAttendanceService,
                keeperLeaveService,
                couponService,
                membershipBenefitService,
                new ObjectMapper());
        walletController = new WalletController(walletService, accountingService);
    }

    @Test
    void createPaymentRejectsMockMethod() {
        PetOrder order = order(OrderStatus.PENDING);
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        assertThrows(BusinessException.class, () -> paymentService.createPayment(OWNER_ID, ORDER_NO, "mock"));

        verify(paymentMapper, never()).insert(any(Payment.class));
    }

    @Test
    void externalPaymentCannotBeCompletedByUserEndpoint() {
        Payment payment = payment("PAY-WECHAT", 99L, ORDER_ID, new BigDecimal("80.00"));
        payment.setMethod_wsh("wechat");
        PetOrder order = order(OrderStatus.PENDING);
        when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);

        assertThrows(BusinessException.class, () -> paymentService.pay(OWNER_ID, "PAY-WECHAT"));

        verify(paymentMapper, never()).update(any(Payment.class), any());
        verify(orderMapper, never()).update(any(PetOrder.class), any());
        verify(accountingService, never()).debit(any(), any(), any(), any(), any(), any(), any(), any());
        verify(accountingService, never()).credit(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void adminStatusUpdateCannotBypassPaymentSettlementOrRefundFlows() {
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order(OrderStatus.PENDING));

        assertThrows(BusinessException.class, () -> orderStatusService.updateOrderStatus(ORDER_NO, OrderStatus.PAID));
        assertThrows(BusinessException.class, () -> orderStatusService.updateOrderStatus(ORDER_NO, OrderStatus.CONFIRMED));
        assertThrows(BusinessException.class, () -> orderStatusService.updateOrderStatus(ORDER_NO, OrderStatus.COMPLETED));
        assertThrows(BusinessException.class, () -> orderStatusService.updateOrderStatus(ORDER_NO, OrderStatus.CANCELLED));
        assertThrows(BusinessException.class, () -> orderStatusService.updateOrderStatus(ORDER_NO, OrderStatus.REFUNDING));

        verify(orderMapper, never()).updateById(any(PetOrder.class));
        verify(orderMapper, never()).update(any(PetOrder.class), any(LambdaUpdateWrapper.class));
        verify(accountingService, never()).transfer(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void adminStatusUpdateAllowsOnlyNonMoneyFulfillmentCorrections() {
        PetOrder order = order(OrderStatus.RECEIVED);
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(order)
                .thenReturn(null);
        Keeper keeper = new Keeper();
        keeper.setId_wsh(order.getKeeper_id_wsh());
        keeper.setStatus_wsh(StatusCode.KEEPER_ACTIVE.getValue());
        keeper.setMax_pets_wsh(3);
        when(keeperMapper.selectById(order.getKeeper_id_wsh())).thenReturn(keeper);
        when(orderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(orderMapper.update(any(PetOrder.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

        assertDoesNotThrow(() -> orderStatusService.updateOrderStatus(ORDER_NO, OrderStatus.IN_PROGRESS));

        assertEquals(OrderStatus.IN_PROGRESS, order.getStatus_wsh());
        verify(orderMapper).update(any(PetOrder.class), any(LambdaUpdateWrapper.class));
        verify(accountingService, never()).transfer(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void refundCanBeCreatedForUnsettledFulfillmentStatesAndRejectRestoresOriginalStatus() {
        PetOrder order = order(OrderStatus.RECEIVED);
        Refund refund = new Refund();
        refund.setId_wsh(88L);
        refund.setOrder_id_wsh(order.getId_wsh());
        refund.setOrder_no_wsh(order.getOrder_no_wsh());
        refund.setStatus_wsh(RefundStatus.PENDING);
        refund.setOrder_status_before_refund_wsh(OrderStatus.RECEIVED);

        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(refundMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(orderMapper.update(any(PetOrder.class), any(LambdaUpdateWrapper.class))).thenReturn(1);
        doAnswer(invocation -> {
            Refund inserted = invocation.getArgument(0);
            inserted.setId_wsh(refund.getId_wsh());
            return 1;
        }).when(refundMapper).insert(any(Refund.class));

        Refund created = refundService.createRefund(OWNER_ID, ORDER_NO, "change plan");

        assertEquals(OrderStatus.RECEIVED, created.getOrder_status_before_refund_wsh());
        assertEquals(OrderStatus.REFUNDING, order.getStatus_wsh());

        when(refundMapper.selectById(refund.getId_wsh())).thenReturn(refund);
        when(orderMapper.selectById(order.getId_wsh())).thenReturn(order);
        when(refundMapper.update(any(Refund.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

        refundService.rejectRefund(refund.getId_wsh());

        assertEquals(RefundStatus.REJECTED, refund.getStatus_wsh());
        assertEquals(OrderStatus.RECEIVED, order.getStatus_wsh());
    }

    @Test
    void refundCreationFailsIfOrderStatusChangedBeforeRefunding() {
        PetOrder order = order(OrderStatus.RECEIVED);
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(refundMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(orderMapper.update(any(PetOrder.class), any(LambdaUpdateWrapper.class))).thenReturn(0);

        assertThrows(BusinessException.class, () -> refundService.createRefund(OWNER_ID, ORDER_NO, "change plan"));

        verify(refundMapper, never()).insert(any(Refund.class));
    }

    @Test
    void refundCreationRejectsCompletedOrdersBecauseTheyMayAlreadyBeSettled() {
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order(OrderStatus.COMPLETED));

        assertThrows(BusinessException.class, () -> refundService.createRefund(OWNER_ID, ORDER_NO, "too late"));

        verify(refundMapper, never()).insert(any(Refund.class));
        verify(orderMapper, never()).updateById(any(PetOrder.class));
    }

    @Test
    void completeOrderDoesNotSettleIfOrderStatusChangedBeforeCompletion() {
        PetOrder order = order(OrderStatus.IN_PROGRESS);
        Keeper keeper = new Keeper();
        keeper.setId_wsh(order.getKeeper_id_wsh());
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(keeperMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(keeper));
        when(orderMapper.update(any(PetOrder.class), any(LambdaUpdateWrapper.class))).thenReturn(0);

        assertThrows(BusinessException.class, () -> orderStatusService.completeOrder(999L, ORDER_NO));

        verify(accountingService, never()).transfer(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void adminAdjustUsesProvidedRequestIdForIdempotency() {
        Wallet wallet = wallet(OWNER_ID, "115.00", "0.00");
        when(accountingService.credit(
                eq(OWNER_ID),
                eq(new BigDecimal("15.00")),
                eq("admin_adjust"),
                eq(null),
                eq("wallet_admin"),
                eq(String.valueOf(OWNER_ID)),
                eq("front-adjust-001"),
                eq("manual credit"))).thenReturn(wallet);
        when(walletService.toDTO(wallet)).thenReturn(null);

        WalletAdjustRequestDTO request = new WalletAdjustRequestDTO();
        request.setUser_id_wsh(OWNER_ID);
        request.setMode_wsh("add");
        request.setAmount_wsh(new BigDecimal("15.00"));
        request.setRemark_wsh("manual credit");
        request.setRequest_id_wsh("front-adjust-001");
        JwtAuthenticationToken token = new JwtAuthenticationToken(1L, "admin", List.of());

        walletController.adjustBalance(token, request);

        verify(accountingService).credit(
                eq(OWNER_ID),
                eq(new BigDecimal("15.00")),
                eq("admin_adjust"),
                eq(null),
                eq("wallet_admin"),
                eq(String.valueOf(OWNER_ID)),
                eq("front-adjust-001"),
                eq("manual credit"));
    }

    @Test
    void adminAdjustGeneratesRequestIdWhenClientDoesNotProvideOne() {
        Wallet wallet = wallet(OWNER_ID, "100.00", "0.00");
        when(accountingService.setBalanceByAdmin(eq(1L), eq(OWNER_ID), eq(new BigDecimal("100.00")), any(), eq("set balance")))
                .thenReturn(wallet);
        when(walletService.toDTO(wallet)).thenReturn(null);

        WalletAdjustRequestDTO request = new WalletAdjustRequestDTO();
        request.setUser_id_wsh(OWNER_ID);
        request.setMode_wsh("set");
        request.setBalance_wsh(new BigDecimal("100.00"));
        request.setRemark_wsh("set balance");
        JwtAuthenticationToken token = new JwtAuthenticationToken(1L, "admin", List.of());

        walletController.adjustBalance(token, request);

        ArgumentCaptor<String> requestIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(accountingService).setBalanceByAdmin(
                eq(1L),
                eq(OWNER_ID),
                eq(new BigDecimal("100.00")),
                requestIdCaptor.capture(),
                eq("set balance"));
        String generatedRequestId = requestIdCaptor.getValue();
        assertEquals(true, generatedRequestId.startsWith("admin-wallet:1:" + OWNER_ID + ":"));
    }

    private PetOrder order(String status) {
        PetOrder order = new PetOrder();
        order.setId_wsh(ORDER_ID);
        order.setOrder_no_wsh(ORDER_NO);
        order.setOwner_id_wsh(OWNER_ID);
        order.setPet_id_wsh(30L);
        order.setKeeper_id_wsh(40L);
        order.setMerchant_id_wsh(50L);
        order.setStatus_wsh(status);
        order.setFinal_amount_wsh(new BigDecimal("80.00"));
        order.setSettlement_amount_wsh(new BigDecimal("80.00"));
        order.setStart_date_wsh(LocalDate.now().plusDays(1));
        order.setEnd_date_wsh(LocalDate.now().plusDays(3));
        order.setCreated_at_wsh(LocalDateTime.now());
        return order;
    }

    private Payment payment(String payNo, Long id, Long orderId, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setId_wsh(id);
        payment.setOrder_id_wsh(orderId);
        payment.setOrder_no_wsh(ORDER_NO);
        payment.setPay_no_wsh(payNo);
        payment.setAmount_wsh(amount);
        payment.setMethod_wsh("balance");
        payment.setStatus_wsh("pending");
        return payment;
    }

    private Wallet wallet(Long userId, String balance, String frozen) {
        Wallet wallet = new Wallet();
        wallet.setId_wsh(1L);
        wallet.setUser_id_wsh(userId);
        wallet.setBalance_wsh(new BigDecimal(balance));
        wallet.setFrozen_amount_wsh(new BigDecimal(frozen));
        return wallet;
    }
}
