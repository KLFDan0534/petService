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

    @Override
    public AdminDashboardVO getAdminDashboard() {
        AdminDashboardVO stats = new AdminDashboardVO();
        stats.setTotalUsers(userService.listAll().size());
        stats.setTotalPets(petService.listAll().size());
        stats.setTotalMerchants(merchantService.listAll().size());
        stats.setTotalKeepers(keeperService.listAll().size());

        List<OrderDTO> allOrders = orderService.listAll();
        stats.setTotalOrders(allOrders.size());

        BigDecimal totalRevenue = allOrders.stream()
                .filter(this::isRevenueOrder)
                .map(this::finalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalRevenue(totalRevenue);

        long pendingOrders = allOrders.stream()
                .filter(o -> OrderStatus.PENDING.equals(o.getStatus_wsh())).count();
        stats.setPendingOrders(pendingOrders);

        long completedOrders = allOrders.stream()
                .filter(o -> OrderStatus.COMPLETED.equals(o.getStatus_wsh())).count();
        stats.setCompletedOrders(completedOrders);

        return stats;
    }

    @Override
    public UserDashboardVO getUserDashboard(Long userId) {
        UserDashboardVO stats = new UserDashboardVO();
        stats.setPets(petService.getPetsByOwner(userId).size());

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

        stats.setActiveOrders(activeOrders);
        stats.setCompletedOrders(completedOrders);
        stats.setTotalSpent(totalSpent);

        return stats;
    }

    @Override
    public MerchantDashboardVO getMerchantDashboard(Long merchantId) {
        MerchantDashboardVO stats = new MerchantDashboardVO();
        List<OrderDTO> orders = orderService.listByMerchant(merchantId);

        stats.setTotalOrders(orders.size());
        BigDecimal totalRevenue = orders.stream()
                .filter(this::isRevenueOrder)
                .map(this::finalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalRevenue(totalRevenue);

        long pending = orders.stream().filter(o -> OrderStatus.PENDING.equals(o.getStatus_wsh())).count();
        long active = orders.stream().filter(this::isActiveOrder).count();
        long completed = orders.stream().filter(o -> OrderStatus.COMPLETED.equals(o.getStatus_wsh())).count();
        stats.setPendingOrders(pending);
        stats.setActiveOrders(active);
        stats.setCompletedOrders(completed);

        stats.setPets((int) orders.stream().map(OrderDTO::getPet_id_wsh).distinct().count());

        return stats;
    }

    @Override
    public ReputationStatsVO getReputationStats(String targetType, Long targetId) {
        String normalizedType = normalizeTargetType(targetType);
        ReputationStatsVO stats = new ReputationStatsVO();

        List<Rating> ratings = ratingMapper.selectList(new LambdaQueryWrapper<Rating>()
                .eq(Rating::getTarget_type_wsh, normalizedType)
                .eq(Rating::getTarget_id_wsh, targetId));
        stats.setTotalRatings(ratings.size());
        stats.setAvgRating(averageRating(ratings));

        Long totalOrders = orderMapper.selectCount(orderTargetWrapper(normalizedType, targetId));
        Long completedOrders = orderMapper.selectCount(orderTargetWrapper(normalizedType, targetId)
                .eq(PetOrder::getStatus_wsh, OrderStatus.COMPLETED));
        long orderCount = totalOrders == null ? 0 : totalOrders;
        long completedCount = completedOrders == null ? 0 : completedOrders;
        stats.setTotalCompleted(completedCount);
        stats.setCompletionRate(percent(completedCount, orderCount));

        Long complaints = complaintMapper.selectCount(new LambdaQueryWrapper<Complaint>()
                .eq(Complaint::getTarget_type_wsh, normalizedType)
                .eq(Complaint::getTarget_id_wsh, targetId));
        stats.setComplaintRate(percent(complaints == null ? 0 : complaints, orderCount));

        stats.setTotalTips(totalTips(normalizedType, targetId));
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
