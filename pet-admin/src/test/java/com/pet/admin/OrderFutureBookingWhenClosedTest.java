package com.pet.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.mapper.ServiceItemMapper;
import com.pet.boarding.service.BusinessHoursService;
import com.pet.boarding.service.BusinessHoursTargetResolver;
import com.pet.boarding.service.KeeperAttendanceService;
import com.pet.boarding.service.KeeperLeaveService;
import com.pet.boarding.service.MerchantService;
import com.pet.common.BookingErrorCode;
import com.pet.common.BusinessException;
import com.pet.common.OrderStatus;
import com.pet.common.StatusCode;
import com.pet.finance.service.AccountingService;
import com.pet.marketing.dto.CouponDiscountResult;
import com.pet.marketing.service.CouponService;
import com.pet.membership.dto.MembershipDiscountDTO;
import com.pet.membership.service.MembershipBenefitService;
import com.pet.mq.MessageSender;
import com.pet.order.dto.OrderCreateRequestDTO;
import com.pet.order.dto.OrderDTO;
import com.pet.order.entity.Payment;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.mapper.PaymentMapper;
import com.pet.order.service.OrderService;
import com.pet.order.service.OrderSnapshotService;
import com.pet.order.service.OrderStatusBroadcaster;
import com.pet.order.service.impl.OrderServiceImpl;
import com.pet.order.service.impl.PaymentServiceImpl;
import com.pet.pet.entity.Pet;
import com.pet.pet.mapper.PetMapper;
import com.pet.qualification.dto.QualificationDTO;
import com.pet.qualification.service.QualificationService;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

/**
 * U3 RED: after splitting real-time store status from the future-booking policy,
 * an approved-but-closed store must still accept, pay and confirm future orders.
 */
@ExtendWith(MockitoExtension.class)
class OrderFutureBookingWhenClosedTest {

    @Mock private OrderMapper orderMapper;
    @Mock private PaymentMapper paymentMapper;
    @Mock private PetMapper petMapper;
    @Mock private KeeperMapper keeperMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private MerchantService merchantService;
    @Mock private ServiceItemMapper serviceItemMapper;
    @Mock private UserMapper userMapper;
    @Mock private OrderSnapshotService orderSnapshotService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private QualificationService qualificationService;
    @Mock private OrderStatusBroadcaster orderStatusBroadcaster;
    @Mock private MessageSender messageSender;
    @Mock private AccountingService accountingService;
    @Mock private KeeperAttendanceService keeperAttendanceService;
    @Mock private KeeperLeaveService keeperLeaveService;
    @Mock private CouponService couponService;
    @Mock private MembershipBenefitService membershipBenefitService;
    @Mock private BusinessHoursService businessHoursService;
    @Mock private OrderService orderServiceDelegate;

    private final BusinessHoursTargetResolver resolver = new BusinessHoursTargetResolver();

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, com.pet.system.entity.User.class);
        TableInfoHelper.initTableInfo(assistant, com.pet.pet.entity.Pet.class);
        TableInfoHelper.initTableInfo(assistant, com.pet.boarding.entity.Keeper.class);
        TableInfoHelper.initTableInfo(assistant, com.pet.boarding.entity.Merchant.class);
        TableInfoHelper.initTableInfo(assistant, com.pet.boarding.entity.ServiceItem.class);
    }

    @Test
    void closedStoreWithPolicyOnCanStillCreateFutureOrder() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2);
        stubCreateSuccessPath(start, 1); // merchant closed (store_status=0) but policy on

        OrderDTO dto = orderService().createOrder(7L, request(start, end));

        assertEquals(OrderStatus.PENDING, dto.getStatus_wsh());
        verify(orderMapper).insert(any(PetOrder.class));
    }

    @Test
    void futureBookingDisabledRejectsClosureAtCreate() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2);
        stubPetAndKeeper();
        stubSingleMerchant(10L, StatusCode.MERCHANT_APPROVED.getValue(), 0, 0);

        BusinessException error = assertThrows(BusinessException.class,
                () -> orderService().createOrder(7L, request(start, end)));

        assertEquals(400, error.getCode());
        assertEquals(BookingErrorCode.FUTURE_BOOKING_DISABLED, error.getErrorCode());
    }

    @Test
    void unapprovedMerchantRejectedAtCreate() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2);
        stubPetAndKeeper();
        stubSingleMerchant(10L, StatusCode.MERCHANT_PENDING.getValue(), 0, 1);

        BusinessException error = assertThrows(BusinessException.class,
                () -> orderService().createOrder(7L, request(start, end)));

        assertEquals(BookingErrorCode.MERCHANT_NOT_APPROVED, error.getErrorCode());
    }

    @Test
    void autoConfirmSucceedsWhenKeeperIsOfflineBecauseStoreClosed() {
        PetOrder order = futurePaidOrder();
        order.setMerchant_id_wsh(10L);
        order.setKeeper_id_wsh(20L);
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        // keeper OFFLINE (store close sync) is still employed -> future-bookable
        when(keeperMapper.selectById(20L)).thenReturn(keeper(StatusCode.KEEPER_OFFLINE.getValue()));
        when(merchantMapper.selectById(10L)).thenReturn(merchant(10L, 1, 1, 1));
        when(qualificationService.listByOwner(QualificationService.OWNER_TYPE_KEEPER, 20L, false))
                .thenReturn(List.of(approvedQualification()));
        when(orderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(orderMapper.update(any(PetOrder.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

        assertTrue(orderService().autoAcceptPaidOrderIfTimeout("ORD-CLOSED"));
        assertEquals(OrderStatus.CONFIRMED, order.getStatus_wsh());
        verify(orderStatusBroadcaster).broadcast(order);
    }

    @Test
    void confirmRejectsWhenPolicyToggledOffAfterPayment() {
        PetOrder order = futurePaidOrder();
        order.setMerchant_id_wsh(10L);
        order.setKeeper_id_wsh(20L);
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        // Policy turned off between payment and auto-confirm.
        when(merchantMapper.selectById(10L)).thenReturn(merchant(10L, 1, 0, 0));

        BusinessException error = assertThrows(BusinessException.class,
                () -> orderService().autoAcceptPaidOrderIfTimeout("ORD-CLOSED"));

        assertEquals(BookingErrorCode.FUTURE_BOOKING_DISABLED, error.getErrorCode());
        assertEquals(OrderStatus.PAID, order.getStatus_wsh());
        verify(orderMapper, never()).update(any(PetOrder.class), any(LambdaUpdateWrapper.class));
        verify(orderStatusBroadcaster, never()).broadcast(any());
    }

    @Test
    void confirmRejectsResignedKeeper() {
        PetOrder order = futurePaidOrder();
        order.setMerchant_id_wsh(10L);
        order.setKeeper_id_wsh(20L);
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(keeperMapper.selectById(20L)).thenReturn(keeper(StatusCode.KEEPER_RESIGNED.getValue()));
        when(merchantMapper.selectById(10L)).thenReturn(merchant(10L, 1, 1, 1));

        BusinessException error = assertThrows(BusinessException.class,
                () -> orderService().autoAcceptPaidOrderIfTimeout("ORD-CLOSED"));

        assertEquals(BookingErrorCode.KEEPER_NOT_BOOKABLE, error.getErrorCode());
        assertEquals(OrderStatus.PAID, order.getStatus_wsh());
        verify(orderMapper, never()).update(any(PetOrder.class), any(LambdaUpdateWrapper.class));
    }

    @Test
    void duplicatePaymentCallbackIsIdempotent() {
        Payment payment = new Payment();
        payment.setId_wsh(51L);
        payment.setOrder_id_wsh(100L);
        payment.setOrder_no_wsh("ORD-CLOSED");
        payment.setPay_no_wsh("PAY-CLOSED");
        payment.setStatus_wsh("success");
        payment.setAmount_wsh(BigDecimal.ZERO);

        PetOrder order = new PetOrder();
        order.setId_wsh(100L);
        order.setOrder_no_wsh("ORD-CLOSED");
        order.setOwner_id_wsh(7L);
        order.setStatus_wsh(OrderStatus.PAID);

        when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);
        when(orderMapper.selectById(100L)).thenReturn(order);

        PaymentServiceImpl paymentService = new PaymentServiceImpl(
                paymentMapper, orderMapper, orderStatusBroadcaster, accountingService, couponService,
                membershipBenefitService, messageSender, orderServiceDelegate());

        paymentService.pay(7L, "PAY-CLOSED");

        // Idempotent: already-success payment is silently accepted, no re-marking.
        verify(paymentMapper, never()).update(any(Payment.class), any(LambdaUpdateWrapper.class));
        verify(orderMapper, never()).update(any(), any());
        verify(orderStatusBroadcaster, never()).broadcast(any());
        verify(accountingService, never()).debit(anyLong(), any(), any(), anyLong(), any(), any(), any(), any());
    }

    private PetOrder futurePaidOrder() {
        PetOrder order = new PetOrder();
        order.setId_wsh(100L);
        order.setOrder_no_wsh("ORD-CLOSED");
        order.setStatus_wsh(OrderStatus.PAID);
        return order;
    }

    private void stubCreateSuccessPath(LocalDate start, int policy) {
        Pet pet = new Pet();
        pet.setId_wsh(1L);
        pet.setOwner_id_wsh(7L);
        when(petMapper.selectById(1L)).thenReturn(pet);

        Keeper kp = new Keeper();
        kp.setId_wsh(20L);
        kp.setMerchant_id_wsh(10L);
        kp.setStatus_wsh(StatusCode.KEEPER_ACTIVE.getValue());
        kp.setPrice_per_day_wsh(new BigDecimal("50"));
        kp.setMax_pets_wsh(3);
        when(keeperMapper.selectById(20L)).thenReturn(kp);

        // store_status=0 (closed) but future booking policy on -> create must succeed
        when(merchantMapper.selectById(10L)).thenReturn(merchant(10L, 1, policy, 0));

        when(qualificationService.listByOwner(eq(QualificationService.OWNER_TYPE_KEEPER),
                eq(20L), eq(false)))
                .thenReturn(List.of(approvedQualification()));
        lenient().when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        lenient().when(orderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        CouponDiscountResult coupon = new CouponDiscountResult();
        coupon.setCoupon_discount_wsh(BigDecimal.ZERO);
        coupon.setPlatform_subsidy_wsh(BigDecimal.ZERO);
        coupon.setSettlement_amount_wsh(new BigDecimal("100"));
        coupon.setFinal_amount_wsh(new BigDecimal("100"));
        lenient().when(couponService.previewForOrder(anyLong(), any(), any(), any(), anyLong(), any()))
                .thenReturn(coupon);

        MembershipDiscountDTO membership = new MembershipDiscountDTO();
        membership.setMembership_discount_wsh(BigDecimal.ZERO);
        lenient().when(membershipBenefitService.previewOrderDiscount(eq(7L), any()))
                .thenReturn(membership);

        when(userMapper.selectById(7L)).thenReturn(ownerUser());
        lenient().when(orderSnapshotService.findDTOMapByOrderIds(any())).thenReturn(new java.util.HashMap<>());
        lenient().when(keeperMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(kp));
        lenient().when(merchantMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(merchant(10L, 1, policy, 0)));
        lenient().when(petMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(pet));
        lenient().when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(ownerUser()));

        ServiceItem service = new ServiceItem();
        service.setId_wsh(5L);
        service.setMerchant_id_wsh(10L);
        service.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        lenient().when(serviceItemMapper.selectById(5L)).thenReturn(service);
        lenient().when(serviceItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(service));
        lenient().doAnswer(invocation -> {
            PetOrder order = invocation.getArgument(0);
            order.setId_wsh(100L);
            order.setService_id_wsh(5L);
            return 1;
        }).when(orderMapper).insert(any(PetOrder.class));
    }

    private User ownerUser() {
        User user = new User();
        user.setId_wsh(7L);
        return user;
    }

    private void stubSingleMerchant(Long id, int merchantStatus, int policy, int storeStatus) {
        when(merchantMapper.selectById(id)).thenReturn(merchant(id, merchantStatus, policy, storeStatus));
        lenient().when(merchantMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
    }

    private void stubPetAndKeeper() {
        Pet pet = new Pet();
        pet.setId_wsh(1L);
        pet.setOwner_id_wsh(7L);
        when(petMapper.selectById(1L)).thenReturn(pet);
        when(keeperMapper.selectById(20L)).thenReturn(keeper(StatusCode.KEEPER_ACTIVE.getValue()));
    }

    private Merchant merchant(Long id, int merchantStatus, int policy, int storeStatus) {
        Merchant m = new Merchant();
        m.setId_wsh(id);
        m.setStatus_wsh(merchantStatus);
        m.setFuture_booking_enabled_wsh(policy);
        m.setStore_status_wsh(storeStatus);
        return m;
    }

    private Keeper keeper(Integer keeperStatus) {
        Keeper k = new Keeper();
        k.setId_wsh(20L);
        k.setMerchant_id_wsh(10L);
        k.setStatus_wsh(keeperStatus);
        k.setPrice_per_day_wsh(new BigDecimal("50"));
        k.setMax_pets_wsh(3);
        return k;
    }

    private QualificationDTO approvedQualification() {
        QualificationDTO q = new QualificationDTO();
        q.setStatus_wsh(QualificationService.STATUS_APPROVED);
        q.setId_wsh(1L);
        return q;
    }

    private OrderCreateRequestDTO request(LocalDate start, LocalDate end) {
        OrderCreateRequestDTO dto = new OrderCreateRequestDTO();
        dto.setPet_id_wsh(1L);
        dto.setKeeper_id_wsh(20L);
        dto.setMerchant_id_wsh(10L);
        dto.setStart_date_wsh(start);
        dto.setEnd_date_wsh(end);
        dto.setDelivery_time_wsh(start.atTime(10, 0));
        return dto;
    }

    private OrderServiceImpl orderService() {
        return new OrderServiceImpl(
                orderMapper, paymentMapper, petMapper, keeperMapper, merchantMapper,
                merchantService, serviceItemMapper, userMapper, orderSnapshotService,
                eventPublisher, qualificationService, orderStatusBroadcaster,
                messageSender, accountingService, keeperAttendanceService,
                keeperLeaveService, couponService, membershipBenefitService,
                new ObjectMapper(), businessHoursService, resolver);
    }

    private OrderService orderServiceDelegate() {
        return orderServiceDelegate;
    }
}