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
import com.pet.common.BusinessException;
import com.pet.common.OrderStatus;
import com.pet.common.ServiceVersions;
import com.pet.common.StatusCode;
import com.pet.common.geo.GeoDistanceUtils;
import com.pet.config.RabbitMQConfig;
import com.pet.finance.service.AccountingService;
import com.pet.marketing.dto.CouponDiscountResult;
import com.pet.marketing.service.CouponService;
import com.pet.membership.dto.MembershipDiscountDTO;
import com.pet.membership.service.MembershipBenefitService;
import com.pet.order.dto.OrderCreateRequestDTO;
import com.pet.order.dto.OrderDeliveredRequestDTO;
import com.pet.order.dto.OrderDTO;
import com.pet.order.dto.OrderReceivedRequestDTO;
import com.pet.order.service.OrderStatusBroadcaster;
import com.pet.order.entity.Payment;
import com.pet.order.entity.PetOrder;
import com.pet.order.event.OrderCompletedEvent;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.mapper.PaymentMapper;
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
import java.time.temporal.ChronoUnit;
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
            OrderStatus.DELIVERED, OrderStatus.RECEIVED, OrderStatus.IN_PROGRESS,
            OrderStatus.COMPLETED, OrderStatus.CANCELLED,
            OrderStatus.REFUNDING, OrderStatus.REFUNDED);

    // 订单状态
    private static final Set<String> BOOKING_STATUSES = Set.of(
            OrderStatus.PENDING, OrderStatus.PAID, OrderStatus.CONFIRMED,
            OrderStatus.DELIVERED, OrderStatus.RECEIVED, OrderStatus.IN_PROGRESS,
            OrderStatus.REFUNDING);

    private static final Set<String> CURRENT_PET_STATUSES = Set.of(
            OrderStatus.CONFIRMED, OrderStatus.DELIVERED, OrderStatus.RECEIVED, OrderStatus.IN_PROGRESS);

    private static final Set<String> ACCOUNTING_MANAGED_STATUSES = Set.of(
            OrderStatus.PAID, OrderStatus.COMPLETED, OrderStatus.CANCELLED,
            OrderStatus.REFUNDING, OrderStatus.REFUNDED);

    private static final Set<String> PAID_FLOW_STATUSES = Set.of(
            OrderStatus.CONFIRMED, OrderStatus.DELIVERED, OrderStatus.RECEIVED, OrderStatus.IN_PROGRESS);

    private static final int RECEIVER_WINDOW_MINUTES = 10;
    private static final Duration PAYMENT_TIMEOUT = Duration.ofMillis(RabbitMQConfig.ORDER_PAYMENT_TIMEOUT_TTL_MS);
    private static final int PAYMENT_TIMEOUT_SCAN_LIMIT = 100;
    private static final Duration ACCEPT_TIMEOUT = Duration.ofMillis(RabbitMQConfig.ORDER_ACCEPT_TIMEOUT_TTL_MS);
    private static final int ACCEPT_TIMEOUT_SCAN_LIMIT = 100;
    private static final Random HANDOVER_CODE_RANDOM = new Random();
    private static final Object CREATE_ORDER_LOCK = new Object();

    @Value("${gao.map.handover-radius-meters:500}")
    private int handoverRadiusMeters;

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
                             BusinessHoursTargetResolver businessHoursTargetResolver) {
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
     * 【查询看护者待处理订单（实现）】
     *
     * 业务作用：
     * 查询看护者待接单/待处理的订单（PENDING或PAID），用于看护者端接单入口。
     *
     * 调用链：
     * OrderService.listPendingByKeeper()
     * ↓
     * orderMapper.selectList(keeperId + in(PENDING,PAID) + 时间降序) → toDTOEnrichedList()
     *
     * 状态影响：
     * 只读操作。
     *
     * @param keeperId 看护者ID
     * @return 待处理订单DTO列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> listPendingByKeeper(Long keeperId) {
        List<PetOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getKeeper_id_wsh, keeperId)
                        .in(PetOrder::getStatus_wsh, List.of(OrderStatus.PENDING, OrderStatus.PAID))
                        .orderByDesc(PetOrder::getCreated_at_wsh));
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
        return dto;
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
     * 【根据ID查询增强DTO（实现）】
     *
     * 业务作用：
     * getById() + toDTOEnriched()组合，先查实体再增强。
     *
     * @param id 订单ID
     * @return 增强DTO
     */
    @Override
    @Transactional(readOnly = true)
    public OrderDTO getDTOById(Long id) {
        return toDTOEnriched(getById(id));
    }

    /**
     * 【根据订单号查询增强DTO（实现）】
     *
     * 业务作用：
     * getByOrderNo() + toDTOEnriched()组合。
     *
     * @param orderNo 订单编号
     * @return 增强DTO
     */
    @Override
    @Transactional(readOnly = true)
    public OrderDTO getDTOByOrderNo(String orderNo) {
        return toDTOEnriched(getByOrderNo(orderNo));
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
     * [synchronized] requirePet() → requireFutureBookableKeeper() → requireFutureBookingEligibleMerchant()
     * → validateKeeperMerchant() → validateKeeperQualification() → validateService()
     * → validateDateRange() → validateFulfillmentWindow()
     * → ensureNoPetDateConflict() → ensureKeeperCapacity() → keeperLeaveService.requireKeeperAvailable()
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

        // 获取到当前用户的下单宠物
        // 教学注释：下单不是单纯 insert，前面还有“查宠物档期/查看护者容量”。
        // 在没有排班表或数据库约束前，同 JVM 先串行化这段，避免两个请求同时通过检查后一起写入。
        synchronized (CREATE_ORDER_LOCK) {
        Pet pet = requirePet(ownerId, request.getPet_id_wsh());
        // 获取到当前用户的下单宠物的keeper
        Keeper keeper = requireFutureBookableKeeper(request.getKeeper_id_wsh());
        // 先加载可下单服务（产品预订必须指定服务），商家与价格均由服务端根据服务派生
        ServiceItem service = loadBookableService(request.getService_id_wsh());
        assertServiceUnitSupported(service);
        Merchant merchant = requireFutureBookingEligibleMerchant(service.getMerchant_id_wsh());
        assertMerchantPayloadCompatible(request.getMerchant_id_wsh(), merchant.getId_wsh());

        // 服务判断
        validateKeeperMerchant(keeper, merchant);
        validateKeeperQualification(keeper.getId_wsh());
        assertServiceVersionMatches(service, request.getService_version_wsh());

        // 下单日期判断
        int days = validateDateRange(request.getStart_date_wsh(), request.getEnd_date_wsh());
        LocalDateTime deliveryTime = defaultDeliveryTime(request);
        LocalDateTime receiverStart = defaultReceiverStart(deliveryTime);
        LocalDateTime receiverEnd = defaultReceiverEnd(deliveryTime);
        LocalDateTime pickupTime = defaultPickupTime(request);
        validateFulfillmentWindow(request.getStart_date_wsh(), request.getEnd_date_wsh(),
                deliveryTime, receiverStart, receiverEnd, pickupTime,
                merchant.getId_wsh());

        // 冲突判断
        ensureNoPetDateConflict(pet.getId_wsh(), request.getStart_date_wsh(), request.getEnd_date_wsh(), null);
        ensureKeeperCapacity(keeper.getId_wsh(), request.getStart_date_wsh(), request.getEnd_date_wsh(),
                null, keeper.getMax_pets_wsh());
        keeperLeaveService.requireKeeperAvailable(keeper.getId_wsh(), request.getStart_date_wsh(), request.getEnd_date_wsh());

        BigDecimal pricePerDay = resolvePricePerDay(service, keeper);
        BigDecimal totalAmount = pricePerDay.multiply(BigDecimal.valueOf(days));
        BigDecimal longStayDiscount = calculateDiscount(totalAmount, days);
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

        PetOrder order = new PetOrder();

        BeanUtils.copyProperties(request, order);

        order.setOrder_no_wsh(generateOrderNo());                       // 订单号
        order.setOwner_id_wsh(ownerId);                                 //  下单用户

        order.setPet_id_wsh(pet.getId_wsh());                           //  下单宠物
        order.setMerchant_id_wsh(merchant.getId_wsh());                 //  下单商户
        order.setKeeper_id_wsh(keeper.getId_wsh());                     //  下单keeper

        order.setPrice_per_day_wsh(pricePerDay);                            //  每日单价（服务优先）
        order.setDiscount_wsh(discount);                                //  优惠
        order.setTotal_amount_wsh(totalAmount);                         //  总价
        order.setCoupon_id_wsh(couponDiscount.getUser_coupon_id_wsh());
        order.setCoupon_template_id_wsh(couponDiscount.getTemplate_id_wsh());
        order.setCoupon_discount_wsh(defaultMoney(couponDiscount.getCoupon_discount_wsh()));
        order.setMembership_id_wsh(membershipDiscount.getMembership_id_wsh());
        order.setMembership_plan_id_wsh(membershipDiscount.getPlan_id_wsh());
        order.setMembership_discount_wsh(memberDiscount);
        order.setMembership_snapshot_wsh(membershipDiscount.getSnapshot_wsh());
        order.setPlatform_subsidy_wsh(defaultMoney(couponDiscount.getPlatform_subsidy_wsh()).add(memberDiscount));
        order.setSettlement_amount_wsh(defaultMoney(couponDiscount.getSettlement_amount_wsh()));
        order.setPromotion_snapshot_wsh(toPromotionSnapshot(couponDiscount, membershipDiscount));
        order.setFinal_amount_wsh(defaultMoney(membershipDiscount.getFinal_amount_wsh()));      //  实付
        order.setStatus_wsh(OrderStatus.PENDING);                       //  订单状态

        order.setHandover_code_wsh(generateHandoverCode());             //  交接码

        order.setDays_wsh(days);                                        //  抚养天数
        order.setDelivery_time_wsh(deliveryTime);                       //  宠物取送时间
        order.setReceiver_available_start_wsh(receiverStart);           //  接收者接收宠物的时间
        order.setReceiver_available_end_wsh(receiverEnd);               //  接收者接收宠物的结束时间
        order.setPickup_time_wsh(pickupTime);                           //  宠物取回时间
        order.setFinal_report_generated_wsh(0);                         //  是否生成了 boarding report
        orderMapper.insert(order);
        couponService.lockForOrder(ownerId, request.getUser_coupon_id_wsh(), order.getId_wsh(),
                order.getOrder_no_wsh(), order.getCoupon_discount_wsh());
        membershipBenefitService.lockForOrder(ownerId, membershipDiscount, order.getId_wsh(), order.getOrder_no_wsh());
        User owner = userMapper.selectById(ownerId);
        orderSnapshotService.createForOrder(order, owner, pet, merchant, keeper, service, request);

        refreshKeeperCurrentPets(keeper.getId_wsh());
        broadcastOrderChange(order);
        schedulePaymentTimeoutCheck(order);
        return toDTOEnriched(order);
        }
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

    /**
     * 【接受订单（实现）】
     *
     * 业务作用：
     * 看护者接单，校验资质、容量、请假状态后推进到CONFIRMED。
     *
     * 调用链：
     * OrderService.acceptOrder()
     * ↓
     * getByOrderNo() → requireFutureBookableKeeper() → requireAssignedKeeperOrderAccess()
     * → validateKeeperQualification() → ensureKeeperCapacity() → requireKeeperAvailable()
     * → 乐观锁更新CONFIRMED → refreshKeeperCurrentPets() → broadcastOrderChange()
     *
     * @param userId 用户ID
     * @param orderNo 订单编号
     */
    @Transactional
    @Override
    public void acceptOrder(Long userId, String orderNo) {
        PetOrder order = getByOrderNo(orderNo);
        Keeper keeper = requireFutureBookableKeeper(order.getKeeper_id_wsh());
        requireAssignedKeeperOrderAccess(order, keeper, userId);
        if (!OrderStatus.PAID.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "订单状态为 " + order.getStatus_wsh() + "，未支付订单不能接单");
        }
        requireFutureBookingEligibleMerchant(order.getMerchant_id_wsh());
        validateKeeperQualification(keeper.getId_wsh());
        ensureKeeperCapacity(keeper.getId_wsh(), order.getStart_date_wsh(), order.getEnd_date_wsh(),
                order.getId_wsh(), keeper.getMax_pets_wsh());
        keeperLeaveService.requireKeeperAvailable(keeper.getId_wsh(), order.getStart_date_wsh(), order.getEnd_date_wsh());
        PetOrder update = new PetOrder();
        update.setStatus_wsh(OrderStatus.CONFIRMED);
        int updated = orderMapper.update(update, new LambdaUpdateWrapper<PetOrder>()
                .eq(PetOrder::getId_wsh, order.getId_wsh())
                .eq(PetOrder::getStatus_wsh, OrderStatus.PAID));
        if (updated == 0) {
            throw new BusinessException(400, "订单状态已变化，无法接单");
        }
        order.setStatus_wsh(OrderStatus.CONFIRMED);
        refreshKeeperCurrentPets(order.getKeeper_id_wsh());
        broadcastOrderChange(order);
    }

    /**
     * 【拒绝订单（实现）】
     *
     * 业务作用：
     * 看护者或商家拒单，触发全额退款、回冲平台补贴、释放优惠券/会员权益。
     *
     * 调用链：
     * OrderService.rejectOrder()
     * ↓
     * getByOrderNo() → checkMerchantOrKeeperOrderAccess()
     * → 乐观锁更新CANCELLED → accountingService.transfer(全额退款) → accountingService.debit(回冲补贴)
     * → couponService.releaseForOrder() → membershipBenefitService.releaseForOrder()
     * → refreshKeeperCurrentPets() → broadcastOrderChange()
     *
     * @param userId 用户ID
     * @param orderNo 订单编号
     */
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

        BigDecimal refundAmount = defaultMoney(order.getFinal_amount_wsh());
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            accountingService.transfer(accountingService.systemUserId(), order.getOwner_id_wsh(),
                    refundAmount, "reject", order.getId_wsh(),
                    "order", String.valueOf(order.getId_wsh()),
                    "reject:order:" + order.getId_wsh(),
                    "商家拒单退款 - " + order.getOrder_no_wsh());
        }

        BigDecimal subsidy = defaultMoney(order.getPlatform_subsidy_wsh());
        if (subsidy.compareTo(BigDecimal.ZERO) > 0) {
            accountingService.debit(accountingService.systemUserId(), subsidy, "reject_subsidy", order.getId_wsh(),
                    "coupon", String.valueOf(order.getCoupon_id_wsh()),
                    "reject:subsidy:" + order.getId_wsh(),
                    "拒单回冲平台补贴 - " + order.getOrder_no_wsh());
        }

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
        boolean accepted = confirmPaidOrder(order);
        if (accepted) {
            log.info("Order auto accepted after paid timeout, orderNo: {}", order.getOrder_no_wsh());
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
        List<Payment> timeoutPayments = paymentMapper.selectList(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getStatus_wsh, "success")
                        .le(Payment::getPaid_at_wsh, cutoff)
                        .isNotNull(Payment::getOrder_no_wsh)
                        .orderByAsc(Payment::getPaid_at_wsh)
                        .last("LIMIT " + ACCEPT_TIMEOUT_SCAN_LIMIT));
        List<String> orderNos = timeoutPayments.stream()
                .map(Payment::getOrder_no_wsh)
                .filter(no -> no != null && !no.isBlank())
                .distinct()
                .collect(Collectors.toList());
        if (orderNos.isEmpty()) {
            return 0;
        }
        List<PetOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getStatus_wsh, OrderStatus.PAID)
                        .in(PetOrder::getOrder_no_wsh, orderNos)
                        .orderByAsc(PetOrder::getUpdated_at_wsh));
        int accepted = 0;
        for (PetOrder order : orders) {
            try {
                if (confirmPaidOrder(order)) {
                    accepted++;
                    log.info("Order auto accepted by fallback scanner, orderNo: {}", order.getOrder_no_wsh());
                }
            } catch (Exception e) {
                log.warn("Auto accept fallback scanner skipped order: {}", order.getOrder_no_wsh(), e);
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
     * 【标记已送达（实现-无参数版本）】
     *
     * 业务作用：
     * 构建空的OrderDeliveredRequestDTO委托给带DTO的重载方法。
     *
     * @param userId  用户ID
     * @param orderNo 订单编号
     */
    @Transactional
    @Override
    public void markDelivered(Long userId, String orderNo) {
        OrderDeliveredRequestDTO request = new OrderDeliveredRequestDTO();
        request.setOrder_no_wsh(orderNo);
        markDelivered(userId, request);
    }

    /**
     * 【标记已送达（实现-完整参数版本）】
     *
     * 业务作用：
     * 主人标记送达，校验归属和状态后更新订单为DELIVERED并记录送达位置信息。
     *
     * 调用链：
     * OrderService.markDelivered()
     * ↓
     * getByOrderNo() → 校验主人/状态 → 乐观锁更新DELIVERED + 位置/时间 → broadcastOrderChange()
     *
     * 数据处理：
     * 保存送达地址、经纬度、精度和送达时间。
     *
     * @param userId  用户ID
     * @param request 送达请求DTO
     */
    @Transactional
    @Override
    public void markDelivered(Long userId, OrderDeliveredRequestDTO request) {
        if (request == null) {
            throw new BusinessException(400, "送达信息不能为空");
        }
        PetOrder order = getByOrderNo(request.getOrder_no_wsh());
        if (!userId.equals(order.getOwner_id_wsh())) {
            throw new BusinessException(403, "只有宠物主人可以标记已送达");
        }
        if (!OrderStatus.CONFIRMED.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "当前订单状态无法标记已送达");
        }
        LocalDateTime deliveredAt = LocalDateTime.now();
        PetOrder update = new PetOrder();
        update.setStatus_wsh(OrderStatus.DELIVERED);
        update.setDelivered_at_wsh(deliveredAt);
        update.setDelivered_address_wsh(request.getDelivered_address_wsh());
        update.setDelivered_latitude_wsh(request.getDelivered_latitude_wsh());
        update.setDelivered_longitude_wsh(request.getDelivered_longitude_wsh());
        update.setDelivered_accuracy_wsh(request.getDelivered_accuracy_wsh());
        int updated = orderMapper.update(update, new LambdaUpdateWrapper<PetOrder>()
                .eq(PetOrder::getId_wsh, order.getId_wsh())
                .eq(PetOrder::getStatus_wsh, OrderStatus.CONFIRMED));
        if (updated == 0) {
            throw new BusinessException(400, "Order status changed, cannot mark delivered");
        }
        order.setStatus_wsh(OrderStatus.DELIVERED);
        order.setDelivered_at_wsh(deliveredAt);
        order.setDelivered_address_wsh(request.getDelivered_address_wsh());
        order.setDelivered_latitude_wsh(request.getDelivered_latitude_wsh());
        order.setDelivered_longitude_wsh(request.getDelivered_longitude_wsh());
        order.setDelivered_accuracy_wsh(request.getDelivered_accuracy_wsh());
        broadcastOrderChange(order);
    }

    /**
     * 【标记已接收（实现-简易参数版本）】
     *
     * 业务作用：
     * 构建OrderReceivedRequestDTO委托给带DTO的重载方法。
     *
     * @param userId       用户ID
     * @param orderNo      订单编号
     * @param handoverCode 交接码
     */
    @Transactional
    @Override
    public void markReceived(Long userId, String orderNo, String handoverCode) {
        OrderReceivedRequestDTO request = new OrderReceivedRequestDTO();
        request.setOrder_no_wsh(orderNo);
        request.setHandover_code_wsh(handoverCode);
        markReceived(userId, request);
    }

    /**
     * 【标记已接收（实现-完整参数版本）】
     *
     * 业务作用：
     * 看护者/商家接收宠物，校验交接码、GPS距离、在岗状态后更新为RECEIVED。
     *
     * 调用链：
     * OrderService.markReceived()
     * ↓
     * getByOrderNo() → checkMerchantOrKeeperOrderAccess() → requireKeeperOnDuty()
     * → 校验交接码/计算距离 → 乐观锁更新RECEIVED + 位置 → broadcastOrderChange()
     *
     * 数据处理：
     * 计算送达坐标与接收坐标之间的距离，必须在配置的交接半径内（默认500米）。
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
        keeperAttendanceService.requireKeeperOnDuty(order.getKeeper_id_wsh(), order.getMerchant_id_wsh());
        if (!OrderStatus.DELIVERED.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "当前订单状态无法标记已接收");
        }
        if (isBlank(request.getHandover_code_wsh())) {
            throw new BusinessException(400, "交接码不能为空");
        }
        if (!request.getHandover_code_wsh().trim().equals(order.getHandover_code_wsh())) {
            throw new BusinessException(400, "交接码不正确");
        }
        LocalDateTime receivedAt = LocalDateTime.now();
        BigDecimal receivedDistance = calculateReceivedDistance(order, request);
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
                .eq(PetOrder::getStatus_wsh, OrderStatus.DELIVERED));
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
     * Calculates the distance in meters between the delivery coordinates and the
     * receive coordinates. Validates both coordinate pairs and enforces a maximum
     * handover radius (configured via {@code gao.map.handover-radius-meters}).
     *
     * @param order   the order containing delivery coordinates
     * @param request the receive request containing receive coordinates
     * @return the distance in meters (1 decimal place), or null if coordinates are missing
     * @throws BusinessException if coordinates are invalid or exceed the handover radius
     */
    private BigDecimal calculateReceivedDistance(PetOrder order, OrderReceivedRequestDTO request) {
        if (order.getDelivery_latitude_wsh() == null || order.getDelivery_longitude_wsh() == null
                || request.getReceived_latitude_wsh() == null || request.getReceived_longitude_wsh() == null) {
            return null;
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
            throw new BusinessException(400, "接收位置超出交接范围");
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
     * 【完成订单（实现）】
     *
     * 业务作用：
     * 完成服务，触发商家结算（settleOrderToMerchant）、AI报告事件发布和看护者容量刷新。
     *
     * 调用链：
     * OrderService.completeOrder()
     * ↓
     * getByOrderNo() → checkMerchantOrKeeperOrderAccess() → requireKeeperOnDuty()
     * → 乐观锁更新COMPLETED + completed_at → settleOrderToMerchant()
     * → refreshKeeperCurrentPets() → publishCompletedEvent() → broadcastOrderChange()
     *
     * @param userId  用户ID
     * @param orderNo 订单编号
     */
    @Transactional
    @Override
    public void completeOrder(Long userId, String orderNo) {
        PetOrder order = getByOrderNo(orderNo);
        checkMerchantOrKeeperOrderAccess(order, userId);
        keeperAttendanceService.requireKeeperOnDuty(order.getKeeper_id_wsh(), order.getMerchant_id_wsh());
        if (!OrderStatus.IN_PROGRESS.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "当前订单状态无法完成");
        }
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
        guardManualStatusUpdate(order, status);
        if (BOOKING_STATUSES.contains(status)) {
            ensureNoPetDateConflict(order.getPet_id_wsh(), order.getStart_date_wsh(), order.getEnd_date_wsh(), order.getId_wsh());
            Keeper keeper = keeperMapper.selectById(order.getKeeper_id_wsh());
            ensureKeeperCapacity(order.getKeeper_id_wsh(), order.getStart_date_wsh(), order.getEnd_date_wsh(),
                    order.getId_wsh(), keeper != null ? keeper.getMax_pets_wsh() : null);
            keeperLeaveService.requireKeeperAvailable(order.getKeeper_id_wsh(), order.getStart_date_wsh(), order.getEnd_date_wsh());
        }
        String previousStatus = order.getStatus_wsh();
        order.setStatus_wsh(status);
        stampStatusTime(order, status);
        int updated = orderMapper.update(order, new LambdaUpdateWrapper<PetOrder>()
                .eq(PetOrder::getId_wsh, order.getId_wsh())
                .eq(PetOrder::getStatus_wsh, previousStatus));
        if (updated == 0) {
            throw new BusinessException(400, "Order status changed, cannot update manually");
        }
        if (OrderStatus.CANCELLED.equals(status)) {
            couponService.releaseForOrder(order.getId_wsh());
            membershipBenefitService.releaseForOrder(order.getId_wsh());
        }
        if (!OrderStatus.COMPLETED.equals(previousStatus) && OrderStatus.COMPLETED.equals(status)) {
            settleOrderToMerchant(order);
        }
        refreshKeeperCurrentPets(order.getKeeper_id_wsh());
        if (!OrderStatus.COMPLETED.equals(previousStatus) && OrderStatus.COMPLETED.equals(status)) {
            publishCompletedEvent(order);
        }
        broadcastOrderChange(order);
    }

    /**
     * Guard rails for manual status updates. Prevents direct status transitions that
     * involve accounting-sensitive states (PAID, COMPLETED, CANCELLED, REFUNDING, REFUNDED).
     * Only allows transitions within the fulfillment flow statuses.
     *
     * @param order        the current order
     * @param targetStatus the proposed target status
     * @throws BusinessException if the target status is accounting-managed or outside
     *                           the fulfillment flow
     */
    private void guardManualStatusUpdate(PetOrder order, String targetStatus) {
        if (Objects.equals(order.getStatus_wsh(), targetStatus)) {
            return;
        }
        if (ACCOUNTING_MANAGED_STATUSES.contains(targetStatus)) {
            throw new BusinessException(400, "请使用支付、取消、完成或退款等专用接口变更资金相关状态");
        }
        if (!PAID_FLOW_STATUSES.contains(order.getStatus_wsh()) || !PAID_FLOW_STATUSES.contains(targetStatus)) {
            throw new BusinessException(400, "通用状态接口仅允许修正履约中的非资金状态");
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
     * 校验服务计费单元必须为 day/天（本期仅支持按天寄养）。
     *
     * @param service 服务项
     * @throws BusinessException 计费单位不是 day/天 时抛出 UNSUPPORTED_SERVICE_UNIT
     */
    static void assertServiceUnitSupported(ServiceItem service) {
        if (service.getUnit_wsh() == null
                || !("day".equals(service.getUnit_wsh()) || "天".equals(service.getUnit_wsh()))) {
            throw new BusinessException(400, BookingErrorCode.UNSUPPORTED_SERVICE_UNIT, "服务计费单位不是 day/天，本期不支持");
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
    static BigDecimal resolvePricePerDay(ServiceItem service, Keeper keeper) {
        if (service != null && service.getPrice_wsh() != null) {
            return service.getPrice_wsh();
        }
        return keeper.getPrice_per_day_wsh();
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
     * Validates the service date range: start date must be today or later,
     * end date must be after start date, and the duration must not exceed 365 days.
     *
     * @param startDate the service start date
     * @param endDate   the service end date
     * @return the number of days between start and end
     * @throws BusinessException if dates are invalid or out of range
     */
    private int validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessException(400, "开始日期和结束日期为必填项");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new BusinessException(400, "开始日期不能是过去的时间");
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        if (days <= 0) {
            throw new BusinessException(400, "结束日期必须在开始日期之后");
        }
        if (days > 365) {
            throw new BusinessException(400, "订单天数不能超过365天");
        }
        return (int) days;
    }

    /**
     * Calculates a long-stay discount based on the number of service days.
     * <ul>
     *   <li>30+ days: 10% discount</li>
     *   <li>7-29 days: 5% discount</li>
     *   <li>Below 7 days: no discount</li>
     * </ul>
     *
     * @param totalAmount the total amount before discount
     * @param days        the number of service days
     * @return the discount amount, or zero if not eligible
     */
    private BigDecimal calculateDiscount(BigDecimal totalAmount, int days) {
        if (days >= 30) {
            return totalAmount.multiply(BigDecimal.valueOf(0.1));
        }
        if (days >= 7) {
            return totalAmount.multiply(BigDecimal.valueOf(0.05));
        }
        return BigDecimal.ZERO;
    }

    /**
     * 验证时间范围
     *
     * @param startDate      开始日期
     * @param endDate        结束日期
     * @param deliveryTime   配送时间
     * @param receiverStart  接单开始时间
     * @param receiverEnd    接单结束时间
     * @param pickupTime     取货时间
     * @param merchantId     商家ID（用于解析目标日期营业时段）
     */
    private void validateFulfillmentWindow(LocalDate startDate,
                                           LocalDate endDate,
                                           LocalDateTime deliveryTime,
                                           LocalDateTime receiverStart,
                                           LocalDateTime receiverEnd,
                                           LocalDateTime pickupTime,
                                           Long merchantId) {
        LocalDateTime serviceStart = startDate.atStartOfDay();
        LocalDateTime serviceEndExclusive = endDate.plusDays(1).atStartOfDay();
        if (deliveryTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException(400, "配送时间不能是过去的时间");
        }
        if (deliveryTime.isBefore(serviceStart) || !deliveryTime.isBefore(serviceEndExclusive)) {
            throw new BusinessException(400, "配送时间必须在服务日期范围内");
        }
        if (!receiverEnd.isAfter(receiverStart)) {
            throw new BusinessException(400, "接收结束时间必须在接收开始时间之后");
        }
        if (deliveryTime.isBefore(receiverStart) || deliveryTime.isAfter(receiverEnd)) {
            throw new BusinessException(400, "配送时间必须在接收方可用的时间段内");
        }
        if (pickupTime != null && !pickupTime.isAfter(deliveryTime)) {
            throw new BusinessException(400, "接宠时间必须在送宠时间之后");
        }
        List<BusinessHours> hours = businessHoursService.getByMerchantId(merchantId);
        if (hours != null && !hours.isEmpty()) {
            if (!businessHoursTargetResolver.isWithinBusinessHours(hours, deliveryTime)) {
                throw new BusinessException(400, BookingErrorCode.FULFILLMENT_OUTSIDE_BUSINESS_HOURS,
                        "送达时间不在目标日期营业时段内");
            }
            if (pickupTime != null && !businessHoursTargetResolver.isWithinBusinessHours(hours, pickupTime)) {
                throw new BusinessException(400, BookingErrorCode.FULFILLMENT_OUTSIDE_BUSINESS_HOURS,
                        "接回时间不在目标日期营业时段内");
            }
        }
    }

    /**
     * 默认配送时间,用户开始送给keeper的时间
     */
    private LocalDateTime defaultDeliveryTime(OrderCreateRequestDTO request) {
        return request.getDelivery_time_wsh() != null ? request.getDelivery_time_wsh() : request.getStart_date_wsh().atTime(10, 0);
    }

    /**
     * 默认接收开始时间,keeper开始接收的时间
     */
    private LocalDateTime defaultReceiverStart(LocalDateTime deliveryTime) {
        return deliveryTime;
    }

    /**
     * 默认接收结束时间,超过了等待时间,keeper将不会接收
     */
    private LocalDateTime defaultReceiverEnd(LocalDateTime deliveryTime) {
        return deliveryTime.plusMinutes(RECEIVER_WINDOW_MINUTES);
    }

    /**
     * 默认接宠时间,用户开始去keeper那接宠的时间
     */
    private LocalDateTime defaultPickupTime(OrderCreateRequestDTO request) {
        return request.getPickup_time_wsh() != null
                ? request.getPickup_time_wsh()
                : request.getEnd_date_wsh().atTime(18, 0);
    }

    /**
     * 确保宠物没有时间冲突
     */
    private void ensureNoPetDateConflict(Long petId, LocalDate startDate, LocalDate endDate, Long excludeOrderId) {
        LambdaQueryWrapper<PetOrder> wrapper = new LambdaQueryWrapper<PetOrder>()
                .eq(PetOrder::getPet_id_wsh, petId)
                .in(PetOrder::getStatus_wsh, BOOKING_STATUSES)
                .lt(PetOrder::getStart_date_wsh, endDate)
                .gt(PetOrder::getEnd_date_wsh, startDate)
                .last("LIMIT 1");
        if (excludeOrderId != null) {
            wrapper.ne(PetOrder::getId_wsh, excludeOrderId);
        }
        PetOrder conflict = orderMapper.selectOne(wrapper);
        if (conflict != null) {
            throw new BusinessException(400, "宠物存在冲突订单: " + conflict.getOrder_no_wsh());
        }
    }

    /**
     * 确保keeper有空闲
     * @param keeperId          keeperId
     * @param startDate         开始日期
     * @param endDate           结束日期
     * @param excludeOrderId    排除订单Id
     * @param maxPets           最大宠物数
     */
    private void ensureKeeperCapacity(Long keeperId, LocalDate startDate, LocalDate endDate,
                                      Long excludeOrderId, Integer maxPets) {
        int capacity = maxPets == null ? 0 : maxPets;
        if (capacity <= 0) {
            throw new BusinessException(400, "看护者容量不足");
        }
        // 数据库行级锁：在事务内锁定看护者行，使同一看护者的容量校验+订单落库在
        // 数据库层面串行化，避免多实例并发下 selectCount 然后 insert 之间的超卖窗口。
        keeperMapper.selectByIdForUpdate(keeperId);
        LambdaQueryWrapper<PetOrder> wrapper = new LambdaQueryWrapper<PetOrder>()
                .eq(PetOrder::getKeeper_id_wsh, keeperId)
                .in(PetOrder::getStatus_wsh, BOOKING_STATUSES)
                .lt(PetOrder::getStart_date_wsh, endDate)
                .gt(PetOrder::getEnd_date_wsh, startDate);
        if (excludeOrderId != null) {
            wrapper.ne(PetOrder::getId_wsh, excludeOrderId);
        }
        Long count = orderMapper.selectCount(wrapper);
        if (count != null && count >= capacity) {
            throw new BusinessException(400, BookingErrorCode.CAPACITY_EXCEEDED, "看护者排班与已有订单冲突");
        }
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
        LocalDate today = LocalDate.now();
        Long current = orderMapper.selectCount(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getKeeper_id_wsh, keeperId)
                        .in(PetOrder::getStatus_wsh, CURRENT_PET_STATUSES)
                        .le(PetOrder::getStart_date_wsh, today)
                        .gt(PetOrder::getEnd_date_wsh, today));
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

    private void checkOrderAccess(PetOrder order, Long userId) {
        if (order.getOwner_id_wsh() != null && order.getOwner_id_wsh().equals(userId)) {
            return;
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

    private BigDecimal defaultMoney(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

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
        return orders.stream()
                .map(order -> {
                    OrderDTO dto = toDTO(order);
                    fillDTO(dto, order, serviceMap, userMap, petMap, keeperMap, merchantMap);
                    dto.setSnapshot_wsh(snapshotMap.get(order.getId_wsh()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // 批量获取id
    private Set<Long> collectIds(List<PetOrder> orders, Function<PetOrder, Long> idGetter) {
        return orders.stream()
                .map(idGetter)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Map<Long, ServiceItem> loadServiceMap(Set<Long> serviceIds) {
        if (serviceIds.isEmpty()) {
            return Map.of();
        }
        return serviceItemMapper.selectList(new LambdaQueryWrapper<ServiceItem>()
                        .select(ServiceItem::getId_wsh, ServiceItem::getName_wsh, ServiceItem::getDescription_wsh)
                        .in(ServiceItem::getId_wsh, serviceIds))
                .stream()
                .collect(Collectors.toMap(ServiceItem::getId_wsh, Function.identity()));
    }

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
        }
        User user = userMap.get(order.getOwner_id_wsh());
        if (user != null) {
            dto.setOwner_name_wsh(defaultText(user.getNickname_wsh(), user.getUsername_wsh()));
        }
        Pet pet = petMap.get(order.getPet_id_wsh());
        if (pet != null) {
            dto.setPet_name_wsh(pet.getName_wsh());
        }
        Keeper keeper = keeperMap.get(order.getKeeper_id_wsh());
        if (keeper != null) {
            dto.setKeeper_name_wsh(keeper.getName_wsh());
            dto.setKeeper_phone_wsh(keeper.getPhone_wsh());
            dto.setKeeper_avatar_wsh(keeper.getAvatar_wsh());
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
        if (OrderStatus.DELIVERED.equals(status) && order.getDelivered_at_wsh() == null) {
            order.setDelivered_at_wsh(now);
        } else if (OrderStatus.RECEIVED.equals(status) && order.getReceived_at_wsh() == null) {
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

    private boolean confirmPaidOrder(PetOrder order) {
        requireFutureBookingEligibleMerchant(order.getMerchant_id_wsh());
        Keeper keeper = requireFutureBookableKeeper(order.getKeeper_id_wsh());
        validateKeeperQualification(keeper.getId_wsh());
        ensureKeeperCapacity(keeper.getId_wsh(), order.getStart_date_wsh(), order.getEnd_date_wsh(),
                order.getId_wsh(), keeper.getMax_pets_wsh());
        keeperLeaveService.requireKeeperAvailable(keeper.getId_wsh(), order.getStart_date_wsh(), order.getEnd_date_wsh());

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

    private String defaultText(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
