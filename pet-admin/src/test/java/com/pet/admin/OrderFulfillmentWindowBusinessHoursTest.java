package com.pet.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.boarding.entity.BusinessHours;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.mapper.BusinessHoursMapper;
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
import com.pet.finance.service.AccountingService;
import com.pet.marketing.service.CouponService;
import com.pet.membership.service.MembershipBenefitService;
import com.pet.mq.MessageSender;
import com.pet.order.dto.OrderCreateRequestDTO;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.mapper.PaymentMapper;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderFulfillmentWindowBusinessHoursTest {

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

    private final BusinessHoursTargetResolver resolver = new BusinessHoursTargetResolver();
    @Mock private RatingMapper ratingMapper;

    @Test
    void deliveryOutsideBusinessHoursIsRejectedAtCreation() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2);
        int deliveryDow = start.getDayOfWeek().getValue();
        PetOrderOverrides.stubCreationPath(petMapper, keeperMapper, merchantMapper,
                merchantService, serviceItemMapper, qualificationService, orderMapper, start);

        OrderCreateRequestDTO request = request(start, end);
        // Default delivery 10:00 is outside the day's 11:00-18:00 -> gate must reject before downstream steps.
        when(businessHoursService.getByMerchantId(10L)).thenReturn(List.of(
                hours(deliveryDow, LocalTime.of(11, 0), LocalTime.of(18, 0), 0)));

        BusinessException error = assertThrows(BusinessException.class, () -> orderService().createOrder(7L, request));
        assertEquals(400, error.getCode());
        assertEquals("送达时间不在目标日期营业时段内", error.getMessage());
    }

    @Test
    void pickupEarlierThanDeliveryIsRejectedAtCreation() {
        // 用户场景：今天下午送、今天上午取 —— 取宠时间早于送宠时间，逻辑上不可能，必须拒绝。
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(1);
        PetOrderOverrides.stubCreationPath(petMapper, keeperMapper, merchantMapper,
                merchantService, serviceItemMapper, qualificationService, orderMapper, start);

        OrderCreateRequestDTO request = request(start, end);
        // 显式构造"下午送、上午取（跨日但取宠时刻早于送宠时刻）"的脏数据
        request.setDelivery_time_wsh(start.atTime(15, 0));
        request.setPickup_time_wsh(start.atTime(9, 0));

        BusinessException error = assertThrows(BusinessException.class, () -> orderService().createOrder(7L, request));
        assertEquals(400, error.getCode());
        assertEquals("接宠时间必须在送宠时间之后", error.getMessage());
    }

    @Test
    void pickupOutsideBusinessHoursIsRejectedAtCreation() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2);
        int deliveryDow = start.getDayOfWeek().getValue();
        int midDow = start.plusDays(1).getDayOfWeek().getValue();
        int pickupDow = end.getDayOfWeek().getValue();
        PetOrderOverrides.stubCreationPath(petMapper, keeperMapper, merchantMapper,
                merchantService, serviceItemMapper, qualificationService, orderMapper, start);

        OrderCreateRequestDTO request = request(start, end);
        // Delivery 10:00 inside the day's 09:00-20:00, but default pickup 18:00 is outside the end day's 09:00-17:00.
        when(businessHoursService.getByMerchantId(10L)).thenReturn(List.of(
                hours(deliveryDow, LocalTime.of(9, 0), LocalTime.of(20, 0), 0),
                hours(midDow, LocalTime.of(9, 0), LocalTime.of(20, 0), 0),
                hours(pickupDow, LocalTime.of(9, 0), LocalTime.of(17, 0), 0)));

        BusinessException error = assertThrows(BusinessException.class, () -> orderService().createOrder(7L, request));
        assertEquals(400, error.getCode());
        assertEquals("接回时间不在目标日期营业时段内", error.getMessage());
    }

    private BusinessHours hours(int day, LocalTime open, LocalTime close, int closed) {
        BusinessHours h = new BusinessHours();
        h.setDay_of_week_wsh(day);
        h.setOpen_time_wsh(open);
        h.setClose_time_wsh(close);
        h.setIs_closed_wsh(closed);
        return h;
    }

    private OrderCreateRequestDTO request(LocalDate start, LocalDate end) {
        OrderCreateRequestDTO dto = new OrderCreateRequestDTO();
        dto.setPet_id_wsh(1L);
        dto.setKeeper_id_wsh(20L);
        dto.setMerchant_id_wsh(10L);
        dto.setService_id_wsh(5L);
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
                new ObjectMapper(), businessHoursService, resolver, ratingMapper);
    }

    /** Stubs the entity lookups needed before validateFulfillmentWindow executes. */
    static final class PetOrderOverrides {
        static void stubCreationPath(PetMapper petMapper, KeeperMapper keeperMapper,
                                     MerchantMapper merchantMapper, MerchantService merchantService,
                                     ServiceItemMapper serviceItemMapper,
                                     QualificationService qualificationService, OrderMapper orderMapper,
                                     LocalDate start) {
            Pet pet = new Pet();
            pet.setId_wsh(1L);
            pet.setOwner_id_wsh(7L);
            when(petMapper.selectById(1L)).thenReturn(pet);

            Keeper keeper = new Keeper();
            keeper.setId_wsh(20L);
            keeper.setMerchant_id_wsh(10L);
            keeper.setStatus_wsh(StatusCode.KEEPER_ACTIVE.getValue());
            keeper.setPrice_per_day_wsh(new BigDecimal("50"));
            keeper.setMax_pets_wsh(3);
            when(keeperMapper.selectById(20L)).thenReturn(keeper);

            Merchant merchant = new Merchant();
            merchant.setId_wsh(10L);
            merchant.setStatus_wsh(StatusCode.MERCHANT_APPROVED.getValue());
            merchant.setStore_status_wsh(1);
            merchant.setFuture_booking_enabled_wsh(1);
            when(merchantMapper.selectById(10L)).thenReturn(merchant);

            ServiceItem service = new ServiceItem();
            service.setId_wsh(5L);
            service.setMerchant_id_wsh(10L);
            service.setUnit_wsh("day");
            service.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
            when(serviceItemMapper.selectById(5L)).thenReturn(service);

            when(qualificationService.listByOwner(eq(QualificationService.OWNER_TYPE_KEEPER),
                    eq(20L), eq(false)))
                    .thenReturn(List.of(approvedQualification()));
            org.mockito.Mockito.lenient().when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            org.mockito.Mockito.lenient().when(orderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        }

        private static QualificationDTO approvedQualification() {
            QualificationDTO q = new QualificationDTO();
            q.setStatus_wsh(QualificationService.STATUS_APPROVED);
            q.setId_wsh(1L);
            return q;
        }
    }
}