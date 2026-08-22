package com.pet.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.pet.order.dto.OrderBatchCreateRequestDTO;
import com.pet.order.dto.OrderBatchItemDTO;
import com.pet.order.dto.OrderDTO;
import com.pet.order.entity.PetOrder;
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
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 多宠物连续下单（批量创建订单）契约测试：
 * - 批次内部容量自洽：A、B 同区间顶满容量成功；再加 C 整批拒绝（读己之写，不超卖）
 * - 同一宠物批次内重叠区间被拒；不重叠允许
 * - 优惠券只应用于批次中 baseAmount 最大的订单
 * - 仅 day 单位支持批量；items 数量 2..10
 *
 * <p>通过 {@code committed/inserted} 两个列表模拟同事务读己之写：
 * selectCount 返回已占用单数（含本批次已插入），selectOne 按宠物+区间重叠
 * 返回冲突订单，与真实数据库在事务内的可见性一致。</p>
 */
@ExtendWith(MockitoExtension.class)
class OrderBatchCreateTest {

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

    /** 批次开始前已存在的订单（模拟既有占用） */
    private final List<PetOrder> committed = new ArrayList<>();
    /** 本批次内已插入的订单（模拟事务内读己之写） */
    private final List<PetOrder> inserted = new ArrayList<>();

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, com.pet.system.entity.User.class);
        TableInfoHelper.initTableInfo(assistant, com.pet.pet.entity.Pet.class);
        TableInfoHelper.initTableInfo(assistant, com.pet.boarding.entity.Keeper.class);
        TableInfoHelper.initTableInfo(assistant, com.pet.boarding.entity.Merchant.class);
        TableInfoHelper.initTableInfo(assistant, com.pet.boarding.entity.ServiceItem.class);
        TableInfoHelper.initTableInfo(assistant, com.pet.order.entity.PetOrder.class);
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

    private void stubCreatePath() {
        Pet pet1 = new Pet();
        pet1.setId_wsh(1L);
        pet1.setOwner_id_wsh(7L);
        pet1.setName_wsh("P1");
        Pet pet2 = new Pet();
        pet2.setId_wsh(2L);
        pet2.setOwner_id_wsh(7L);
        pet2.setName_wsh("P2");
        Pet pet3 = new Pet();
        pet3.setId_wsh(3L);
        pet3.setOwner_id_wsh(7L);
        pet3.setName_wsh("P3");
        lenient().when(petMapper.selectById(1L)).thenReturn(pet1);
        lenient().when(petMapper.selectById(2L)).thenReturn(pet2);
        lenient().when(petMapper.selectById(3L)).thenReturn(pet3);

        Keeper kp = new Keeper();
        kp.setId_wsh(20L);
        kp.setMerchant_id_wsh(10L);
        kp.setStatus_wsh(StatusCode.KEEPER_ACTIVE.getValue());
        kp.setPrice_per_day_wsh(new BigDecimal("100"));
        kp.setMax_pets_wsh(2);
        lenient().when(keeperMapper.selectById(20L)).thenReturn(kp);

        ServiceItem service = new ServiceItem();
        service.setId_wsh(5L);
        service.setMerchant_id_wsh(10L);
        service.setUnit_wsh("day");
        service.setPrice_wsh(new BigDecimal("100"));
        service.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        service.setUpdated_at_wsh(java.time.LocalDateTime.of(2026, 8, 11, 13, 45, 20));
        lenient().when(serviceItemMapper.selectById(5L)).thenReturn(service);

        Merchant m = new Merchant();
        m.setId_wsh(10L);
        m.setStatus_wsh(StatusCode.MERCHANT_APPROVED.getValue());
        m.setFuture_booking_enabled_wsh(1);
        m.setStore_status_wsh(1);
        lenient().when(merchantMapper.selectById(10L)).thenReturn(m);

        lenient().when(qualificationService.listByOwner(eq(QualificationService.OWNER_TYPE_KEEPER), eq(20L), eq(false)))
                .thenReturn(List.of(approvedQualification()));

        // 容量计数 = 既有占用 + 本批次已插入（读己之写）
        lenient().when(orderMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenAnswer(inv -> (long) (committed.size() + inserted.size()));
        // 宠物冲突查询：按宠物 + 半开区间 [start,end) 重叠返回冲突订单
        lenient().when(orderMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenAnswer(inv -> simulatePetConflict(inv.getArgument(0)));

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

        User owner = new User();
        owner.setId_wsh(7L);
        owner.setUsername_wsh("owner");
        lenient().when(userMapper.selectById(7L)).thenReturn(owner);

        lenient().when(serviceItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(service));
        lenient().when(merchantMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        lenient().when(petMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(pet1, pet2));
        lenient().when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(owner));

        lenient().doAnswer(invocation -> {
            PetOrder order = invocation.getArgument(0);
            order.setId_wsh(100L + inserted.size());
            order.setService_id_wsh(5L);
            order.setOrder_no_wsh("ORD-B" + inserted.size());
            inserted.add(order);
            return 1;
        }).when(orderMapper).insert(any(PetOrder.class));
    }

    /**
     * 解析 ensureNoPetDateConflict 的查询条件（eq 宠物 → MPGENVAL1，in 状态 → MPGENVAL2，
     * lt(start < endDate) → MPGENVAL3，gt(end > startDate) → MPGENVAL4），
     * 返回候选区间 [startDate, endDate) 与之重叠的同一宠物订单；无则 null。
     */
    private PetOrder simulatePetConflict(LambdaQueryWrapper<PetOrder> wrapper) {
        // getSqlSegment() 触发参数名/值生成，之后 getParamNameValuePairs() 才有内容
        wrapper.getSqlSegment();
        // 按值提取：包装器里只有一个 Long（宠物ID）和两个 LocalDate（候选区间起止，较大者为结束）
        Long petId = null;
        LocalDate d1 = null;
        LocalDate d2 = null;
        for (Object value : wrapper.getParamNameValuePairs().values()) {
            if (value instanceof Long && petId == null) {
                petId = (Long) value;
            } else if (value instanceof LocalDate) {
                if (d1 == null) {
                    d1 = (LocalDate) value;
                } else if (d2 == null) {
                    d2 = (LocalDate) value;
                }
            }
        }
        if (petId == null || d1 == null || d2 == null) {
            return null;
        }
        LocalDate candidateEnd = d1.isAfter(d2) ? d1 : d2;
        LocalDate candidateStart = d1.isAfter(d2) ? d2 : d1;
        for (PetOrder o : allOrders()) {
            if (!petId.equals(o.getPet_id_wsh())
                    || o.getStart_date_wsh() == null || o.getEnd_date_wsh() == null) {
                continue;
            }
            if (o.getStart_date_wsh().isBefore(candidateEnd) && o.getEnd_date_wsh().isAfter(candidateStart)) {
                return o;
            }
        }
        return null;
    }

    private List<PetOrder> allOrders() {
        List<PetOrder> all = new ArrayList<>(committed);
        all.addAll(inserted);
        return all;
    }

    private QualificationDTO approvedQualification() {
        QualificationDTO q = new QualificationDTO();
        q.setStatus_wsh(QualificationService.STATUS_APPROVED);
        q.setId_wsh(1L);
        return q;
    }

    private OrderBatchCreateRequestDTO batch(List<OrderBatchItemDTO> items, Long couponId) {
        OrderBatchCreateRequestDTO dto = new OrderBatchCreateRequestDTO();
        dto.setKeeper_id_wsh(20L);
        dto.setMerchant_id_wsh(10L);
        dto.setService_id_wsh(5L);
        dto.setService_version_wsh("2026-08-11T13:45:20");
        dto.setBilling_unit_wsh("day");
        dto.setExpected_unit_price_wsh(new BigDecimal("100"));
        dto.setUser_coupon_id_wsh(couponId);
        dto.setDelivery_address_wsh("测试路1号");
        dto.setDelivery_location_source_wsh("merchant");
        dto.setEmergency_contact_name_wsh("Tom");
        dto.setEmergency_contact_phone_wsh("13800000002");
        dto.setItems(items);
        return dto;
    }

    private OrderBatchItemDTO item(Long petId, LocalDate start, LocalDate end) {
        OrderBatchItemDTO item = new OrderBatchItemDTO();
        item.setPet_id_wsh(petId);
        item.setStart_date_wsh(start);
        item.setEnd_date_wsh(end);
        item.setDelivery_time_wsh(start.atTime(10, 0));
        item.setPickup_time_wsh(end.atTime(18, 0));
        return item;
    }

    // ============ 批次内部容量与冲突 ============

    @Test
    void batchABSameIntervalFillsCapacityAndSucceeds() {
        stubCreatePath();
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(5);

        List<OrderDTO> orders = orderService().createOrders(7L,
                batch(List.of(item(1L, start, end), item(2L, start, end)), null));

        assertEquals(2, orders.size());
        assertEquals(OrderStatus.PENDING, orders.get(0).getStatus_wsh());
        assertEquals(OrderStatus.PENDING, orders.get(1).getStatus_wsh());
        // 容量 2 被批次内两单顶满仍成功（读己之写不把批次内部算作冲突）
        verify(orderMapper, times(2)).insert(any(PetOrder.class));
    }

    @Test
    void batchThirdOrderOverCapacityRollsBackWholeBatch() {
        stubCreatePath();
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(5);

        BusinessException error = assertThrows(BusinessException.class,
                () -> orderService().createOrders(7L, batch(List.of(
                        item(1L, start, end),
                        item(2L, start, end),
                        item(3L, start, end)), null)));

        assertEquals(BookingErrorCode.CAPACITY_EXCEEDED, error.getErrorCode());
        // 第三单容量检查失败 => 只插入 2 单；事务回滚后不应有第 3 单
        verify(orderMapper, times(2)).insert(any(PetOrder.class));
    }

    @Test
    void samePetOverlappingWithinBatchRejected() {
        stubCreatePath();
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(5);

        BusinessException error = assertThrows(BusinessException.class,
                () -> orderService().createOrders(7L, batch(List.of(
                        item(1L, start, end),
                        item(1L, start.plusDays(1), start.plusDays(3))), null)));

        assertEquals(400, error.getCode());
        assertTrue(error.getMessage().contains("冲突"));
        verify(orderMapper, times(1)).insert(any(PetOrder.class));
    }

    @Test
    void samePetNonOverlappingWithinBatchAllowed() {
        stubCreatePath();
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(5);
        LocalDate start2 = start.plusDays(10);
        LocalDate end2 = start2.plusDays(3);

        List<OrderDTO> orders = orderService().createOrders(7L, batch(List.of(
                item(1L, start, end),
                item(1L, start2, end2)), null));

        assertEquals(2, orders.size());
        verify(orderMapper, times(2)).insert(any(PetOrder.class));
    }

    // ============ 优惠券归属 ============

    @Test
    void couponAppliedOnlyToLargestBaseAmountOrder() {
        stubCreatePath();
        LocalDate start = LocalDate.now().plusDays(1);
        // 宠物A 5天 × 100 = 500（最大，持券）；宠物B 3天 × 100 = 300（无券）
        List<OrderDTO> orders = orderService().createOrders(7L,
                batch(List.of(item(1L, start, start.plusDays(5)), item(2L, start, start.plusDays(3))), 9L));

        assertEquals(2, orders.size());
        verify(couponService).lockForOrder(eq(7L), eq(9L), any(), any(), any());
        verify(couponService).lockForOrder(eq(7L), isNull(), any(), any(), any());
    }

    // ============ 批量边界 ============

    @Test
    void nonDayUnitRejectedForBatch() {
        stubCreatePath();
        ServiceItem sessionService = new ServiceItem();
        sessionService.setId_wsh(5L);
        sessionService.setMerchant_id_wsh(10L);
        sessionService.setUnit_wsh("session");
        sessionService.setPrice_wsh(new BigDecimal("100"));
        sessionService.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        when(serviceItemMapper.selectById(5L)).thenReturn(sessionService);
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(1);

        BusinessException error = assertThrows(BusinessException.class,
                () -> orderService().createOrders(7L, batch(List.of(item(1L, start, end), item(2L, start, end)), null)));

        assertEquals(BookingErrorCode.UNSUPPORTED_SERVICE_UNIT, error.getErrorCode());
        verify(orderMapper, never()).insert(any(PetOrder.class));
    }

    @Test
    void singleItemBatchRejected() {
        stubCreatePath();
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2);

        BusinessException error = assertThrows(BusinessException.class,
                () -> orderService().createOrders(7L, batch(List.of(item(1L, start, end)), null)));

        assertEquals(400, error.getCode());
        verify(orderMapper, never()).insert(any(PetOrder.class));
    }

    @Test
    void moreThanTenItemsRejected() {
        stubCreatePath();
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2);
        List<OrderBatchItemDTO> items = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            items.add(item((long) i, start, end));
        }

        BusinessException error = assertThrows(BusinessException.class,
                () -> orderService().createOrders(7L, batch(items, null)));

        assertEquals(400, error.getCode());
        verify(orderMapper, never()).insert(any(PetOrder.class));
    }
}
