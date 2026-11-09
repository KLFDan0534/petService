package com.pet.order.service.impl;

import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.BusinessHours;
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
import com.pet.qualification.service.QualificationService;
import com.pet.common.BookingErrorCode;
import com.pet.common.BookingUnit;
import com.pet.common.BusinessException;
import com.pet.common.OrderStatus;
import com.pet.common.ServiceVersions;
import com.pet.common.StatusCode;
import com.pet.common.geo.GeoDistanceUtils;
import com.pet.config.RabbitMQConfig;
import com.pet.customer.mapper.RatingMapper;
import com.pet.finance.service.AccountingService;
import com.pet.marketing.dto.CouponDiscountResult;
import com.pet.marketing.service.CouponService;
import com.pet.membership.dto.MembershipDiscountDTO;
import com.pet.membership.service.MembershipBenefitService;
import com.pet.order.dto.OrderBatchCreateRequestDTO;
import com.pet.order.dto.OrderBatchItemDTO;
import com.pet.order.dto.OrderCheckoutDTO;
import com.pet.order.dto.OrderCompleteRequestDTO;
import com.pet.order.dto.OrderCreateRequestDTO;
import com.pet.order.dto.OrderDTO;
import com.pet.order.dto.OrderPreviewRequestDTO;
import com.pet.order.dto.OrderQuoteDTO;
import com.pet.order.dto.OrderReceivedRequestDTO;
import com.pet.order.service.OrderStatusBroadcaster;
import com.pet.order.entity.Payment;
import com.pet.order.entity.PetOrder;
import com.pet.order.event.OrderCompletedEvent;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.mapper.PaymentMapper;
import com.pet.order.occupancy.OrderOccupancy;
import com.pet.order.pricing.NightPricingRule;
import com.pet.order.pricing.OrderPricing;
import com.pet.order.pricing.PaymentPurpose;
import com.pet.order.service.OrderService;
import com.pet.order.service.OrderSnapshotService;
import com.pet.mq.MessageSender;
import com.pet.pet.entity.Pet;
import com.pet.pet.mapper.PetMapper;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    // 订单状态
    private static final Set<String> VALID_STATUSES = Set.of(
            OrderStatus.PENDING, OrderStatus.PAID, OrderStatus.CONFIRMED,
            OrderStatus.RECEIVED, OrderStatus.IN_PROGRESS,
            OrderStatus.COMPLETED, OrderStatus.CANCELLED,
            OrderStatus.REFUNDING, OrderStatus.REFUNDED);

    // 订单状态
    private static final Set<String> BOOKING_STATUSES = Set.of(
            OrderStatus.PENDING, OrderStatus.PAID, OrderStatus.CONFIRMED,
            OrderStatus.RECEIVED, OrderStatus.IN_PROGRESS,
            OrderStatus.REFUNDING);

    /**
     * 在养状态：宠物已经在看护者处、尚未完成。
     * <p>
     * CONFIRMED 只是预约（还没接收），不能算作「当前在养」。
     */
    private static final Set<String> CURRENT_PET_STATUSES = Set.of(
            OrderStatus.RECEIVED, OrderStatus.IN_PROGRESS);

    /** 占用候选订单的加载上限：超限时明确失败，不允许静默截断后得出「有空位」。 */
    private static final int OCCUPANCY_CANDIDATE_LIMIT = 200;

    private static final int RECEIVER_WINDOW_MINUTES = 10;
    private static final Duration PAYMENT_TIMEOUT = Duration.ofMillis(RabbitMQConfig.ORDER_PAYMENT_TIMEOUT_TTL_MS);
    private static final int PAYMENT_TIMEOUT_SCAN_LIMIT = 100;
    private static final Duration ACCEPT_TIMEOUT = Duration.ofMillis(RabbitMQConfig.ORDER_ACCEPT_TIMEOUT_TTL_MS);
    private static final int ACCEPT_TIMEOUT_SCAN_LIMIT = 100;

    /** 待分配池单次返回上限：避免一次拉全库订单，也避免前端渲染上千张卡片。 */
    private static final int ASSIGNABLE_POOL_LIMIT = 100;

    /** 可接单的看护者状态：在职 / 休息 / 忙碌（与 requireFutureBookableKeeper 同一口径）。 */
    private static final List<Integer> BOOKABLE_KEEPER_STATUSES = List.of(
            StatusCode.KEEPER_ACTIVE.getValue(),
            StatusCode.KEEPER_OFFLINE.getValue(),
            StatusCode.KEEPER_BUSY.getValue());
    private static final Random HANDOVER_CODE_RANDOM = new Random();
    private static final Object CREATE_ORDER_LOCK = new Object();

    @Value("${booking.max-order-days:365}")
    private int maxOrderDays = 365;

    /** 交接距离允许范围（米），与 GeoController 暴露给前端的配置同源 */
    @Value("${gao.map.handover-radius-meters:500}")
    private int handoverRadiusMeters;

    /**
     * 调试开关：是否允许前端通过 skip_distance_check_wsh 跳过距离校验。
     * 默认关闭；验证阶段可在 application.yml 打开，上线前务必关掉。
     */
    @Value("${order.handover.allow-debug-skip:false}")
    private boolean allowDebugSkipDistance;

    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final PetMapper petMapper;
    private final KeeperMapper keeperMapper;
    private final MerchantMapper merchantMapper;
    private final MerchantService merchantService;
    private final ServiceItemMapper serviceItemMapper;
    private final UserMapper userMapper;
    private final OrderSnapshotService orderSnapshotService;
    private final ApplicationEventPublisher eventPublisher;
    private final QualificationService qualificationService;
    private final OrderStatusBroadcaster orderStatusBroadcaster;
    private final MessageSender messageSender;
    private final AccountingService accountingService;
    private final KeeperAttendanceService keeperAttendanceService;
    private final KeeperLeaveService keeperLeaveService;
    private final CouponService couponService;
    private final MembershipBenefitService membershipBenefitService;
    private final ObjectMapper objectMapper;
    private final BusinessHoursService businessHoursService;
    private final BusinessHoursTargetResolver businessHoursTargetResolver;
    private final RatingMapper ratingMapper;

    public OrderServiceImpl(OrderMapper orderMapper,
                            PaymentMapper paymentMapper,
                            PetMapper petMapper,
                            KeeperMapper keeperMapper,
                            MerchantMapper merchantMapper,
                            MerchantService merchantService,
                            ServiceItemMapper serviceItemMapper,
                            UserMapper userMapper,
                            OrderSnapshotService orderSnapshotService,
                            ApplicationEventPublisher eventPublisher,
                            QualificationService qualificationService,
                            OrderStatusBroadcaster orderStatusBroadcaster,
                            MessageSender messageSender,
                            AccountingService accountingService,
                            KeeperAttendanceService keeperAttendanceService,
                            KeeperLeaveService keeperLeaveService,
                            CouponService couponService,
                            MembershipBenefitService membershipBenefitService,
                            ObjectMapper objectMapper,
                            BusinessHoursService businessHoursService,
                            BusinessHoursTargetResolver businessHoursTargetResolver,
                            RatingMapper ratingMapper) {
        this.ratingMapper = ratingMapper;
        this.orderMapper = orderMapper;
        this.paymentMapper = paymentMapper;
        this.petMapper = petMapper;
        this.keeperMapper = keeperMapper;
        this.merchantMapper = merchantMapper;
        this.merchantService = merchantService;
        this.serviceItemMapper = serviceItemMapper;
        this.userMapper = userMapper;
        this.orderSnapshotService = orderSnapshotService;
        this.eventPublisher = eventPublisher;
        this.qualificationService = qualificationService;
        this.orderStatusBroadcaster = orderStatusBroadcaster;
        this.messageSender = messageSender;
        this.accountingService = accountingService;
        this.keeperAttendanceService = keeperAttendanceService;
        this.keeperLeaveService = keeperLeaveService;
        this.couponService = couponService;
        this.membershipBenefitService = membershipBenefitService;
        this.objectMapper = objectMapper;
        this.businessHoursService = businessHoursService;
        this.businessHoursTargetResolver = businessHoursTargetResolver;
    }


    /**
     * 【查询全部订单（实现）】
     *
     * 业务作用：
     * 查询所有订单记录，批量加载关联实体并组装为增强DTO，避免N+1查询问题。
     *
     * 调用场景：
     * 管理后台全量订单查询。
     *
     * 调用链：
     * OrderService.listAll()
     * ↓
     * orderMapper.selectList() → toDTOEnrichedList()（批量加载）
     *
     * 数据处理：
     * 使用MyBatis-Plus LambdaQueryWrapper按创建时间降序查询，然后通过toDTOEnrichedList
     * 批量加载关联的服务、用户、宠物、看护者、商家和快照数据。
     *
     * 状态影响：
     * 只读操作，@Transactional(readOnly = true)。
     *
     * @return 增强订单DTO列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> listAll() {
        List<PetOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>().orderByDesc(PetOrder::getCreated_at_wsh));
        return toDTOEnrichedList(orders);
    }

    /**
     * 【查询宠物主人订单列表（实现）】
     *
     * 业务作用：
     * 根据主人ID查询其名下所有订单，用于主人端"我的订单"功能。
     *
     * 调用链：
     * OrderService.listByOwner()
     * ↓
     * orderMapper.selectList(ownerId过滤 + 时间降序) → toDTOEnrichedList()
     *
     * 数据处理：
     * 按owner_id_wsh字段过滤，按created_at_wsh降序排列。
     *
     * 状态影响：
     * 只读操作。
     *
     * @param ownerId 宠物主人用户ID
     * @return 该主人的增强订单DTO列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> listByOwner(Long ownerId) {
        List<PetOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getOwner_id_wsh, ownerId)
                        .orderByDesc(PetOrder::getCreated_at_wsh));
        return toDTOEnrichedList(orders);
    }

    /**
     * 【查询商家订单列表（实现）】
     *
     * 业务作用：
     * 根据商家ID查询该商家相关订单，用于商家端订单管理。
     *
     * 调用链：
     * OrderService.listByMerchant()
     * ↓
     * orderMapper.selectList(merchantId过滤 + 时间降序) → toDTOEnrichedList()
     *
     * 状态影响：
     * 只读操作。
     *
     * @param merchantId 商家ID
     * @return 该商家的增强订单DTO列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> listByMerchant(Long merchantId) {
        List<PetOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getMerchant_id_wsh, merchantId)
                        .orderByDesc(PetOrder::getCreated_at_wsh));
        return toDTOEnrichedList(orders);
    }

    /**
     * 【查询看护者活跃订单（实现）】
     *
     * 业务作用：
     * 查询看护者的履约中订单（排除待付款和已支付），用于看护者端"我的任务"。
     *
     * 调用链：
     * OrderService.listByKeeper()
     * ↓
     * orderMapper.selectList(keeperId + notIn(PENDING,PAID) + 时间降序) → toDTOEnrichedList()
     *
     * 状态影响：
     * 只读操作。
     *
     * @param keeperId 看护者ID
     * @return 活跃订单DT列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> listByKeeper(Long keeperId) {
        List<PetOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getKeeper_id_wsh, keeperId)
                        .notIn(PetOrder::getStatus_wsh, List.of(OrderStatus.PENDING, OrderStatus.PAID))
                        .orderByDesc(PetOrder::getCreated_at_wsh));
        return toDTOEnrichedList(orders);
    }

    /**
     * 【待分配订单池（看护者视角）】
     *
     * 业务作用：
     * 列出当前看护者所属商家名下、已支付但尚未分配负责人的订单，供其抢单。
     *
     * @param userId 当前登录用户ID
     * @return 可抢订单列表；看护者未绑定商家时返回空列表
     */
    @Transactional(readOnly = true)
    @Override
    public List<OrderDTO> listAssignableForKeeper(Long userId) {
        // 只有「当前可接单」的看护者才应该看到池子：离职/待审/被拒的人不应看到客户信息
        List<Keeper> keepers = keeperMapper.selectList(
                new LambdaQueryWrapper<Keeper>()
                        .eq(Keeper::getUser_id_wsh, userId)
                        .in(Keeper::getStatus_wsh, BOOKABLE_KEEPER_STATUSES));
        List<Long> merchantIds = keepers.stream()
                .map(Keeper::getMerchant_id_wsh)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (merchantIds.isEmpty()) {
            return List.of();
        }
        List<PetOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>()
                        .in(PetOrder::getMerchant_id_wsh, merchantIds)
                        .eq(PetOrder::getStatus_wsh, OrderStatus.PAID)
                        .isNull(PetOrder::getKeeper_id_wsh)
                        // 服务已经开始的不该再出现在可抢池里（自动分配失败的历史残留）
                        .ge(PetOrder::getEnd_date_wsh, LocalDate.now())
                        .orderByAsc(PetOrder::getCreated_at_wsh)
                        .last("LIMIT " + ASSIGNABLE_POOL_LIMIT));
        return toDTOEnrichedList(orders);
    }

    /**
     * 【实体转基础DTO（实现）】
     *
     * 业务作用：
     * 使用BeanUtils.copyProperties浅拷贝属性，不含关联实体信息。
     *
     * 状态影响：
     * 纯内存操作。
     *
     * @param entity 订单实体
     * @return 基础DTO
     */
    @Override
    public OrderDTO toDTO(PetOrder entity) {
        if (entity == null) return null;
        OrderDTO dto = new OrderDTO();
        BeanUtils.copyProperties(entity, dto);
        // 交接码/接走码属于防伪凭证，默认不外泄：所有列表接口都不会带出。
        // 仅订单详情接口在确认当前用户是主人或管理员后再显式回填。
        dto.setHandover_code_wsh(null);
        dto.setPickup_code_wsh(null);
        applyBillingReadCompat(dto, entity);
        return dto;
    }

    /**
     * 【多单位读取兼容（静态辅助）】
     *
     * 业务作用：
     * 旧订单缺失 billing_unit_wsh/quantity_wsh/unit_price_wsh/duration_minutes_wsh
     * 新字段时，按旧字段（days_wsh/price_per_day_wsh）推导为按夜语义，保证历史订单
     * 读取不出现 null 计价字段；新订单已存储新字段的原样保留，不覆盖。
     */
    static void applyBillingReadCompat(OrderDTO dto, PetOrder entity) {
        if (entity == null || dto == null) {
            return;
        }
        if (entity.getBilling_unit_wsh() == null || entity.getBilling_unit_wsh().isBlank()) {
            // 迁移后唯一的长区间单位是按夜；历史行由迁移脚本补齐，这里只做只读兜底展示
            dto.setBilling_unit_wsh(BookingUnit.NIGHT);
            dto.setQuantity_wsh(entity.getDays_wsh());
            dto.setUnit_price_wsh(entity.getPrice_per_day_wsh());
            dto.setDuration_minutes_wsh(BookingUnit.resolveDurationMinutes(BookingUnit.NIGHT, null));
        }
    }

    /**
     * 【实体转增强DTO（实现）】
     *
     * 业务作用：
     * 委托给toDTOEnrichedList批量增强方法，加载关联实体名称和订单快照。
     *
     * 调用链：
     * → toDTOEnrichedList(List.of(entity))
     *
     * @param entity 订单实体
     * @return 增强DTO
     */
    @Override
    public OrderDTO toDTOEnriched(PetOrder entity) {
        if (entity == null) return null;
        return toDTOEnrichedList(List.of(entity)).get(0);
    }

    /**
     * 【删除订单（逻辑删除实现）】
     *
     * 业务作用：
     * 管理员删除订单记录，仅置 deleted_wsh=1，不物理删除，不触碰资金与状态。
     *
     * 调用链：
     * OrderController.delete()
     * ↓
     * delete @Transactional
     * ↓
     * getById → OrderMapper.deleteById（@TableLogic 转为 UPDATE deleted_wsh=1）
     *
     * 状态影响：
     * deleted_wsh 置 1；订单状态字段不变。
     *
     * 异常情况：
     * 订单不存在时 getById 抛 BusinessException。
     */
    @Override
    @Transactional
    public void delete(Long id) {
        log.info("delete() 被调用: id={}", id);
        getById(id);
        orderMapper.deleteById(id);
    }

    /**
     * 【根据订单号查询实体（实现）】
     *
     * 业务作用：
     * 使用LambdaQueryWrapper按订单号精确匹配查询订单实体。
     *
     * 异常情况：
     * 订单号为空抛400；订单不存在抛BusinessException。
     *
     * @param orderNo 订单编号
     * @return 订单实体
     */
    @Override
    @Transactional(readOnly = true)
    public PetOrder getByOrderNo(String orderNo) {
        if (isBlank(orderNo)) {
            throw new BusinessException(400, "订单号不能为空");
        }
        PetOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<PetOrder>().eq(PetOrder::getOrder_no_wsh, orderNo));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    /**
     * 【根据ID查询实体（实现）】
     *
     * 业务作用：
     * 使用MyBatis-Plus selectById查询订单实体，不存在则抛异常。
     *
     * @param id 订单ID
     * @return 订单实体
     */
    @Override
    @Transactional(readOnly = true)
    public PetOrder getById(Long id) {
        PetOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    /**
     * 【创建订单（实现）】
     *
     * 业务作用：
     * 完整的订单创建流程：校验→计算金额→插入订单→锁定优惠券/会员权益→创建快照→刷新容量→发送超时检查。
     *
     * 调用链：
     * OrderService.createOrder()
     * ↓
     * [synchronized] requirePet() → requireFutureBookingEligibleMerchant()
     * → quoteOrder()（单位/夜数/日期一致性/占用区间校验 + 计价）
     * → 计算金额(discount + coupon + member) → orderMapper.insert()
     * → couponService.lockForOrder() → membershipBenefitService.lockForOrder()
     * → orderSnapshotService.createForOrder() → refreshKeeperCurrentPets()
     * → broadcastOrderChange() → schedulePaymentTimeoutCheck()
     *
     * 数据处理：
     * 1. 计算总价：price_per_day × days
     * 2. 长住折扣：≥30天10% off，7-29天5% off
     * 3. 优惠券和会员折扣通过各自服务预览并锁定
     * 4. 最终金额 = 总价 - 折扣 - 优惠券减免 - 会员折扣
     * 5. 平台补贴 = 优惠券平台补贴 + 会员折扣
     * 6. 结算金额 = 优惠券结算价（商家实际收入）
     *
     * 并发控制：
     * 使用synchronized(CREATE_ORDER_LOCK)在单JVM内串行化创建，防止并发导致的容量/冲突检查失效。
     *
     * 业务规则：
     * 同接口定义。
     *
     * 状态影响：
     * 创建后订单状态为PENDING。
     *
     * @param ownerId 主人ID
     * @param request 创建订单请求体
     * @return 创建后的增强订单DTO
     */
    @Transactional
    @Override
    public OrderDTO createOrder(Long ownerId, OrderCreateRequestDTO request) {
        Assert.notNull(request, "订单请求不能为空");
        // 教学注释：下单不是单纯 insert，前面还有“查宠物档期/查看护者容量”。
        // 在没有排班表或数据库约束前，同 JVM 先串行化这段，避免两个请求同时通过检查后一起写入。
        synchronized (CREATE_ORDER_LOCK) {
            return doCreateOrder(ownerId, request);
        }
    }

    /**
     * 【批量创建订单（多宠物连续下单）】
     *
     * 业务作用：
     * 在一个事务内为多只宠物分别创建独立订单（每单一只宠物，各自日期区间），
     * 任一失败整批回滚。共享校验（服务/商家/看护人/单位/价格/版本）只执行一次；
     * 容量与宠物冲突复用单笔路径——同事务顺序插入的读己之写保证批次内部互相挤占
     * （如 A、B 同区间顶满容量）也不会漏检。
     *
     * 业务规则：
     * 1. 仅支持 day 单位服务；items 数量 2..10
     * 2. 同一宠物重叠区间被拒（不重叠允许）
     * 3. 优惠券只应用于批次中 baseAmount 最大的订单（并列取 items 顺序靠前者）
     * 4. 每单 PENDING、各自独立支付
     */
    @Transactional
    @Override
    public List<OrderDTO> createOrders(Long ownerId, OrderBatchCreateRequestDTO request) {
        Assert.notNull(request, "批量订单请求不能为空");
        List<OrderBatchItemDTO> items = request.getItems();
        if (items == null || items.size() < OrderBatchCreateRequestDTO.MIN_ITEMS) {
            throw new BusinessException(400, "批量下单至少需要" + OrderBatchCreateRequestDTO.MIN_ITEMS
                    + "只宠物，单只宠物请使用单笔下单");
        }
        if (items.size() > OrderBatchCreateRequestDTO.MAX_ITEMS) {
            throw new BusinessException(400, "批量下单最多支持" + OrderBatchCreateRequestDTO.MAX_ITEMS + "只宠物");
        }
        synchronized (CREATE_ORDER_LOCK) {
            // 共享契约只校验一次（与单笔一致的口径）
            // 下单不再支持指定负责人：订单一律先进待分配池，由看护者抢单 / 商家指派 / 超时自动分配。
            ServiceItem service = loadBookableService(request.getService_id_wsh());
            String unit = assertServiceUnitSupported(service);
            if (!BookingUnit.NIGHT.equals(unit)) {
                throw new BusinessException(400, BookingErrorCode.UNSUPPORTED_SERVICE_UNIT,
                        "批量下单仅支持按夜寄养服务");
            }
            Merchant merchant = requireFutureBookingEligibleMerchant(service.getMerchant_id_wsh());
            assertMerchantPayloadCompatible(request.getMerchant_id_wsh(), merchant.getId_wsh());
            assertServiceVersionMatches(service, request.getService_version_wsh());
            assertBillingUnitCompatible(request.getBilling_unit_wsh(), unit);
            assertExpectedUnitPrice(service, request.getExpected_unit_price_wsh());

            // 券归属：先算每单 baseAmount，最大的一单（并列取先出现的）才应用优惠券。
            // 此处只做纯计算与日期校验，不落库；真正的落库校验在 doCreateOrder 内。
            List<OrderCreateRequestDTO> singles = new ArrayList<>(items.size());
            int couponTarget = -1;
            BigDecimal maxBase = BigDecimal.ZERO;
            for (int i = 0; i < items.size(); i++) {
                OrderCreateRequestDTO single = toSingleRequest(request, items.get(i));
                singles.add(single);
                // 券归属基准与实际计价同源：直接用按夜初始原价函数
                BigDecimal base = nightBatchBase(service, single);
                if (base.compareTo(maxBase) > 0) {
                    maxBase = base;
                    couponTarget = i;
                }
            }

            List<OrderDTO> results = new ArrayList<>(items.size());
            for (int i = 0; i < singles.size(); i++) {
                OrderCreateRequestDTO single = singles.get(i);
                if (i != couponTarget) {
                    single.setUser_coupon_id_wsh(null);
                }
                results.add(doCreateOrder(ownerId, single));
            }
            return results;
        }
    }

    /** 组装单笔请求：共享字段 + 单项宠物参数。 */
    private OrderCreateRequestDTO toSingleRequest(OrderBatchCreateRequestDTO batch, OrderBatchItemDTO item) {
        OrderCreateRequestDTO single = new OrderCreateRequestDTO();
        BeanUtils.copyProperties(batch, single);
        single.setPet_id_wsh(item.getPet_id_wsh());
        single.setStart_date_wsh(item.getStart_date_wsh());
        single.setEnd_date_wsh(item.getEnd_date_wsh());
        single.setDelivery_time_wsh(item.getDelivery_time_wsh());
        single.setPickup_time_wsh(item.getPickup_time_wsh());
        single.setQuantity_wsh(item.getQuantity_wsh());
        return single;
    }

    /** 批量按夜寄养的券目标基准价：与单笔 quoteOrder 使用同一个 night 纯函数。 */
    private static BigDecimal nightBatchBase(ServiceItem service, OrderCreateRequestDTO single) {
        LocalDate checkinDate = single.getStart_date_wsh();
        if (checkinDate == null) {
            throw new BusinessException(400, "按夜寄养每行必须提供入住日期");
        }
        int nights = single.getQuantity_wsh() == null ? 0 : single.getQuantity_wsh();
        if (nights < 1) {
            throw new BusinessException(400, "预订夜数必须为正整数");
        }
        LocalDateTime delivery = single.getDelivery_time_wsh() != null
                ? single.getDelivery_time_wsh()
                : OrderPricing.checkinAnchor(checkinDate);
        Duration early = OrderPricing.earlyDuration(OrderPricing.checkinAnchor(checkinDate), delivery);
        return OrderPricing.nightInitialAmount(resolveUnitPrice(service), nights, early);
    }

    /**
     * 单笔订单创建实现（供单笔与批量共用）。调用方必须持有 CREATE_ORDER_LOCK。
     */
    private OrderDTO doCreateOrder(Long ownerId, OrderCreateRequestDTO request) {
        Quote q = quoteOrder(ownerId, request);
        Pet pet = q.pet();
        ServiceItem service = q.service();
        Merchant merchant = q.merchant();
        Keeper keeper = q.keeper();
        String unit = q.unit();
        int quantity = q.quantity();
        int durationMinutes = q.durationMinutes();
        LocalDate startDate = q.startDate();
        LocalDate endDate = q.endDate();
        LocalDateTime deliveryTime = q.deliveryTime();
        LocalDateTime receiverStart = q.receiverStart();
        LocalDateTime receiverEnd = q.receiverEnd();
        LocalDateTime pickupTime = q.pickupTime();
        BigDecimal unitPrice = q.unitPrice();
        BigDecimal totalAmount = q.totalAmount();
        BigDecimal discount = q.discount();
        CouponDiscountResult couponDiscount = q.coupon();
        MembershipDiscountDTO membershipDiscount = q.membership();
        BigDecimal memberDiscount = q.memberDiscount();

        PetOrder order = new PetOrder();

        BeanUtils.copyProperties(request, order);

        order.setOrder_no_wsh(generateOrderNo());                       // 订单号
        order.setOwner_id_wsh(ownerId);                                 //  下单用户

        order.setPet_id_wsh(pet.getId_wsh());                           //  下单宠物
        order.setMerchant_id_wsh(merchant.getId_wsh());                 //  下单商户
        order.setKeeper_id_wsh(keeper == null ? null : keeper.getId_wsh());  // 下单keeper（可空=待分配）

        // 服务端权威下单字段：单位/数量/单价/时长快照（客户端不可伪造）
        order.setBilling_unit_wsh(unit);                                //  计费单位快照
        order.setQuantity_wsh(quantity);                                //  计费数量
        order.setUnit_price_wsh(unitPrice);                             //  单单位单价快照（计价权威）
        order.setDuration_minutes_wsh(durationMinutes);                 //  单单位时长快照
        order.setStart_date_wsh(startDate);                             //  服务开始日期
        order.setEnd_date_wsh(endDate);                                 //  服务结束日期
        // 兼容投影：day 令 days_wsh=quantity；boarding 按计费小时折算天数；session/hour 令 days_wsh=1
        order.setDays_wsh(q.daysDisplay());
        order.setPrice_per_day_wsh(unitPrice);                          //  每日单价（兼容投影=单单位单价）
        order.setDiscount_wsh(discount);                                //  优惠
        order.setTotal_amount_wsh(totalAmount);                         //  总价
        order.setCoupon_id_wsh(couponDiscount.getUser_coupon_id_wsh());
        order.setCoupon_template_id_wsh(couponDiscount.getTemplate_id_wsh());
        order.setCoupon_discount_wsh(q.couponDiscount());
        order.setMembership_id_wsh(membershipDiscount.getMembership_id_wsh());
        order.setMembership_plan_id_wsh(membershipDiscount.getPlan_id_wsh());
        order.setMembership_discount_wsh(memberDiscount);
        order.setMembership_snapshot_wsh(membershipDiscount.getSnapshot_wsh());
        order.setPlatform_subsidy_wsh(q.platformSubsidy());
        order.setSettlement_amount_wsh(defaultMoney(couponDiscount.getSettlement_amount_wsh()));
        order.setPromotion_snapshot_wsh(toPromotionSnapshot(couponDiscount, membershipDiscount));
        order.setFinal_amount_wsh(q.finalAmount());      //  实付
        order.setStatus_wsh(OrderStatus.PENDING);                       //  订单状态

        order.setHandover_code_wsh(generateHandoverCode());             //  交接码
        // 独立接走码：与送达码不同，仅订单主人/管理员在服务中且未确认接走时可读
        order.setPickup_code_wsh(generatePickupCode(order.getHandover_code_wsh()));
        order.setOvertime_hours_wsh(0);
        order.setOvertime_amount_wsh(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        order.setDelivery_time_wsh(deliveryTime);                       //  宠物取送时间/服务开始
        order.setReceiver_available_start_wsh(receiverStart);           //  接收者接收宠物的时间
        order.setReceiver_available_end_wsh(receiverEnd);               //  接收者接收宠物的结束时间
        order.setPickup_time_wsh(pickupTime);                           //  宠物取回时间/服务结束
        order.setFinal_report_generated_wsh(0);                         //  是否生成了 boarding report
        orderMapper.insert(order);
        couponService.lockForOrder(ownerId, request.getUser_coupon_id_wsh(), order.getId_wsh(),
                order.getOrder_no_wsh(), order.getCoupon_discount_wsh());
        membershipBenefitService.lockForOrder(ownerId, membershipDiscount, order.getId_wsh(), order.getOrder_no_wsh());
        User owner = userMapper.selectById(ownerId);
        orderSnapshotService.createForOrder(order, owner, pet, merchant, keeper, service, request);

        refreshKeeperCurrentPets(order.getKeeper_id_wsh());
        broadcastOrderChange(order);
        schedulePaymentTimeoutCheck(order);
        return toDTOEnriched(order);
    }

    /**
     * 【报价内核：下单与预览共用】
     *
     * 业务作用：
     * 执行「能不能约 + 该收多少」的全部判定，返回一个不可变的报价结果，**不产生任何写副作用**
     * （不插订单、不锁券、不锁会员权益、不发广播、不排定时器）。
     *
     * 为什么必须共用：
     * 价格预览接口与真实下单如果各写一份，两边一定会在某次改动后分叉。这里把两者收敛到
     * 同一个方法，预览金额与扣款金额的一致性由「调用同一个方法」保证，而不是靠人工比对。
     *
     * 调用链：
     * OrderServiceImpl.doCreateOrder（下单）→ quoteOrder
     * OrderServiceImpl.previewOrder（预览）→ quoteOrder
     *
     * 注意：本方法内的冲突/容量/休假校验会读库，但都是只读；写操作全部留在 doCreateOrder。
     */
    private Quote quoteOrder(Long ownerId, OrderCreateRequestDTO request) {
        // 获取到当前用户的下单宠物
        Pet pet = requirePet(ownerId, request.getPet_id_wsh()); // 验证宠物存在且属于当前用户
        // 下单不产生负责人：订单一律进「待分配」池，
        // 由看护者抢单 / 商家指派 / 超时自动分配产生负责人（见 assignTo）。
        Keeper keeper = null;
        // 先加载可下单服务（产品预订必须指定服务），商家与价格均由服务端根据服务派生
        ServiceItem service = loadBookableService(request.getService_id_wsh()); // 确保服务存在,未被下架
        String unit = assertServiceUnitSupported(service);  // 验证服务单位
        Merchant merchant = requireFutureBookingEligibleMerchant(service.getMerchant_id_wsh()); // 验证商家可预约
        assertMerchantPayloadCompatible(request.getMerchant_id_wsh(), merchant.getId_wsh()); // 验证服务归属
        assertServiceVersionMatches(service, request.getService_version_wsh()); // 验证服务版本,避免商家改价,但是用户没有刷新
        assertBillingUnitCompatible(request.getBilling_unit_wsh(), unit); // 验证计费单位,确保客户端提交的计费单位与服务单位一致
        assertExpectedUnitPrice(service, request.getExpected_unit_price_wsh()); // 验证期望单价,确保客户端提交的单价与服务单价一致

        // 下单时间/数量/时长（半开区间 [start, end)）
        int quantity;
        int durationMinutes;
        Duration early = Duration.ZERO;
        LocalDate startDate;
        LocalDate endDate;
        LocalDateTime deliveryTime;
        LocalDateTime receiverStart;
        LocalDateTime receiverEnd;
        LocalDateTime pickupTime;

        if (BookingUnit.NIGHT.equals(unit)) {
            // night：用户明确选择入住日 D 与预订夜数 N；A=预约送达、B=预约接走。
            // 标准离店锚点只依赖 N，提前送达只加钱、不顺延离店日期。
            int nights = resolveNightCount(request);
            LocalDate checkinDate = request.getStart_date_wsh();
            if (checkinDate == null) {
                throw new BusinessException(400, "按夜寄养必须提供入住日期");
            }
            LocalDateTime anchor = OrderPricing.checkinAnchor(checkinDate);
            LocalDateTime delivery = request.getDelivery_time_wsh() != null
                    ? request.getDelivery_time_wsh() : anchor;
            LocalDateTime pickup = request.getPickup_time_wsh() != null
                    ? request.getPickup_time_wsh() : OrderPricing.freeCheckoutUntil(checkinDate, nights);
            if (!delivery.toLocalDate().equals(checkinDate)) {
                throw new BusinessException(400, "送达时间必须落在入住日期当天");
            }
            if (!delivery.isAfter(LocalDateTime.now())) {
                throw new BusinessException(400, "送达时间必须晚于当前时间");
            }
            LocalDate checkoutDate = checkinDate.plusDays(nights);
            if (!pickup.toLocalDate().equals(checkoutDate)) {
                throw new BusinessException(400,
                        "预约接走时间必须落在离店日期 " + checkoutDate + "；如需多住请增加夜数");
            }
            if (!pickup.isAfter(delivery)) {
                throw new BusinessException(400, "预约接走时间必须晚于送达时间");
            }
            if (request.getEnd_date_wsh() != null && !request.getEnd_date_wsh().equals(checkoutDate)) {
                throw new BusinessException(400, "结束日期与服务端按夜数推导的离店日期不一致");
            }
            assertClientEndTimeConsistent(request.getEnd_time_wsh(), pickup);
            // 送达与预约接走都要落在营业窗口内；实际接走确认不再要求仍在营业时段
            validateBoardingBusinessHours(merchant.getId_wsh(), delivery, pickup);
            // 预约占用终点 = max(预约接走 B, 免费截止 G)：宽限期仍占容量
            LocalDateTime occupancyEndAt = maxInstant(pickup, OrderPricing.freeCheckoutUntil(checkinDate, nights));
            ensureNoPetIntervalConflict(pet.getId_wsh(), delivery, occupancyEndAt, null);
            early = OrderPricing.earlyDuration(anchor, delivery);
            startDate = checkinDate;
            endDate = checkoutDate;
            deliveryTime = delivery;
            receiverStart = delivery;
            receiverEnd = delivery.plusMinutes(RECEIVER_WINDOW_MINUTES);
            pickupTime = pickup;
            durationMinutes = BookingUnit.NIGHT_DURATION_MINUTES;   // 单位元数据：一夜 24 小时
            quantity = nights;                                     // 预订夜数 N
        } else {
            // session/hour：从可用性槽位选择开始时间，服务端按时长/数量计算结束时间
            LocalDateTime slotStart = requireSlotStart(request);
            durationMinutes = BookingUnit.resolveDurationMinutes(unit, service.getDuration_minutes_wsh());
            quantity = resolveSlotQuantity(unit, request, durationMinutes);
            LocalDateTime computedEnd = slotStart.plusMinutes((long) quantity * durationMinutes);
            assertClientEndTimeConsistent(request.getEnd_time_wsh(), computedEnd);
            if (!slotStart.isBefore(LocalDateTime.now())) {
                startDate = slotStart.toLocalDate();
                endDate = startDate.plusDays(1);
                serviceWindowCheck(merchant.getId_wsh(), slotStart, computedEnd);
                ensureNoPetIntervalConflict(pet.getId_wsh(), slotStart, computedEnd, null);
                deliveryTime = slotStart;
                receiverStart = slotStart;
                receiverEnd = slotStart.plusMinutes(RECEIVER_WINDOW_MINUTES);
                pickupTime = computedEnd;
            } else {
                throw new BusinessException(400, "选择的开始时间已过，请重新选择槽位");
            }
        }

        BigDecimal unitPrice = resolveUnitPrice(service);
        BigDecimal totalAmount;
        int discountDays;
        if (BookingUnit.NIGHT.equals(unit)) {
            // 预付原价 = 夜费 P×N + 提前费；晚接费不预收，实际接走时单独结算
            totalAmount = OrderPricing.nightInitialAmount(unitPrice, quantity, early);
            // 长住档位只看预订夜数 N，提前加收的一夜不参与档位
            discountDays = quantity;
        } else if (BookingUnit.HOUR.equals(unit)) {
            // 按实际分钟折算：数量 × 单位分钟数 ÷ 60，避免半小时粒度被当成整小时收费
            totalAmount = OrderPricing.hourBase(unitPrice, quantity, durationMinutes);
            discountDays = resolveDiscountDays(unit, quantity);
        } else {
            totalAmount = resolveBaseAmount(unitPrice, quantity);
            discountDays = resolveDiscountDays(unit, quantity);
        }
        BigDecimal longStayDiscount = calculateDiscount(totalAmount, discountDays);
        CouponDiscountResult couponDiscount = couponService.previewForOrder(
                ownerId,
                request.getUser_coupon_id_wsh(),
                totalAmount,
                longStayDiscount,
                merchant.getId_wsh(),
                service != null ? service.getId_wsh() : null);
        MembershipDiscountDTO membershipDiscount = membershipBenefitService.previewOrderDiscount(
                ownerId, couponDiscount.getFinal_amount_wsh());
        BigDecimal memberDiscount = defaultMoney(membershipDiscount.getMembership_discount_wsh());
        BigDecimal discount = longStayDiscount
                .add(defaultMoney(couponDiscount.getCoupon_discount_wsh()))
                .add(memberDiscount);

        return new Quote(pet, service, merchant, keeper, unit, quantity, durationMinutes,
                early, startDate, endDate, deliveryTime, receiverStart, receiverEnd, pickupTime,
                unitPrice, totalAmount, longStayDiscount, couponDiscount, memberDiscount, discount,
                membershipDiscount);
    }

    /**
     * 【根据ID取消订单（实现）】
     *
     * 业务作用：
     * 取消待付款订单，释放优惠券和会员权益，刷新看护者容量。
     *
     * 数据处理：
     * 使用乐观锁（WHERE status=PENDING）更新，防止并发取消。
     *
     * 业务规则：
     * 1. 校验订单归属
     * 2. 校验订单状态为PENDING
     * 3. 乐观锁更新失败则说明状态已变化
     *
     * @param ownerId 主人ID
     * @param orderId 订单ID
     */
    @Transactional
    @Override
    public void cancelOrderById(Long ownerId, Long orderId) {
        PetOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!ownerId.equals(order.getOwner_id_wsh())) {
            throw new BusinessException("无权限取消此订单");
        }
        if (!OrderStatus.PENDING.equals(order.getStatus_wsh())) {
            throw new BusinessException("只有待付款订单可以取消");
        }
        PetOrder update = new PetOrder();
        update.setStatus_wsh(OrderStatus.CANCELLED);
        int updated = orderMapper.update(update, new LambdaUpdateWrapper<PetOrder>()
                .eq(PetOrder::getId_wsh, order.getId_wsh())
                .eq(PetOrder::getStatus_wsh, OrderStatus.PENDING));
        if (updated == 0) {
            throw new BusinessException("订单状态已变化，无法取消");
        }
        couponService.releaseForOrder(order.getId_wsh());
        membershipBenefitService.releaseForOrder(order.getId_wsh());
        order.setStatus_wsh(OrderStatus.CANCELLED);
        refreshKeeperCurrentPets(order.getKeeper_id_wsh());
        broadcastOrderChange(order);
    }

    /**
     * 【根据订单号取消订单（已废弃）】
     *
     * @deprecated 请使用 cancelOrderById
     * @param ownerId 主人ID
     * @param orderNo 订单编号
     */
    @Deprecated
    @Transactional
    @Override
    public void cancelOrder(Long ownerId, String orderNo) {
        PetOrder order = getByOrderNo(orderNo);
        cancelOrderById(ownerId, order.getId_wsh());
    }

    // ==================== 负责人分配（抢单 / 指派 / 超时自动分配） ====================

    /**
     * 【看护者抢单】
     *
     * 业务作用：
     * 已支付且尚未分配负责人的订单，由订单所属商家名下的看护者主动领取；
     * 抢单成功即进入 confirmed，不再需要额外一次接单确认。
     *
     * 并发控制：
     * CAS 更新（WHERE status='paid' AND keeper_id IS NULL），同一订单只会被一人抢到。
     */
    @Transactional
    @Override
    public void claimOrder(Long userId, String orderNo) {
        PetOrder order = getByOrderNo(orderNo);
        if (!OrderStatus.PAID.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "当前订单状态无法接单");
        }
        if (order.getKeeper_id_wsh() != null) {
            throw new BusinessException(400, "订单已有负责人，无法抢单");
        }
        Keeper keeper = keeperMapper.selectOne(new LambdaQueryWrapper<Keeper>()
                .eq(Keeper::getUser_id_wsh, userId)
                .eq(Keeper::getMerchant_id_wsh, order.getMerchant_id_wsh())
                .in(Keeper::getStatus_wsh, BOOKABLE_KEEPER_STATUSES)
                .last("LIMIT 1"));
        if (keeper == null) {
            throw new BusinessException(403, "只有该商家名下在职的看护者可以抢单");
        }
        if (!assignTo(order, keeper)) {
            throw new BusinessException(400, "订单已被其他看护者接单");
        }
        log.info("看护者[{}]抢单成功, 订单编号: {}", keeper.getId_wsh(), order.getOrder_no_wsh());
    }

    /**
     * 【商家指派负责人】
     *
     * 业务作用：
     * 商家管理员把待分配订单指派给名下某个看护者，指派成功即 confirmed。
     * 指派前重新执行资质、休假、容量校验——与抢单、自动分配共用同一条校验链。
     */
    @Transactional
    @Override
    public void assignOrder(Long userId, String orderNo, Long keeperId) {
        PetOrder order = getByOrderNo(orderNo);
        if (!OrderStatus.PAID.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "当前订单状态无法指派");
        }
        if (!merchantService.isOwner(order.getMerchant_id_wsh(), userId)) {
            throw new BusinessException(403, "只有订单所属商家的管理员可以指派");
        }
        Keeper keeper = requireFutureBookableKeeper(keeperId);
        Merchant assignMerchant = merchantMapper.selectById(order.getMerchant_id_wsh());
        if (assignMerchant == null) {
            throw new BusinessException(400, "订单所属商家不存在，无法指派");
        }
        validateKeeperMerchant(keeper, assignMerchant);
        if (!assignTo(order, keeper)) {
            throw new BusinessException(400, "订单状态已变化，无法指派");
        }
        log.info("订单[{}]被指派给看护者[{}]", order.getOrder_no_wsh(), keeper.getId_wsh());
    }

    /**
     * 把已支付订单分配给指定看护者并置为 confirmed。
     *
     * @return true 表示本次分配成功；false 表示订单已被他人抢先分配或状态已变化
     */
    private boolean assignTo(PetOrder order, Keeper keeper) {
        validateKeeperQualification(keeper.getId_wsh());
        OrderOccupancy.Interval interval = OrderOccupancy.intervalOf(order);
        if (interval == null) {
            throw new BusinessException(400, "订单服务时间缺失，无法分配负责人");
        }
        ensureKeeperIntervalCapacity(keeper.getId_wsh(), interval.start(), interval.endExclusive(),
                order.getId_wsh(), keeper.getMax_pets_wsh(), true);
        requireKeeperAvailableForKeeper(keeper.getId_wsh(), order);

        PetOrder update = new PetOrder();
        update.setKeeper_id_wsh(keeper.getId_wsh());
        update.setStatus_wsh(OrderStatus.CONFIRMED);
        int updated = orderMapper.update(update, new LambdaUpdateWrapper<PetOrder>()
                .eq(PetOrder::getId_wsh, order.getId_wsh())
                .eq(PetOrder::getStatus_wsh, OrderStatus.PAID)
                .isNull(PetOrder::getKeeper_id_wsh));
        if (updated == 0) {
            return false;
        }
        order.setKeeper_id_wsh(keeper.getId_wsh());
        order.setStatus_wsh(OrderStatus.CONFIRMED);
        // 下单时负责人为空，快照里也就没有看护者；分配成功后补上，避免对账链断裂。
        // 快照是审计辅助数据，写失败不能让「分配」这个主操作回滚。
        try {
            orderSnapshotService.refreshKeeperSnapshot(order.getId_wsh(), keeper);
        } catch (Exception e) {
            log.warn("回填看护者快照失败, 订单={}, keeper={}", order.getId_wsh(), keeper.getId_wsh(), e);
        }
        refreshKeeperCurrentPets(keeper.getId_wsh());
        broadcastOrderChange(order);
        // 已被接走：通知同商家其它看护者把这一单从「待抢单」移除
        orderStatusBroadcaster.broadcastPoolChange(order, "pool-removed");
        return true;
    }

    /**
     * 确认已带负责人的历史订单（兼容路径）。
     * <p>
     * 新下单流程不再指定负责人，订单一律进待分配池；但历史订单（含早期 AI 智能下单）
     * 可能以 paid + keeper 的状态存在，超时扫描仍需能把这批订单正常确认，
     * 否则它们会被当成「无人可分配」而取消退款。
     */
    private boolean confirmPaidOrder(PetOrder order) {
        requireFutureBookingEligibleMerchant(order.getMerchant_id_wsh());
        Keeper keeper = requireFutureBookableKeeper(order.getKeeper_id_wsh());
        validateKeeperQualification(keeper.getId_wsh());
        ensureKeeperCapacityForOrder(order, keeper.getMax_pets_wsh());
        requireKeeperAvailableForOrder(order);

        PetOrder update = new PetOrder();
        update.setStatus_wsh(OrderStatus.CONFIRMED);
        int updated = orderMapper.update(update, new LambdaUpdateWrapper<PetOrder>()
                .eq(PetOrder::getId_wsh, order.getId_wsh())
                .eq(PetOrder::getStatus_wsh, OrderStatus.PAID));
        if (updated == 0) {
            return false;
        }
        order.setStatus_wsh(OrderStatus.CONFIRMED);
        refreshKeeperCurrentPets(order.getKeeper_id_wsh());
        broadcastOrderChange(order);
        return true;
    }

    /**
     * 超时自动分配：按负载比升序、评分降序依次尝试候选人，第一个通过校验的即中标。
     * 全部候选人都不可用时取消订单并全额退款——不允许留下无负责人的有效订单。
     */
    private boolean autoAssignOrCancel(PetOrder order) {
        for (Keeper candidate : rankAssignableKeepers(order)) {
            try {
                if (assignTo(order, candidate)) {
                    log.info("订单[{}]超时自动分配给看护者[{}]",
                            order.getOrder_no_wsh(), candidate.getId_wsh());
                    return true;
                }
            } catch (BusinessException e) {
                log.debug("自动分配候选人不可用, 订单={}, keeper={}, reason={}",
                        order.getOrder_no_wsh(), candidate.getId_wsh(), e.getMessage());
            } catch (Exception e) {
                // 非业务异常（DB/序列化等）也必须继续尝试下一个候选人，
                // 否则异常会冒出循环，订单既没分配也没取消退款，永久停在 paid
                log.warn("自动分配候选人时发生异常, 订单={}, keeper={}",
                        order.getOrder_no_wsh(), candidate.getId_wsh(), e);
            }
        }
        cancelUnassignableOrder(order);
        return false;
    }

    /** 候选看护者排序：负载比升序 → 评分降序 → ID 升序（保证结果稳定）。 */
    private List<Keeper> rankAssignableKeepers(PetOrder order) {
        List<Keeper> keepers = keeperMapper.selectList(new LambdaQueryWrapper<Keeper>()
                .eq(Keeper::getMerchant_id_wsh, order.getMerchant_id_wsh())
                .in(Keeper::getStatus_wsh, List.of(
                        StatusCode.KEEPER_ACTIVE.getValue(),
                        StatusCode.KEEPER_OFFLINE.getValue(),
                        StatusCode.KEEPER_BUSY.getValue())));
        return keepers.stream()
                .sorted(Comparator.comparingDouble(this::keeperLoadRatio)
                        .thenComparing(k -> k.getRating_wsh() == null ? BigDecimal.ZERO : k.getRating_wsh(),
                                Comparator.reverseOrder())
                        .thenComparing(Keeper::getId_wsh))
                .toList();
    }

    /** 负载比 = 当前在住 / 容量上限；容量未知时按 1 处理，避免除零。 */
    private double keeperLoadRatio(Keeper keeper) {
        int max = keeper.getMax_pets_wsh() == null || keeper.getMax_pets_wsh() <= 0
                ? 1 : keeper.getMax_pets_wsh();
        int current = keeper.getCurrent_pets_wsh() == null ? 0 : keeper.getCurrent_pets_wsh();
        return (double) current / max;
    }

    /** 无人可分配：取消订单并全额退款，释放券与会员权益。 */
    private void cancelUnassignableOrder(PetOrder order) {
        PetOrder update = new PetOrder();
        update.setStatus_wsh(OrderStatus.CANCELLED);
        int updated = orderMapper.update(update, new LambdaUpdateWrapper<PetOrder>()
                .eq(PetOrder::getId_wsh, order.getId_wsh())
                .eq(PetOrder::getStatus_wsh, OrderStatus.PAID)
                .isNull(PetOrder::getKeeper_id_wsh));
        if (updated == 0) {
            return;
        }
        order.setStatus_wsh(OrderStatus.CANCELLED);
        refundPaidOrder(order, "unassigned", "订单无人可分配退款");
        couponService.releaseForOrder(order.getId_wsh());
        membershipBenefitService.releaseForOrder(order.getId_wsh());
        broadcastOrderChange(order);
        orderStatusBroadcaster.broadcastPoolChange(order, "pool-removed");
        log.warn("订单超时无人可分配，已取消并退款: {}", order.getOrder_no_wsh());
    }

    /** 已支付订单的全额退款 + 平台补贴回冲（拒单与无人可分配共用）。 */
    private void refundPaidOrder(PetOrder order, String reason, String description) {
        BigDecimal refundAmount = defaultMoney(order.getFinal_amount_wsh());
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            accountingService.transfer(accountingService.systemUserId(), order.getOwner_id_wsh(),
                    refundAmount, reason, order.getId_wsh(),
                    "order", String.valueOf(order.getId_wsh()),
                    reason + ":order:" + order.getId_wsh(),
                    description + " - " + order.getOrder_no_wsh());
        }
        BigDecimal subsidy = defaultMoney(order.getPlatform_subsidy_wsh());
        if (subsidy.compareTo(BigDecimal.ZERO) > 0) {
            accountingService.debit(accountingService.systemUserId(), subsidy, reason + "_subsidy", order.getId_wsh(),
                    "coupon", String.valueOf(order.getCoupon_id_wsh()),
                    reason + ":subsidy:" + order.getId_wsh(),
                    description + "回冲平台补贴 - " + order.getOrder_no_wsh());
        }
    }

    @Transactional
    @Override
    public void rejectOrder(Long userId, String orderNo) {
        PetOrder order = getByOrderNo(orderNo);
        checkMerchantOrKeeperOrderAccess(order, userId);
        if (!OrderStatus.PAID.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "当前订单状态无法拒单");
        }
        PetOrder update = new PetOrder();
        update.setStatus_wsh(OrderStatus.CANCELLED);
        int updated = orderMapper.update(update, new LambdaUpdateWrapper<PetOrder>()
                .eq(PetOrder::getId_wsh, order.getId_wsh())
                .eq(PetOrder::getStatus_wsh, OrderStatus.PAID));
        if (updated == 0) {
            throw new BusinessException(400, "订单状态已变化，无法拒单");
        }
        order.setStatus_wsh(OrderStatus.CANCELLED);

        refundPaidOrder(order, "reject", "商家拒单退款");

        couponService.releaseForOrder(order.getId_wsh());
        membershipBenefitService.releaseForOrder(order.getId_wsh());

        refreshKeeperCurrentPets(order.getKeeper_id_wsh());
        broadcastOrderChange(order);
    }

    /**
     * 【自动接单超时处理（实现）】
     *
     * 业务作用：
     * 在独立事务（REQUIRES_NEW）中处理MQ接单超时消息，查询订单并尝试自动确认。
     *
     * 调用链：
     * OrderService.autoAcceptPaidOrderIfTimeout()
     * ↓
     * [REQUIRES_NEW] → orderMapper查询 → confirmPaidOrder()
     *
     * @param orderNo 订单编号
     * @return 是否自动接单成功
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public boolean autoAcceptPaidOrderIfTimeout(String orderNo) {
        if (isBlank(orderNo)) {
            return false;
        }
        PetOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<PetOrder>().eq(PetOrder::getOrder_no_wsh, orderNo));
        if (order == null || !OrderStatus.PAID.equals(order.getStatus_wsh())) {
            return false;
        }
        // 下单不再指定负责人，新订单一律走自动分配；
        // 历史订单（含早期 AI 下单）可能已带负责人，仍走原「超时自动确认」路径。
        boolean accepted = order.getKeeper_id_wsh() == null
                ? autoAssignOrCancel(order)
                : confirmPaidOrder(order);
        if (accepted) {
            log.info("订单已支付超时后自动接单, 订单编号: {}", order.getOrder_no_wsh());
        }
        return accepted;
    }

    /**
     * 【批量自动接单兜底（实现）】
     *
     * 业务作用：
     * 定时任务兜底，扫描已超时未接单的已支付订单并自动确认。单次上限100条。
     *
     * 调用链：
     * OrderService.autoAcceptPaidOrdersIfTimeout()
     * ↓
     * 查询超时payment记录 → 关联order → 遍历confirmPaidOrder()
     *
     * @return 自动接单数量
     */
    @Transactional
    @Override
    public int autoAcceptPaidOrdersIfTimeout() {
        LocalDateTime cutoff = LocalDateTime.now().minus(ACCEPT_TIMEOUT);
        // 直接扫订单表，而不是「先取最早 100 笔支付再反查订单」：
        // 后者一旦这 100 笔对应的订单离开 paid，查询结果永远是空，第 101 笔之后再也兜不住。
        List<PetOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getStatus_wsh, OrderStatus.PAID)
                        .le(PetOrder::getUpdated_at_wsh, cutoff)
                        .orderByAsc(PetOrder::getUpdated_at_wsh)
                        .last("LIMIT " + ACCEPT_TIMEOUT_SCAN_LIMIT));
        int accepted = 0;
        for (PetOrder order : orders) {
            try {
                boolean assigned = order.getKeeper_id_wsh() == null
                        ? autoAssignOrCancel(order)
                        : confirmPaidOrder(order);
                if (assigned) {
                    accepted++;
                    log.info("兜底扫描器自动接单, 订单编号: {}", order.getOrder_no_wsh());
                }
            } catch (Exception e) {
                log.warn("自动接单兜底扫描器跳过订单: {}", order.getOrder_no_wsh(), e);
            }
        }
        return accepted;
    }

    /**
     * 【取消超时未支付订单（实现）】
     *
     * 业务作用：
     * 在独立事务中处理MQ支付超时消息，取消超时未支付订单。
     *
     * 调用链：
     * OrderService.cancelPendingOrderIfPaymentTimeout()
     * ↓
     * [REQUIRES_NEW] → orderMapper查询 → cancelPaymentTimeoutOrder()
     *
     * @param orderNo 订单编号
     * @return 是否取消成功
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public boolean cancelPendingOrderIfPaymentTimeout(String orderNo) {
        if (isBlank(orderNo)) {
            return false;
        }
        PetOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<PetOrder>().eq(PetOrder::getOrder_no_wsh, orderNo));
        if (order == null) {
            return false;
        }
        return cancelPaymentTimeoutOrder(order, LocalDateTime.now().minus(PAYMENT_TIMEOUT));
    }

    /**
     * 【批量取消超时未支付订单（实现）】
     *
     * 业务作用：
     * 定时任务兜底，扫描超时PENDING订单并取消。单次上限100条。
     *
     * 调用链：
     * OrderService.cancelPaymentTimeoutOrders()
     * ↓
     * 查询超时PENDING订单 → 遍历cancelPaymentTimeoutOrder()
     *
     * @return 取消的订单数量
     */
    @Transactional
    @Override
    public int cancelPaymentTimeoutOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minus(PAYMENT_TIMEOUT);
        List<PetOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getStatus_wsh, OrderStatus.PENDING)
                        .le(PetOrder::getCreated_at_wsh, cutoff)
                        .orderByAsc(PetOrder::getCreated_at_wsh)
                        .last("LIMIT " + PAYMENT_TIMEOUT_SCAN_LIMIT));
        int cancelled = 0;
        for (PetOrder order : orders) {
            if (cancelPaymentTimeoutOrder(order, cutoff)) {
                cancelled++;
            }
        }
        return cancelled;
    }

    /**
     * 【标记已接收（实现-完整参数版本）】
     *
     * 业务作用：
     * 看护者接收宠物，校验交接码与在岗状态后更新为RECEIVED。
     *
     * 业务规则：
     * 不再校验 GPS 距离。寄养场景是主人把宠物送到看护者处，下单地址与实际
     * 交接地点本就可能不同（例如帮他人下单、地址填商家地址），用下单坐标
     * 做基准无法证明"宠物是否交到看护者手上"，因此只保留交接码 + 在岗校验。
     *
     * 调用链：
     * OrderService.markReceived()
     * ↓
     * getByOrderNo() → checkMerchantOrKeeperOrderAccess() → requireKeeperOnDuty()
     * → 校验交接码 → 乐观锁更新RECEIVED → broadcastOrderChange()
     *
     * @param userId  用户ID
     * @param request 接收请求DTO
     */
    @Transactional
    @Override
    public void markReceived(Long userId, OrderReceivedRequestDTO request) {
        if (request == null) {
            throw new BusinessException(400, "接收信息不能为空");
        }
        PetOrder order = getByOrderNo(request.getOrder_no_wsh());
        checkMerchantOrKeeperOrderAccess(order, userId);
        // 订单行锁重读：与「退款申请」并发时只能一方成功（退款要求尚未接收）。
        // 锁顺序：订单行锁 → keeper 容量锁。
        PetOrder locked = orderMapper.selectByIdForUpdate(order.getId_wsh());
        if (locked == null) {
            throw new BusinessException(404, "订单不存在");
        }
        order = locked;
        keeperAttendanceService.requireKeeperOnDuty(order.getKeeper_id_wsh(), order.getMerchant_id_wsh());
        if (!OrderStatus.CONFIRMED.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "当前订单状态无法标记已接收");
        }
        if (isBlank(request.getHandover_code_wsh())) {
            throw new BusinessException(400, "交接码不能为空");
        }
        if (!request.getHandover_code_wsh().trim().equals(order.getHandover_code_wsh())) {
            throw new BusinessException(400, "交接码不正确");
        }
        // 到店复核容量：上一只晚接未完成时仍占名额，不能在 keeper 已经超员的情况下再接收。
        // 容量锁排在订单锁之后，与下单/接走确认保持同一加锁顺序。
        ensureKeeperInStoreCapacityForReceive(order);
        // 距离校验：看护者必须在订单约定的送达地点附近才能确认接收
        BigDecimal receivedDistance = resolveHandoverDistance(order, request);
        LocalDateTime receivedAt = LocalDateTime.now();
        PetOrder update = new PetOrder();
        update.setStatus_wsh(OrderStatus.RECEIVED);
        update.setReceived_at_wsh(receivedAt);
        update.setReceived_address_wsh(request.getReceived_address_wsh());
        update.setReceived_latitude_wsh(request.getReceived_latitude_wsh());
        update.setReceived_longitude_wsh(request.getReceived_longitude_wsh());
        update.setReceived_accuracy_wsh(request.getReceived_accuracy_wsh());
        update.setReceived_distance_m_wsh(receivedDistance);
        int updated = orderMapper.update(update, new LambdaUpdateWrapper<PetOrder>()
                .eq(PetOrder::getId_wsh, order.getId_wsh())
                .eq(PetOrder::getStatus_wsh, OrderStatus.CONFIRMED));
        if (updated == 0) {
            throw new BusinessException(400, "Order status changed, cannot mark received");
        }
        order.setStatus_wsh(OrderStatus.RECEIVED);
        order.setReceived_at_wsh(receivedAt);
        order.setReceived_address_wsh(request.getReceived_address_wsh());
        order.setReceived_latitude_wsh(request.getReceived_latitude_wsh());
        order.setReceived_longitude_wsh(request.getReceived_longitude_wsh());
        order.setReceived_accuracy_wsh(request.getReceived_accuracy_wsh());
        order.setReceived_distance_m_wsh(receivedDistance);
        broadcastOrderChange(order);
    }

    /**
     * 校验交接距离：看护者上报位置与订单约定送达地点的距离必须落在
     * {@code gao.map.handover-radius-meters}（默认 500 米）内。
     *
     * 与旧实现的差别：
     * 1. 旧实现坐标缺失时直接 return null，等于校验不存在；这里改为拒绝，
     *    否则前端不传坐标就能绕过；
     * 2. 旧实现确实也是拿 delivery_* 与 received_* 比，但前端从不上报
     *    received_*，所以长期空转；现在前端会带坐标。
     *
     * 调试开关：request.skip_distance_check_wsh=true 且配置
     * {@code order.handover.allow-debug-skip=true} 时跳过校验，仅用于验证阶段。
     *
     * @return 实际距离（米，保留 1 位小数）；跳过校验时返回 null
     */
    private BigDecimal resolveHandoverDistance(PetOrder order, OrderReceivedRequestDTO request) {
        if (Boolean.TRUE.equals(request.getSkip_distance_check_wsh())) {
            if (allowDebugSkipDistance) {
                log.warn("调试开关已跳过交接距离校验，订单号={}", order.getOrder_no_wsh());
                return null;
            }
            log.warn("请求携带 skip_distance_check_wsh 但后端未开启 allow-debug-skip，按正常流程校验，订单号={}",
                    order.getOrder_no_wsh());
        }
        if (order.getDelivery_latitude_wsh() == null || order.getDelivery_longitude_wsh() == null) {
            throw new BusinessException(400, "订单未记录约定送达坐标，无法校验交接位置，请联系平台处理");
        }
        if (request.getReceived_latitude_wsh() == null || request.getReceived_longitude_wsh() == null) {
            throw new BusinessException(400, "未获取到当前定位，请允许定位后重试");
        }
        if (!GeoDistanceUtils.isValidCoordinate(order.getDelivery_latitude_wsh(), order.getDelivery_longitude_wsh())
                || !GeoDistanceUtils.isValidCoordinate(request.getReceived_latitude_wsh(), request.getReceived_longitude_wsh())) {
            throw new BusinessException(400, "交接位置坐标无效");
        }
        double meters = GeoDistanceUtils.distanceMeters(
                order.getDelivery_latitude_wsh(),
                order.getDelivery_longitude_wsh(),
                request.getReceived_latitude_wsh(),
                request.getReceived_longitude_wsh());
        if (meters > handoverRadiusMeters) {
            throw new BusinessException(400,
                    "当前距离约定送达地点约 " + Math.round(meters) + " 米，超出 " + handoverRadiusMeters + " 米交接范围");
        }
        return BigDecimal.valueOf(meters).setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * 【开始服务（实现）】
     *
     * 业务作用：
     * 看护者/商家在接收宠物后开始照护，记录开始时间和宠物状态照片。
     *
     * 调用链：
     * OrderService.startService()
     * ↓
     * getByOrderNo() → checkMerchantOrKeeperOrderAccess() → requireKeeperOnDuty()
     * → 乐观锁更新IN_PROGRESS + started_at + start_photo → refreshKeeperCurrentPets() → broadcastOrderChange()
     *
     * @param userId     用户ID
     * @param orderNo    订单编号
     * @param startPhoto 开始照片URL
     */
    @Transactional
    @Override
    public void startService(Long userId, String orderNo, String startPhoto) {
        PetOrder order = getByOrderNo(orderNo);
        checkMerchantOrKeeperOrderAccess(order, userId);
        keeperAttendanceService.requireKeeperOnDuty(order.getKeeper_id_wsh(), order.getMerchant_id_wsh());
        if (!OrderStatus.RECEIVED.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "当前订单状态无法开始服务");
        }
        if (isBlank(startPhoto)) {
            throw new BusinessException(400, "开始照片不能为空");
        }
        LocalDateTime startedAt = LocalDateTime.now();
        String normalizedPhoto = startPhoto.trim();
        PetOrder update = new PetOrder();
        update.setStarted_at_wsh(startedAt);
        update.setStart_photo_wsh(normalizedPhoto);
        update.setStatus_wsh(OrderStatus.IN_PROGRESS);
        int updated = orderMapper.update(update, new LambdaUpdateWrapper<PetOrder>()
                .eq(PetOrder::getId_wsh, order.getId_wsh())
                .eq(PetOrder::getStatus_wsh, OrderStatus.RECEIVED));
        if (updated == 0) {
            throw new BusinessException(400, "Order status changed, cannot start service");
        }
        order.setStarted_at_wsh(startedAt);
        order.setStart_photo_wsh(normalizedPhoto);
        order.setStatus_wsh(OrderStatus.IN_PROGRESS);
        refreshKeeperCurrentPets(order.getKeeper_id_wsh());
        broadcastOrderChange(order);
    }

    /**
     * 【预校验开始服务权限（实现）】
     *
     * 业务作用：
     * 只读预校验，不修改数据库。校验逻辑与startService相同。
     *
     * @param userId  用户ID
     * @param orderNo 订单编号
     */
    @Override
    @Transactional(readOnly = true)
    public void validateStartServiceAccess(Long userId, String orderNo) {
        PetOrder order = getByOrderNo(orderNo);
        checkMerchantOrKeeperOrderAccess(order, userId);
        keeperAttendanceService.requireKeeperOnDuty(order.getKeeper_id_wsh(), order.getMerchant_id_wsh());
        if (!OrderStatus.RECEIVED.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "当前订单状态无法开始服务");
        }
    }

    /**
     * 【确认接走（完成订单）】
     *
     * 业务作用：
     * keeper 在宠物实际被接走时输入独立接走码，服务端在此刻固定计费交接时间 T，
     * 按冻结快照计算实际晚接小时数 H 与补费金额：
     * - 0 费：直接完成、结算、释放容量；
     * - 有费：保持「服务中」并创建唯一一笔 night_overtime 补费单，等主人余额支付成功后完成。
     *
     * 幂等：
     * 已确认接走的订单再次提交（网络重试）直接返回原冻结结果，不重新计时、不生成第二笔补费单。
     * 因此「已确认」分支必须在校验在岗/状态/接走码之前返回。
     *
     * 调用链：
     * OrderController.complete → OrderService.completeOrder
     * → 参与权限校验 → [已确认则返回原结果] → 在岗/状态/码校验
     * → 订单行锁重读 → 取一次服务端时间 T → 计算 H/金额 → 落库
     * → 0 费走共用完成逻辑 / 有费建补费单并绑定
     */
    @Transactional
    @Override
    public OrderCheckoutDTO completeOrder(Long userId, OrderCompleteRequestDTO request) {
        if (request == null) {
            throw new BusinessException(400, "接走信息不能为空");
        }
        PetOrder order = getByOrderNo(request.getOrder_no_wsh());
        // 先过参与权限：重复确认也要能返回原结果，不能因为后续状态变化而让网络重试失败
        checkMerchantOrKeeperOrderAccess(order, userId);
        if (order.getCheckout_confirmed_at_wsh() != null) {
            return buildCheckoutResult(order);
        }
        keeperAttendanceService.requireKeeperOnDuty(order.getKeeper_id_wsh(), order.getMerchant_id_wsh());
        if (!OrderStatus.IN_PROGRESS.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "当前订单状态无法完成");
        }
        assertPickupCode(request.getPickup_code_wsh(), order);

        // 订单行锁后重读：并发重复点击只能有一方固定 T 并建补费单
        PetOrder locked = orderMapper.selectByIdForUpdate(order.getId_wsh());
        if (locked == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (locked.getCheckout_confirmed_at_wsh() != null) {
            return buildCheckoutResult(locked);
        }
        if (!OrderStatus.IN_PROGRESS.equals(locked.getStatus_wsh())) {
            throw new BusinessException(400, "当前订单状态无法完成");
        }
        assertPickupCode(request.getPickup_code_wsh(), locked);

        // 取一次服务端时间作为计费交接时刻 T，之后冻结
        LocalDateTime checkoutAt = LocalDateTime.now();
        int overtimeHours = 0;
        BigDecimal overtimeAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (BookingUnit.NIGHT.equals(BookingUnit.normalize(locked.getBilling_unit_wsh()))) {
            // 只读下单时冻结的夜价与锚点，绝不回读当前服务价（商家改价不影响原单）
            NightPricingRule rule = orderSnapshotService.requireNightPricingRule(locked.getId_wsh());
            overtimeHours = OrderPricing.overtimeHours(rule.freeCheckoutUntil(), checkoutAt);
            overtimeAmount = OrderPricing.overtimeAmount(rule.nightPrice(), overtimeHours);
        }

        PetOrder update = new PetOrder();
        update.setCheckout_confirmed_at_wsh(checkoutAt);
        update.setOvertime_hours_wsh(overtimeHours);
        update.setOvertime_amount_wsh(overtimeAmount);
        int updated = orderMapper.update(update, new LambdaUpdateWrapper<PetOrder>()
                .eq(PetOrder::getId_wsh, locked.getId_wsh())
                .eq(PetOrder::getStatus_wsh, OrderStatus.IN_PROGRESS)
                .isNull(PetOrder::getCheckout_confirmed_at_wsh));
        if (updated == 0) {
            // 并发下另一方已完成首次确认：返回对方的冻结结果
            PetOrder latest = orderMapper.selectById(locked.getId_wsh());
            if (latest != null && latest.getCheckout_confirmed_at_wsh() != null) {
                return buildCheckoutResult(latest);
            }
            throw new BusinessException(400, "订单状态已变化，无法确认接走");
        }
        locked.setCheckout_confirmed_at_wsh(checkoutAt);
        locked.setOvertime_hours_wsh(overtimeHours);
        locked.setOvertime_amount_wsh(overtimeAmount);

        if (overtimeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            // 免费接走：直接完成、结算、释放容量
            finishOrder(locked);
            return buildCheckoutResult(locked);
        }

        // 有补费：创建唯一一笔补费单并绑定；订单保持服务中，等主人余额支付
        Payment overtimePayment = createOvertimePayment(locked, overtimeAmount);
        PetOrder bind = new PetOrder();
        bind.setId_wsh(locked.getId_wsh());
        bind.setOvertime_payment_id_wsh(overtimePayment.getId_wsh());
        orderMapper.updateById(bind);
        locked.setOvertime_payment_id_wsh(overtimePayment.getId_wsh());
        broadcastOrderChange(locked);
        OrderCheckoutDTO dto = buildCheckoutResult(locked);
        dto.setOvertime_pay_no_wsh(overtimePayment.getPay_no_wsh());
        dto.setCompleted_wsh(false);
        return dto;
    }

    /**
     * 校验独立接走码：恰好 4 位数字，且必须等于本单接走码（送达码不能用于完成订单）。
     */
    private void assertPickupCode(String submitted, PetOrder order) {
        String code = submitted == null ? null : submitted.trim();
        if (code == null || !code.matches("\\d{4}")) {
            throw new BusinessException(400, "接走码格式不正确，应为 4 位数字");
        }
        if (order.getPickup_code_wsh() == null || !code.equals(order.getPickup_code_wsh())) {
            throw new BusinessException(400, "接走码不正确");
        }
    }

    /**
     * 组装接走结果：已完成返回 completed=true；已确认但仍在等补费返回 false + 补费单号。
     */
    private OrderCheckoutDTO buildCheckoutResult(PetOrder order) {
        OrderCheckoutDTO dto = new OrderCheckoutDTO();
        dto.setCheckout_confirmed_at_wsh(order.getCheckout_confirmed_at_wsh());
        dto.setOvertime_hours_wsh(order.getOvertime_hours_wsh() == null ? 0 : order.getOvertime_hours_wsh());
        dto.setOvertime_amount_wsh(defaultMoney(order.getOvertime_amount_wsh()));
        dto.setCompleted_wsh(OrderStatus.COMPLETED.equals(order.getStatus_wsh()));
        if (order.getOvertime_payment_id_wsh() != null) {
            Payment payment = paymentMapper.selectById(order.getOvertime_payment_id_wsh());
            if (payment != null) {
                dto.setOvertime_pay_no_wsh(payment.getPay_no_wsh());
            }
        }
        return dto;
    }

    /**
     * 创建唯一一笔晚接补费支付单（pending，仅余额）。
     * <p>
     * 补费单不接受前端自定义金额：金额取自订单冻结的 overtime_amount_wsh。
     */
    private Payment createOvertimePayment(PetOrder order, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setOrder_id_wsh(order.getId_wsh());
        payment.setOrder_no_wsh(order.getOrder_no_wsh());
        payment.setPay_no_wsh("PAY" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        payment.setAmount_wsh(amount);
        payment.setMethod_wsh("balance");
        payment.setPurpose_wsh(PaymentPurpose.NIGHT_OVERTIME);
        payment.setStatus_wsh("pending");
        paymentMapper.insert(payment);
        return payment;
    }

    /**
     * 共用完成逻辑：置 COMPLETED、结算、刷新容量、发布完成事件、广播。
     * <p>
     * 0 费接走与补费支付成功两条路径都走这里，保证结算与事件只发生一次。
     */
    private void finishOrder(PetOrder order) {
        LocalDateTime completedAt = LocalDateTime.now();
        PetOrder update = new PetOrder();
        update.setStatus_wsh(OrderStatus.COMPLETED);
        update.setCompleted_at_wsh(completedAt);
        int updated = orderMapper.update(update, new LambdaUpdateWrapper<PetOrder>()
                .eq(PetOrder::getId_wsh, order.getId_wsh())
                .eq(PetOrder::getStatus_wsh, OrderStatus.IN_PROGRESS));
        if (updated == 0) {
            throw new BusinessException(400, "Order status changed, cannot complete order");
        }
        order.setStatus_wsh(OrderStatus.COMPLETED);
        order.setCompleted_at_wsh(completedAt);
        settleOrderToMerchant(order);
        refreshKeeperCurrentPets(order.getKeeper_id_wsh());
        publishCompletedEvent(order);
        broadcastOrderChange(order);
    }

    /**
     * 【晚接补费支付成功回调（供支付模块调用）】
     *
     * 业务作用：
     * 补费支付成功后，把补费金额一次性累计进订单的三个金额字段，并完成订单。
     * 这是 OrderService 提供给 PaymentService 的受控内部方法，不暴露为 HTTP 接口。
     *
     * 幂等：
     * 只有「服务中」且补费单号匹配时才累计；订单已完成直接返回，不会重复累计或重复结算。
     *
     * @param orderId   订单ID
     * @param paymentId 补费支付单ID
     */
    @Transactional
    @Override
    public void applyOvertimePayment(Long orderId, Long paymentId) {
        PetOrder order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!paymentId.equals(order.getOvertime_payment_id_wsh())) {
            throw new BusinessException(400, "补费支付单与订单不匹配");
        }
        if (OrderStatus.COMPLETED.equals(order.getStatus_wsh())) {
            // 已完成：幂等返回，不重复累计
            return;
        }
        if (!OrderStatus.IN_PROGRESS.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "当前订单状态无法结算补费");
        }
        BigDecimal overtime = defaultMoney(order.getOvertime_amount_wsh());
        PetOrder update = new PetOrder();
        update.setId_wsh(order.getId_wsh());
        update.setTotal_amount_wsh(defaultMoney(order.getTotal_amount_wsh()).add(overtime));
        update.setFinal_amount_wsh(defaultMoney(order.getFinal_amount_wsh()).add(overtime));
        update.setSettlement_amount_wsh(defaultMoney(order.getSettlement_amount_wsh()).add(overtime));
        orderMapper.updateById(update);
        order.setTotal_amount_wsh(update.getTotal_amount_wsh());
        order.setFinal_amount_wsh(update.getFinal_amount_wsh());
        order.setSettlement_amount_wsh(update.getSettlement_amount_wsh());
        finishOrder(order);
    }

    /**
     * 【手动更新订单状态（实现）】
     *
     * 业务作用：
     * 通用状态修正接口，带安全防护禁止操作资金相关状态。
     *
     * 调用链：
     * OrderService.updateOrderStatus()
     * ↓
     * 校验状态合法性 → guardManualStatusUpdate() → 预订状态时检查冲突/容量
     * → 乐观锁更新 → 取消时释放券/会员 → 完成时结算/发事件 → refreshKeeperCurrentPets() → broadcastOrderChange()
     *
     * @param orderNo 订单编号
     * @param status  目标状态
     */
    @Transactional
    @Override
    public void updateOrderStatus(String orderNo, String status) {
        if (status == null || !VALID_STATUSES.contains(status)) {
            throw new BusinessException("无效的订单状态: " + status);
        }
        PetOrder order = getByOrderNo(orderNo);
        // 同状态直接返回：避免整实体覆盖写、重复释放权益、重复广播
        if (Objects.equals(order.getStatus_wsh(), status)) {
            return;
        }
        guardManualStatusUpdate(order, status);
        if (BOOKING_STATUSES.contains(status)) {
            ensureNoPetConflictForOrder(order);
            Keeper keeper = keeperMapper.selectById(order.getKeeper_id_wsh());
            ensureKeeperCapacityForOrder(order, keeper != null ? keeper.getMax_pets_wsh() : null);
            requireKeeperAvailableForOrder(order);
        }
        String previousStatus = order.getStatus_wsh();
        // 防御性检查前置：转移表只允许 confirmed→received→in_progress，
        // 资金态与完成态必须走专用接口；走到这里说明转移表被改坏了，先暴露再写库
        if (OrderStatus.CANCELLED.equals(status) || OrderStatus.COMPLETED.equals(status)) {
            throw new IllegalStateException("手动状态接口不应到达资金/完成分支: " + status);
        }
        order.setStatus_wsh(status);
        stampStatusTime(order, status);
        int updated = orderMapper.update(order, new LambdaUpdateWrapper<PetOrder>()
                .eq(PetOrder::getId_wsh, order.getId_wsh())
                .eq(PetOrder::getStatus_wsh, previousStatus));
        if (updated == 0) {
            throw new BusinessException(400, "Order status changed, cannot update manually");
        }
        refreshKeeperCurrentPets(order.getKeeper_id_wsh());
        broadcastOrderChange(order);
    }

    /**
     * 手动状态修正的护栏：只允许沿履约链向前一步。
     *
     * 用显式转移表（{@link OrderStatus#MANUAL_TRANSITIONS}）替代原来的
     * "黑名单资金态 + 白名单履约态"双重判断，原因：
     * 1. 原写法允许 confirmed 直接跳到 in_progress，绕过交接码与开始照片；
     * 2. 原写法不禁止跳步，规则只能靠读代码推断；
     * 3. 转移表是单一事实源，前端可以用同一张表决定下拉可选项。
     *
     * @param order        当前订单
     * @param targetStatus 目标状态
     * @throws BusinessException 目标状态不在当前状态的允许转移内
     */
    private void guardManualStatusUpdate(PetOrder order, String targetStatus) {
        String currentStatus = order.getStatus_wsh();
        Set<String> allowed = OrderStatus.MANUAL_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(targetStatus)) {
            throw new BusinessException(400,
                    "手动接口只允许沿履约链向前一步（confirmed→received→in_progress），当前 "
                            + currentStatus + " 不能改为 " + targetStatus);
        }
    }

    /**
     * Validates that the pet exists and belongs to the specified owner.
     *
     * @param ownerId the pet owner's user ID
     * @param petId   the pet ID to validate
     * @return the validated Pet entity
     * @throws BusinessException if pet not found (404) or not owned by user (403)
     */
    private Pet requirePet(Long ownerId, Long petId) {
        Pet pet = petMapper.selectById(petId);
        if (pet == null) {
            throw new BusinessException(404, "宠物不存在");
        }
        if (!ownerId.equals(pet.getOwner_id_wsh())) {
            throw new BusinessException(403, "无权使用此宠物");
        }
        return pet;
    }

    /**
     * Validates that the keeper is bookable for a future appointment.
     *
     * <p>Employment is the hard gate: ACTIVE, OFFLINE and BUSY are all still
     * employed and schedulable for future dates. OFFLINE/BUSY reflect the current
     * live presence (which may be set by store close), not the ability to serve a
     * future booking. PENDING, REJECTED, RESIGNED and TERMINATED are not bookable.
     *
     * @param keeperId the keeper ID to validate
     * @return the validated Keeper entity
     * @throws BusinessException if keeper not found (404) or not bookable (400)
     */
    private Keeper requireFutureBookableKeeper(Long keeperId) {
        Keeper keeper = keeperMapper.selectById(keeperId);
        if (keeper == null) {
            throw new BusinessException(404, "看护者不存在");
        }
        Integer status = keeper.getStatus_wsh();
        boolean inService = status != null
                && (status == StatusCode.KEEPER_ACTIVE.getValue()
                || status == StatusCode.KEEPER_OFFLINE.getValue()
                || status == StatusCode.KEEPER_BUSY.getValue());
        if (!inService) {
            throw new BusinessException(400, BookingErrorCode.KEEPER_NOT_BOOKABLE, "看护者当前不可接单");
        }
        return keeper;
    }

    /**
     * Validates that the merchant is eligible to accept future bookings.
     * This is the split successor to requireMerchant: it keeps the base approval
     * check but drops the real-time store_status gate, and instead enforces the
     * merchant's future-booking policy flag. Both creating an order and re-checking
     * eligibility during payment confirmation must use this path.
     *
     * @param merchantId the merchant ID to validate
     * @return the validated Merchant entity
     */
    private Merchant requireFutureBookingEligibleMerchant(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(404, "商户不存在");
        }
        if (merchant.getStatus_wsh() == null
                || merchant.getStatus_wsh() != StatusCode.MERCHANT_APPROVED.getValue()) {
            throw new BusinessException(400, BookingErrorCode.MERCHANT_NOT_APPROVED, "商家未通过审核");
        }
        if (merchant.getFuture_booking_enabled_wsh() == null
                || merchant.getFuture_booking_enabled_wsh() != 1) {
            throw new BusinessException(400, BookingErrorCode.FUTURE_BOOKING_DISABLED, "商家未开放未来预约");
        }
        return merchant;
    }

    /**
     * Validates that the keeper belongs to the given merchant's organization.
     *
     * @param keeper   the keeper entity
     * @param merchant the merchant entity
     * @throws BusinessException if the keeper is not affiliated with the merchant
     */
    private void validateKeeperMerchant(Keeper keeper, Merchant merchant) {
        if (keeper.getMerchant_id_wsh() == null || !keeper.getMerchant_id_wsh().equals(merchant.getId_wsh())) {
            throw new BusinessException(400, "看护者不属于所选商户");
        }
    }

    /**
     * Validates that the keeper has at least one approved qualification.
     *
     * @param keeperId the keeper ID
     * @throws BusinessException if no approved qualification exists
     */
    private void validateKeeperQualification(Long keeperId) {
        List<com.pet.qualification.dto.QualificationDTO> quals = qualificationService.listByOwner(
                QualificationService.OWNER_TYPE_KEEPER, keeperId, false);
        boolean hasApproved = quals.stream().anyMatch(
                q -> QualificationService.STATUS_APPROVED.equals(q.getStatus_wsh()));
        if (!hasApproved) {
            throw new BusinessException(400, "该看护者资质尚未通过审核，无法接单");
        }
    }

    /**
     * 校验服务计费单位并返回规范化单位。day/session/hour 及其中文别名均受支持；
     * unit 为空的历史服务按 day 语义兼容；未知的非空单位拒绝（UNSUPPORTED_SERVICE_UNIT）。
     *
     * @param service 服务项
     * @return 规范化计费单位（night/session/hour）
     * @throws BusinessException 未知单位时抛出 UNSUPPORTED_SERVICE_UNIT
     */
    static String assertServiceUnitSupported(ServiceItem service) {
        if (service == null || service.getUnit_wsh() == null || service.getUnit_wsh().isBlank()) {
            // 迁移后唯一的长区间单位是按夜；空白单位按夜兜底，避免历史服务完全不可下单
            return BookingUnit.NIGHT;
        }
        String unit = BookingUnit.normalize(service.getUnit_wsh());
        if (unit == null) {
            throw new BusinessException(400, BookingErrorCode.UNSUPPORTED_SERVICE_UNIT, "服务计费单位不受支持");
        }
        return unit;
    }

    /**
     * 校验客户端提交的计费单位（可选）与服务端派生单位一致；空值跳过校验（旧客户端兼容）。
     *
     * @param clientUnit  客户端提交的单位（可空）
     * @param serverUnit  服务端根据服务派生的规范化单位
     * @throws BusinessException 客户端单位与服务单位不一致时抛出 UNIT_MISMATCH
     */
    static void assertBillingUnitCompatible(String clientUnit, String serverUnit) {
        if (clientUnit == null || clientUnit.isBlank()) {
            return;
        }
        String normalized = BookingUnit.normalize(clientUnit);
        if (normalized == null || !normalized.equals(serverUnit)) {
            throw new BusinessException(400, BookingErrorCode.UNIT_MISMATCH,
                    "提交的计费单位与服务不一致，服务端以服务记录为准");
        }
    }

    /**
     * 校验客户端期望单价（可选）与服务当前单价一致；不一致返回 PRICE_CHANGED。
     *
     * @param service          服务项
     * @param expectedUnitPrice 客户端携带的期望单价（可空）
     * @throws BusinessException 单价不一致或服务无价格时抛出 PRICE_CHANGED
     */
    static void assertExpectedUnitPrice(ServiceItem service, BigDecimal expectedUnitPrice) {
        if (expectedUnitPrice == null) {
            return;
        }
        BigDecimal actual = service == null ? null : service.getPrice_wsh();
        if (actual == null || actual.compareTo(expectedUnitPrice) != 0) {
            throw new BusinessException(400, BookingErrorCode.PRICE_CHANGED,
                    "服务价格已变化，请刷新后重新确认");
        }
    }

    /**
     * 校验客户端携带的服务版本与当前服务版本一致。
     * <p>
     * 版本不一致说明服务价格/内容已变化，强制前端刷新后重新确认，
     * 防止用户按旧价格下单（PRICE_CHANGED）。客户端未携带版本时跳过校验，兼容旧客户端。
     *
     * @param service       服务项
     * @param clientVersion 客户端携带的服务版本（可空）
     * @throws BusinessException 版本不一致时抛出 PRICE_CHANGED
     */
    static void assertServiceVersionMatches(ServiceItem service, String clientVersion) {
        if (clientVersion == null || clientVersion.isBlank()) {
            return;
        }
        String current = ServiceVersions.format(service.getUpdated_at_wsh());
        if (current == null || !current.equals(clientVersion)) {
            throw new BusinessException(400, BookingErrorCode.PRICE_CHANGED, "服务价格或版本已变化，请刷新后重新确认");
        }
    }

    /**
     * 解析订单单价：指定服务时以服务单价为准（服务驱动计价），
     * 未指定服务时沿用看护者单价（兼容旧客户端）。
     *
     * @param service 服务项（可空）
     * @param keeper  看护者
     * @return 每日单价
     */
    /**
     * 单价唯一来源：服务单价。
     * <p>
     * 看护者个人定价已下线（价格由商家按服务设置），这里不再保留任何按人回退的分支——
     * 一旦服务没有价格，说明数据异常，直接拒绝下单，而不是静默用某个人的价顶上。
     */
    static BigDecimal resolveUnitPrice(ServiceItem service) {
        if (service == null || service.getPrice_wsh() == null) {
            throw new BusinessException(400, BookingErrorCode.PRICE_CHANGED, "服务价格缺失，无法下单");
        }
        return service.getPrice_wsh();
    }

    /**
     * 【多单位基础金额（静态辅助）】
     *
     * 业务作用：
     * 计费基数为服务端计算出的 quantity * unitPrice，作为折扣/优惠券/会员/支付/
     * 结算的输入基数，客户端无法伪造。
     *
     * @param unitPrice 单单位单价
     * @param quantity  计费数量
     * @return 基础金额
     */
    static BigDecimal resolveBaseAmount(BigDecimal unitPrice, int quantity) {
        BigDecimal price = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * 【折扣适用天数（静态辅助）】
     *
     * 业务作用：
     * 按夜订单按预订夜数（quantity）享受长住折扣；session/hour 单次服务
     * 不因小时数放大折扣档位，固定按 1 天判定，避免虚构多日优惠。
     *
     * @param unit     计费单位
     * @param quantity 计费数量
     * @return 折扣判定天数
     */
    static int resolveDiscountDays(String unit, int quantity) {
        return BookingUnit.NIGHT.equals(BookingUnit.normalize(unit)) ? quantity : 1;
    }

    /**
     * 加载可下单服务（产品预订必填）。
     * <p>
     * 商家与价格均由服务端根据服务派生：商家 = service.merchant_id_wsh，
     * 每日单价 = service.price_wsh，客户端提交的 merchantId 不再参与选择。
     *
     * @param serviceId 服务产品ID
     * @return 校验通过的服务实体
     * @throws BusinessException serviceId 为空或服务不存在 SERVICE_NOT_FOUND、
     *                           服务未上架 SERVICE_OFF_SHELF
     */
    private ServiceItem loadBookableService(Long serviceId) {
        if (serviceId == null) {
            throw new BusinessException(400, BookingErrorCode.SERVICE_NOT_FOUND, "产品预订必须指定服务");
        }
        ServiceItem service = serviceItemMapper.selectById(serviceId);
        if (service == null) {
            throw new BusinessException(400, BookingErrorCode.SERVICE_NOT_FOUND, "服务不存在");
        }
        if (service.getStatus_wsh() == null || service.getStatus_wsh() != StatusCode.SERVICE_ENABLED.getValue()) {
            throw new BusinessException(400, BookingErrorCode.SERVICE_OFF_SHELF, "服务未上架");
        }
        return service;
    }

    /**
     * 兼容窗口内的商家字段校验：允许旧客户端继续携带 merchant_id_wsh，
     * 但与服务端基于服务派生的商家不一致时拒绝（SERVICE_MERCHANT_MISMATCH）。
     * 该字段永不参与商家选择。
     *
     * @param clientMerchantId 旧客户端提交的商家ID（可空）
     * @param derivedMerchantId 服务端根据服务派生的商家ID
     * @throws BusinessException 客户端值与派生值不一致时抛出 SERVICE_MERCHANT_MISMATCH
     */
    static void assertMerchantPayloadCompatible(Long clientMerchantId, Long derivedMerchantId) {
        if (clientMerchantId != null && !clientMerchantId.equals(derivedMerchantId)) {
            throw new BusinessException(400, BookingErrorCode.SERVICE_MERCHANT_MISMATCH,
                    "提交的商家与服务归属不一致，商家由服务端根据服务派生");
        }
    }

    /**
     * 长住折扣：按「折扣判定天数」计算金额。
     * <ul>
     *   <li>30 天及以上：10%</li>
     *   <li>7..29 天：5%</li>
     *   <li>不足 7 天：不打折</li>
     * </ul>
     * 按夜订单的判定天数 = 预订夜数 N（提前加收的一夜不参与档位）。
     *
     * @param totalAmount 折扣前金额
     * @param days        折扣判定天数
     * @return 折扣金额，不满足条件返回 0
     */
    BigDecimal calculateDiscount(BigDecimal totalAmount, int days) {
        if (days >= 30) {
            return totalAmount.multiply(BigDecimal.valueOf(0.1));
        }
        if (days >= 7) {
            return totalAmount.multiply(BigDecimal.valueOf(0.05));
        }
        return BigDecimal.ZERO;
    }

    /**
     * session/hour 模式必须提交开始槽位（来自可用性接口）。
     */
    private LocalDateTime requireSlotStart(OrderCreateRequestDTO request) {
        LocalDateTime slotStart = request.getStart_time_wsh();
        if (slotStart == null) {
            throw new BusinessException(400, "session/hour 服务必须选择开始槽位(startTime)");
        }
        return slotStart;
    }

    /**
     * 解析计费数量：session 固定为 1；hour 为连续整小时数（默认 1，总时长不跨天）。
     * day 模式不经过此方法。
     */
    private int resolveSlotQuantity(String unit, OrderCreateRequestDTO request, int durationMinutes) {
        if (BookingUnit.SESSION.equals(unit)) {
            if (request.getQuantity_wsh() != null && request.getQuantity_wsh() != 1) {
                throw new BusinessException(400, BookingErrorCode.QUANTITY_INVALID,
                        "按次服务每次订单数量固定为1，需要多次请分别下单");
            }
            return 1;
        }
        if (BookingUnit.HOUR.equals(unit)) {
            int qty = request.getQuantity_wsh() == null ? 1 : request.getQuantity_wsh();
            if (qty < 1) {
                throw new BusinessException(400, BookingErrorCode.QUANTITY_INVALID, "按小时服务数量必须大于0");
            }
            if ((long) qty * durationMinutes > BookingUnit.MAX_DURATION_MINUTES) {
                throw new BusinessException(400, BookingErrorCode.QUANTITY_INVALID,
                        "按小时服务单笔订单总时长不能超过24小时");
            }
            return qty;
        }
        throw new BusinessException(400, BookingErrorCode.UNSUPPORTED_SERVICE_UNIT, "不支持的计费单位");
    }

    /**
     * 客户端提交的结束时间仅做一致性校验，权威结束时间由服务端按开始+时长计算。
     */
    private void assertClientEndTimeConsistent(LocalDateTime clientEndTime, LocalDateTime computedEnd) {
        if (clientEndTime != null && !clientEndTime.equals(computedEnd)) {
            throw new BusinessException(400, BookingErrorCode.SLOT_UNAVAILABLE,
                    "客户端提交的结束时间与服务端计算不一致，请刷新后重试");
        }
    }

    /**
     * 完整服务时段必须落在营业窗口内（可能跨午夜，逐日切片校验）。
     * 无营业时间配置（legacy_unrestricted）时不做限制。
     */
    private void serviceWindowCheck(Long merchantId, LocalDateTime start, LocalDateTime end) {
        List<BusinessHours> hours = businessHoursService.getByMerchantId(merchantId);
        if (hours == null || hours.isEmpty()) {
            return;
        }
        LocalDate date = start.toLocalDate();
        LocalDate lastDate = end.minusNanos(1).toLocalDate();
        while (!date.isAfter(lastDate)) {
            LocalDateTime sliceStart = start.isAfter(date.atStartOfDay()) ? start : date.atStartOfDay();
            LocalDateTime sliceEnd = end.isBefore(date.plusDays(1).atStartOfDay())
                    ? end : date.plusDays(1).atStartOfDay();
            List<BusinessHoursTargetResolver.BusinessWindow> windows =
                    businessHoursTargetResolver.windowsForDate(hours, date);
            boolean covered = false;
            for (BusinessHoursTargetResolver.BusinessWindow w : windows) {
                if (!sliceStart.isBefore(w.start()) && !sliceEnd.isAfter(w.end())) {
                    covered = true;
                    break;
                }
            }
            if (!covered) {
                throw new BusinessException(400, BookingErrorCode.SLOT_UNAVAILABLE,
                        "所选时段不完整落在营业窗口内，请重新选择");
            }
            date = date.plusDays(1);
        }
    }

    /**
     * 宠物区间冲突：候选订单按「日期窗口 或 已在店未完成」加载后再做精确重叠判定。
     * <p>
     * 占用区间统一由 {@link OrderOccupancy} 计算（night 含 30 分钟免费宽限，
     * 已接收/服务中的逾期单视为终点开放），不在本方法内另写一套解释。
     */
    private void ensureNoPetIntervalConflict(Long petId, LocalDateTime start, LocalDateTime end, Long excludeOrderId) {
        List<PetOrder> candidates = loadOccupancyCandidates(petId, null, start, end, excludeOrderId);
        OrderOccupancy.Interval candidate = new OrderOccupancy.Interval(start, end);
        for (PetOrder existing : candidates) {
            if (OrderOccupancy.overlaps(OrderOccupancy.intervalOf(existing), candidate)) {
                throw new BusinessException(400, BookingErrorCode.PET_BOOKING_CONFLICT,
                        "宠物存在冲突订单: " + existing.getOrder_no_wsh());
            }
        }
    }

    /**
     * 加载占用判定候选订单。
     * <p>
     * SQL 不能先用旧 end_date 把「跨日仍在店」的订单过滤掉，否则容量会被低估：
     * 候选集 = (日期窗口相交的预约单) ∪ (已接收/服务中且未完成的在店单)。
     * 超过上限时明确失败，不允许静默截断。
     */
    private List<PetOrder> loadOccupancyCandidates(Long petId, Long keeperId,
                                                   LocalDateTime start, LocalDateTime end,
                                                   Long excludeOrderId) {
        LocalDate startDate = start.toLocalDate();
        LocalDate endDate = end.minusNanos(1).toLocalDate().plusDays(1);
        LambdaQueryWrapper<PetOrder> wrapper = new LambdaQueryWrapper<>();
        if (petId != null) {
            wrapper.eq(PetOrder::getPet_id_wsh, petId);
        }
        if (keeperId != null) {
            wrapper.eq(PetOrder::getKeeper_id_wsh, keeperId);
        }
        wrapper.in(PetOrder::getStatus_wsh, BOOKING_STATUSES)
                .and(w -> w
                        .and(x -> x.lt(PetOrder::getStart_date_wsh, endDate)
                                .ge(PetOrder::getEnd_date_wsh, startDate))
                        .or()
                        .in(PetOrder::getStatus_wsh, CURRENT_PET_STATUSES));
        if (excludeOrderId != null) {
            wrapper.ne(PetOrder::getId_wsh, excludeOrderId);
        }
        wrapper.last("LIMIT " + (OCCUPANCY_CANDIDATE_LIMIT + 1));
        List<PetOrder> candidates = orderMapper.selectList(wrapper);
        if (candidates.size() > OCCUPANCY_CANDIDATE_LIMIT) {
            throw new BusinessException(400, "订单占用数据过多，无法校验排班，请稍后重试");
        }
        return candidates;
    }

    /**
     * 看护员区间容量：候选订单按「日期窗口 或 已在店未完成」加载后，
     * 用事件扫描求「任一时刻同时在养数」的峰值，峰值超过上限才拒绝。
     *
     * @param lockRow 是否对看护者行加锁。下单必须为 true（与插入串行化，防超卖）；
     *                价格预览必须为 false —— 预览是只读事务，MySQL 的 READ ONLY 事务里
     *                执行 SELECT ... FOR UPDATE 会直接报 1792，而且即便不报错，
     *                预览每次改日期都抢一把行锁会阻塞真实下单。
     */
    private void ensureKeeperIntervalCapacity(Long keeperId, LocalDateTime start, LocalDateTime end,
                                              Long excludeOrderId, Integer maxPets, boolean lockRow) {
        int capacity = maxPets == null ? 0 : maxPets;
        if (capacity <= 0) {
            throw new BusinessException(400, "看护者容量不足");
        }
        if (lockRow) {
            keeperMapper.selectByIdForUpdate(keeperId);
        }
        List<PetOrder> candidates = loadOccupancyCandidates(null, keeperId, start, end, excludeOrderId);
        List<OrderOccupancy.Interval> existing = new ArrayList<>(candidates.size());
        for (PetOrder order : candidates) {
            existing.add(OrderOccupancy.intervalOf(order));
        }
        int peak = OrderOccupancy.peakConcurrency(existing, new OrderOccupancy.Interval(start, end));
        if (peak > capacity) {
            throw new BusinessException(400, BookingErrorCode.CAPACITY_EXCEEDED,
                    "看护者排班与已有订单冲突");
        }
    }

    /**
     * 解析按夜寄养的预订夜数 N：必须为正整数且不超过最大夜数上限。
     * <p>
     * N 是「预订事实」，与提前送达加收的一夜、实际晚接小时数都是不同含义。
     */
    private int resolveNightCount(OrderCreateRequestDTO request) {
        Integer nights = request.getQuantity_wsh();
        if (nights == null || nights < 1) {
            throw new BusinessException(400, BookingErrorCode.QUANTITY_INVALID, "预订夜数必须为正整数");
        }
        if (nights > maxOrderDays) {
            throw new BusinessException(400, "预订夜数不能超过" + maxOrderDays + "夜");
        }
        return nights;
    }

    /** 取两个时刻中较晚的一个（任一为 null 时取另一个）。 */
    private static LocalDateTime maxInstant(LocalDateTime a, LocalDateTime b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }

    /**
     * 接收宠物前在 keeper 行锁保护下复核「当前在养数」。
     * <p>
     * 预约容量在下单时已经校验过，但实际入店是另一时刻：上一只晚接未完成时仍占名额，
     * 必须在这里再复核一次，否则会出现实际在养超员（AE16）。
     */
    private void ensureKeeperInStoreCapacityForReceive(PetOrder order) {
        Long keeperId = order.getKeeper_id_wsh();
        if (keeperId == null) {
            return;
        }
        Keeper keeper = keeperMapper.selectByIdForUpdate(keeperId);
        if (keeper == null) {
            return;
        }
        Integer maxPets = keeper.getMax_pets_wsh();
        if (maxPets == null || maxPets <= 0) {
            return;
        }
        Long inStore = orderMapper.selectCount(new LambdaQueryWrapper<PetOrder>()
                .eq(PetOrder::getKeeper_id_wsh, keeperId)
                .in(PetOrder::getStatus_wsh, CURRENT_PET_STATUSES)
                .ne(PetOrder::getId_wsh, order.getId_wsh()));
        if (inStore != null && inStore >= maxPets) {
            throw new BusinessException(400, BookingErrorCode.CAPACITY_EXCEEDED,
                    "看护者当前在养宠物已达上限，无法接收");
        }
    }

    /**
     * 寄养首尾交接时刻的营业时间校验：两个交接时刻都必须落在营业窗口内。
     * <p>
     * 交接是人对人完成的，闭店时无人交接；寄养期间的过夜不受限——
     * 受限的只是「送来」和「接走」这两个时刻。
     */
    private void validateBoardingBusinessHours(Long merchantId, LocalDateTime careStartAt, LocalDateTime careEndAt) {
        List<BusinessHours> hours = businessHoursService.getByMerchantId(merchantId);
        if (hours == null || hours.isEmpty()) {
            return;
        }
        if (!businessHoursTargetResolver.isWithinBusinessHours(hours, careStartAt)) {
            throw new BusinessException(400, BookingErrorCode.FULFILLMENT_OUTSIDE_BUSINESS_HOURS,
                    "寄养开始时间不在营业时段内");
        }
        if (!businessHoursTargetResolver.isWithinBusinessHours(hours, careEndAt)) {
            throw new BusinessException(400, BookingErrorCode.FULFILLMENT_OUTSIDE_BUSINESS_HOURS,
                    "寄养结束时间不在营业时段内");
        }
    }

    /**
     * 休假校验：KeeperLeaveService 的契约是右开区间 [start, endExclusive)。
     * <p>
     * 只有 session/hour 的 end_date 是排他边界（= start+1）；
     * boarding 与 day 的 end_date 是最后一个服务日（闭区间），需要 +1 天。
     */
    private void requireKeeperAvailableForOrder(PetOrder order) {
        requireKeeperAvailableForKeeper(order.getKeeper_id_wsh(), order);
    }

    /** 指定看护者的休假校验（分配环节订单上还没有 keeper_id，需要显式传入候选人）。 */
    private void requireKeeperAvailableForKeeper(Long keeperId, PetOrder order) {
        LocalDate endExclusive = leaveEndExclusive(order);
        if (keeperId == null || order.getStart_date_wsh() == null || endExclusive == null) {
            return;
        }
        keeperLeaveService.requireKeeperAvailable(keeperId, order.getStart_date_wsh(), endExclusive);
    }

    /** 休假校验所需的右开端点；日期缺失时返回 null（与 hasLeaveOverlap 的宽松语义一致）。 */
    private static LocalDate leaveEndExclusive(PetOrder order) {
        if (order.getEnd_date_wsh() == null) {
            return null;
        }
        String mode = BookingUnit.bookingMode(BookingUnit.normalize(order.getBilling_unit_wsh()));
        boolean slotUnit = BookingUnit.MODE_SLOT.equals(mode);
        return slotUnit ? order.getEnd_date_wsh() : order.getEnd_date_wsh().plusDays(1);
    }

    /**
     * 按订单自身的占用区间做宠物冲突校验。
     * <p>
     * 占用区间由 {@link OrderOccupancy} 统一推导（night 含免费宽限、在店逾期视为终点开放）。
     */
    private void ensureNoPetConflictForOrder(PetOrder order) {
        OrderOccupancy.Interval interval = OrderOccupancy.intervalOf(order);
        if (interval == null) {
            return;
        }
        ensureNoPetIntervalConflict(order.getPet_id_wsh(), interval.start(), interval.endExclusive(),
                order.getId_wsh());
    }

    /**
     * 按订单自身的占用区间做容量校验。
     */
    private void ensureKeeperCapacityForOrder(PetOrder order, Integer maxPets) {
        OrderOccupancy.Interval interval = OrderOccupancy.intervalOf(order);
        if (interval == null) {
            return;
        }
        ensureKeeperIntervalCapacity(order.getKeeper_id_wsh(), interval.start(), interval.endExclusive(),
                order.getId_wsh(), maxPets, true);
    }

    /**
     * 刷新keeper的当前宠物数
     */
    private void refreshKeeperCurrentPets(Long keeperId) {
        if (keeperId == null) {
            return;
        }
        Keeper keeper = keeperMapper.selectById(keeperId);
        if (keeper == null) {
            return;
        }
        // 当前在养 = 已接收/服务中且未完成，不受预约 end_date 过期影响：
        // 逾期未完成的订单仍占着名额，不能因为日期过了就不算。
        Long current = orderMapper.selectCount(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getKeeper_id_wsh, keeperId)
                        .in(PetOrder::getStatus_wsh, CURRENT_PET_STATUSES));
        keeper.setCurrent_pets_wsh(current == null ? 0 : current.intValue());
        Merchant merchant = merchantMapper.selectById(keeper.getMerchant_id_wsh());
        boolean merchantOpen = merchant != null
                && merchant.getStatus_wsh() != null
                && merchant.getStatus_wsh() == StatusCode.MERCHANT_APPROVED.getValue()
                && merchant.getStore_status_wsh() != null
                && merchant.getStore_status_wsh() == 1;
        // 看护员主动离线（offline_source_wsh == 1）不被容量刷新覆盖回在线
        boolean manualOffline = keeper.getOffline_source_wsh() != null
                && keeper.getOffline_source_wsh() == KeeperService.OFFLINE_SOURCE_MANUAL;
        if (keeper.getMax_pets_wsh() != null && keeper.getMax_pets_wsh() > 0) {
            if (current >= keeper.getMax_pets_wsh()) {
                if (keeper.getStatus_wsh() != StatusCode.KEEPER_BUSY.getValue()) {
                    keeper.setStatus_wsh(StatusCode.KEEPER_BUSY.getValue());
                }
            } else if (merchantOpen && !manualOffline) {
                if (keeper.getStatus_wsh() != StatusCode.KEEPER_ACTIVE.getValue()) {
                    keeper.setStatus_wsh(StatusCode.KEEPER_ACTIVE.getValue());
                    keeper.setOffline_source_wsh(KeeperService.OFFLINE_SOURCE_SYSTEM);
                }
            } else if (!merchantOpen && keeper.getStatus_wsh() != StatusCode.KEEPER_OFFLINE.getValue()) {
                keeper.setStatus_wsh(StatusCode.KEEPER_OFFLINE.getValue());
                keeper.setOffline_source_wsh(KeeperService.OFFLINE_SOURCE_SYSTEM);
            }
        } else if (merchantOpen && !manualOffline) {
            if (keeper.getStatus_wsh() != StatusCode.KEEPER_ACTIVE.getValue()) {
                keeper.setStatus_wsh(StatusCode.KEEPER_ACTIVE.getValue());
                keeper.setOffline_source_wsh(KeeperService.OFFLINE_SOURCE_SYSTEM);
            }
        } else if (!merchantOpen && keeper.getStatus_wsh() != StatusCode.KEEPER_OFFLINE.getValue()) {
            keeper.setStatus_wsh(StatusCode.KEEPER_OFFLINE.getValue());
            keeper.setOffline_source_wsh(KeeperService.OFFLINE_SOURCE_SYSTEM);
        }
        keeperMapper.updateById(keeper);
    }


    /**
     * Verifies that the given user is either the assigned keeper (by user mapping)
     * or the merchant owner of this order. Throws a 403 exception if neither matches.
     *
     * @param order  the order to check access against
     * @param userId the user ID to verify
     * @throws BusinessException if the user has no permission (403)
     */
    private void checkMerchantOrKeeperOrderAccess(PetOrder order, Long userId) {
        if (order == null || userId == null) {
            throw new BusinessException(403, "无权限操作此订单");
        }
        if (order.getKeeper_id_wsh() != null) {
            List<Keeper> keepers = keeperMapper.selectList(
                    new LambdaQueryWrapper<Keeper>().eq(Keeper::getUser_id_wsh, userId));
            if (keepers.stream().anyMatch(k -> k.getId_wsh().equals(order.getKeeper_id_wsh()))) {
                return;
            }
        }
        if (order.getMerchant_id_wsh() != null) {
            Merchant merchant = merchantMapper.selectOne(
                    new LambdaQueryWrapper<Merchant>().eq(Merchant::getUser_id_wsh, userId).last("LIMIT 1"));
            if (merchant != null && merchant.getId_wsh().equals(order.getMerchant_id_wsh())) {
                return;
            }
        }
        throw new BusinessException(403, "无权限操作此订单");
    }

    /**
     * Strict access check that ensures the user is <em>exactly</em> the keeper
     * assigned to this order. Used for operations that only the designated keeper
     * can perform (e.g., acceptOrder).
     *
     * @param order  the order to check
     * @param keeper the keeper entity (must match the order's keeper_id)
     * @param userId the user ID trying to act
     * @throws BusinessException if the user is not the assigned keeper (403)
     */
    private void requireAssignedKeeperOrderAccess(PetOrder order, Keeper keeper, Long userId) {
        if (order == null || keeper == null || userId == null
                || order.getKeeper_id_wsh() == null
                || !order.getKeeper_id_wsh().equals(keeper.getId_wsh())
                || keeper.getUser_id_wsh() == null
                || !keeper.getUser_id_wsh().equals(userId)) {
            throw new BusinessException(403, "Only the assigned keeper can accept this order");
        }
    }

    /**
     * Transfers the settlement amount from the system account to the merchant's
     * account upon order completion. The settlement amount is determined by
     * {@link #settlementAmount}. Throws if the merchant or its user ID cannot be resolved.
     *
     * @param order the completed order to settle
     * @throws BusinessException if the merchant or merchant user ID is not found,
     *                           or the settlement amount is not positive
     */
    private void settleOrderToMerchant(PetOrder order) {
        Merchant merchant = merchantMapper.selectById(order.getMerchant_id_wsh());
        if (merchant == null || merchant.getUser_id_wsh() == null) {
            throw new BusinessException(400, "无法确定订单结算商家");
        }
        accountingService.transfer(accountingService.systemUserId(), merchant.getUser_id_wsh(),
                settlementAmount(order), "settlement", order.getId_wsh(),
                "order", String.valueOf(order.getId_wsh()),
                "settlement:order:" + order.getId_wsh(),
                "订单完成结算 - " + order.getOrder_no_wsh());
    }

    /**
     * Returns the settlement amount for an order, defaulting to the settlement_amount_wsh
     * field, or falling back to final_amount_wsh. The amount must be positive.
     *
     * @param order the order to compute settlement for
     * @return the positive settlement amount
     * @throws BusinessException if the resolved amount is zero or negative
     */
    private BigDecimal settlementAmount(PetOrder order) {
        BigDecimal amount = order.getSettlement_amount_wsh();
        if (amount == null) {
            amount = order.getFinal_amount_wsh();
        }
        amount = defaultMoney(amount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "订单结算金额必须大于0");
        }
        return amount;
    }

    /**
     * 【金额空值兜底】
     *
     * 业务作用：把 null 金额视为 0，避免后续运算空指针。
     *
     * 调用场景：下单金额汇总、取消订单退款金额、结算金额解析等处。
     *
     * 调用链：createOrder() / cancelOrder() / settlementAmount() → defaultMoney()。
     *
     * 数据处理：null 返回 BigDecimal.ZERO，否则原值返回。
     *
     * 业务规则：不改变金额精度。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：与 money() 不同，这里不做 setScale，两位小数由调用方自行保证。
     */
    private BigDecimal defaultMoney(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    /**
     * 【生成促销快照JSON】
     *
     * 业务作用：把优惠券与会员折扣的明细快照合并成一个促销快照字符串。
     *
     * 调用场景：createOrder 写入 order.promotion_snapshot_wsh。
     *
     * 调用链：createOrder() → toPromotionSnapshot() → objectMapper.writeValueAsString()。
     *
     * 数据处理：写入 coupon_wsh（券快照字符串）、membership_wsh（会员折扣快照字符串）、membership_discount_wsh（会员折扣额）。
     *
     * 业务规则：入参为 null 时对应字段写 null，会员折扣额缺省为 0。
     *
     * 状态影响：无，仅生成字符串。
     *
     * 异常情况：序列化异常时回退返回券快照字符串，券也为 null 则返回 "{}"。
     *
     * 注意事项：异常回退路径返回的是券快照本身而非合并结构，读取方需容忍结构差异。
     */
    private String toPromotionSnapshot(CouponDiscountResult couponDiscount, MembershipDiscountDTO membershipDiscount) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("coupon_wsh", couponDiscount == null ? null : couponDiscount.getSnapshot_wsh());
        data.put("membership_wsh", membershipDiscount == null ? null : membershipDiscount.getSnapshot_wsh());
        data.put("membership_discount_wsh", membershipDiscount == null
                ? BigDecimal.ZERO
                : defaultMoney(membershipDiscount.getMembership_discount_wsh()));
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return couponDiscount == null ? "{}" : couponDiscount.getSnapshot_wsh();
        }
    }

    /**
     * Batch-enriches a list of PetOrder entities into enriched OrderDTOs.
     * Loads all associated entities (service, user, pet, keeper, merchant) and
     * order snapshots in bulk queries, then assembles the DTOs in memory to
     * avoid N+1 SQL problems.
     *
     * @param orders the list of PetOrder entities to enrich
     * @return the list of enriched OrderDTOs, or an empty list if input is null/empty
     */
    private List<OrderDTO> toDTOEnrichedList(List<PetOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }
        Map<Long, ServiceItem> serviceMap = loadServiceMap(collectIds(orders, PetOrder::getService_id_wsh));
        Map<Long, User> userMap = loadUserMap(collectIds(orders, PetOrder::getOwner_id_wsh));
        Map<Long, Pet> petMap = loadPetMap(collectIds(orders, PetOrder::getPet_id_wsh));
        Map<Long, Keeper> keeperMap = loadKeeperMap(collectIds(orders, PetOrder::getKeeper_id_wsh));
        Map<Long, Merchant> merchantMap = loadMerchantMap(collectIds(orders, PetOrder::getMerchant_id_wsh));
        Map<Long, com.pet.order.dto.OrderSnapshotDTO> snapshotMap =
                orderSnapshotService.findDTOMapByOrderIds(collectIds(orders, PetOrder::getId_wsh));
        Set<Long> ratedOrderIds = new HashSet<>(ratingMapper.selectOrderIdsWithRatings(collectIds(orders, PetOrder::getId_wsh)));
        Map<Long, Payment> overtimePayments = loadOvertimePayments(orders);
        return orders.stream()
                .map(order -> {
                    OrderDTO dto = toDTO(order);
                    fillDTO(dto, order, serviceMap, userMap, petMap, keeperMap, merchantMap);
                    dto.setSnapshot_wsh(snapshotMap.get(order.getId_wsh()));
                    dto.setHas_feedback_wsh(ratedOrderIds.contains(order.getId_wsh()));
                    Payment overtimePayment = order.getOvertime_payment_id_wsh() == null
                            ? null : overtimePayments.get(order.getOvertime_payment_id_wsh());
                    if (overtimePayment != null) {
                        dto.setOvertime_pay_no_wsh(overtimePayment.getPay_no_wsh());
                        dto.setOvertime_payment_status_wsh(overtimePayment.getStatus_wsh());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /** 批量加载晚接补费支付单，供列表/详情展示「待补费」与支付状态。 */
    private Map<Long, Payment> loadOvertimePayments(List<PetOrder> orders) {
        Set<Long> paymentIds = orders.stream()
                .map(PetOrder::getOvertime_payment_id_wsh)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (paymentIds.isEmpty()) {
            return Map.of();
        }
        List<Payment> payments = paymentMapper.selectBatchIds(paymentIds);
        if (payments == null || payments.isEmpty()) {
            return Map.of();
        }
        return payments.stream().collect(Collectors.toMap(Payment::getId_wsh, Function.identity(), (a, b) -> a));
    }

    // 批量获取id
    /**
     * 【批量收集关联ID】
     *
     * 业务作用：从订单列表中按指定字段抽取非空 ID 集合，用于批量查询。
     *
     * 调用场景：toDTOEnrichedList 批量加载服务、用户、宠物、看护者、商家前。
     *
     * 调用链：toDTOEnrichedList() → collectIds()。
     *
     * 数据处理：按传入的函数取出 ID，过滤 null，去重收集为 Set。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：orders 为 null 时会触发空指针，调用方 toDTOEnrichedList 已提前判空。
     */
    private Set<Long> collectIds(List<PetOrder> orders, Function<PetOrder, Long> idGetter) {
        return orders.stream()
                .map(idGetter)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 【批量加载服务项】
     *
     * 业务作用：按 ID 集合批量查询服务项，组装 id → ServiceItem 的映射。
     *
     * 调用场景：toDTOEnrichedList 组装订单 DTO 前。
     *
     * 调用链：toDTOEnrichedList() → loadServiceMap() → serviceItemMapper.selectList()。
     *
     * 数据处理：只投影 id、name、description、images 四个字段，按 id IN 查询。
     *
     * 业务规则：ID 集合为空时直接返回空 Map，不发起 SQL。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：无。
     *
     * 注意事项：只查展示所需字段，避免 N+1 与多余字段传输。
     */
    private Map<Long, ServiceItem> loadServiceMap(Set<Long> serviceIds) {
        if (serviceIds.isEmpty()) {
            return Map.of();
        }
        return serviceItemMapper.selectList(new LambdaQueryWrapper<ServiceItem>()
                        .select(ServiceItem::getId_wsh, ServiceItem::getName_wsh, ServiceItem::getDescription_wsh, ServiceItem::getImages_wsh)
                        .in(ServiceItem::getId_wsh, serviceIds))
                .stream()
                .collect(Collectors.toMap(ServiceItem::getId_wsh, Function.identity()));
    }

    /**
     * 【批量加载用户】
     *
     * 业务作用：按 ID 集合批量查询用户，组装 id → User 的映射。
     *
     * 调用场景：toDTOEnrichedList 填充订单主人昵称前。
     *
     * 调用链：toDTOEnrichedList() → loadUserMap() → userMapper.selectList()。
     *
     * 数据处理：只投影 id、nickname、username 三个字段，按 id IN 查询。
     *
     * 业务规则：ID 集合为空时直接返回空 Map，不发起 SQL。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：无。
     *
     * 注意事项：昵称与用户名的取舍由 fillDTO 中的 defaultText 决定。
     */
    private Map<Long, User> loadUserMap(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                        .select(User::getId_wsh, User::getNickname_wsh, User::getUsername_wsh)
                        .in(User::getId_wsh, userIds))
                .stream()
                .collect(Collectors.toMap(User::getId_wsh, Function.identity()));
    }

    /**
     * 【批量加载宠物】
     *
     * 业务作用：按 ID 集合批量查询宠物，组装 id → Pet 的映射。
     *
     * 调用场景：toDTOEnrichedList 填充订单宠物名前。
     *
     * 调用链：toDTOEnrichedList() → loadPetMap() → petMapper.selectList()。
     *
     * 数据处理：只投影 id、name 两个字段，按 id IN 查询。
     *
     * 业务规则：ID 集合为空时直接返回空 Map，不发起 SQL。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：无。
     *
     * 注意事项：宠物已删除时 Map 中查不到，DTO 对应字段保持为空。
     */
    private Map<Long, Pet> loadPetMap(Set<Long> petIds) {
        if (petIds.isEmpty()) {
            return Map.of();
        }
        return petMapper.selectList(new LambdaQueryWrapper<Pet>()
                        .select(Pet::getId_wsh, Pet::getName_wsh)
                        .in(Pet::getId_wsh, petIds))
                .stream()
                .collect(Collectors.toMap(Pet::getId_wsh, Function.identity()));
    }

    /**
     * 【批量加载看护者】
     *
     * 业务作用：按 ID 集合批量查询看护者，组装 id → Keeper 的映射。
     *
     * 调用场景：toDTOEnrichedList 填充订单看护者信息前。
     *
     * 调用链：toDTOEnrichedList() → loadKeeperMap() → keeperMapper.selectList()。
     *
     * 数据处理：只投影 id、name、phone、avatar 四个字段，按 id IN 查询。
     *
     * 业务规则：ID 集合为空时直接返回空 Map，不发起 SQL。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：无。
     *
     * 注意事项：无。
     */
    private Map<Long, Keeper> loadKeeperMap(Set<Long> keeperIds) {
        if (keeperIds.isEmpty()) {
            return Map.of();
        }
        return keeperMapper.selectList(new LambdaQueryWrapper<Keeper>()
                        .select(Keeper::getId_wsh, Keeper::getName_wsh,
                                Keeper::getPhone_wsh, Keeper::getAvatar_wsh)
                        .in(Keeper::getId_wsh, keeperIds))
                .stream()
                .collect(Collectors.toMap(Keeper::getId_wsh, Function.identity()));
    }

    /**
     * 【批量加载商家】
     *
     * 业务作用：按 ID 集合批量查询商家，组装 id → Merchant 的映射。
     *
     * 调用场景：toDTOEnrichedList 填充订单商家信息前。
     *
     * 调用链：toDTOEnrichedList() → loadMerchantMap() → merchantMapper.selectList()。
     *
     * 数据处理：只投影 id、name、phone、address 四个字段，按 id IN 查询。
     *
     * 业务规则：ID 集合为空时直接返回空 Map，不发起 SQL。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：无。
     *
     * 注意事项：无。
     */
    private Map<Long, Merchant> loadMerchantMap(Set<Long> merchantIds) {
        if (merchantIds.isEmpty()) {
            return Map.of();
        }
        return merchantMapper.selectList(new LambdaQueryWrapper<Merchant>()
                        .select(Merchant::getId_wsh, Merchant::getName_wsh,
                                Merchant::getPhone_wsh, Merchant::getAddress_wsh)
                        .in(Merchant::getId_wsh, merchantIds))
                .stream()
                .collect(Collectors.toMap(Merchant::getId_wsh, Function.identity()));
    }

    /**
     * 从预先加载好的 5 个 Map 中取出关联数据填入 DTO，纯内存操作，0 SQL
     */
    private void fillDTO(OrderDTO dto,
                         PetOrder order,
                         Map<Long, ServiceItem> serviceMap,
                         Map<Long, User> userMap,
                         Map<Long, Pet> petMap,
                         Map<Long, Keeper> keeperMap,
                         Map<Long, Merchant> merchantMap) {
        ServiceItem service = order.getService_id_wsh() == null ? null : serviceMap.get(order.getService_id_wsh());
        if (service != null) {
            dto.setService_name_wsh(service.getName_wsh());
            dto.setService_description_wsh(service.getDescription_wsh());
            dto.setService_images_wsh(service.getImages_wsh());
        }
        User user = userMap.get(order.getOwner_id_wsh());
        if (user != null) {
            dto.setOwner_name_wsh(defaultText(user.getNickname_wsh(), user.getUsername_wsh()));
        }
        Pet pet = petMap.get(order.getPet_id_wsh());
        if (pet != null) {
            dto.setPet_name_wsh(pet.getName_wsh());
        }
        // 待分配订单没有负责人：Map.of() 对 null key 会抛 NPE，必须先判空
        if (order.getKeeper_id_wsh() != null) {
            Keeper keeper = keeperMap.get(order.getKeeper_id_wsh());
            if (keeper != null) {
                dto.setKeeper_name_wsh(keeper.getName_wsh());
                dto.setKeeper_phone_wsh(keeper.getPhone_wsh());
                dto.setKeeper_avatar_wsh(keeper.getAvatar_wsh());
            }
        }
        Merchant merchant = merchantMap.get(order.getMerchant_id_wsh());
        if (merchant != null) {
            dto.setMerchant_name_wsh(merchant.getName_wsh());
            dto.setMerchant_phone_wsh(merchant.getPhone_wsh());
            dto.setMerchant_address_wsh(merchant.getAddress_wsh());
        }
    }

    /**
     * Stamps the current timestamp onto the order's status-specific time field
     * (delivered_at, received_at, started_at, completed_at) only if that field
     * is not already set and the status matches.
     *
     * @param order  the order to stamp
     * @param status the target status that determines which field to set
     */
    private void stampStatusTime(PetOrder order, String status) {
        LocalDateTime now = LocalDateTime.now();
        if (OrderStatus.RECEIVED.equals(status) && order.getReceived_at_wsh() == null) {
            order.setReceived_at_wsh(now);
        } else if (OrderStatus.IN_PROGRESS.equals(status) && order.getStarted_at_wsh() == null) {
            order.setStarted_at_wsh(now);
        } else if (OrderStatus.COMPLETED.equals(status) && order.getCompleted_at_wsh() == null) {
            order.setCompleted_at_wsh(now);
        }
    }

    /**
     * Publishes an {@link OrderCompletedEvent} to trigger downstream processes
     * such as AI-generated boarding report creation and merchant settlement
     * confirmation. Failures are logged but swallowed to avoid rollback.
     *
     * @param order the completed order
     */
    private void publishCompletedEvent(PetOrder order) {
        try {
            eventPublisher.publishEvent(new OrderCompletedEvent(
                    order.getId_wsh(), order.getPet_id_wsh(), order.getKeeper_id_wsh()));
        } catch (Exception e) {
            log.warn("发布订单完成事件失败，订单编号: {}", order.getOrder_no_wsh(), e);
        }
    }

    /**
     * Sends a delayed message to check payment timeout after the transaction commits.
     * Uses {@link TransactionSynchronization#afterCommit()} to schedule the check
     * only after the current transaction succeeds. If no transaction is active,
     * sends immediately.
     *
     * @param order the newly created order whose payment timeout should be monitored
     */
    private void schedulePaymentTimeoutCheck(PetOrder order) {
        String orderNo = order.getOrder_no_wsh();
        if (isBlank(orderNo)) {
            return;
        }
        Runnable task = () -> {
            try {
                messageSender.sendOrderPaymentTimeout(orderNo);
            } catch (Exception e) {
                log.warn("发送订单支付超时检查消息失败，订单编号: {}", orderNo, e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    /**
     * 【已支付订单自动确认接单】
     *
     * 业务作用：把 PAID 状态订单推进为 CONFIRMED，接单前重新校验商家、看护者、资质、容量与休假。
     *
     * 调用场景：MQ 接单超时消息（autoAcceptPaidOrderIfTimeout）与定时兜底扫描（autoAcceptPaidOrdersIfTimeout）。
     *
     * 调用链：autoAcceptPaidOrderIfTimeout() / autoAcceptPaidOrdersIfTimeout() → confirmPaidOrder() → requireFutureBookingEligibleMerchant() / requireFutureBookableKeeper() / validateKeeperQualification() / ensureKeeperCapacityForOrder() / keeperLeaveService.requireKeeperAvailable() → orderMapper.update() → refreshKeeperCurrentPets() → broadcastOrderChange()。
     *
     * 数据处理：先做五项可用性校验，再用乐观锁按 id + status=PAID 更新为 CONFIRMED。
     *
     * 业务规则：只有 PAID 状态可被确认；更新影响行数为 0 视为并发失败，直接返回 false。
     *
     * 状态影响：订单状态改为 CONFIRMED，刷新看护者当前在托数量，并广播 SSE 状态事件。
     *
     * 异常情况：商家不可预约、看护者不可接单、资质未通过、容量超限、休假冲突等抛 BusinessException。
     *
     * 注意事项：方法本身无 @Transactional，事务边界由调用方决定；返回 false 表示订单已被其他流程改变状态。
     */
        /**
     * Cancels a single order if it is still PENDING and was created before the cutoff
     * time. Releases locked coupons and membership benefits. Skips silently if the
     * order is null, not PENDING, or was created after the cutoff.
     *
     * @param order  the order to evaluate and possibly cancel
     * @param cutoff the time threshold: orders created before this are eligible
     * @return true if the order was cancelled; false otherwise
     */
    private boolean cancelPaymentTimeoutOrder(PetOrder order, LocalDateTime cutoff) {
        if (order == null || order.getId_wsh() == null || order.getCreated_at_wsh() == null) {
            return false;
        }
        if (!OrderStatus.PENDING.equals(order.getStatus_wsh()) || order.getCreated_at_wsh().isAfter(cutoff)) {
            return false;
        }
        PetOrder update = new PetOrder();
        update.setStatus_wsh(OrderStatus.CANCELLED);
        int updated = orderMapper.update(update, new LambdaUpdateWrapper<PetOrder>()
                .eq(PetOrder::getId_wsh, order.getId_wsh())
                .eq(PetOrder::getStatus_wsh, OrderStatus.PENDING)
                .le(PetOrder::getCreated_at_wsh, cutoff));
        if (updated == 0) {
            return false;
        }
        couponService.releaseForOrder(order.getId_wsh());
        membershipBenefitService.releaseForOrder(order.getId_wsh());
        order.setStatus_wsh(OrderStatus.CANCELLED);
        refreshKeeperCurrentPets(order.getKeeper_id_wsh());
        broadcastOrderChange(order);
        log.info("订单支付超时自动取消，订单编号: {}", order.getOrder_no_wsh());
        return true;
    }

    /**
     * Broadcasts the order's current status to all relevant recipients
     * (owner, keeper, merchant) via SSE. Failures are logged but swallowed
     * to avoid disrupting the transaction.
     *
     * @param order the order whose status change to broadcast
     */
    private void broadcastOrderChange(PetOrder order) {
        try {
            orderStatusBroadcaster.broadcast(order);
        } catch (Exception e) {
            log.warn("广播订单状态失败，订单编号: {}", order.getOrder_no_wsh(), e);
        }
    }

    /**
     * Generates a unique order number in the format {@code ORDyyyyMMddXXXXXXXX}
     * where the suffix is an 8-character uppercase UUID segment.
     *
     * @return a unique order number
     */
    private String generateOrderNo() {
        String date = LocalDate.now().toString().replace("-", "");
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD" + date + uuid;
    }

    /**
     * Generates a random 4-digit handover code used for pet delivery verification.
     * The owner provides this code to the keeper/merchant at drop-off to confirm receipt.
     *
     * @return a 4-digit numeric string (0000-9999)
     */
    private String generateHandoverCode() {
        synchronized (HANDOVER_CODE_RANDOM) {
            return String.format("%04d", HANDOVER_CODE_RANDOM.nextInt(10000));
        }
    }

    /**
     * 生成独立接走码：4 位数字字符串（保留前导 0），且与同单送达码不同。
     *
     * @param handoverCode 同单送达码
     * @return 接走码
     */
    private String generatePickupCode(String handoverCode) {
        synchronized (HANDOVER_CODE_RANDOM) {
            for (int i = 0; i < 100; i++) {
                String code = String.format("%04d", HANDOVER_CODE_RANDOM.nextInt(10000));
                if (!code.equals(handoverCode)) {
                    return code;
                }
            }
        }
        // 理论上不会走到这里：100 次都撞上同一个码的概率可忽略
        throw new BusinessException(500, "接走码生成失败，请重试");
    }

    /**
     * 【文本空值兜底】
     *
     * 业务作用：主值空白时返回兜底文本，否则返回 trim 后的主值。
     *
     * 调用场景：fillDTO 填充订单主人昵称。
     *
     * 调用链：fillDTO() → defaultText() → isBlank()。
     *
     * 数据处理：value 空白返回 fallback，否则返回 value.trim()。
     *
     * 业务规则：fallback 不再做 trim。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：无。
     */
    private String defaultText(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    /**
     * 【判断字符串空白】
     *
     * 业务作用：判断字符串是否为 null 或仅含空白字符。
     *
     * 调用场景：订单号判空、昵称兜底判断等。
     *
     * 调用链：autoAcceptPaidOrderIfTimeout() / schedulePaymentTimeoutCheck() / defaultText() → isBlank()。
     *
     * 数据处理：null 或 trim 后为空串即返回 true。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：项目未统一使用 StringUtils，这里是本地实现，语义与 StringUtils.isBlank 一致。
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
    /**
     * 【价格预览】
     *
     * 业务作用：
     * 返回与真实下单完全一致的金额明细与时间语义，前端据此展示，不再自行实现一套计价。
     *
     * 调用链：
     * OrderController.preview → OrderService.previewOrder → quoteOrder（与 doCreateOrder 同一个方法）
     *
     * 无写副作用：
     * 不插订单、不锁优惠券、不锁会员权益、不发 SSE 广播、不排支付超时定时器。
     *
     * 异常情况：
     * 参数非法、服务下架、价格变更、时间冲突等与下单完全相同的业务异常会原样抛出，
     * 前端可据此在提交前提示用户，而不是等到提交才失败。
     *
     * @param ownerId 当前登录用户（用于优惠券/会员折扣预览，不写库）
     * @param request 预览请求（与下单请求同构，但不要求紧急联系人）
     * @return 金额明细与规范化后的服务时间
     */
    @Transactional(readOnly = true)
    @Override
    public OrderQuoteDTO previewOrder(Long ownerId, OrderPreviewRequestDTO request) {
        Quote q = quoteOrder(ownerId, toCreateRequest(request));
        OrderQuoteDTO dto = new OrderQuoteDTO();
        dto.setBilling_unit_wsh(q.unit());
        dto.setQuantity_wsh(q.quantity());
        dto.setDuration_minutes_wsh(q.durationMinutes());
        // days_wsh 与订单落库口径保持完全一致，避免「预览显示 2 天、订单显示 3 天」
        dto.setDays_wsh(q.daysDisplay());
        dto.setUnit_price_wsh(q.unitPrice());
        dto.setTotal_amount_wsh(q.totalAmount());
        dto.setLong_stay_discount_wsh(q.longStayDiscount());
        dto.setCoupon_discount_wsh(q.couponDiscount());
        dto.setMembership_discount_wsh(q.memberDiscount());
        dto.setPlatform_subsidy_wsh(q.platformSubsidy());
        dto.setDiscount_wsh(q.discount());
        dto.setFinal_amount_wsh(q.finalAmount());
        dto.setStart_date_wsh(q.startDate());
        dto.setEnd_date_wsh(q.endDate());
        dto.setDelivery_time_wsh(q.deliveryTime());
        dto.setPickup_time_wsh(q.pickupTime());
        dto.setCoupon_id_wsh(q.coupon().getUser_coupon_id_wsh());
        dto.setMembership_id_wsh(q.membership().getMembership_id_wsh());
        if (q.isNight()) {
            // 按夜计价：本次预付 = 夜费 + 提前费；晚接费只给预估，实际接走时结算
            dto.setBooked_nights_wsh(q.quantity());
            dto.setEarly_extra_nights_wsh(OrderPricing.earlyExtraNights(q.early()));
            dto.setEarly_extra_hours_wsh(OrderPricing.earlyExtraHours(q.early()));
            dto.setEarly_amount_wsh(OrderPricing.earlyAmount(q.unitPrice(), q.quantity(), q.early()));
            dto.setStandard_checkout_at_wsh(q.standardCheckoutAt());
            dto.setFree_checkout_until_wsh(q.freeCheckoutUntil());
            int estimatedHours = OrderPricing.overtimeHours(q.freeCheckoutUntil(), q.pickupTime());
            dto.setEstimated_overtime_hours_wsh(estimatedHours);
            dto.setEstimated_overtime_amount_wsh(OrderPricing.overtimeAmount(q.unitPrice(), estimatedHours));
        }
        return dto;
    }

    /** 预览请求 → 下单请求：字段同名直接拷贝，仅补下单必需但预览不需要的紧急联系人占位。 */
    private OrderCreateRequestDTO toCreateRequest(OrderPreviewRequestDTO preview) {
        OrderCreateRequestDTO request = new OrderCreateRequestDTO();
        BeanUtils.copyProperties(preview, request);
        if (isBlank(request.getEmergency_contact_name_wsh())) {
            request.setEmergency_contact_name_wsh("preview");
        }
        if (isBlank(request.getEmergency_contact_phone_wsh())) {
            request.setEmergency_contact_phone_wsh("preview");
        }
        return request;
    }

    /**
     * 报价结果：quoteOrder 的唯一输出。
     * <p>
     * 字段全部为「校验后的时间语义 + 最终金额」，不含任何实体写操作。
     * 用 record 是为了让 doCreateOrder 与 previewOrder 不可能拿到不一致的中间值。
     */
    private record Quote(
            Pet pet,    // 宠物
            ServiceItem service,    // 服务项
            Merchant merchant,    // 商家
            Keeper keeper,    // 看护者
            String unit,    // 计价单位
            int quantity,    // 下单数量（night=预订夜数 N）
            int durationMinutes,    // 持续分钟
            Duration early,    // 提前送达时长 E（night 用）
            LocalDate startDate,    // 开始日期
            LocalDate endDate,    // 结束日期
            LocalDateTime deliveryTime,    // 照护开始时间
            LocalDateTime receiverStart,    // 接收开始时间
            LocalDateTime receiverEnd,    // 接收结束时间
            LocalDateTime pickupTime,    // 照护结束时间
            BigDecimal unitPrice,    // 单价
            BigDecimal totalAmount,    // 总金额
            BigDecimal longStayDiscount,    // 长住折扣
            CouponDiscountResult coupon,    // 券折扣
            BigDecimal memberDiscount,    // 会员折扣
            BigDecimal discount,    // 折扣
            MembershipDiscountDTO membership    // 会员折扣
    ) {

        /**
         * days_wsh 展示口径：night=预订夜数、session/hour=1。
         * 下单与预览共用，避免「预览显示 2 夜、订单显示 3 夜」。
         */
        int daysDisplay() {
            if (BookingUnit.NIGHT.equals(unit)) {
                return quantity;
            }
            return 1;
        }

        /** 是否按夜计价的订单。 */
        boolean isNight() {
            return BookingUnit.NIGHT.equals(unit);
        }

        /** 标准离店锚点 O = D+N 日 14:00（仅 night 有意义）。 */
        LocalDateTime standardCheckoutAt() {
            return isNight() ? OrderPricing.standardCheckoutAt(startDate, quantity) : null;
        }

        /** 免费接走截止 G = O+30 分钟（仅 night 有意义）。 */
        LocalDateTime freeCheckoutUntil() {
            return isNight() ? OrderPricing.freeCheckoutUntil(startDate, quantity) : null;
        }

        /** 券抵扣（null 归一为 0）。 */
        BigDecimal couponDiscount() {
            return coupon == null || coupon.getCoupon_discount_wsh() == null
                    ? BigDecimal.ZERO : coupon.getCoupon_discount_wsh();
        }

        /** 平台补贴 = 券补贴 + 会员折扣；商家结算不受影响。 */
        BigDecimal platformSubsidy() {
            BigDecimal couponSubsidy = coupon == null || coupon.getPlatform_subsidy_wsh() == null
                    ? BigDecimal.ZERO : coupon.getPlatform_subsidy_wsh();
            return couponSubsidy.add(memberDiscount == null ? BigDecimal.ZERO : memberDiscount);
        }

        /**
         * 实付金额。
         * <p>
         * 会员折扣结果缺失时回退到「券后金额」，而不是静默写 0 元——
         * 0 元实付的订单仍然可以支付，会直接造成资损。
         */
        BigDecimal finalAmount() {
            if (membership != null && membership.getFinal_amount_wsh() != null) {
                return membership.getFinal_amount_wsh();
            }
            return coupon == null || coupon.getFinal_amount_wsh() == null
                    ? BigDecimal.ZERO : coupon.getFinal_amount_wsh();
        }
    }
}
