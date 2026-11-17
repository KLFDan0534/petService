package com.pet.admin.statistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import com.pet.common.OrderStatus;
import com.pet.order.dto.OrderDTO;
import com.pet.order.entity.PetOrder;
import com.pet.order.entity.Tip;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.mapper.TipMapper;
import com.pet.order.service.OrderService;
import com.pet.customer.entity.Complaint;
import com.pet.customer.entity.Rating;
import com.pet.customer.mapper.ComplaintMapper;
import com.pet.customer.mapper.RatingMapper;
import com.pet.pet.service.PetService;
import com.pet.boarding.service.KeeperService;
import com.pet.boarding.service.MerchantService;
import com.pet.system.service.UserService;
import com.pet.admin.statistics.service.StatisticsService;
import org.springframework.stereotype.Service;

import com.pet.admin.statistics.vo.AdminDashboardVO;
import com.pet.admin.statistics.vo.MerchantDashboardVO;
import com.pet.admin.statistics.vo.ReputationStatsVO;
import com.pet.admin.statistics.vo.UserDashboardVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    private static final Set<String> REVENUE_STATUSES = Set.of(
            OrderStatus.PAID,
            OrderStatus.CONFIRMED,
            OrderStatus.RECEIVED,
            OrderStatus.IN_PROGRESS,
            OrderStatus.COMPLETED);

    private static final Set<String> ACTIVE_ORDER_STATUSES = Set.of(
            OrderStatus.PAID,
            OrderStatus.CONFIRMED,
            OrderStatus.RECEIVED,
            OrderStatus.IN_PROGRESS);

    /** 宠物实际在店的订单状态：已交接进店到服务完成之间 */
    private static final Set<String> SERVING_STATUSES = Set.of(
            OrderStatus.RECEIVED,
            OrderStatus.IN_PROGRESS);

    private final UserService userService;
    private final PetService petService;
    private final MerchantService merchantService;
    private final KeeperService keeperService;
    private final OrderService orderService;
    private final RatingMapper ratingMapper;
    private final ComplaintMapper complaintMapper;
    private final OrderMapper orderMapper;
    private final TipMapper tipMapper;

    /**
     * 业务作用：注入用户、宠物、商家、看护者、订单服务以及评价、投诉、订单、打赏四个 Mapper。
     * 注意事项：直接注入 Mapper 用于跨模块统计，避免为聚合查询新增 Service 方法。
     */
    public StatisticsServiceImpl(UserService userService, PetService petService,
                                  MerchantService merchantService, KeeperService keeperService,
                                  OrderService orderService,
                                  RatingMapper ratingMapper,
                                  ComplaintMapper complaintMapper,
                                  OrderMapper orderMapper,
                                  TipMapper tipMapper) {
        this.userService = userService;
        this.petService = petService;
        this.merchantService = merchantService;
        this.keeperService = keeperService;
        this.orderService = orderService;
        this.ratingMapper = ratingMapper;
        this.complaintMapper = complaintMapper;
        this.orderMapper = orderMapper;
        this.tipMapper = tipMapper;
    }

    /**
     * Computes the admin dashboard with aggregated platform statistics
     * including user, pet, merchant, and keeper counts, total orders,
     * total revenue, pending orders, and completed orders.
     *
     * @return the admin dashboard VO
     */
    /**
     * 管理员全局数据看板实现
     *
     * 业务作用：
     * 从各Service获取全量数据，聚合计算平台运营概览指标。
     * 数据来源：UserService.listAll() / PetService.listAll() / MerchantService.listAll() / KeeperService.listAll() / OrderService.listAll()
     * 业务规则：isRevenueOrder判断——订单状态在REVENUE_STATUSES集合中（PAID/CONFIRMED/RECEIVED/IN_PROGRESS/COMPLETED）。
     * 注意事项：TODO 前端返回数据太久了，需要优化——全量listAll在大数据量下存在性能瓶颈。
     */
    @Override
    public AdminDashboardVO getAdminDashboard() {
        AdminDashboardVO stats = new AdminDashboardVO();
        // 全量拉取后计数，数据量大时是瓶颈
        stats.setTotal_users_wsh(userService.listAll().size());
        stats.setTotal_pets_wsh(petService.listAll().size());
        stats.setTotal_merchants_wsh(merchantService.listAll().size());
        stats.setTotal_keepers_wsh(keeperService.listAll().size());

        List<OrderDTO> allOrders = orderService.listAll();
        stats.setTotal_orders_wsh(allOrders.size());

        // 营收只统计已支付之后的订单状态
        BigDecimal totalRevenue = allOrders.stream()
                .filter(this::isRevenueOrder)
                .map(this::finalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotal_revenue_wsh(totalRevenue);

        long pendingOrders = allOrders.stream()
                .filter(o -> OrderStatus.PENDING.equals(o.getStatus_wsh())).count();
        stats.setPending_orders_wsh(pendingOrders);

        long completedOrders = allOrders.stream()
                .filter(o -> OrderStatus.COMPLETED.equals(o.getStatus_wsh())).count();
        stats.setCompleted_orders_wsh(completedOrders);

        return stats;
    }

    /**
     * Computes the user dashboard showing pet count, active/completed
     * orders, and total spending across all orders.
     *
     * @param userId the user ID
     * @return the user dashboard VO
     */
    /**
     * 用户个人数据看板实现
     *
     * 业务作用：统计用户个人数据——宠物数、进行中订单、已完成订单、累计消费。 数据来源：PetService.getPetsByOwner() / OrderService.listByOwner()
     * 业务规则：isActiveOrder判断——订单状态在ACTIVE_ORDER_STATUSES集合中（PAID/CONFIRMED/RECEIVED/IN_PROGRESS）。
     * 注意事项：仅限本人数据查询，Service层不跨用户。
     */
    @Override
    public UserDashboardVO getUserDashboard(Long userId) {
        UserDashboardVO stats = new UserDashboardVO();
        stats.setPets_wsh(petService.getPetsByOwner(userId).size());

        List<OrderDTO> orders = orderService.listByOwner(userId);

        long activeOrders = orders.stream()
                .filter(this::isActiveOrder)
                .count();
        long completedOrders = orders.stream()
                .filter(o -> OrderStatus.COMPLETED.equals(o.getStatus_wsh()))
                .count();
        // 累计消费按实付金额汇总
        BigDecimal totalSpent = orders.stream()
                .filter(this::isRevenueOrder)
                .map(this::finalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.setActive_orders_wsh(activeOrders);
        stats.setCompleted_orders_wsh(completedOrders);
        stats.setTotal_spent_wsh(totalSpent);

        return stats;
    }

    /**
     * Computes the merchant dashboard showing total orders, revenue,
     * pending/active/completed breakdown, and distinct pets served.
     *
     * @param merchantId the merchant ID
     * @return the merchant dashboard VO
     */
    /**
     * 商家数据看板实现
     *
     * 业务作用：统计商家经营数据——总订单数、总营收、待处理/进行中/已完成订单数、服务过的宠物种类数。 数据来源：OrderService.listByMerchant(merchantId)
     * 业务规则：营收状态集=REVENUE_STATUSES；进行中状态集=ACTIVE_ORDER_STATUSES。
     * 注意事项：merchantId需与登录用户关联，Controller层做权限控制。
     */
    @Override
    public MerchantDashboardVO getMerchantDashboard(Long merchantId) {
        MerchantDashboardVO stats = new MerchantDashboardVO();
        List<OrderDTO> orders = orderService.listByMerchant(merchantId);

        stats.setTotal_orders_wsh(orders.size());
        BigDecimal totalRevenue = orders.stream()
                .filter(this::isRevenueOrder)
                .map(this::finalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotal_revenue_wsh(totalRevenue);

        long pending = orders.stream().filter(o -> OrderStatus.PENDING.equals(o.getStatus_wsh())).count();
        long active = orders.stream().filter(this::isActiveOrder).count();
        long completed = orders.stream().filter(o -> OrderStatus.COMPLETED.equals(o.getStatus_wsh())).count();
        stats.setPending_orders_wsh(pending);
        stats.setActive_orders_wsh(active);
        stats.setCompleted_orders_wsh(completed);

        // 服务中的宠物：只算宠物已经进店的订单，按 pet_id 去重
        stats.setPets_wsh((int) orders.stream()
                .filter(this::isServingOrder)
                .map(OrderDTO::getPet_id_wsh)
                .distinct()
                .count());

        return stats;
    }

    /**
     * Computes reputation statistics for a merchant or keeper including
     * average rating, total ratings, completion rate, complaint rate,
     * and total tip count.
     *
     * @param targetType the target type ("merchant" or "keeper")
     * @param targetId   the target ID
     * @return the reputation stats VO
     */
    /**
     * 信誉统计看板实现
     *
     * 业务作用：统计商家或看护人的信誉数据——评价数/平均分、完成订单数/完成率、投诉率、打赏数。 数据来源：RatingMapper/OrderMapper/ComplaintMapper/TipMapper
     * 业务规则：targetType仅支持"merchant"或"keeper"；百分比计算使用HALF_UP四舍五入保留1位小数；total=0时百分比返回0.0。
     * 注意事项：打赏统计需先查询该目标的所有订单ID，再关联Tip表count。
     */
    @Override
    public ReputationStatsVO getReputationStats(String targetType, Long targetId) {
        String normalizedType = normalizeTargetType(targetType);
        ReputationStatsVO stats = new ReputationStatsVO();

        List<Rating> ratings = ratingMapper.selectList(new LambdaQueryWrapper<Rating>()
                .eq(Rating::getTarget_type_wsh, normalizedType)
                .eq(Rating::getTarget_id_wsh, targetId));
        stats.setTotal_ratings_wsh(ratings.size());
        stats.setAvg_rating_wsh(averageRating(ratings));

        Long totalOrders = orderMapper.selectCount(orderTargetWrapper(normalizedType, targetId));
        Long completedOrders = orderMapper.selectCount(orderTargetWrapper(normalizedType, targetId)
                .eq(PetOrder::getStatus_wsh, OrderStatus.COMPLETED));
        long orderCount = totalOrders == null ? 0 : totalOrders;
        long completedCount = completedOrders == null ? 0 : completedOrders;
        stats.setTotal_completed_wsh(completedCount);
        // 完成率以订单总数为分母
        stats.setCompletion_rate_wsh(percent(completedCount, orderCount));

        Long complaints = complaintMapper.selectCount(new LambdaQueryWrapper<Complaint>()
                .eq(Complaint::getTarget_type_wsh, normalizedType)
                .eq(Complaint::getTarget_id_wsh, targetId));
        // 投诉率同样以订单总数为分母
        stats.setComplaint_rate_wsh(percent(complaints == null ? 0 : complaints, orderCount));

        stats.setTotal_tips_wsh(totalTips(normalizedType, targetId));
        return stats;
    }

    /**
     * 业务作用：判断订单状态是否属于计入营收的状态集合。
     * 业务规则：状态集为 PAID、CONFIRMED、RECEIVED、IN_PROGRESS、COMPLETED。
     * 异常情况：order 为 null 返回 false。
     * 注意事项：退款中与已退款不计入营收，待付款与已取消同样排除。
     */
    private boolean isRevenueOrder(OrderDTO order) {
        return order != null && REVENUE_STATUSES.contains(order.getStatus_wsh());
    }

    /**
     * 业务作用：判断订单是否处于已付款到服务中之间的进行中状态。
     * 业务规则：状态集为 PAID、CONFIRMED、RECEIVED、IN_PROGRESS，不含 COMPLETED。
     * 异常情况：order 为 null 返回 false。
     * 注意事项：已完成订单单独统计，避免重复计入。
     */
    private boolean isActiveOrder(OrderDTO order) {
        return order != null && ACTIVE_ORDER_STATUSES.contains(order.getStatus_wsh());
    }

    /**
     * 业务作用：判断订单是否处于宠物已交接进店、尚未完成服务的状态。
     * 业务规则：status_wsh 命中 SERVING_STATUSES，即 RECEIVED、IN_PROGRESS。
     * 注意事项：待付款与已确认尚未进店，已完成与已取消不计入。
     */
    private boolean isServingOrder(OrderDTO order) {
        return order != null && SERVING_STATUSES.contains(order.getStatus_wsh());
    }

    /**
     * 业务作用：取出订单实付金额，空值统一按 0 处理。
     * 业务规则：以实付金额而非订单原价为统计口径。
     * 注意事项：order 为 null 时会抛空指针，调用前需确保订单非空。
     */
    private BigDecimal finalAmount(OrderDTO order) {
        return order.getFinal_amount_wsh() == null ? BigDecimal.ZERO : order.getFinal_amount_wsh();
    }

    /**
     * 业务作用：校验并标准化目标类型，仅允许 merchant 与 keeper。
     * 业务规则：其他取值一律视为非法参数。
     * 异常情况：非 merchant/keeper → IllegalArgumentException。
     * 注意事项：抛的是 IllegalArgumentException，会被全局异常处理器按 500 处理。
     */
    private String normalizeTargetType(String targetType) {
        if ("merchant".equalsIgnoreCase(targetType)) {
            return "merchant";
        }
        if ("keeper".equalsIgnoreCase(targetType)) {
            return "keeper";
        }
        throw new IllegalArgumentException("targetType must be merchant or keeper");
    }

    /**
     * 业务作用：按目标类型生成订单查询条件，商家查 merchant_id，看护者查 keeper_id。
     * 业务规则：非 merchant 一律按 keeper 处理，不做二次校验。
     * 注意事项：调用前需先经 normalizeTargetType 校验。
     */
    private LambdaQueryWrapper<PetOrder> orderTargetWrapper(String targetType, Long targetId) {
        LambdaQueryWrapper<PetOrder> wrapper = new LambdaQueryWrapper<>();
        if ("merchant".equals(targetType)) {
            wrapper.eq(PetOrder::getMerchant_id_wsh, targetId);
        } else {
            wrapper.eq(PetOrder::getKeeper_id_wsh, targetId);
        }
        return wrapper;
    }

    /**
     * 业务作用：根据评价列表计算平均分，保留一位小数。
     * 业务规则：空列表返回 0.0，舍入方式为 HALF_UP。
     * 注意事项：null 评分按 0 计入会拉低均值，需与评价写入逻辑保持一致。
     */
    private BigDecimal averageRating(List<Rating> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            return BigDecimal.ZERO.setScale(1);
        }
        // 评分为空按 0 计入，避免空指针
        BigDecimal total = ratings.stream()
                .map(r -> BigDecimal.valueOf(r.getScore_wsh() == null ? 0 : r.getScore_wsh()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(ratings.size()), 1, RoundingMode.HALF_UP);
    }

    /**
     * 业务作用：计算部分占整体的百分比，保留一位小数。
     * 业务规则：total 小于等于 0 时返回 0.0，舍入方式为 HALF_UP。
     * 注意事项：不做上限截断，投诉率理论上可能超过 100。
     */
    private BigDecimal percent(long part, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    /**
     * 业务作用：统计某个商家或看护者名下订单获得的打赏条数。
     * 业务规则：无订单时直接返回 0。
     * 异常情况：count 为 null 时按 0 处理。
     * 注意事项：订单量大时 in 条件会很长，后续可改为联表统计。
     */
    private long totalTips(String targetType, Long targetId) {
        List<PetOrder> orders = orderMapper.selectList(orderTargetWrapper(targetType, targetId)
                .select(PetOrder::getId_wsh));
        List<Long> orderIds = orders.stream().map(PetOrder::getId_wsh).toList();
        // 无订单时无需再查打赏表
        if (orderIds.isEmpty()) {
            return 0;
        }
        Long count = tipMapper.selectCount(new LambdaQueryWrapper<Tip>()
                .in(Tip::getOrder_id_wsh, orderIds));
        return count == null ? 0 : count;
    }
}
