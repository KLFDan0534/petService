package com.pet.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.mapper.ServiceItemMapper;
import com.pet.boarding.service.BusinessHoursService;
import com.pet.boarding.service.BusinessHoursTargetResolver;
import com.pet.boarding.service.KeeperAttendanceService;
import com.pet.boarding.service.KeeperLeaveService;
import com.pet.boarding.service.MerchantService;
import com.pet.common.BusinessException;
import com.pet.common.OrderStatus;
import com.pet.common.StatusCode;
import com.pet.finance.service.AccountingService;
import com.pet.marketing.service.CouponService;
import com.pet.membership.service.MembershipBenefitService;
import com.pet.mq.MessageSender;
import com.pet.order.entity.Payment;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.mapper.PaymentMapper;
import com.pet.order.mq.OrderAcceptTimeoutListener;
import com.pet.order.service.OrderService;
import com.pet.order.service.OrderSnapshotService;
import com.pet.order.service.OrderStatusBroadcaster;
import com.pet.order.service.impl.OrderServiceImpl;
import com.pet.order.service.impl.PaymentServiceImpl;
import com.pet.pet.mapper.PetMapper;
import com.pet.qualification.dto.QualificationDTO;
import com.pet.qualification.service.QualificationService;
import com.pet.system.mapper.UserMapper;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderAutoAcceptTimeoutTest {

    @Mock private PaymentMapper paymentMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderStatusBroadcaster orderStatusBroadcaster;
    @Mock private AccountingService accountingService;
    @Mock private CouponService couponService;
    @Mock private MembershipBenefitService membershipBenefitService;
    @Mock private MessageSender messageSender;
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
    @Mock private OrderService orderService;
    @Mock private BusinessHoursService businessHoursService;
    @Mock private BusinessHoursTargetResolver businessHoursTargetResolver;
    @Mock private Channel channel;

    @Test
    void paySchedulesAcceptTimeoutAfterOrderBecomesPaid() {
        Payment payment = payment("PAY001", 10L, 100L, BigDecimal.ZERO);
        PetOrder order = order(100L, "ORD001", OrderStatus.PENDING);
        order.setOwner_id_wsh(7L);
        order.setCreated_at_wsh(LocalDateTime.now());

        when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);
        when(orderMapper.selectById(100L)).thenReturn(order);
        when(paymentMapper.update(any(Payment.class), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(orderMapper.update(any(PetOrder.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

        PaymentServiceImpl paymentService = new PaymentServiceImpl(
                paymentMapper, orderMapper, orderStatusBroadcaster, accountingService, couponService,
                membershipBenefitService, messageSender, orderService);

        paymentService.pay(7L, "PAY001");

        verify(couponService).markUsedForOrder(100L, "ORD001");
        verify(orderStatusBroadcaster).broadcast(order);
        verify(messageSender).sendOrderAcceptTimeout("ORD001");
    }

    @Test
    void createPaymentCancelsExpiredPendingOrderBeforeCreatingPayment() {
        PetOrder order = order(101L, "ORD_EXPIRED_CREATE", OrderStatus.PENDING);
        order.setOwner_id_wsh(7L);
        order.setCreated_at_wsh(LocalDateTime.now().minusMinutes(16));
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        PaymentServiceImpl paymentService = new PaymentServiceImpl(
                paymentMapper, orderMapper, orderStatusBroadcaster, accountingService, couponService,
                membershipBenefitService, messageSender, orderService);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> paymentService.createPayment(7L, "ORD_EXPIRED_CREATE", "balance"));

        assertEquals(400, exception.getCode());
        verify(orderService).cancelPendingOrderIfPaymentTimeout("ORD_EXPIRED_CREATE");
        verify(paymentMapper, never()).insert(any(Payment.class));
    }

    @Test
    void payCancelsExpiredPendingOrderBeforeMarkingPaymentSuccess() {
        Payment payment = payment("PAY_EXPIRED", 11L, 102L, BigDecimal.ZERO);
        payment.setOrder_no_wsh("ORD_EXPIRED_PAY");
        PetOrder order = order(102L, "ORD_EXPIRED_PAY", OrderStatus.PENDING);
        order.setOwner_id_wsh(7L);
        order.setCreated_at_wsh(LocalDateTime.now().minusMinutes(16));
        when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);
        when(orderMapper.selectById(102L)).thenReturn(order);

        PaymentServiceImpl paymentService = new PaymentServiceImpl(
                paymentMapper, orderMapper, orderStatusBroadcaster, accountingService, couponService,
                membershipBenefitService, messageSender, orderService);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> paymentService.pay(7L, "PAY_EXPIRED"));

        assertEquals(400, exception.getCode());
        verify(orderService).cancelPendingOrderIfPaymentTimeout("ORD_EXPIRED_PAY");
        verify(paymentMapper, never()).update(any(Payment.class), any(LambdaUpdateWrapper.class));
        verify(orderMapper, never()).update(any(PetOrder.class), any(LambdaUpdateWrapper.class));
    }

    @Test
    void autoAcceptTimeoutConfirmsOnlyStillPaidOrders() {
        PetOrder order = order(100L, "ORD002", OrderStatus.PAID);
        order.setKeeper_id_wsh(20L);
        order.setMerchant_id_wsh(10L);
        Keeper keeper = new Keeper();
        keeper.setId_wsh(20L);
        keeper.setStatus_wsh(StatusCode.KEEPER_ACTIVE.getValue());
        keeper.setMax_pets_wsh(3);

        QualificationDTO qualification = new QualificationDTO();
        qualification.setStatus_wsh(QualificationService.STATUS_APPROVED);

        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(keeperMapper.selectById(20L)).thenReturn(keeper);
        when(merchantMapper.selectById(10L)).thenReturn(futureBookingMerchant());
        when(qualificationService.listByOwner(QualificationService.OWNER_TYPE_KEEPER, 20L, false))
                .thenReturn(List.of(qualification));
        when(orderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(orderMapper.update(any(PetOrder.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

        OrderServiceImpl orderServiceImpl = orderService();

        assertTrue(orderServiceImpl.autoAcceptPaidOrderIfTimeout("ORD002"));
        assertEquals(OrderStatus.CONFIRMED, order.getStatus_wsh());
        verify(keeperLeaveService).requireKeeperAvailable(20L, order.getStart_date_wsh(), order.getEnd_date_wsh());
        verify(orderStatusBroadcaster).broadcast(order);
    }

    @Test
    void autoAcceptTimeoutIgnoresOrdersThatAreNoLongerPaid() {
        PetOrder order = order(100L, "ORD003", OrderStatus.CANCELLED);
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        OrderServiceImpl orderServiceImpl = orderService();

        assertFalse(orderServiceImpl.autoAcceptPaidOrderIfTimeout("ORD003"));
        verify(orderMapper, never()).update(any(PetOrder.class), any(LambdaUpdateWrapper.class));
        verify(orderStatusBroadcaster, never()).broadcast(any());
    }

    @Test
    void fallbackScannerAutoAcceptsPaidOrdersWithOldSuccessfulPayments() {
        Payment payment = payment("PAY_TIMEOUT", 12L, 103L, BigDecimal.ZERO);
        payment.setOrder_no_wsh("ORD_TIMEOUT");
        payment.setStatus_wsh("success");
        payment.setPaid_at_wsh(LocalDateTime.now().minusMinutes(31));

        PetOrder order = order(103L, "ORD_TIMEOUT", OrderStatus.PAID);
        order.setKeeper_id_wsh(20L);
        order.setMerchant_id_wsh(10L);

        Keeper keeper = new Keeper();
        keeper.setId_wsh(20L);
        keeper.setStatus_wsh(StatusCode.KEEPER_ACTIVE.getValue());
        keeper.setMax_pets_wsh(3);

        QualificationDTO qualification = new QualificationDTO();
        qualification.setStatus_wsh(QualificationService.STATUS_APPROVED);

        when(paymentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(payment));
        when(orderMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(order));
        when(keeperMapper.selectById(20L)).thenReturn(keeper);
        when(merchantMapper.selectById(10L)).thenReturn(futureBookingMerchant());
        when(qualificationService.listByOwner(QualificationService.OWNER_TYPE_KEEPER, 20L, false))
                .thenReturn(List.of(qualification));
        when(orderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(orderMapper.update(any(PetOrder.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

        int accepted = orderService().autoAcceptPaidOrdersIfTimeout();

        assertEquals(1, accepted);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus_wsh());
        verify(keeperLeaveService).requireKeeperAvailable(20L, order.getStart_date_wsh(), order.getEnd_date_wsh());
        verify(orderStatusBroadcaster).broadcast(order);
    }

    @Test
    void acceptTimeoutListenerAcksHandledMessage() throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(42L);
        Message message = new Message("ORD004".getBytes(), properties);
        when(orderService.autoAcceptPaidOrderIfTimeout("ORD004")).thenReturn(true);

        new OrderAcceptTimeoutListener(orderService).handleAcceptTimeout("ORD004", message, channel);

        verify(orderService).autoAcceptPaidOrderIfTimeout("ORD004");
        verify(channel).basicAck(42L, false);
        verify(channel, never()).basicNack(eq(42L), eq(false), eq(false));
    }

    @Test
    void acceptTimeoutListenerNacksFailedMessage() throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(43L);
        Message message = new Message("ORD005".getBytes(), properties);
        when(orderService.autoAcceptPaidOrderIfTimeout("ORD005")).thenThrow(new RuntimeException("boom"));

        new OrderAcceptTimeoutListener(orderService).handleAcceptTimeout("ORD005", message, channel);

        verify(channel).basicNack(43L, false, false);
        verify(channel, never()).basicAck(eq(43L), eq(false));
    }

    @Test
    void autoAcceptUsesConditionalPaidToConfirmedUpdate() {
        PetOrder order = order(100L, "ORD006", OrderStatus.PAID);
        order.setKeeper_id_wsh(20L);
        order.setMerchant_id_wsh(10L);
        Keeper keeper = new Keeper();
        keeper.setId_wsh(20L);
        keeper.setStatus_wsh(StatusCode.KEEPER_ACTIVE.getValue());
        keeper.setMax_pets_wsh(3);
        QualificationDTO qualification = new QualificationDTO();
        qualification.setStatus_wsh(QualificationService.STATUS_APPROVED);

        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(keeperMapper.selectById(20L)).thenReturn(keeper);
        when(merchantMapper.selectById(10L)).thenReturn(futureBookingMerchant());
        when(qualificationService.listByOwner(QualificationService.OWNER_TYPE_KEEPER, 20L, false))
                .thenReturn(List.of(qualification));
        when(orderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(orderMapper.update(any(PetOrder.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

        orderService().autoAcceptPaidOrderIfTimeout("ORD006");

        ArgumentCaptor<PetOrder> updateCaptor = ArgumentCaptor.forClass(PetOrder.class);
        verify(orderMapper).update(updateCaptor.capture(), any(LambdaUpdateWrapper.class));
        assertEquals(OrderStatus.CONFIRMED, updateCaptor.getValue().getStatus_wsh());
    }

    @Test
    void merchantOwnerCannotAcceptOrderUnlessTheyAreAssignedKeeper() {
        PetOrder order = order(104L, "ORD_MERCHANT_DENIED", OrderStatus.PAID);
        order.setMerchant_id_wsh(10L);
        order.setKeeper_id_wsh(20L);
        Keeper keeper = keeper(20L, 21L);

        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(keeperMapper.selectById(20L)).thenReturn(keeper);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService().acceptOrder(30L, "ORD_MERCHANT_DENIED"));

        assertEquals(403, exception.getCode());
        verify(orderMapper, never()).update(any(PetOrder.class), any(LambdaUpdateWrapper.class));
        verify(qualificationService, never()).listByOwner(any(), any(), eq(false));
    }

    @Test
    void assignedKeeperCanAcceptPaidOrderAfterQualificationApproval() {
        PetOrder order = order(105L, "ORD_KEEPER_ACCEPT", OrderStatus.PAID);
        order.setMerchant_id_wsh(10L);
        order.setKeeper_id_wsh(20L);
        Keeper keeper = keeper(20L, 21L);

        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(keeperMapper.selectById(20L)).thenReturn(keeper);
        when(merchantMapper.selectById(10L)).thenReturn(futureBookingMerchant());
        when(qualificationService.listByOwner(QualificationService.OWNER_TYPE_KEEPER, 20L, false))
                .thenReturn(List.of(approvedQualification()));
        when(orderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(orderMapper.update(any(PetOrder.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

        orderService().acceptOrder(21L, "ORD_KEEPER_ACCEPT");

        assertEquals(OrderStatus.CONFIRMED, order.getStatus_wsh());
        verify(keeperLeaveService).requireKeeperAvailable(20L, order.getStart_date_wsh(), order.getEnd_date_wsh());
        verify(orderStatusBroadcaster).broadcast(order);
    }

    @Test
    void merchantUserCanAcceptOnlyWhenTheirAssignedKeeperProfileIsCertified() {
        PetOrder order = order(106L, "ORD_MERCHANT_KEEPER_ACCEPT", OrderStatus.PAID);
        order.setMerchant_id_wsh(10L);
        order.setKeeper_id_wsh(20L);
        Keeper keeper = keeper(20L, 30L);

        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(keeperMapper.selectById(20L)).thenReturn(keeper);
        when(merchantMapper.selectById(10L)).thenReturn(futureBookingMerchant());
        when(qualificationService.listByOwner(QualificationService.OWNER_TYPE_KEEPER, 20L, false))
                .thenReturn(List.of(approvedQualification()));
        when(orderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(orderMapper.update(any(PetOrder.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

        orderService().acceptOrder(30L, "ORD_MERCHANT_KEEPER_ACCEPT");

        assertEquals(OrderStatus.CONFIRMED, order.getStatus_wsh());
        verify(orderStatusBroadcaster).broadcast(order);
    }

    private OrderServiceImpl orderService() {
        return new OrderServiceImpl(
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
                new ObjectMapper(),
                businessHoursService,
                businessHoursTargetResolver);
    }

    private Payment payment(String payNo, Long id, Long orderId, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setId_wsh(id);
        payment.setOrder_id_wsh(orderId);
        payment.setOrder_no_wsh("ORD001");
        payment.setPay_no_wsh(payNo);
        payment.setAmount_wsh(amount);
        payment.setMethod_wsh("balance");
        payment.setStatus_wsh("pending");
        return payment;
    }

    private PetOrder order(Long id, String orderNo, String status) {
        PetOrder order = new PetOrder();
        order.setId_wsh(id);
        order.setOrder_no_wsh(orderNo);
        order.setStatus_wsh(status);
        order.setStart_date_wsh(LocalDate.now().plusDays(1));
        order.setEnd_date_wsh(LocalDate.now().plusDays(3));
        return order;
    }

    private Keeper keeper(Long keeperId, Long userId) {
        Keeper keeper = new Keeper();
        keeper.setId_wsh(keeperId);
        keeper.setUser_id_wsh(userId);
        keeper.setStatus_wsh(StatusCode.KEEPER_ACTIVE.getValue());
        keeper.setMax_pets_wsh(3);
        return keeper;
    }

    private Merchant futureBookingMerchant() {
        Merchant merchant = new Merchant();
        merchant.setId_wsh(10L);
        merchant.setStatus_wsh(StatusCode.MERCHANT_APPROVED.getValue());
        merchant.setFuture_booking_enabled_wsh(1);
        return merchant;
    }

    private QualificationDTO approvedQualification() {
        QualificationDTO qualification = new QualificationDTO();
        qualification.setStatus_wsh(QualificationService.STATUS_APPROVED);
        return qualification;
    }
}
