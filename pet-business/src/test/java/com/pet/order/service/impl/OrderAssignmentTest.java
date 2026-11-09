package com.pet.order.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import com.pet.common.BookingErrorCode;
import com.pet.common.BusinessException;
import com.pet.common.OrderStatus;
import com.pet.common.StatusCode;
import com.pet.customer.mapper.RatingMapper;
import com.pet.finance.service.AccountingService;
import com.pet.marketing.service.CouponService;
import com.pet.membership.service.MembershipBenefitService;
import com.pet.mq.MessageSender;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.mapper.PaymentMapper;
import com.pet.order.service.OrderSnapshotService;
import com.pet.order.service.OrderStatusBroadcaster;
import com.pet.pet.mapper.PetMapper;
import com.pet.qualification.dto.QualificationDTO;
import com.pet.qualification.service.QualificationService;
import com.pet.system.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 负责人分配：抢单 / 商家指派 / 超时自动分配。
 * <p>
 * 覆盖三条关键约束：只有本商家看护者能抢；只有商家管理员能指派；
 * 无人可分配时必须取消并退款，不允许留下无负责人的有效订单。
 */
@ExtendWith(MockitoExtension.class)
class OrderAssignmentTest {

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
        TableInfoHelper.initTableInfo(assistant, Keeper.class);
        TableInfoHelper.initTableInfo(assistant, Merchant.class);
        TableInfoHelper.initTableInfo(assistant, PetOrder.class);
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

    private PetOrder unassignedPaidOrder() {
        PetOrder order = new PetOrder();
        order.setId_wsh(100L);
        order.setOrder_no_wsh("ORD-UNASSIGNED");
        order.setOwner_id_wsh(7L);
        order.setPet_id_wsh(1L);
        order.setMerchant_id_wsh(10L);
        order.setKeeper_id_wsh(null);
        order.setBilling_unit_wsh("day");
        order.setStart_date_wsh(LocalDate.now().plusDays(2));
        order.setEnd_date_wsh(LocalDate.now().plusDays(4));
        order.setStatus_wsh(OrderStatus.PAID);
        order.setFinal_amount_wsh(new BigDecimal("300.00"));
        order.setPlatform_subsidy_wsh(BigDecimal.ZERO);
        return order;
    }

    private Keeper keeper(Long id, Long merchantId, int current, int max) {
        Keeper k = new Keeper();
        k.setId_wsh(id);
        k.setMerchant_id_wsh(merchantId);
        k.setStatus_wsh(StatusCode.KEEPER_ACTIVE.getValue());
        k.setMax_pets_wsh(max);
        k.setCurrent_pets_wsh(current);
        k.setRating_wsh(new BigDecimal("5.00"));
        return k;
    }

    private void stubApprovedQualification() {
        QualificationDTO q = new QualificationDTO();
        q.setStatus_wsh(QualificationService.STATUS_APPROVED);
        q.setId_wsh(1L);
        lenient().when(qualificationService.listByOwner(
                eq(QualificationService.OWNER_TYPE_KEEPER), anyLong(), eq(false)))
                .thenReturn(List.of(q));
    }

    private void stubEmptyCandidates() {
        lenient().when(orderMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        // 第二个参数是 LambdaUpdateWrapper，与 LambdaQueryWrapper 不是同一类型，必须用 any()
        lenient().when(orderMapper.update(any(PetOrder.class), any())).thenReturn(1);
        lenient().when(keeperMapper.selectById(anyLong())).thenReturn(null);
        lenient().when(orderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
    }

    // ============ 抢单 ============

    @Test
    void claimSucceedsForKeeperOfSameMerchant() {
        PetOrder order = unassignedPaidOrder();
        stubEmptyCandidates();
        stubApprovedQualification();
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        Keeper me = keeper(20L, 10L, 0, 3);
        lenient().when(keeperMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(me);

        service().claimOrder(21L, "ORD-UNASSIGNED");

        assertEquals(OrderStatus.CONFIRMED, order.getStatus_wsh());
        assertEquals(20L, order.getKeeper_id_wsh());
        verify(orderStatusBroadcaster).broadcast(order);
        // 被接走后必须通知同商家其它看护者把这一单从「待抢单」移除
        verify(orderStatusBroadcaster).broadcastPoolChange(order, "pool-removed");
    }

    @Test
    void claimRejectedWhenOrderAlreadyAssigned() {
        PetOrder order = unassignedPaidOrder();
        order.setKeeper_id_wsh(99L);
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service().claimOrder(21L, "ORD-UNASSIGNED"));
        assertEquals(400, error.getCode());
    }

    @Test
    void claimRejectedForKeeperOfAnotherMerchant() {
        PetOrder order = unassignedPaidOrder();
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        // 该用户是别家商家的看护者 → 查不到本商家的 keeper 记录
        lenient().when(keeperMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service().claimOrder(21L, "ORD-UNASSIGNED"));
        assertEquals(403, error.getCode());
    }

    // ============ 商家指派 ============

    @Test
    void assignRejectedWhenCallerIsNotMerchantOwner() {
        PetOrder order = unassignedPaidOrder();
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(merchantService.isOwner(10L, 21L)).thenReturn(false);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service().assignOrder(21L, "ORD-UNASSIGNED", 20L));
        assertEquals(403, error.getCode());
    }

    @Test
    void assignSucceedsForMerchantOwner() {
        PetOrder order = unassignedPaidOrder();
        stubEmptyCandidates();
        stubApprovedQualification();
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(merchantService.isOwner(10L, 21L)).thenReturn(true);
        Keeper target = keeper(20L, 10L, 0, 3);
        when(keeperMapper.selectById(20L)).thenReturn(target);
        Merchant merchant = new Merchant();
        merchant.setId_wsh(10L);
        lenient().when(merchantMapper.selectById(10L)).thenReturn(merchant);

        service().assignOrder(21L, "ORD-UNASSIGNED", 20L);

        assertEquals(OrderStatus.CONFIRMED, order.getStatus_wsh());
        assertEquals(20L, order.getKeeper_id_wsh());
    }

    // ============ 超时自动分配 ============

    @Test
    void autoAssignPicksLowestLoadKeeper() {
        PetOrder order = unassignedPaidOrder();
        stubEmptyCandidates();
        stubApprovedQualification();
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        // 负载 0.8 与 0.2 两名候选人，应选中 0.2
        Keeper busy = keeper(30L, 10L, 4, 5);
        Keeper idle = keeper(31L, 10L, 1, 5);
        when(keeperMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(busy, idle));

        service().autoAcceptPaidOrderIfTimeout("ORD-UNASSIGNED");

        assertEquals(31L, order.getKeeper_id_wsh());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus_wsh());
    }

    @Test
    void autoAssignCancelsAndRefundsWhenNoCandidate() {
        PetOrder order = unassignedPaidOrder();
        stubEmptyCandidates();
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(keeperMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        service().autoAcceptPaidOrderIfTimeout("ORD-UNASSIGNED");

        assertEquals(OrderStatus.CANCELLED, order.getStatus_wsh());
        verify(accountingService).transfer(anyLong(), eq(7L), eq(new BigDecimal("300.00")),
                eq("unassigned"), eq(100L), any(), any(), any(), any());
        verify(couponService).releaseForOrder(100L);
    }

    @Test
    void autoAssignSkipsKeepersWithoutQualification() {
        PetOrder order = unassignedPaidOrder();
        stubEmptyCandidates();
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(keeperMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(keeper(30L, 10L, 0, 5)));
        // 资质未通过 → 分配失败 → 无其他候选人 → 取消
        lenient().when(qualificationService.listByOwner(
                eq(QualificationService.OWNER_TYPE_KEEPER), anyLong(), eq(false)))
                .thenReturn(List.of());

        service().autoAcceptPaidOrderIfTimeout("ORD-UNASSIGNED");

        assertEquals(OrderStatus.CANCELLED, order.getStatus_wsh());
        assertNotNull(order.getOrder_no_wsh());
    }
    // ============ 待分配池的可见范围与上限 ============

    @Test
    void assignablePoolOnlyVisibleToBookableKeepers() {
        lenient().when(keeperMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        service().listAssignableForKeeper(21L);

        ArgumentCaptor<LambdaQueryWrapper<Keeper>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(keeperMapper).selectList(captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("status_wsh"),
                "待分配池必须限定看护者在职状态，否则离职/待审的人也能看到客户信息");
    }

    @Test
    void assignablePoolScopesToMerchantAndSkipsExpired() {
        Keeper me = keeper(20L, 10L, 0, 3);
        lenient().when(keeperMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(me));
        lenient().when(orderMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        service().listAssignableForKeeper(21L);

        ArgumentCaptor<LambdaQueryWrapper<PetOrder>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(orderMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("merchant_id_wsh"), "可抢池必须限定在本商家范围内");
        assertTrue(sql.contains("end_date_wsh"), "服务已结束的订单不该再出现在可抢池");
        assertTrue(sql.contains("LIMIT 100"), "可抢池必须限制单次返回量");
    }

    @Test
    void claimOnlyAcceptsBookableKeeperStatus() {
        PetOrder order = unassignedPaidOrder();
        stubEmptyCandidates();
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        lenient().when(keeperMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class, () -> service().claimOrder(21L, "ORD-UNASSIGNED"));

        ArgumentCaptor<LambdaQueryWrapper<Keeper>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(keeperMapper).selectOne(captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("status_wsh"),
                "抢单必须限定在职状态，否则离职看护者也能抢单");
    }
    // ============ 容量校验发生在分配环节（下单不再指定负责人） ============

    @Test
    void claimRejectedWhenKeeperCapacityIsFull() {
        PetOrder order = unassignedPaidOrder();
        stubEmptyCandidates();
        stubApprovedQualification();
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        Keeper me = keeper(20L, 10L, 3, 3);
        lenient().when(keeperMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(me);
        // 三张同区间的已接订单，把 max_pets=3 占满
        lenient().when(orderMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(
                        occupiedOrder(201L, order), occupiedOrder(202L, order), occupiedOrder(203L, order)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service().claimOrder(21L, "ORD-UNASSIGNED"));

        assertEquals(BookingErrorCode.CAPACITY_EXCEEDED, error.getErrorCode());
    }

    @Test
    void claimRejectedWhenKeeperIsOnLeave() {
        PetOrder order = unassignedPaidOrder();
        stubEmptyCandidates();
        stubApprovedQualification();
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        Keeper me = keeper(20L, 10L, 0, 3);
        lenient().when(keeperMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(me);
        doThrow(new BusinessException(400, "看护者在所选服务日期内有休假安排"))
                .when(keeperLeaveService).requireKeeperAvailable(eq(20L), any(), any());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service().claimOrder(21L, "ORD-UNASSIGNED"));

        assertEquals(400, error.getCode());
    }

    /** 构造一张与目标订单同区间、已分配给同一看护者的订单，用于占满容量。 */
    private PetOrder occupiedOrder(Long id, PetOrder target) {
        PetOrder o = new PetOrder();
        o.setId_wsh(id);
        o.setOrder_no_wsh("ORD-OCCUPIED-" + id);
        o.setOwner_id_wsh(target.getOwner_id_wsh());
        o.setPet_id_wsh(target.getPet_id_wsh());
        o.setMerchant_id_wsh(target.getMerchant_id_wsh());
        o.setKeeper_id_wsh(20L);
        o.setBilling_unit_wsh("day");
        o.setStart_date_wsh(target.getStart_date_wsh());
        o.setEnd_date_wsh(target.getEnd_date_wsh());
        o.setStatus_wsh(OrderStatus.CONFIRMED);
        return o;
    }
}
