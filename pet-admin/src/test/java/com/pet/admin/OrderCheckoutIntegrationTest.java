package com.pet.admin;

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
import com.pet.order.service.impl.OrderServiceImpl;
import com.pet.order.service.impl.PaymentServiceImpl;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 接走结算集成契约。
 * <p>
 * 这里把 {@link OrderServiceImpl} 与 {@link PaymentServiceImpl} 真实串起来（共用同一批 Mapper mock），
 * 验证「确认接走 → 生成补费单 → 主人余额支付 → 累计金额 → 完成并结算」整条链路只扣一次款、
 * 只累计一次、只结算一次，并且金额与支付单总额一致。
 * <p>
 * 另外校验 MySQL 与 H2 两份 schema 的新增列/默认值完全一致，防止两边漂移。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderCheckoutIntegrationTest {

    private static final Long OWNER_ID = 7L;
    private static final Long KEEPER_USER_ID = 30L;
    private static final Long KEEPER_ID = 20L;
    private static final Long MERCHANT_ID = 10L;
    private static final Long ORDER_ID = 100L;
    private static final String ORDER_NO = "ORD-IT-CHECKOUT";

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

    private OrderServiceImpl orderService() {
        return new OrderServiceImpl(
                orderMapper, paymentMapper, petMapper, keeperMapper, merchantMapper,
                merchantService, serviceItemMapper, userMapper, orderSnapshotService,
                eventPublisher, qualificationService, orderStatusBroadcaster,
                messageSender, accountingService, keeperAttendanceService,
                keeperLeaveService, couponService, membershipBenefitService,
                new ObjectMapper(), businessHoursService, resolver, ratingMapper);
    }

    private PaymentServiceImpl paymentService(OrderServiceImpl orderService) {
        return new PaymentServiceImpl(paymentMapper, orderMapper, orderStatusBroadcaster,
                accountingService, couponService, membershipBenefitService, messageSender, orderService);
    }

    private PetOrder nightOrderInProgress() {
        PetOrder order = new PetOrder();
        order.setId_wsh(ORDER_ID);
        order.setOrder_no_wsh(ORDER_NO);
        order.setOwner_id_wsh(OWNER_ID);
        order.setPet_id_wsh(1L);
        order.setKeeper_id_wsh(KEEPER_ID);
        order.setMerchant_id_wsh(MERCHANT_ID);
        order.setBilling_unit_wsh("night");
        order.setQuantity_wsh(3);
        order.setUnit_price_wsh(new BigDecimal("150.00"));
        order.setStart_date_wsh(LocalDate.now().minusDays(5));
        order.setEnd_date_wsh(LocalDate.now().minusDays(2));
        order.setDelivery_time_wsh(LocalDate.now().minusDays(5).atTime(14, 0));
        order.setPickup_time_wsh(LocalDate.now().minusDays(2).atTime(14, 30));
        order.setTotal_amount_wsh(new BigDecimal("450.00"));
        order.setFinal_amount_wsh(new BigDecimal("450.00"));
        order.setSettlement_amount_wsh(new BigDecimal("450.00"));
        order.setHandover_code_wsh("1234");
        order.setPickup_code_wsh("0007");
        order.setOvertime_hours_wsh(0);
        order.setOvertime_amount_wsh(new BigDecimal("0.00"));
        order.setStatus_wsh(OrderStatus.IN_PROGRESS);
        order.setCreated_at_wsh(LocalDateTime.now().minusDays(6));
        return order;
    }

    private void stubShared(PetOrder order, AtomicReference<Payment> inserted) {
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(orderMapper.selectByIdForUpdate(ORDER_ID)).thenReturn(order);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(orderMapper.update(any(PetOrder.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

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

        when(accountingService.systemUserId()).thenReturn(1L);
        when(paymentMapper.insert(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId_wsh(555L);
            inserted.set(p);
            return 1;
        });
        when(paymentMapper.selectById(555L)).thenAnswer(inv -> inserted.get());
    }

    private OrderCompleteRequestDTO checkoutRequest(String code) {
        OrderCompleteRequestDTO dto = new OrderCompleteRequestDTO();
        dto.setOrder_no_wsh(ORDER_NO);
        dto.setPickup_code_wsh(code);
        return dto;
    }

    // ============ 完整链路：接走 → 待补费 → 余额支付 → 完成 ============

    @Test
    void checkoutThenBalancePaymentCompletesOnceAndAccumulatesOnce() {
        PetOrder order = nightOrderInProgress();
        AtomicReference<Payment> insertedPayment = new AtomicReference<>();
        stubShared(order, insertedPayment);
        // 免费截止在 2 小时前 → H=2 → 12.50
        when(orderSnapshotService.requireNightPricingRule(ORDER_ID)).thenReturn(new NightPricingRule(
                new BigDecimal("150.00"), 3,
                LocalDateTime.now().minusHours(2).plusSeconds(30),
                LocalDateTime.now().minusHours(2).plusSeconds(30)));

        OrderServiceImpl orders = orderService();
        PaymentServiceImpl payments = paymentService(orders);

        // 1. keeper 输入接走码：生成待补费单，订单仍服务中
        OrderCheckoutDTO checkout = orders.completeOrder(KEEPER_USER_ID, checkoutRequest("0007"));
        assertFalse(checkout.getCompleted_wsh());
        assertEquals(2, checkout.getOvertime_hours_wsh());
        assertEquals(new BigDecimal("12.50"), checkout.getOvertime_amount_wsh());
        assertNotNull(checkout.getOvertime_pay_no_wsh());
        assertNotNull(insertedPayment.get());
        assertEquals(PaymentPurpose.NIGHT_OVERTIME, insertedPayment.get().getPurpose_wsh());

        // 2. 主人余额支付同一笔补费单
        Payment pending = insertedPayment.get();
        order.setOvertime_payment_id_wsh(pending.getId_wsh());
        when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(pending);
        when(paymentMapper.update(any(Payment.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

        payments.pay(OWNER_ID, pending.getPay_no_wsh());

        // 3. 只扣一次、只入账一次
        verify(accountingService, times(1)).debit(eq(OWNER_ID), eq(new BigDecimal("12.50")),
                eq("payment"), eq(ORDER_ID), any(), any(), any(), any());
        verify(accountingService, times(1)).credit(eq(1L), eq(new BigDecimal("12.50")),
                eq("payment"), eq(ORDER_ID), any(), any(), any(), any());
        // 4. 累计一次并结算一次（450 + 12.50）
        ArgumentCaptor<PetOrder> updates = ArgumentCaptor.forClass(PetOrder.class);
        verify(orderMapper, atLeast(1)).updateById(updates.capture());
        long accumulateCount = updates.getAllValues().stream()
                .filter(o -> o.getTotal_amount_wsh() != null)
                .count();
        assertEquals(1, accumulateCount, "补费金额只能累计一次");
        verify(accountingService, times(1)).transfer(any(), any(),
                eq(new BigDecimal("462.50")), any(), any(), any(), any(), any(), any());
        assertEquals(OrderStatus.COMPLETED, order.getStatus_wsh());
        assertEquals(new BigDecimal("462.50"), order.getFinal_amount_wsh());
        // 5. 原支付路径不得被补费触发
        verify(couponService, never()).markUsedForOrder(anyLong(), any());
        verify(membershipBenefitService, never()).markUsedForOrder(anyLong(), any());
    }

    @Test
    void duplicateCheckoutThenDuplicatePaymentSettleExactlyOnce() {
        PetOrder order = nightOrderInProgress();
        AtomicReference<Payment> insertedPayment = new AtomicReference<>();
        stubShared(order, insertedPayment);
        when(orderSnapshotService.requireNightPricingRule(ORDER_ID)).thenReturn(new NightPricingRule(
                new BigDecimal("150.00"), 3,
                LocalDateTime.now().minusHours(2).plusSeconds(30),
                LocalDateTime.now().minusHours(2).plusSeconds(30)));

        OrderServiceImpl orders = orderService();
        PaymentServiceImpl payments = paymentService(orders);

        orders.completeOrder(KEEPER_USER_ID, checkoutRequest("0007"));
        // 模拟落库后的冻结结果
        order.setCheckout_confirmed_at_wsh(LocalDateTime.now());
        order.setOvertime_hours_wsh(2);
        order.setOvertime_amount_wsh(new BigDecimal("12.50"));
        order.setOvertime_payment_id_wsh(555L);
        // 第二次接走确认：返回原结果，不建第二笔补费单
        orders.completeOrder(KEEPER_USER_ID, checkoutRequest("0007"));
        verify(paymentMapper, times(1)).insert(any(Payment.class));

        // 两次支付：只成功一次
        Payment pending = insertedPayment.get();
        when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(pending);
        when(paymentMapper.update(any(Payment.class), any(LambdaUpdateWrapper.class))).thenReturn(1, 0);
        payments.pay(OWNER_ID, pending.getPay_no_wsh());
        payments.pay(OWNER_ID, pending.getPay_no_wsh());

        verify(accountingService, times(1)).debit(any(), any(), any(), any(), any(), any(), any(), any());
        ArgumentCaptor<PetOrder> updates = ArgumentCaptor.forClass(PetOrder.class);
        verify(orderMapper, atLeast(1)).updateById(updates.capture());
        assertEquals(1, updates.getAllValues().stream()
                .filter(o -> o.getTotal_amount_wsh() != null).count(), "补费金额只能累计一次");
    }

    @Test
    void wrongPickupCodeDoesNotCreateAnyPayment() {
        PetOrder order = nightOrderInProgress();
        AtomicReference<Payment> insertedPayment = new AtomicReference<>();
        stubShared(order, insertedPayment);

        assertThrowsBusiness(() -> orderService().completeOrder(KEEPER_USER_ID, checkoutRequest("9999")));

        verify(paymentMapper, never()).insert(any(Payment.class));
        assertEquals(OrderStatus.IN_PROGRESS, order.getStatus_wsh());
    }

    private void assertThrowsBusiness(Runnable action) {
        try {
            action.run();
            throw new AssertionError("expected BusinessException");
        } catch (BusinessException expected) {
            assertEquals(400, expected.getCode());
        }
    }

    // ============ schema 一致性：MySQL 与 H2 同步 ============

    @Test
    void mysqlAndH2SchemasDeclareSameCheckoutColumns() throws Exception {
        String mysql = read("src/main/resources/db/schema.sql");
        String h2 = read("src/main/resources/h2-schema.sql");

        for (String column : new String[]{
                "pickup_code_wsh", "checkout_confirmed_at_wsh",
                "overtime_hours_wsh", "overtime_amount_wsh", "overtime_payment_id_wsh"}) {
            assertTrue(mysql.contains(column), "MySQL schema 缺少 " + column);
            assertTrue(h2.contains(column), "H2 schema 缺少 " + column);
        }
        assertTrue(mysql.contains("purpose_wsh"), "MySQL payment_wsh 缺少 purpose_wsh");
        assertTrue(h2.contains("purpose_wsh"), "H2 payment_wsh 缺少 purpose_wsh");
        // 补费/晚接金额必须有非空默认值，历史行迁移后不为 null
        assertTrue(mysql.contains("overtime_hours_wsh` INT NOT NULL DEFAULT 0"));
        assertTrue(h2.contains("overtime_hours_wsh` INT NOT NULL DEFAULT 0"));
        assertTrue(mysql.contains("overtime_amount_wsh` DECIMAL(10, 2) NOT NULL DEFAULT 0"));
        assertTrue(h2.contains("overtime_amount_wsh` DECIMAL(10, 2) NOT NULL DEFAULT 0"));
        assertTrue(mysql.contains("purpose_wsh` VARCHAR(32) NOT NULL DEFAULT 'order'"));
        assertTrue(h2.contains("purpose_wsh` VARCHAR(32) NOT NULL DEFAULT 'order'"));
    }

    private String read(String relativePath) throws Exception {
        // 测试工作目录是模块目录（pet-admin），同时兼容从仓库根目录运行的情况
        Path path = Path.of(relativePath);
        if (!Files.exists(path)) {
            path = Path.of("pet-admin").resolve(relativePath);
        }
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
