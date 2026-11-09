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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;

/**
 * 按夜下单的窗口校验与长住档位。
 * <p>
 * 覆盖：入住日必须与送达日一致、预约接走必须落在离店日、夜数上限、送达不能是过去、
 * 以及长住折扣档位（只看预订夜数 N）。
 */
@ExtendWith(MockitoExtension.class)
class OrderWindowValidationTest {

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
        // 无营业时间配置 → 只校验日期窗口与时刻先后，隔离出待测逻辑
        lenient().when(businessHoursService.getByMerchantId(MERCHANT_ID)).thenReturn(List.of());
        return new OrderServiceImpl(
                orderMapper, paymentMapper, petMapper, keeperMapper, merchantMapper,
                merchantService, serviceItemMapper, userMapper, orderSnapshotService,
                eventPublisher, qualificationService, orderStatusBroadcaster,
                messageSender, accountingService, keeperAttendanceService,
                keeperLeaveService, couponService, membershipBenefitService,
                new ObjectMapper(), businessHoursService, resolver, ratingMapper);
    }

    private LocalDate tomorrow() {
        return LocalDate.now().plusDays(1);
    }

    private void stubCommon() {
        Pet pet = new Pet();
        pet.setId_wsh(PET_ID);
        pet.setOwner_id_wsh(OWNER_ID);
        lenient().when(petMapper.selectById(PET_ID)).thenReturn(pet);

        Merchant merchant = new Merchant();
        merchant.setId_wsh(MERCHANT_ID);
        merchant.setStatus_wsh(StatusCode.MERCHANT_APPROVED.getValue());
        merchant.setFuture_booking_enabled_wsh(1);
        lenient().when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(merchant);

        ServiceItem item = new ServiceItem();
        item.setId_wsh(SERVICE_ID);
        item.setMerchant_id_wsh(MERCHANT_ID);
        item.setUnit_wsh("night");
        item.setPrice_wsh(new BigDecimal("150.00"));
        item.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        lenient().when(serviceItemMapper.selectById(SERVICE_ID)).thenReturn(item);

        CouponDiscountResult coupon = new CouponDiscountResult();
        coupon.setCoupon_discount_wsh(BigDecimal.ZERO);
        coupon.setPlatform_subsidy_wsh(BigDecimal.ZERO);
        coupon.setSettlement_amount_wsh(BigDecimal.ZERO);
        coupon.setFinal_amount_wsh(BigDecimal.ZERO);
        lenient().when(couponService.previewForOrder(anyLong(), any(), any(), any(), anyLong(), any()))
                .thenAnswer(inv -> {
                    BigDecimal base = inv.getArgument(2);
                    BigDecimal longStay = inv.getArgument(3);
                    BigDecimal after = base == null || longStay == null ? BigDecimal.ZERO : base.subtract(longStay);
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
        lenient().when(orderMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
    }

    private OrderPreviewRequestDTO nightRequest(LocalDate checkin, int nights,
                                                LocalDateTime delivery, LocalDateTime pickup) {
        OrderPreviewRequestDTO r = new OrderPreviewRequestDTO();
        r.setPet_id_wsh(PET_ID);
        r.setService_id_wsh(SERVICE_ID);
        r.setMerchant_id_wsh(MERCHANT_ID);
        r.setBilling_unit_wsh("night");
        r.setStart_date_wsh(checkin);
        r.setQuantity_wsh(nights);
        r.setDelivery_time_wsh(delivery);
        r.setPickup_time_wsh(pickup);
        return r;
    }

    @Test
    void deliveryMustBeOnCheckinDate() {
        stubCommon();
        LocalDate checkin = tomorrow();
        BusinessException error = assertThrows(BusinessException.class, () -> service().previewOrder(OWNER_ID,
                nightRequest(checkin, 2, checkin.plusDays(1).atTime(14, 0), checkin.plusDays(2).atTime(14, 30))));
        assertEquals(400, error.getCode());
    }

    @Test
    void deliveryMustBeInTheFuture() {
        stubCommon();
        LocalDate checkin = tomorrow();
        BusinessException error = assertThrows(BusinessException.class, () -> service().previewOrder(OWNER_ID,
                nightRequest(checkin, 1, LocalDateTime.now().minusHours(1), checkin.plusDays(1).atTime(14, 30))));
        assertEquals(400, error.getCode());
    }

    @Test
    void pickupMustBeLaterThanDelivery() {
        stubCommon();
        LocalDate checkin = tomorrow();
        BusinessException error = assertThrows(BusinessException.class, () -> service().previewOrder(OWNER_ID,
                nightRequest(checkin, 1, checkin.atTime(14, 0), checkin.atTime(13, 0))));
        assertEquals(400, error.getCode());
    }

    @Test
    void nightsOverMaxAreRejected() {
        stubCommon();
        LocalDate checkin = tomorrow();
        BusinessException error = assertThrows(BusinessException.class, () -> service().previewOrder(OWNER_ID,
                nightRequest(checkin, 366, checkin.atTime(14, 0), checkin.plusDays(366).atTime(14, 30))));
        assertEquals(400, error.getCode());
    }

    @Test
    void validNightRequestQuotesNightlyRate() {
        stubCommon();
        LocalDate checkin = tomorrow();
        OrderQuoteDTO quote = service().previewOrder(OWNER_ID,
                nightRequest(checkin, 2, checkin.atTime(14, 0), checkin.plusDays(2).atTime(14, 30)));

        assertEquals(2, quote.getBooked_nights_wsh());
        assertEquals(checkin.plusDays(2), quote.getEnd_date_wsh());
        assertEquals(new BigDecimal("300.00"), quote.getTotal_amount_wsh());
    }

    // ============ 长住折扣档位（只看预订夜数） ============

    private void assertDiscount(String expected, int nights) {
        BigDecimal amount = service().calculateDiscount(new BigDecimal("1000.00"), nights);
        assertEquals(0, new BigDecimal(expected).compareTo(amount),
                "nights=" + nights + " 实际=" + amount);
    }

    @Test
    void longStayDiscountTiers() {
        assertDiscount("0", 6);
        assertDiscount("50", 7);
        assertDiscount("50", 29);
        assertDiscount("100", 30);
        assertDiscount("100", 31);
    }
}
