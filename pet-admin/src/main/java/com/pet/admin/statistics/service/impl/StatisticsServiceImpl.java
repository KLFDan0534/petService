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
            OrderStatus.DELIVERED,
            OrderStatus.RECEIVED,
            OrderStatus.IN_PROGRESS,
            OrderStatus.COMPLETED);

    private static final Set<String> ACTIVE_ORDER_STATUSES = Set.of(
            OrderStatus.PAID,
            OrderStatus.CONFIRMED,
            OrderStatus.DELIVERED,
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
     * 【业务名称】管理员全局数据看板实现
     * <p>业务作用：从各Service获取全量数据，聚合计算平台运营概览指标。</p>
     * <p>数据来源：UserService.listAll() / PetService.listAll() / MerchantService.listAll() / KeeperService.listAll() / OrderService.listAll()</p>
     * <p>数据处理：listAll()获取全量数据后用size()计数；订单流式filter(isRevenueOrder)+map(finalAmount)+reduce求和；按状态filter统计pending/completed。</p>
     * <p>业务规则：isRevenueOrder判断——订单状态在REVENUE_STATUSES集合中（PAID/CONFIRMED/DELIVERED/RECEIVED/IN_PROGRESS/COMPLETED）。</p>
     * <p>注意事项：TODO 前端返回数据太久了，需要优化——全量listAll在大数据量下存在性能瓶颈。</p>
     */
    @Override
    public AdminDashboardVO getAdminDashboard() {
        AdminDashboardVO stats = new AdminDashboardVO();
        stats.setTotal_users_wsh(userService.listAll().size());
        stats.setTotal_pets_wsh(petService.listAll().size());
        stats.setTotal_merchants_wsh(merchantService.listAll().size());
        stats.setTotal_keepers_wsh(keeperService.listAll().size());

        List<OrderDTO> allOrders = orderService.listAll();
        stats.setTotal_orders_wsh(allOrders.size());

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
     * 【业务名称】用户个人数据看板实现
     * <p>业务作用：统计用户个人数据——宠物数、进行中订单、已完成订单、累计消费。</p>
     * <p>数据来源：PetService.getPetsByOwner() / OrderService.listByOwner()</p>
     * <p>数据处理：getPetsByOwner获取宠物列表size()；listByOwner获取订单列表；isActiveOrder过滤进行中订单；isRevenueOrder+finalAmount计算累计消费。</p>
     * <p>业务规则：isActiveOrder判断——订单状态在ACTIVE_ORDER_STATUSES集合中（PAID/CONFIRMED/DELIVERED/RECEIVED/IN_PROGRESS）。</p>
     * <p>注意事项：仅限本人数据查询，Service层不跨用户。</p>
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
     * 【业务名称】商家数据看板实现
     * <p>业务作用：统计商家经营数据——总订单数、总营收、待处理/进行中/已完成订单数、服务过的宠物种类数。</p>
     * <p>数据来源：OrderService.listByMerchant(merchantId)</p>
     * <p>数据处理：listByMerchant获取商家关联订单；isRevenueOrder过滤+finalAmount求和统计营收；按订单状态分组统计pending/active/completed；distinct pet_id统计宠物种类数。</p>
     * <p>业务规则：营收状态集=REVENUE_STATUSES；进行中状态集=ACTIVE_ORDER_STATUSES。</p>
     * <p>注意事项：merchantId需与登录用户关联，Controller层做权限控制。</p>
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

        stats.setPets_wsh((int) orders.stream().map(OrderDTO::getPet_id_wsh).distinct().count());

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
     * 【业务名称】信誉统计看板实现
     * <p>业务作用：统计商家或看护人的信誉数据——评价数/平均分、完成订单数/完成率、投诉率、打赏数。</p>
     * <p>数据来源：RatingMapper/OrderMapper/ComplaintMapper/TipMapper</p>
     * <p>数据处理：normalizeTargetType校验并标准化targetType；Rating表按targetType+targetId查询计算平均分；Order表按merchant_id或keeper_id统计总订单数和已完成数计算完成率；Complaint表统计投诉数计算投诉率；Tip表通过订单ID关联统计打赏总数。</p>
     * <p>业务规则：targetType仅支持"merchant"或"keeper"；百分比计算使用HALF_UP四舍五入保留1位小数；total=0时百分比返回0.0。</p>
     * <p>注意事项：打赏统计需先查询该目标的所有订单ID，再关联Tip表count。</p>
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
        stats.setCompletion_rate_wsh(percent(completedCount, orderCount));

        Long complaints = complaintMapper.selectCount(new LambdaQueryWrapper<Complaint>()
                .eq(Complaint::getTarget_type_wsh, normalizedType)
                .eq(Complaint::getTarget_id_wsh, targetId));
        stats.setComplaint_rate_wsh(percent(complaints == null ? 0 : complaints, orderCount));

        stats.setTotal_tips_wsh(totalTips(normalizedType, targetId));
        return stats;
    }

    private boolean isRevenueOrder(OrderDTO order) {
        return order != null && REVENUE_STATUSES.contains(order.getStatus_wsh());
    }

    private boolean isActiveOrder(OrderDTO order) {
        return order != null && ACTIVE_ORDER_STATUSES.contains(order.getStatus_wsh());
    }

    private BigDecimal finalAmount(OrderDTO order) {
        return order.getFinal_amount_wsh() == null ? BigDecimal.ZERO : order.getFinal_amount_wsh();
    }

    private String normalizeTargetType(String targetType) {
        if ("merchant".equalsIgnoreCase(targetType)) {
            return "merchant";
        }
        if ("keeper".equalsIgnoreCase(targetType)) {
            return "keeper";
        }
        throw new IllegalArgumentException("targetType must be merchant or keeper");
    }

    private LambdaQueryWrapper<PetOrder> orderTargetWrapper(String targetType, Long targetId) {
        LambdaQueryWrapper<PetOrder> wrapper = new LambdaQueryWrapper<>();
        if ("merchant".equals(targetType)) {
            wrapper.eq(PetOrder::getMerchant_id_wsh, targetId);
        } else {
            wrapper.eq(PetOrder::getKeeper_id_wsh, targetId);
        }
        return wrapper;
    }

    private BigDecimal averageRating(List<Rating> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            return BigDecimal.ZERO.setScale(1);
        }
        BigDecimal total = ratings.stream()
                .map(r -> BigDecimal.valueOf(r.getScore_wsh() == null ? 0 : r.getScore_wsh()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(ratings.size()), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(long part, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    private long totalTips(String targetType, Long targetId) {
        List<PetOrder> orders = orderMapper.selectList(orderTargetWrapper(targetType, targetId)
                .select(PetOrder::getId_wsh));
        List<Long> orderIds = orders.stream().map(PetOrder::getId_wsh).toList();
        if (orderIds.isEmpty()) {
            return 0;
        }
        Long count = tipMapper.selectCount(new LambdaQueryWrapper<Tip>()
                .in(Tip::getOrder_id_wsh, orderIds));
        return count == null ? 0 : count;
    }
}
