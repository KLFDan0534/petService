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
import com.pet.boarding.service.KeeperService;
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
import com.pet.order.dto.OrderReceivedRequestDTO;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.mapper.PaymentMapper;
import com.pet.order.service.OrderService;
import com.pet.order.service.OrderSnapshotService;
import com.pet.order.service.OrderStatusBroadcaster;
import com.pet.customer.mapper.RatingMapper;
import com.pet.order.service.impl.OrderServiceImpl;
import com.pet.pet.entity.Pet;
import com.pet.pet.mapper.PetMapper;
import com.pet.qualification.dto.QualificationDTO;
import com.pet.qualification.service.QualificationService;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * U4 RED: employment/qualification/leave/capacity are the future-booking gates,
 * independent of real-time presence. Capacity and leave re-checks run again at
 * accept/confirm; termination and qualification revocation must fail them.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderCapacityAndEmploymentTest {

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
    @Mock private RatingMapper ratingMapper;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, com.pet.system.entity.User.class);
        TableInfoHelper.initTableInfo(assistant, com.pet.pet.entity.Pet.class);
        TableInfoHelper.initTableInfo(assistant, com.pet.boarding.entity.Keeper.class);
        TableInfoHelper.initTableInfo(assistant, com.pet.boarding.entity.Merchant.class);
        TableInfoHelper.initTableInfo(assistant, com.pet.boarding.entity.ServiceItem.class);
        // 鍐茬獊/瀹归噺鏍￠獙鐜板湪浼氳鍙?wrapper 鐨?SQL 鐗囨锛堝尯闂寸簿鍒わ級锛岄渶瑕?PetOrder 鐨?TableInfo
        TableInfoHelper.initTableInfo(assistant, com.pet.order.entity.PetOrder.class);
    }

    @Test
    void offlineBusyKeeperStillPassesFutureBookableGate() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2);
        stubCreatePath();
        when(keeperMapper.selectById(20L)).thenReturn(keeper(StatusCode.KEEPER_BUSY.getValue()));
        when(orderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        OrderDTO dto = orderService().createOrder(7L, request(start, end));

        assertEquals(OrderStatus.PENDING, dto.getStatus_wsh());
        verify(orderMapper).insert(any(PetOrder.class));
    }

    @Test
    void capacityRefreshDoesNotResetManualOfflineKeeperToActive() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2);
        stubCreatePath();
        // keeper manually offline, store open -> capacity refresh must not flip to ACTIVE
        Keeper manualOffline = keeper(StatusCode.KEEPER_OFFLINE.getValue());
        manualOffline.setOffline_source_wsh(KeeperService.OFFLINE_SOURCE_MANUAL);
        when(keeperMapper.selectById(20L)).thenReturn(manualOffline);
        when(orderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        // store open in the capacity-refresh path, so the manual-offline guard is exercised
        when(merchantMapper.selectById(10L)).thenReturn(merchant(10L, 1, 1, 1));

        orderService().createOrder(7L, request(start, end));

        assertEquals(StatusCode.KEEPER_OFFLINE.getValue(), manualOffline.getStatus_wsh(),
                "manual offline keeper must stay offline after order creation on an open store");
        assertEquals(KeeperService.OFFLINE_SOURCE_MANUAL,
                manualOffline.getOffline_source_wsh());
    }

    private void stubCreatePath() {
        Pet pet = new Pet();
        pet.setId_wsh(1L);
        pet.setOwner_id_wsh(7L);
        when(petMapper.selectById(1L)).thenReturn(pet);

        Keeper kp = keeper(StatusCode.KEEPER_ACTIVE.getValue());
        kp.setMax_pets_wsh(3);

        ServiceItem service = new ServiceItem();
        service.setId_wsh(5L);
        service.setMerchant_id_wsh(10L);
        service.setUnit_wsh("night");
        // 浠锋牸鍙鏈嶅姟鍗曚环锛堢湅鎶よ€呬釜浜哄畾浠峰凡涓嬬嚎锛?
        service.setPrice_wsh(new BigDecimal("100.00"));
        service.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        lenient().when(serviceItemMapper.selectById(5L)).thenReturn(service);
        lenient().when(serviceItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(service));
        when(keeperMapper.selectById(20L)).thenReturn(kp);
        when(merchantMapper.selectById(10L)).thenReturn(merchant(10L, 1, 1, 0));

        when(qualificationService.listByOwner(eq(QualificationService.OWNER_TYPE_KEEPER), eq(20L), eq(false)))
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

        lenient().when(userMapper.selectById(7L)).thenReturn(ownerUser());
        lenient().when(merchantMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        lenient().when(petMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(pet));
        lenient().when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(ownerUser()));
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

    /** 宸插崰鐢ㄧ殑 DAY 璁㈠崟锛堢敤浜庡閲忓€欓€夛級銆?*/
    private PetOrder existingOrder(LocalDate start, LocalDate end) {
        PetOrder order = new PetOrder();
        order.setBilling_unit_wsh("day");
        order.setStart_date_wsh(start);
        order.setEnd_date_wsh(end);
        return order;
    }

    private PetOrder paidOrder(Long keeperId) {
        PetOrder order = new PetOrder();
        order.setId_wsh(100L);
        order.setOrder_no_wsh("ORD-U4");
        order.setOwner_id_wsh(9L);
        order.setMerchant_id_wsh(10L);
        order.setKeeper_id_wsh(keeperId);
        order.setStart_date_wsh(LocalDate.now().plusDays(1));
        order.setEnd_date_wsh(LocalDate.now().plusDays(3));
        order.setStatus_wsh(OrderStatus.PAID);
        return order;
    }

    private PetOrder futurePaidOrder() {
        return paidOrder(20L);
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
        dto.setMerchant_id_wsh(10L);
        dto.setService_id_wsh(5L);
        dto.setStart_date_wsh(start);
        dto.setEnd_date_wsh(end);
        dto.setQuantity_wsh((int) java.time.temporal.ChronoUnit.DAYS.between(start, end));
        dto.setDelivery_time_wsh(start.atTime(14, 0));
        dto.setPickup_time_wsh(end.atTime(14, 30));
        return dto;
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

    // ============ 鍒板簵瀹归噺澶嶆牳锛圓E16锛?============

    /** 涓婁竴鍙櫄鎺ユ湭瀹屾垚鏃朵粛鍗犲悕棰濓細keeper 宸叉弧鍛樻椂蹇呴』鎷掔粷鎺ユ敹涓嬩竴鍙€?*/
    @Test
    void markReceivedRejectsWhenInStorePetsAlreadyAtCapacity() {
        PetOrder confirmed = paidOrder(20L);
        confirmed.setStatus_wsh(OrderStatus.CONFIRMED);
        confirmed.setHandover_code_wsh("1234");
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(confirmed);
        when(orderMapper.selectByIdForUpdate(100L)).thenReturn(confirmed);

        Keeper keeper = keeper(StatusCode.KEEPER_ACTIVE.getValue());
        keeper.setUser_id_wsh(30L);
        keeper.setMax_pets_wsh(1);
        when(keeperMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(keeper));
        when(keeperMapper.selectByIdForUpdate(20L)).thenReturn(keeper);
        // 宸叉湁涓€鍙?RECEIVED 鍦ㄥ簵
        when(orderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        OrderReceivedRequestDTO request = new OrderReceivedRequestDTO();
        request.setOrder_no_wsh("ORD-U4");
        request.setHandover_code_wsh("1234");
        request.setSkip_distance_check_wsh(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService().markReceived(30L, request));

        assertEquals(BookingErrorCode.CAPACITY_EXCEEDED, ex.getErrorCode());
        verify(orderMapper, never()).update(any(PetOrder.class), any(LambdaUpdateWrapper.class));
    }
}
