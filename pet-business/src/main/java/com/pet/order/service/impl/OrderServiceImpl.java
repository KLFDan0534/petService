package com.pet.order.service.impl;

import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.mapper.ServiceItemMapper;
import com.pet.boarding.service.KeeperAttendanceService;
import com.pet.boarding.service.KeeperLeaveService;
import com.pet.boarding.service.MerchantService;
import com.pet.qualification.service.QualificationService;
import com.pet.common.BusinessException;
import com.pet.common.OrderStatus;
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
                            ObjectMapper objectMapper) {
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
    }


    /**
     * 获取所有订单列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> listAll() {
        List<PetOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>().orderByDesc(PetOrder::getCreated_at_wsh));
        return toDTOEnrichedList(orders);
    }

    /**
     * 根据主人ID获取订单列表
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
     * 根据商家ID获取订单列表
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
     * 根据看护人ID获取订单列表
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
     * 获取看护人待处理订单列表
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

    @Override
    public OrderDTO toDTO(PetOrder entity) {
        if (entity == null) return null;
        OrderDTO dto = new OrderDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public OrderDTO toDTOEnriched(PetOrder entity) {
        if (entity == null) return null;
        return toDTOEnrichedList(List.of(entity)).get(0);
    }

    @Transactional(readOnly = true)
    public OrderDTO getDTOById(Long id) {
        return toDTOEnriched(getById(id));
    }

    @Transactional(readOnly = true)
    public OrderDTO getDTOByOrderNo(String orderNo) {
        return toDTOEnriched(getByOrderNo(orderNo));
    }

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
     * 根据ID获取订单
     * @param id 订单ID
     * @return 订单实体
     * @author: wsh
     * @date: 2026/06/24 11:05
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
     * 创建订单
     * @param ownerId 主人ID
     * @param request 创建订单请求体
     * @return 创建后的订单
     * @author: wsh
     * @date: 2026/06/24 11:05
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
        Keeper keeper = requireExistingKeeper(request.getKeeper_id_wsh());
        // 获取到当前用户的下单宠物所属的merchant
        Merchant merchant = requireMerchant(request.getMerchant_id_wsh());

        // 服务判断
        validateKeeperMerchant(keeper, merchant);
        validateKeeperQualification(keeper.getId_wsh());
        ServiceItem service = validateService(request.getService_id_wsh(), merchant.getId_wsh());

        // 下单日期判断
        int days = validateDateRange(request.getStart_date_wsh(), request.getEnd_date_wsh());
        LocalDateTime deliveryTime = defaultDeliveryTime(request);
        LocalDateTime receiverStart = defaultReceiverStart(deliveryTime);
        LocalDateTime receiverEnd = defaultReceiverEnd(deliveryTime);
        LocalDateTime pickupTime = defaultPickupTime(request);
        validateFulfillmentWindow(request.getStart_date_wsh(), request.getEnd_date_wsh(),
                deliveryTime, receiverStart, receiverEnd, pickupTime);

        // 冲突判断
        ensureNoPetDateConflict(pet.getId_wsh(), request.getStart_date_wsh(), request.getEnd_date_wsh(), null);
        ensureKeeperCapacity(keeper.getId_wsh(), request.getStart_date_wsh(), request.getEnd_date_wsh(),
                null, keeper.getMax_pets_wsh());
        keeperLeaveService.requireKeeperAvailable(keeper.getId_wsh(), request.getStart_date_wsh(), request.getEnd_date_wsh());

        BigDecimal totalAmount = keeper.getPrice_per_day_wsh().multiply(BigDecimal.valueOf(days));
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

        order.setPrice_per_day_wsh(keeper.getPrice_per_day_wsh());      //  keepr价格
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
     * 根据ID取消订单
     * @param ownerId 主人ID
     * @param orderId 订单ID
     * @author: wsh
     * @date: 2026/06/24 11:05
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
     * 根据订单编号取消订单
     * @deprecated 请使用 cancelOrderById
     * @param ownerId 主人ID
     * @param orderNo 订单编号
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Deprecated
    @Transactional
    @Override
    public void cancelOrder(Long ownerId, String orderNo) {
        PetOrder order = getByOrderNo(orderNo);
        cancelOrderById(ownerId, order.getId_wsh());
    }

    /**
     * 接受订单
     * @param userId 用户ID
     * @param orderNo 订单编号
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Transactional
    @Override
    public void acceptOrder(Long userId, String orderNo) {
        PetOrder order = getByOrderNo(orderNo);
        Keeper keeper = requireKeeper(order.getKeeper_id_wsh());
        requireAssignedKeeperOrderAccess(order, keeper, userId);
        if (!OrderStatus.PAID.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "订单状态为 " + order.getStatus_wsh() + "，未支付订单不能接单");
        }
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
     * 拒绝订单
     * @param userId 用户ID
     * @param orderNo 订单编号
     * @author: wsh
     * @date: 2026/06/24 11:05
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
     * 标记订单已送达
     * @param userId 用户ID
     * @param orderNo 订单编号
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Transactional
    @Override
    public void markDelivered(Long userId, String orderNo) {
        OrderDeliveredRequestDTO request = new OrderDeliveredRequestDTO();
        request.setOrder_no_wsh(orderNo);
        markDelivered(userId, request);
    }

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
     * 标记订单已接收
     * @param userId 用户ID
     * @param orderNo 订单编号
     * @param handoverCode 交接码
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Transactional
    @Override
    public void markReceived(Long userId, String orderNo, String handoverCode) {
        OrderReceivedRequestDTO request = new OrderReceivedRequestDTO();
        request.setOrder_no_wsh(orderNo);
        request.setHandover_code_wsh(handoverCode);
        markReceived(userId, request);
    }

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
     * 开始服务
     * @param userId 用户ID
     * @param orderNo 订单编号
     * @param startPhoto 开始照片URL
     * @author: wsh
     * @date: 2026/06/24 11:05
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
     * 完成订单
     * @param userId 用户ID
     * @param orderNo 订单编号
     * @author: wsh
     * @date: 2026/06/24 11:05
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
     * 更新订单状态
     * @param orderNo 订单编号
     * @param status 新状态
     * @author: wsh
     * @date: 2026/06/24 11:05
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

    private Keeper requireKeeper(Long keeperId) {
        Keeper keeper = keeperMapper.selectById(keeperId);
        if (keeper == null) {
            throw new BusinessException(404, "看护者不存在");
        }
        if (keeper.getStatus_wsh() == null || keeper.getStatus_wsh() != StatusCode.KEEPER_ACTIVE.getValue()) {
            throw new BusinessException(400, "看护者未激活");
        }
        return keeper;
    }

    /**
     * 获取一个keeper对象，如果keeper不存在则返回null
     */
    private Keeper requireExistingKeeper(Long keeperId) {
        Keeper keeper = keeperMapper.selectById(keeperId);
        if (keeper == null) {
            throw new BusinessException(404, "看护者不存在");
        }
        return keeper;
    }

    private Merchant requireMerchant(Long merchantId) {
        merchantService.refreshStoreState(merchantId);
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(404, "商户不存在");
        }
        if (merchant.getStatus_wsh() == null
                || merchant.getStatus_wsh() != StatusCode.MERCHANT_APPROVED.getValue()) {
            throw new BusinessException(400, "商家未通过审核");
        }
        if (merchant.getStore_status_wsh() == null
                || merchant.getStore_status_wsh() != 1) {
            throw new BusinessException(400, "商家休息中，暂不接受订单");
        }
        return merchant;
    }

    private void validateKeeperMerchant(Keeper keeper, Merchant merchant) {
        if (keeper.getMerchant_id_wsh() == null || !keeper.getMerchant_id_wsh().equals(merchant.getId_wsh())) {
            throw new BusinessException(400, "看护者不属于所选商户");
        }
    }

    private void validateKeeperQualification(Long keeperId) {
        List<com.pet.qualification.dto.QualificationDTO> quals = qualificationService.listByOwner(
                QualificationService.OWNER_TYPE_KEEPER, keeperId, false);
        boolean hasApproved = quals.stream().anyMatch(
                q -> QualificationService.STATUS_APPROVED.equals(q.getStatus_wsh()));
        if (!hasApproved) {
            throw new BusinessException(400, "该看护者资质尚未通过审核，无法接单");
        }
    }

    private ServiceItem validateService(Long serviceId, Long merchantId) {
        if (serviceId == null) {
            return null;
        }
        ServiceItem service = serviceItemMapper.selectById(serviceId);
        if (service == null) {
            throw new BusinessException(400, "服务不存在");
        }
        if (service.getMerchant_id_wsh() != null && !service.getMerchant_id_wsh().equals(merchantId)) {
            throw new BusinessException(400, "服务不属于所选商户");
        }
        if (service.getStatus_wsh() == null || service.getStatus_wsh() != StatusCode.SERVICE_ENABLED.getValue()) {
            throw new BusinessException(400, "服务未上架");
        }
        return service;
    }

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
     */
    private void validateFulfillmentWindow(LocalDate startDate,
                                           LocalDate endDate,
                                           LocalDateTime deliveryTime,
                                           LocalDateTime receiverStart,
                                           LocalDateTime receiverEnd,
                                           LocalDateTime pickupTime) {
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
            throw new BusinessException(400, "看护者排班与已有订单冲突");
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
        if (keeper.getMax_pets_wsh() != null && keeper.getMax_pets_wsh() > 0) {
            if (current >= keeper.getMax_pets_wsh()) {
                if (keeper.getStatus_wsh() != StatusCode.KEEPER_BUSY.getValue()) {
                    keeper.setStatus_wsh(StatusCode.KEEPER_BUSY.getValue());
                }
            } else if (merchantOpen) {
                if (keeper.getStatus_wsh() != StatusCode.KEEPER_ACTIVE.getValue()) {
                    keeper.setStatus_wsh(StatusCode.KEEPER_ACTIVE.getValue());
                }
            } else if (keeper.getStatus_wsh() != StatusCode.KEEPER_OFFLINE.getValue()) {
                keeper.setStatus_wsh(StatusCode.KEEPER_OFFLINE.getValue());
            }
        } else if (merchantOpen) {
            if (keeper.getStatus_wsh() != StatusCode.KEEPER_ACTIVE.getValue()) {
                keeper.setStatus_wsh(StatusCode.KEEPER_ACTIVE.getValue());
            }
        } else if (keeper.getStatus_wsh() != StatusCode.KEEPER_OFFLINE.getValue()) {
            keeper.setStatus_wsh(StatusCode.KEEPER_OFFLINE.getValue());
        }
        keeperMapper.updateById(keeper);
    }


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


    // 订单详情
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
        ServiceItem service = serviceMap.get(order.getService_id_wsh());
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

    private void publishCompletedEvent(PetOrder order) {
        try {
            eventPublisher.publishEvent(new OrderCompletedEvent(
                    order.getId_wsh(), order.getPet_id_wsh(), order.getKeeper_id_wsh()));
        } catch (Exception e) {
            log.warn("发布订单完成事件失败，订单编号: {}", order.getOrder_no_wsh(), e);
        }
    }

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
        Keeper keeper = requireKeeper(order.getKeeper_id_wsh());
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

    private void broadcastOrderChange(PetOrder order) {
        try {
            orderStatusBroadcaster.broadcast(order);
        } catch (Exception e) {
            log.warn("广播订单状态失败，订单编号: {}", order.getOrder_no_wsh(), e);
        }
    }

    // 订单编号生成
    private String generateOrderNo() {
        String date = LocalDate.now().toString().replace("-", "");
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD" + date + uuid;
    }

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
