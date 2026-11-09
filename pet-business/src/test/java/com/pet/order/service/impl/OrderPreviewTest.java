package com.pet.order.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.pet.common.StatusCode;
import com.pet.customer.mapper.RatingMapper;
import com.pet.finance.service.AccountingService;
import com.pet.marketing.dto.CouponDiscountResult;
import com.pet.marketing.service.CouponService;
import com.pet.membership.dto.MembershipDiscountDTO;
import com.pet.membership.service.MembershipBenefitService;
import com.pet.mq.MessageSender;
import com.pet.order.dto.OrderBatchCreateRequestDTO;
import com.pet.order.dto.OrderBatchItemDTO;
import com.pet.order.dto.OrderCreateRequestDTO;
import com.pet.order.dto.OrderDTO;
import com.pet.order.dto.OrderPreviewRequestDTO;
import com.pet.order.dto.OrderQuoteDTO;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.mapper.PaymentMapper;
import com.pet.order.service.OrderSnapshotService;
import com.pet.order.service.OrderStatusBroadcaster;
import com.pet.pet.entity.Pet;
import com.pet.pet.mapper.PetMapper;
import com.pet.qualification.service.QualificationService;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 价格预览契约：预览与下单必须给出同一个数字，且预览不得产生任何写副作用。
 * <p>
 * 这是本次重构最核心的一条不变量——「预览金额 = 扣款金额」由两者调用同一个 quoteOrder 保证，
 * 本测试用真实调用链验证这一点，而不是断言两份实现各自的中间变量。
 * <p>
 * 计价口径已统一为按夜（night）：夜数 N、离店日 D+N、提前送达只加钱不顺延。
 */
@ExtendWith(MockitoExtension.class)
class OrderPreviewTest {

    private static final Long OWNER_ID = 7L;
    private static final Long PET_ID = 1L;
    private static final Long MERCHANT_ID = 10L;
    private static final Long SERVICE_ID = 5L;

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
        TableInfoHelper.initTableInfo(assistant, Pet.class);
        TableInfoHelper.initTableInfo(assistant, Merchant.class);
        TableInfoHelper.initTableInfo(assistant, ServiceItem.class);
        TableInfoHelper.initTableInfo(assistant, PetOrder.class);
        TableInfoHelper.initTableInfo(assistant, Keeper.class);
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

    private ServiceItem serviceItem(String unit, String price) {
        ServiceItem s = new ServiceItem();
        s.setId_wsh(SERVICE_ID);
        s.setMerchant_id_wsh(MERCHANT_ID);
        s.setUnit_wsh(unit);
        s.setPrice_wsh(new BigDecimal(price));
        s.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        return s;
    }

    private void stubCommon(ServiceItem serviceItem) {
        Pet pet = new Pet();
        pet.setId_wsh(PET_ID);
        pet.setOwner_id_wsh(OWNER_ID);
        lenient().when(petMapper.selectById(PET_ID)).thenReturn(pet);
        Pet pet2 = new Pet();
        pet2.setId_wsh(PET_ID + 1);
        pet2.setOwner_id_wsh(OWNER_ID);
        lenient().when(petMapper.selectById(PET_ID + 1)).thenReturn(pet2);

        Merchant merchant = new Merchant();
        merchant.setId_wsh(MERCHANT_ID);
        merchant.setStatus_wsh(StatusCode.MERCHANT_APPROVED.getValue());
        merchant.setFuture_booking_enabled_wsh(1);
        lenient().when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(merchant);
        lenient().when(serviceItemMapper.selectById(SERVICE_ID)).thenReturn(serviceItem);

        CouponDiscountResult coupon = new CouponDiscountResult();
        coupon.setCoupon_discount_wsh(BigDecimal.ZERO);
        coupon.setPlatform_subsidy_wsh(BigDecimal.ZERO);
        coupon.setSettlement_amount_wsh(BigDecimal.ZERO);
        coupon.setFinal_amount_wsh(BigDecimal.ZERO);
        lenient().when(couponService.previewForOrder(anyLong(), any(), any(), any(), anyLong(), any()))
                .thenAnswer(inv -> {
                    BigDecimal base = inv.getArgument(2);
                    BigDecimal longStay = inv.getArgument(3);
                    BigDecimal after = base == null || longStay == null
                            ? BigDecimal.ZERO : base.subtract(longStay);
                    CouponDiscountResult r = new CouponDiscountResult();
                    r.setCoupon_discount_wsh(BigDecimal.ZERO);
                    r.setPlatform_subsidy_wsh(BigDecimal.ZERO);
                    r.setSettlement_amount_wsh(after);
                    r.setFinal_amount_wsh(after);
                    return r;
                });

        lenient().when(membershipBenefitService.previewOrderDiscount(anyLong(), any()))
                .thenAnswer(inv -> {
                    BigDecimal base = inv.getArgument(1);
                    MembershipDiscountDTO m = new MembershipDiscountDTO();
                    m.setMembership_discount_wsh(BigDecimal.ZERO);
                    m.setFinal_amount_wsh(base == null ? BigDecimal.ZERO : base);
                    return m;
                });

        // 冲突查询默认无冲突
        lenient().when(orderMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
    }

    /** 入住日统一取「后天」，避免与「送达必须晚于当前时间」冲突。 */
    private LocalDate checkin() {
        return LocalDate.now().plusDays(2);
    }

    private OrderPreviewRequestDTO nightPreview(LocalDate checkinDate, int nights,
                                                LocalDateTime delivery, LocalDateTime pickup) {
        OrderPreviewRequestDTO r = new OrderPreviewRequestDTO();
        r.setPet_id_wsh(PET_ID);
        r.setService_id_wsh(SERVICE_ID);
        r.setMerchant_id_wsh(MERCHANT_ID);
        r.setBilling_unit_wsh("night");
        r.setStart_date_wsh(checkinDate);
        r.setQuantity_wsh(nights);
        r.setDelivery_time_wsh(delivery);
        r.setPickup_time_wsh(pickup);
        return r;
    }

    private OrderCreateRequestDTO nightCreate(LocalDate checkinDate, int nights,
                                              LocalDateTime delivery, LocalDateTime pickup) {
        OrderCreateRequestDTO create = new OrderCreateRequestDTO();
        create.setPet_id_wsh(PET_ID);
        create.setService_id_wsh(SERVICE_ID);
        create.setMerchant_id_wsh(MERCHANT_ID);
        create.setBilling_unit_wsh("night");
        create.setStart_date_wsh(checkinDate);
        create.setQuantity_wsh(nights);
        create.setDelivery_time_wsh(delivery);
        create.setPickup_time_wsh(pickup);
        create.setEmergency_contact_name_wsh("赵测试");
        create.setEmergency_contact_phone_wsh("13800001009");
        return create;
    }

    // ============ 按夜计价 ============

    @Test
    void nightPreviewChargesNightsPlusEarlyHours() {
        stubCommon(serviceItem("night", "150.00"));
        LocalDate d = checkin();
        OrderQuoteDTO q = service().previewOrder(OWNER_ID,
                nightPreview(d, 3, d.atTime(11, 0), d.plusDays(3).atTime(14, 30)));

        assertEquals("night", q.getBilling_unit_wsh());
        assertEquals(3, q.getBooked_nights_wsh());
        assertEquals(3, q.getQuantity_wsh());
        assertEquals(3, q.getDays_wsh());
        assertEquals(3, q.getEarly_extra_hours_wsh());
        assertEquals(0, q.getEarly_extra_nights_wsh());
        // 450 + 3 小时 × (150/24) = 468.75
        assertEquals(new BigDecimal("468.75"), q.getTotal_amount_wsh());
        assertEquals(new BigDecimal("18.75"), q.getEarly_amount_wsh());
        assertEquals(new BigDecimal("468.75"), q.getFinal_amount_wsh());
        assertEquals(d.plusDays(3).atTime(14, 0), q.getStandard_checkout_at_wsh());
        assertEquals(d.plusDays(3).atTime(14, 30), q.getFree_checkout_until_wsh());
        // 预约在免费截止时刻，预估晚接为 0
        assertEquals(0, q.getEstimated_overtime_hours_wsh());
        assertEquals(new BigDecimal("0.00"), q.getEstimated_overtime_amount_wsh());
    }

    @Test
    void nightEarlyBeyondThreeHoursChargesOneNightAndKeepsCheckoutDate() {
        stubCommon(serviceItem("night", "150.00"));
        LocalDate d = checkin();
        OrderQuoteDTO q = service().previewOrder(OWNER_ID,
                nightPreview(d, 3, d.atTime(10, 59, 59), d.plusDays(3).atTime(14, 30)));

        assertEquals(3, q.getBooked_nights_wsh());
        assertEquals(1, q.getEarly_extra_nights_wsh());
        assertEquals(0, q.getEarly_extra_hours_wsh());
        assertEquals(new BigDecimal("600.00"), q.getTotal_amount_wsh());
        // 提前加收一夜不改离店日期
        assertEquals(d.plusDays(3), q.getEnd_date_wsh());
        assertEquals(d.plusDays(3).atTime(14, 0), q.getStandard_checkout_at_wsh());
    }

    @Test
    void nightPreviewDoesNotPrepayOvertime() {
        stubCommon(serviceItem("night", "150.00"));
        LocalDate d = checkin();
        // 预约 16:00 接走：预估 2 小时，但本次预付与 14:30 接走完全相同
        OrderQuoteDTO late = service().previewOrder(OWNER_ID,
                nightPreview(d, 3, d.atTime(14, 0), d.plusDays(3).atTime(16, 0)));
        OrderQuoteDTO normal = service().previewOrder(OWNER_ID,
                nightPreview(d, 3, d.atTime(14, 0), d.plusDays(3).atTime(14, 30)));

        assertEquals(2, late.getEstimated_overtime_hours_wsh());
        assertEquals(new BigDecimal("12.50"), late.getEstimated_overtime_amount_wsh());
        assertEquals(0, normal.getEstimated_overtime_hours_wsh());
        assertEquals(new BigDecimal("450.00"), late.getFinal_amount_wsh());
        assertEquals(normal.getFinal_amount_wsh(), late.getFinal_amount_wsh());
    }

    @Test
    void nightRejectsZeroNights() {
        stubCommon(serviceItem("night", "150.00"));
        LocalDate d = checkin();
        BusinessException ex = assertThrows(BusinessException.class, () -> service().previewOrder(OWNER_ID,
                nightPreview(d, 0, d.atTime(14, 0), d.plusDays(1).atTime(14, 30))));
        assertEquals(400, ex.getCode());
    }

    @Test
    void nightRejectsPickupOutsideCheckoutDate() {
        stubCommon(serviceItem("night", "150.00"));
        LocalDate d = checkin();
        // 3 夜的离店日是 D+3；把接走时间推到 D+4 必须拒绝（想多住应增加夜数）
        BusinessException ex = assertThrows(BusinessException.class, () -> service().previewOrder(OWNER_ID,
                nightPreview(d, 3, d.atTime(14, 0), d.plusDays(4).atTime(14, 0))));
        assertEquals(400, ex.getCode());
    }

    @Test
    void nightRejectsDeliveryNotOnCheckinDate() {
        stubCommon(serviceItem("night", "150.00"));
        LocalDate d = checkin();
        BusinessException ex = assertThrows(BusinessException.class, () -> service().previewOrder(OWNER_ID,
                nightPreview(d, 3, d.plusDays(1).atTime(14, 0), d.plusDays(3).atTime(14, 30))));
        assertEquals(400, ex.getCode());
    }

    @Test
    void nightRejectsEndDateNotMatchingDerivedCheckout() {
        stubCommon(serviceItem("night", "150.00"));
        LocalDate d = checkin();
        OrderPreviewRequestDTO r = nightPreview(d, 3, d.atTime(14, 0), d.plusDays(3).atTime(14, 30));
        r.setEnd_date_wsh(d.plusDays(2));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service().previewOrder(OWNER_ID, r));
        assertEquals(400, ex.getCode());
    }

    @Test
    void nightPreviewAndCreateReturnSameAmount() {
        stubCommon(serviceItem("night", "100.00"));
        PetOrder[] inserted = new PetOrder[1];
        lenient().when(orderMapper.insert(any(PetOrder.class))).thenAnswer(inv -> {
            PetOrder o = inv.getArgument(0);
            o.setId_wsh(920L);
            inserted[0] = o;
            return 1;
        });
        User owner = new User();
        owner.setId_wsh(OWNER_ID);
        lenient().when(userMapper.selectById(OWNER_ID)).thenReturn(owner);

        LocalDate d = checkin();
        LocalDateTime delivery = d.atTime(13, 0);
        LocalDateTime pickup = d.plusDays(1).atTime(14, 30);

        OrderQuoteDTO quote = service().previewOrder(OWNER_ID, nightPreview(d, 1, delivery, pickup));
        OrderDTO order = service().createOrder(OWNER_ID, nightCreate(d, 1, delivery, pickup));

        // AE18：100/夜 + 提前 1 小时 = 104.17
        assertEquals(new BigDecimal("104.17"), quote.getTotal_amount_wsh());
        assertEquals(quote.getTotal_amount_wsh(), order.getTotal_amount_wsh());
        assertEquals(quote.getFinal_amount_wsh(), order.getFinal_amount_wsh());
        assertEquals(1, order.getQuantity_wsh());
        assertEquals(d.plusDays(1), order.getEnd_date_wsh());
        // 接走码与送达码是两套独立凭证（DTO 不外泄，直接检查落库实体）
        assertEquals(4, inserted[0].getPickup_code_wsh().length());
        assertEquals(4, inserted[0].getHandover_code_wsh().length());
        assertNotEquals(inserted[0].getHandover_code_wsh(), inserted[0].getPickup_code_wsh());
        assertEquals(0, inserted[0].getOvertime_hours_wsh());
        assertEquals(new BigDecimal("0.00"), inserted[0].getOvertime_amount_wsh());
    }

    @Test
    void previewProducesNoWriteSideEffects() {
        stubCommon(serviceItem("night", "150.00"));
        LocalDate d = checkin();
        service().previewOrder(OWNER_ID, nightPreview(d, 1, d.atTime(14, 0), d.plusDays(1).atTime(14, 30)));

        // 预览绝不能建单、锁券、锁会员权益、发广播、排超时
        verify(orderMapper, never()).insert(any(PetOrder.class));
        verify(couponService, never()).lockForOrder(anyLong(), any(), anyLong(), any(), any());
        verify(membershipBenefitService, never()).lockForOrder(anyLong(), any(), anyLong(), any());
        verify(orderSnapshotService, never()).createForOrder(any(), any(), any(), any(), any(), any(), any());
        verify(orderStatusBroadcaster, never()).broadcast(any());
    }

    @Test
    void previewRejectsPetOwnedByOthers() {
        stubCommon(serviceItem("night", "150.00"));
        Pet other = new Pet();
        other.setId_wsh(PET_ID);
        other.setOwner_id_wsh(999L);
        lenient().when(petMapper.selectById(PET_ID)).thenReturn(other);

        LocalDate d = checkin();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service().previewOrder(OWNER_ID,
                        nightPreview(d, 1, d.atTime(14, 0), d.plusDays(1).atTime(14, 30))));
        assertEquals(403, ex.getCode());
    }

    @Test
    void previewReportsPriceChangeWhenExpectedPriceDiffers() {
        stubCommon(serviceItem("night", "150.00"));
        LocalDate d = checkin();
        OrderPreviewRequestDTO r = nightPreview(d, 1, d.atTime(14, 0), d.plusDays(1).atTime(14, 30));
        r.setExpected_unit_price_wsh(new BigDecimal("140.00"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service().previewOrder(OWNER_ID, r));
        assertEquals(400, ex.getCode());
    }

    /** 有券 + 有会员折扣时，预览与下单的每一个金额字段都必须一致（此时 final != total）。 */
    @Test
    void previewAndCreateAgreeWhenCouponAndMembershipApply() {
        stubCommon(serviceItem("night", "150.00"));
        lenient().when(couponService.previewForOrder(anyLong(), any(), any(), any(), anyLong(), any()))
                .thenAnswer(inv -> {
                    BigDecimal base = inv.getArgument(2);
                    BigDecimal longStay = inv.getArgument(3);
                    BigDecimal afterLongStay = base.subtract(longStay);
                    CouponDiscountResult r = new CouponDiscountResult();
                    r.setCoupon_discount_wsh(new BigDecimal("10.00"));
                    r.setPlatform_subsidy_wsh(new BigDecimal("10.00"));
                    r.setSettlement_amount_wsh(afterLongStay.subtract(new BigDecimal("10.00")));
                    r.setFinal_amount_wsh(afterLongStay.subtract(new BigDecimal("10.00")));
                    return r;
                });
        lenient().when(membershipBenefitService.previewOrderDiscount(anyLong(), any()))
                .thenAnswer(inv -> {
                    BigDecimal afterCoupon = inv.getArgument(1);
                    MembershipDiscountDTO m = new MembershipDiscountDTO();
                    m.setMembership_discount_wsh(new BigDecimal("5.00"));
                    m.setFinal_amount_wsh(afterCoupon.subtract(new BigDecimal("5.00")));
                    return m;
                });
        lenient().when(orderMapper.insert(any(PetOrder.class))).thenAnswer(inv -> {
            PetOrder o = inv.getArgument(0);
            o.setId_wsh(901L);
            return 1;
        });
        User owner = new User();
        owner.setId_wsh(OWNER_ID);
        lenient().when(userMapper.selectById(OWNER_ID)).thenReturn(owner);

        LocalDate d = checkin();
        LocalDateTime delivery = d.atTime(14, 0);
        LocalDateTime pickup = d.plusDays(3).atTime(14, 30);

        OrderQuoteDTO quote = service().previewOrder(OWNER_ID, nightPreview(d, 3, delivery, pickup));
        OrderDTO order = service().createOrder(OWNER_ID, nightCreate(d, 3, delivery, pickup));

        // 450 - 10 - 5 = 435，确保 final 与 total 不同，能杀掉 setFinal→setTotal 的变异
        assertEquals(new BigDecimal("450.00"), quote.getTotal_amount_wsh());
        assertEquals(new BigDecimal("435.00"), quote.getFinal_amount_wsh());
        assertEquals(new BigDecimal("10.00"), quote.getCoupon_discount_wsh());
        assertEquals(new BigDecimal("5.00"), quote.getMembership_discount_wsh());
        assertEquals(new BigDecimal("15.00"), quote.getPlatform_subsidy_wsh());

        assertEquals(quote.getTotal_amount_wsh(), order.getTotal_amount_wsh());
        assertEquals(quote.getFinal_amount_wsh(), order.getFinal_amount_wsh());
        assertEquals(quote.getCoupon_discount_wsh(), order.getCoupon_discount_wsh());
        assertEquals(quote.getMembership_discount_wsh(), order.getMembership_discount_wsh());
        assertEquals(quote.getPlatform_subsidy_wsh(), order.getPlatform_subsidy_wsh());
        assertEquals(quote.getDays_wsh(), order.getDays_wsh());
        assertEquals(new BigDecimal("15.00"), quote.getDiscount_wsh());
        assertEquals(quote.getDiscount_wsh(), order.getDiscount_wsh());
        // 时间字段逐项比对：把 Quote 里相邻的 LocalDateTime 参数写错位就会红
        assertEquals(quote.getStart_date_wsh(), order.getStart_date_wsh());
        assertEquals(quote.getEnd_date_wsh(), order.getEnd_date_wsh());
        assertEquals(quote.getDelivery_time_wsh(), order.getDelivery_time_wsh());
        assertEquals(quote.getPickup_time_wsh(), order.getPickup_time_wsh());
    }

    /** 批量下单不再要求指定看护者：不传 keeper 时整批进入待分配池。 */
    @Test
    void batchCreateWithoutKeeperEntersAssignmentPool() {
        stubCommon(serviceItem("night", "150.00"));
        lenient().when(orderMapper.insert(any(PetOrder.class))).thenAnswer(inv -> {
            PetOrder o = inv.getArgument(0);
            o.setId_wsh(910L + Math.abs(o.hashCode() % 50));
            return 1;
        });
        User owner = new User();
        owner.setId_wsh(OWNER_ID);
        lenient().when(userMapper.selectById(OWNER_ID)).thenReturn(owner);

        LocalDate d = checkin();
        OrderBatchCreateRequestDTO batch = new OrderBatchCreateRequestDTO();
        batch.setService_id_wsh(SERVICE_ID);
        batch.setMerchant_id_wsh(MERCHANT_ID);
        OrderBatchItemDTO first = new OrderBatchItemDTO();
        first.setPet_id_wsh(PET_ID);
        first.setStart_date_wsh(d);
        first.setEnd_date_wsh(d.plusDays(1));
        first.setQuantity_wsh(1);
        first.setDelivery_time_wsh(d.atTime(14, 0));
        first.setPickup_time_wsh(d.plusDays(1).atTime(14, 30));
        OrderBatchItemDTO second = new OrderBatchItemDTO();
        second.setPet_id_wsh(PET_ID + 1);
        second.setStart_date_wsh(d);
        second.setEnd_date_wsh(d.plusDays(1));
        second.setQuantity_wsh(1);
        second.setDelivery_time_wsh(d.atTime(14, 0));
        second.setPickup_time_wsh(d.plusDays(1).atTime(14, 30));
        batch.setItems(List.of(first, second));

        List<OrderDTO> orders = service().createOrders(OWNER_ID, batch);

        assertEquals(2, orders.size());
        assertNull(orders.get(0).getKeeper_id_wsh());
        assertNull(orders.get(1).getKeeper_id_wsh());
        assertEquals(new BigDecimal("150.00"), orders.get(0).getFinal_amount_wsh());
        assertEquals(1, orders.get(0).getQuantity_wsh());
        assertEquals(d.plusDays(1), orders.get(0).getEnd_date_wsh());
    }
}
