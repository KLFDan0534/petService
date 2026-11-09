package com.pet.order.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.pet.common.BusinessException;
import com.pet.common.OrderStatus;
import com.pet.common.StatusCode;
import com.pet.customer.mapper.RatingMapper;
import com.pet.finance.service.AccountingService;
import com.pet.marketing.service.CouponService;
import com.pet.membership.service.MembershipBenefitService;
import com.pet.mq.MessageSender;
import com.pet.order.dto.OrderCheckoutDTO;
import com.pet.order.dto.OrderCompleteRequestDTO;
import com.pet.order.entity.Payment;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.mapper.PaymentMapper;
import com.pet.order.pricing.NightPricingRule;
import com.pet.order.pricing.PaymentPurpose;
import com.pet.order.service.OrderSnapshotService;
import com.pet.order.service.OrderStatusBroadcaster;
import com.pet.pet.mapper.PetMapper;
import com.pet.qualification.service.QualificationService;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 接走确认契约（U5）。
 * <p>
 * 覆盖：独立接走码（错码/送达码/格式不合法一律无写入）、keeper 越权与不在岗、
 * 0 费直接完成并结算、有费创建唯一补费单且保持服务中、重复确认返回原结果不重复建单。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderCheckoutTest {

    private static final Long OWNER_ID = 7L;
    private static final Long KEEPER_USER_ID = 30L;
    private static final Long KEEPER_ID = 20L;
    private static final Long MERCHANT_ID = 10L;
    private static final String ORDER_NO = "ORD-CHECKOUT-1";

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
    @Mock private RatingMapper ratingMapper;

    private final BusinessHoursTargetResolver resolver = new BusinessHoursTargetResolver();

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, PetOrder.class);
        TableInfoHelper.initTableInfo(assistant, Keeper.class);
        TableInfoHelper.initTableInfo(assistant, Merchant.class);
        TableInfoHelper.initTableInfo(assistant, ServiceItem.class);
        TableInfoHelper.initTableInfo(assistant, Payment.class);
    }

    private OrderServiceImpl service() {
        return new OrderServiceImpl(
                orderMapper, paymentMapper, petMapper, keeperMapper, merchantMapper,
                merchantService, serviceItemMapper, userMapper, orderSnapshotService,
                eventPublisher, qualificationService, orderStatusBroadcaster,
                messageSender, accountingService, keeperAttendanceService,
                keeperLeaveService, couponService, membershipBenefitService,
                new ObjectMapper(), businessHoursService, resolver, ratingMapper);
    }

    private PetOrder inProgressNightOrder() {
        PetOrder order = new PetOrder();
        order.setId_wsh(100L);
        order.setOrder_no_wsh(ORDER_NO);
        order.setOwner_id_wsh(OWNER_ID);
        order.setPet_id_wsh(1L);
        order.setKeeper_id_wsh(KEEPER_ID);
        order.setMerchant_id_wsh(MERCHANT_ID);
        order.setBilling_unit_wsh("night");
        order.setQuantity_wsh(1);
        order.setUnit_price_wsh(new BigDecimal("150.00"));
        order.setStart_date_wsh(LocalDate.now().minusDays(2));
        order.setEnd_date_wsh(LocalDate.now().minusDays(1));
        order.setDelivery_time_wsh(LocalDate.now().minusDays(2).atTime(14, 0));
        order.setPickup_time_wsh(LocalDate.now().minusDays(1).atTime(14, 30));
        order.setTotal_amount_wsh(new BigDecimal("150.00"));
        order.setFinal_amount_wsh(new BigDecimal("150.00"));
        order.setSettlement_amount_wsh(new BigDecimal("150.00"));
        order.setHandover_code_wsh("1234");
        order.setPickup_code_wsh("0042");
        order.setOvertime_hours_wsh(0);
        order.setOvertime_amount_wsh(new BigDecimal("0.00"));
        order.setStatus_wsh(OrderStatus.IN_PROGRESS);
        order.setCreated_at_wsh(LocalDateTime.now().minusDays(3));
        return order;
    }

    private void stubAccess(PetOrder order) {
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(orderMapper.selectByIdForUpdate(order.getId_wsh())).thenReturn(order);

        Keeper keeper = new Keeper();
        keeper.setId_wsh(KEEPER_ID);
        keeper.setUser_id_wsh(KEEPER_USER_ID);
        keeper.setMerchant_id_wsh(MERCHANT_ID);
        keeper.setStatus_wsh(StatusCode.KEEPER_ACTIVE.getValue());
        keeper.setMax_pets_wsh(3);
        when(keeperMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(keeper));
        when(keeperMapper.selectById(KEEPER_ID)).thenReturn(keeper);

        Merchant merchant = new Merchant();
        merchant.setId_wsh(MERCHANT_ID);
        merchant.setUser_id_wsh(99L);
        merchant.setStatus_wsh(StatusCode.MERCHANT_APPROVED.getValue());
        merchant.setStore_status_wsh(1);
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(merchant);

        when(orderMapper.update(any(PetOrder.class), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(paymentMapper.insert(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId_wsh(555L);
            return 1;
        });
    }

    private OrderCompleteRequestDTO request(String code) {
        OrderCompleteRequestDTO dto = new OrderCompleteRequestDTO();
        dto.setOrder_no_wsh(ORDER_NO);
        dto.setPickup_code_wsh(code);
        return dto;
    }

    // ============ 接走码 ============

    @Test
    void wrongPickupCodeWritesNothing() {
        PetOrder order = inProgressNightOrder();
        stubAccess(order);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service().completeOrder(KEEPER_USER_ID, request("9999")));

        assertEquals(400, ex.getCode());
        verify(orderMapper, never()).update(any(PetOrder.class), any(LambdaUpdateWrapper.class));
        verify(paymentMapper, never()).insert(any(Payment.class));
    }

    @Test
    void handoverCodeCannotBeUsedToCheckout() {
        PetOrder order = inProgressNightOrder();
        stubAccess(order);

        // AE14：送达码 1234 不能确认接走
        assertThrows(BusinessException.class,
                () -> service().completeOrder(KEEPER_USER_ID, request("1234")));
        verify(orderMapper, never()).update(any(PetOrder.class), any(LambdaUpdateWrapper.class));
    }

    @Test
    void malformedPickupCodeRejected() {
        PetOrder order = inProgressNightOrder();
        stubAccess(order);

        assertThrows(BusinessException.class,
                () -> service().completeOrder(KEEPER_USER_ID, request("42")));
        assertThrows(BusinessException.class,
                () -> service().completeOrder(KEEPER_USER_ID, request("00a2")));
        verify(orderMapper, never()).update(any(PetOrder.class), any(LambdaUpdateWrapper.class));
    }

    @Test
    void keeperNotOnDutyRejected() {
        PetOrder order = inProgressNightOrder();
        stubAccess(order);
        org.mockito.Mockito.doThrow(new BusinessException(400, "不在岗"))
                .when(keeperAttendanceService)
                .requireKeeperOnDuty(eq(KEEPER_ID), eq(MERCHANT_ID));

        assertThrows(BusinessException.class,
                () -> service().completeOrder(KEEPER_USER_ID, request("0042")));
        verify(paymentMapper, never()).insert(any(Payment.class));
    }

    @Test
    void unrelatedUserRejected() {
        PetOrder order = inProgressNightOrder();
        stubAccess(order);
        // 该用户既不是本单 keeper 也不是商家
        when(keeperMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service().completeOrder(888L, request("0042")));
        assertEquals(403, ex.getCode());
    }

    @Test
    void wrongStatusRejected() {
        PetOrder order = inProgressNightOrder();
        order.setStatus_wsh(OrderStatus.CONFIRMED);
        stubAccess(order);

        assertThrows(BusinessException.class,
                () -> service().completeOrder(KEEPER_USER_ID, request("0042")));
        verify(paymentMapper, never()).insert(any(Payment.class));
    }

    // ============ 0 费：直接完成 ============

    @Test
    void freeCheckoutCompletesAndSettlesImmediately() {
        PetOrder order = inProgressNightOrder();
        stubAccess(order);
        // 免费截止远在未来 → H=0
        when(orderSnapshotService.requireNightPricingRule(100L)).thenReturn(new NightPricingRule(
                new BigDecimal("150.00"), 1,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(1).plusMinutes(30)));

        OrderCheckoutDTO result = service().completeOrder(KEEPER_USER_ID, request("0042"));

        assertTrue(result.getCompleted_wsh());
        assertEquals(0, result.getOvertime_hours_wsh());
        assertEquals(new BigDecimal("0.00"), result.getOvertime_amount_wsh());
        assertNull(result.getOvertime_pay_no_wsh());
        // 0 费不创建补费单
        verify(paymentMapper, never()).insert(any(Payment.class));
        // 结算一次
        verify(accountingService, times(1)).transfer(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    // ============ 有费：建补费单并保持服务中 ============

    @Test
    void overtimeCreatesSinglePaymentAndKeepsOrderInProgress() {
        PetOrder order = inProgressNightOrder();
        stubAccess(order);
        // 免费截止在 2 小时前 → H=2 → 12.50
        when(orderSnapshotService.requireNightPricingRule(100L)).thenReturn(new NightPricingRule(
                new BigDecimal("150.00"), 1,
                LocalDateTime.now().minusHours(2).plusSeconds(30),
                LocalDateTime.now().minusHours(2).plusSeconds(30)));

        OrderCheckoutDTO result = service().completeOrder(KEEPER_USER_ID, request("0042"));

        assertFalse(result.getCompleted_wsh());
        assertEquals(2, result.getOvertime_hours_wsh());
        assertEquals(new BigDecimal("12.50"), result.getOvertime_amount_wsh());
        assertNotNull(result.getOvertime_pay_no_wsh());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentMapper, times(1)).insert(captor.capture());
        Payment payment = captor.getValue();
        assertEquals(PaymentPurpose.NIGHT_OVERTIME, payment.getPurpose_wsh());
        assertEquals(new BigDecimal("12.50"), payment.getAmount_wsh());
        assertEquals("pending", payment.getStatus_wsh());
        assertEquals("balance", payment.getMethod_wsh());
        // 有费时不得结算、不得完成
        verify(accountingService, never()).transfer(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void repeatConfirmationReturnsFrozenResultWithoutSecondPayment() {
        PetOrder order = inProgressNightOrder();
        stubAccess(order);
        when(orderSnapshotService.requireNightPricingRule(100L)).thenReturn(new NightPricingRule(
                new BigDecimal("150.00"), 1,
                LocalDateTime.now().minusHours(2).plusSeconds(30),
                LocalDateTime.now().minusHours(2).plusSeconds(30)));
        service().completeOrder(KEEPER_USER_ID, request("0042"));

        // 模拟已落库的冻结结果
        order.setCheckout_confirmed_at_wsh(LocalDateTime.now().minusMinutes(5));
        order.setOvertime_hours_wsh(2);
        order.setOvertime_amount_wsh(new BigDecimal("12.50"));
        order.setOvertime_payment_id_wsh(555L);
        Payment payment = new Payment();
        payment.setId_wsh(555L);
        payment.setPay_no_wsh("PAY-OVERTIME");
        payment.setStatus_wsh("pending");
        when(paymentMapper.selectById(555L)).thenReturn(payment);

        OrderCheckoutDTO second = service().completeOrder(KEEPER_USER_ID, request("0042"));

        assertEquals(2, second.getOvertime_hours_wsh());
        assertEquals(new BigDecimal("12.50"), second.getOvertime_amount_wsh());
        assertEquals("PAY-OVERTIME", second.getOvertime_pay_no_wsh());
        // 只建过一笔补费单
        verify(paymentMapper, times(1)).insert(any(Payment.class));
        // 重复确认不重新计时
        assertEquals(order.getCheckout_confirmed_at_wsh(), second.getCheckout_confirmed_at_wsh());
    }

    @Test
    void confirmedOrderRetrySucceedsEvenIfKeeperWentOffDuty() {
        PetOrder order = inProgressNightOrder();
        order.setCheckout_confirmed_at_wsh(LocalDateTime.now().minusMinutes(5));
        order.setOvertime_hours_wsh(0);
        order.setOvertime_amount_wsh(new BigDecimal("0.00"));
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        Keeper keeper = new Keeper();
        keeper.setId_wsh(KEEPER_ID);
        keeper.setUser_id_wsh(KEEPER_USER_ID);
        when(keeperMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(keeper));

        OrderCheckoutDTO result = service().completeOrder(KEEPER_USER_ID, request("0042"));

        assertEquals(order.getCheckout_confirmed_at_wsh(), result.getCheckout_confirmed_at_wsh());
        // 已确认分支不再校验在岗
        verify(keeperAttendanceService, never()).requireKeeperOnDuty(anyLong(), anyLong());
    }

    // ============ 补费成功后的累计与完成（U6） ============

    @Test
    void applyOvertimePaymentAccumulatesOnceAndCompletes() {
        PetOrder order = inProgressNightOrder();
        order.setOvertime_hours_wsh(2);
        order.setOvertime_amount_wsh(new BigDecimal("12.50"));
        order.setOvertime_payment_id_wsh(555L);
        stubAccess(order);

        service().applyOvertimePayment(100L, 555L);

        ArgumentCaptor<PetOrder> captor = ArgumentCaptor.forClass(PetOrder.class);
        verify(orderMapper).updateById(captor.capture());
        // 只累计一次：150 + 12.50
        assertEquals(new BigDecimal("162.50"), captor.getValue().getTotal_amount_wsh());
        assertEquals(new BigDecimal("162.50"), captor.getValue().getFinal_amount_wsh());
        assertEquals(new BigDecimal("162.50"), captor.getValue().getSettlement_amount_wsh());
        // 结算按累计后的金额，只发生一次
        verify(accountingService, times(1)).transfer(any(), any(),
                eq(new BigDecimal("162.50")), any(), any(), any(), any(), any(), any());
    }

    @Test
    void applyOvertimePaymentIsIdempotentForCompletedOrder() {
        PetOrder order = inProgressNightOrder();
        order.setStatus_wsh(OrderStatus.COMPLETED);
        order.setOvertime_payment_id_wsh(555L);
        when(orderMapper.selectByIdForUpdate(100L)).thenReturn(order);

        service().applyOvertimePayment(100L, 555L);

        verify(orderMapper, never()).updateById(any(PetOrder.class));
        verify(accountingService, never()).transfer(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void applyOvertimePaymentRejectsMismatchedPaymentId() {
        PetOrder order = inProgressNightOrder();
        order.setOvertime_payment_id_wsh(555L);
        when(orderMapper.selectByIdForUpdate(100L)).thenReturn(order);

        assertThrows(BusinessException.class, () -> service().applyOvertimePayment(100L, 999L));
        verify(orderMapper, never()).updateById(any(PetOrder.class));
    }
}
