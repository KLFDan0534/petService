package com.pet.order.service.impl;

import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.mapper.ServiceItemMapper;
import com.pet.common.BusinessException;
import com.pet.common.OrderStatus;
import com.pet.common.StatusCode;
import com.pet.order.dto.CreateOrderRequest;
import com.pet.order.entity.PetOrder;
import com.pet.order.event.OrderCompletedEvent;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.service.OrderService;
import com.pet.pet.entity.Pet;
import com.pet.pet.mapper.PetMapper;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

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

    //
    private static final Set<String> BOOKING_STATUSES = Set.of(
            OrderStatus.PENDING, OrderStatus.PAID, OrderStatus.CONFIRMED,
            OrderStatus.DELIVERED, OrderStatus.RECEIVED, OrderStatus.IN_PROGRESS,
            OrderStatus.REFUNDING);

    private static final Set<String> CURRENT_PET_STATUSES = Set.of(
            OrderStatus.CONFIRMED, OrderStatus.DELIVERED, OrderStatus.RECEIVED, OrderStatus.IN_PROGRESS);

    private static final int RECEIVER_WINDOW_MINUTES = 10;
    private static final Random HANDOVER_CODE_RANDOM = new Random();

    private final OrderMapper orderMapper;
    private final PetMapper petMapper;
    private final KeeperMapper keeperMapper;
    private final MerchantMapper merchantMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    public OrderServiceImpl(OrderMapper orderMapper,
                            PetMapper petMapper,
                            KeeperMapper keeperMapper,
                            MerchantMapper merchantMapper,
                            ServiceItemMapper serviceItemMapper,
                            UserMapper userMapper,
                            ApplicationEventPublisher eventPublisher) {
        this.orderMapper = orderMapper;
        this.petMapper = petMapper;
        this.keeperMapper = keeperMapper;
        this.merchantMapper = merchantMapper;
        this.serviceItemMapper = serviceItemMapper;
        this.userMapper = userMapper;
        this.eventPublisher = eventPublisher;
    }


    /**
     * 获取所有订单列表
     * @return 订单列表
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Override
    public List<PetOrder> listAll() {
        List<PetOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>().orderByDesc(PetOrder::getCreated_at_wsh));
        orders.forEach(this::enrichOrder);
        return orders;
    }

    /**
     * 根据主人ID获取订单列表
     * @param ownerId 主人ID
     * @return 订单列表
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Override
    public List<PetOrder> listByOwner(Long ownerId) {
        List<PetOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getOwner_id_wsh, ownerId)
                        .orderByDesc(PetOrder::getCreated_at_wsh));
        orders.forEach(this::enrichOrder);
        return orders;
    }

    /**
     * 根据商家ID获取订单列表
     * @param merchantId 商家ID
     * @return 订单列表
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Override
    public List<PetOrder> listByMerchant(Long merchantId) {
        List<PetOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getMerchant_id_wsh, merchantId)
                        .orderByDesc(PetOrder::getCreated_at_wsh));
        orders.forEach(this::enrichOrder);
        orders.forEach(this::hideHandoverCode);
        return orders;
    }

    /**
     * 根据看护人ID获取订单列表
     * @param keeperId 看护人ID
     * @return 订单列表
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Override
    public List<PetOrder> listByKeeper(Long keeperId) {
        List<PetOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getKeeper_id_wsh, keeperId)
                        .notIn(PetOrder::getStatus_wsh, List.of(OrderStatus.PENDING, OrderStatus.PAID))
                        .orderByDesc(PetOrder::getCreated_at_wsh));
        orders.forEach(this::enrichOrder);
        orders.forEach(this::hideHandoverCode);
        return orders;
    }

    /**
     * 获取看护人待处理订单列表
     * @param keeperId 看护人ID
     * @return 待处理订单列表
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Override
    public List<PetOrder> listPendingByKeeper(Long keeperId) {
        List<PetOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getKeeper_id_wsh, keeperId)
                        .in(PetOrder::getStatus_wsh, List.of(OrderStatus.PENDING, OrderStatus.PAID))
                        .orderByDesc(PetOrder::getCreated_at_wsh));
        orders.forEach(this::enrichOrder);
        orders.forEach(this::hideHandoverCode);
        return orders;
    }

    /**
     * 根据订单编号获取订单
     * @param orderNo 订单编号
     * @return 订单实体
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Override
    public PetOrder getByOrderNo(String orderNo) {
        if (isBlank(orderNo)) {
            throw new BusinessException(400, "订单号不能为空");
        }
        PetOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<PetOrder>().eq(PetOrder::getOrder_no_wsh, orderNo));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        enrichOrder(order);
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
    public PetOrder getById(Long id) {
        PetOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        enrichOrder(order);
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
    public PetOrder createOrder(Long ownerId, CreateOrderRequest request) {

        Assert.notNull(request, "订单请求不能为空");

        // 获取到当前用户的下单宠物
        Pet pet = requirePet(ownerId, request.getPet_id_wsh());
        // 获取到当前用户的下单宠物的keeper
        Keeper keeper = requireExistingKeeper(request.getKeeper_id_wsh());
        // 获取到当前用户的下单宠物所属的merchant
        Merchant merchant = requireMerchant(request.getMerchant_id_wsh());

        // 服务判断
        validateKeeperMerchant(keeper, merchant);
        validateService(request.getService_id_wsh(), merchant.getId_wsh());

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

        BigDecimal totalAmount = keeper.getPrice_per_day_wsh().multiply(BigDecimal.valueOf(days));
        BigDecimal discount = calculateDiscount(totalAmount, days);

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
        order.setFinal_amount_wsh(totalAmount.subtract(discount));      //  实付(优惠-总价)
        order.setStatus_wsh(OrderStatus.PENDING);                       //  订单状态

        order.setHandover_code_wsh(generateHandoverCode());             //  交接码

        order.setDays_wsh(days);                                        //  抚养天数
        order.setDelivery_time_wsh(deliveryTime);                       //  宠物取送时间
        order.setReceiver_available_start_wsh(receiverStart);           //  接收者接收宠物的时间
        order.setReceiver_available_end_wsh(receiverEnd);               //  接收者接收宠物的结束时间
        order.setPickup_time_wsh(pickupTime);                           //  宠物取回时间
        order.setFinal_report_generated_wsh(0);                         //  是否生成了 boarding report
        orderMapper.insert(order);

        refreshKeeperCurrentPets(keeper.getId_wsh());
        enrichOrder(order);
        return order;
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
        order.setStatus_wsh(OrderStatus.CANCELLED);
        orderMapper.updateById(order);
        refreshKeeperCurrentPets(order.getKeeper_id_wsh());
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
        checkOrderAccess(order, userId);
        if (!OrderStatus.PENDING.equals(order.getStatus_wsh()) && !OrderStatus.PAID.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "订单状态为 " + order.getStatus_wsh() + "，无法再次接受");
        }
        Keeper keeper = requireKeeper(order.getKeeper_id_wsh());
        ensureKeeperCapacity(keeper.getId_wsh(), order.getStart_date_wsh(), order.getEnd_date_wsh(),
                order.getId_wsh(), keeper.getMax_pets_wsh());
        order.setStatus_wsh(OrderStatus.CONFIRMED);
        orderMapper.updateById(order);
        refreshKeeperCurrentPets(order.getKeeper_id_wsh());
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
        checkOrderAccess(order, userId);
        if (!OrderStatus.PENDING.equals(order.getStatus_wsh()) && !OrderStatus.PAID.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "当前订单状态无法拒单");
        }
        order.setStatus_wsh(OrderStatus.CANCELLED);
        orderMapper.updateById(order);
        refreshKeeperCurrentPets(order.getKeeper_id_wsh());
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
        PetOrder order = getByOrderNo(orderNo);
        if (!userId.equals(order.getOwner_id_wsh())) {
            throw new BusinessException(403, "只有宠物主人可以标记已送达");
        }
        if (!OrderStatus.CONFIRMED.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "当前订单状态无法标记已送达");
        }
        order.setStatus_wsh(OrderStatus.DELIVERED);
        order.setDelivered_at_wsh(LocalDateTime.now());
        orderMapper.updateById(order);
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
        PetOrder order = getByOrderNo(orderNo);
        checkOrderAccess(order, userId);
        if (!OrderStatus.DELIVERED.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "当前订单状态无法标记已接收");
        }
        if (isBlank(handoverCode)) {
            throw new BusinessException(400, "交接码不能为空");
        }
        if (!handoverCode.trim().equals(order.getHandover_code_wsh())) {
            throw new BusinessException(400, "交接码不正确");
        }
        order.setStatus_wsh(OrderStatus.RECEIVED);
        order.setReceived_at_wsh(LocalDateTime.now());
        orderMapper.updateById(order);
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
        checkOrderAccess(order, userId);
        if (!OrderStatus.RECEIVED.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "当前订单状态无法开始服务");
        }
        if (isBlank(startPhoto)) {
            throw new BusinessException(400, "开始照片不能为空");
        }
        order.setStarted_at_wsh(LocalDateTime.now());
        order.setStart_photo_wsh(startPhoto.trim());
        order.setStatus_wsh(OrderStatus.IN_PROGRESS);
        orderMapper.updateById(order);
        refreshKeeperCurrentPets(order.getKeeper_id_wsh());
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
        checkOrderAccess(order, userId);
        if (!OrderStatus.IN_PROGRESS.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "当前订单状态无法完成");
        }
        order.setStatus_wsh(OrderStatus.COMPLETED);
        order.setCompleted_at_wsh(LocalDateTime.now());
        orderMapper.updateById(order);
        refreshKeeperCurrentPets(order.getKeeper_id_wsh());
        publishCompletedEvent(order);
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
        if (BOOKING_STATUSES.contains(status)) {
            ensureNoPetDateConflict(order.getPet_id_wsh(), order.getStart_date_wsh(), order.getEnd_date_wsh(), order.getId_wsh());
            Keeper keeper = keeperMapper.selectById(order.getKeeper_id_wsh());
            ensureKeeperCapacity(order.getKeeper_id_wsh(), order.getStart_date_wsh(), order.getEnd_date_wsh(),
                    order.getId_wsh(), keeper != null ? keeper.getMax_pets_wsh() : null);
        }
        String previousStatus = order.getStatus_wsh();
        order.setStatus_wsh(status);
        stampStatusTime(order, status);
        orderMapper.updateById(order);
        refreshKeeperCurrentPets(order.getKeeper_id_wsh());
        if (!OrderStatus.COMPLETED.equals(previousStatus) && OrderStatus.COMPLETED.equals(status)) {
            publishCompletedEvent(order);
        }
    }

    /**
     * 生成测试订单数据
     * @param userId 用户ID
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Transactional
    @Override
    public void seedTestOrders(Long userId) {
        Keeper keeper = keeperMapper.selectOne(new LambdaQueryWrapper<Keeper>().last("LIMIT 1"));
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>().last("LIMIT 1"));
        ServiceItem service = serviceItemMapper.selectOne(new LambdaQueryWrapper<ServiceItem>().last("LIMIT 1"));
        if (keeper == null || merchant == null || service == null) {
            throw new BusinessException("缺少基础数据: 商户/看护者/服务");
        }

        Pet pet = petMapper.selectOne(
                new LambdaQueryWrapper<Pet>().eq(Pet::getOwner_id_wsh, userId).last("LIMIT 1"));
        if (pet == null) {
            pet = new Pet();
            pet.setOwner_id_wsh(userId);
            pet.setName_wsh("AdminPet");
            pet.setType_wsh("Dog");
            pet.setBreed_wsh("Teddy");
            pet.setAge_wsh(2);
            pet.setWeight_wsh(new BigDecimal("6.0"));
            pet.setGender_wsh(1);
            pet.setSterilized_wsh(1);
            pet.setVaccinated_wsh(1);
            pet.setDescription_wsh("Admin test pet");
            petMapper.insert(pet);
        }

        List<PetOrder> existing = orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>().eq(PetOrder::getOwner_id_wsh, userId));
        if (!existing.isEmpty()) {
            return;
        }

        String[][] data = {
                {"ORD_ADMIN_PENDING", OrderStatus.PENDING, "1", "3"},
                {"ORD_ADMIN_PAID", OrderStatus.PAID, "4", "6"},
                {"ORD_ADMIN_CONFIRMED", OrderStatus.CONFIRMED, "7", "9"},
                {"ORD_ADMIN_INPROGRESS", OrderStatus.IN_PROGRESS, "0", "3"},
                {"ORD_ADMIN_COMPLETED", OrderStatus.COMPLETED, "-5", "-2"},
        };
        for (String[] row : data) {
            LocalDate start = LocalDate.now().plusDays(Integer.parseInt(row[2]));
            LocalDate end = LocalDate.now().plusDays(Integer.parseInt(row[3]));
            int days = (int) ChronoUnit.DAYS.between(start, end);
            BigDecimal total = keeper.getPrice_per_day_wsh().multiply(BigDecimal.valueOf(days));

            PetOrder order = new PetOrder();
            order.setOrder_no_wsh(row[0]);
            order.setOwner_id_wsh(userId);
            order.setPet_id_wsh(pet.getId_wsh());
            order.setKeeper_id_wsh(keeper.getId_wsh());
            order.setMerchant_id_wsh(merchant.getId_wsh());
            order.setService_id_wsh(service.getId_wsh());
            order.setStart_date_wsh(start);
            order.setEnd_date_wsh(end);
            order.setDays_wsh(days);
            order.setPrice_per_day_wsh(keeper.getPrice_per_day_wsh());
            order.setTotal_amount_wsh(total);
            order.setDiscount_wsh(BigDecimal.ZERO);
            order.setFinal_amount_wsh(total);
            order.setStatus_wsh(row[1]);
            order.setHandover_code_wsh(generateHandoverCode());
            order.setDelivery_address_wsh(merchant.getAddress_wsh());
            order.setDelivery_time_wsh(start.atTime(9, 0));
            order.setReceiver_available_start_wsh(start.atTime(8, 0));
            order.setReceiver_available_end_wsh(start.atTime(20, 0));
            order.setPickup_address_wsh(merchant.getAddress_wsh());
            order.setPickup_time_wsh(end.atTime(18, 0));
            order.setFinal_report_generated_wsh(OrderStatus.COMPLETED.equals(row[1]) ? 1 : 0);
            stampStatusTime(order, row[1]);
            orderMapper.insert(order);
        }
        refreshKeeperCurrentPets(keeper.getId_wsh());
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
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(404, "商户不存在");
        }
        return merchant;
    }

    private void validateKeeperMerchant(Keeper keeper, Merchant merchant) {
        if (keeper.getMerchant_id_wsh() == null || !keeper.getMerchant_id_wsh().equals(merchant.getId_wsh())) {
            throw new BusinessException(400, "看护者不属于所选商户");
        }
    }

    private void validateService(Long serviceId, Long merchantId) {
        if (serviceId == null) {
            return;
        }
        ServiceItem service = serviceItemMapper.selectById(serviceId);
        if (service == null) {
            throw new BusinessException(400, "服务不存在");
        }
        if (service.getMerchant_id_wsh() != null && !service.getMerchant_id_wsh().equals(merchantId)) {
            throw new BusinessException(400, "服务不属于所选商户");
        }
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
    private LocalDateTime defaultDeliveryTime(CreateOrderRequest request) {
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
    private LocalDateTime defaultPickupTime(CreateOrderRequest request) {
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
        keeperMapper.updateById(keeper);
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

    private void enrichOrder(PetOrder order) {
        if (order == null) {
            return;
        }
        if (order.getService_id_wsh() != null) {
            ServiceItem service = serviceItemMapper.selectById(order.getService_id_wsh());
            if (service != null) {
                order.setService_name_wsh(service.getName_wsh());
                order.setService_description_wsh(service.getDescription_wsh());
            }
        }
        if (order.getOwner_id_wsh() != null) {
            User user = userMapper.selectById(order.getOwner_id_wsh());
            if (user != null) {
                order.setOwner_name_wsh(user.getNickname_wsh() != null ? user.getNickname_wsh() : user.getUsername_wsh());
            }
        }
        if (order.getPet_id_wsh() != null) {
            Pet pet = petMapper.selectById(order.getPet_id_wsh());
            if (pet != null) {
                order.setPet_name_wsh(pet.getName_wsh());
            }
        }
        if (order.getKeeper_id_wsh() != null) {
            Keeper keeper = keeperMapper.selectById(order.getKeeper_id_wsh());
            if (keeper != null) {
                order.setKeeper_name_wsh(keeper.getName_wsh());
                order.setKeeper_phone_wsh(keeper.getPhone_wsh());
                order.setKeeper_avatar_wsh(keeper.getAvatar_wsh());
            }
        }
        if (order.getMerchant_id_wsh() != null) {
            Merchant merchant = merchantMapper.selectById(order.getMerchant_id_wsh());
            if (merchant != null) {
                order.setMerchant_name_wsh(merchant.getName_wsh());
                order.setMerchant_phone_wsh(merchant.getPhone_wsh());
                order.setMerchant_address_wsh(merchant.getAddress_wsh());
            }
        }
    }

    private void hideHandoverCode(PetOrder order) {
        if (order != null) {
            order.setHandover_code_wsh(null);
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
