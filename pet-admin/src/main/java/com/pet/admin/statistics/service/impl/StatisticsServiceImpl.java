package com.pet.admin.statistics.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.OrderStatus;
import com.pet.order.entity.PetOrder;
import com.pet.order.service.OrderService;
import com.pet.pet.service.PetService;
import com.pet.boarding.service.KeeperService;
import com.pet.boarding.service.MerchantService;
import com.pet.system.service.UserService;
import com.pet.admin.statistics.service.StatisticsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    private final UserService userService;
    private final PetService petService;
    private final MerchantService merchantService;
    private final KeeperService keeperService;
    private final OrderService orderService;

    public StatisticsServiceImpl(UserService userService, PetService petService,
                                 MerchantService merchantService, KeeperService keeperService,
                                 OrderService orderService) {
        this.userService = userService;
        this.petService = petService;
        this.merchantService = merchantService;
        this.keeperService = keeperService;
        this.orderService = orderService;
    }

    @Override
    public Map<String, Object> getAdminDashboard() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userService.listAll().size());
        stats.put("totalPets", petService.listAll().size());
        stats.put("totalMerchants", merchantService.listAll().size());
        stats.put("totalKeepers", keeperService.listAll().size());

        List<PetOrder> allOrders = orderService.listAll();
        stats.put("totalOrders", allOrders.size());

        BigDecimal totalRevenue = allOrders.stream()
                .filter(o -> OrderStatus.PAID.equals(o.getStatus_wsh()) || OrderStatus.COMPLETED.equals(o.getStatus_wsh()))
                .map(PetOrder::getFinal_amount_wsh)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalRevenue", totalRevenue);

        long pendingOrders = allOrders.stream()
                .filter(o -> OrderStatus.PENDING.equals(o.getStatus_wsh())).count();
        stats.put("pendingOrders", pendingOrders);

        long completedOrders = allOrders.stream()
                .filter(o -> OrderStatus.COMPLETED.equals(o.getStatus_wsh())).count();
        stats.put("completedOrders", completedOrders);

        return stats;
    }

    @Override
    public Map<String, Object> getUserDashboard(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pets", petService.getPetsByOwner(userId).size());

        List<PetOrder> orders = orderService.listByOwner(userId);

        long activeOrders = orders.stream()
                .filter(o -> OrderStatus.PAID.equals(o.getStatus_wsh()) || OrderStatus.IN_PROGRESS.equals(o.getStatus_wsh()))
                .count();
        long completedOrders = orders.stream()
                .filter(o -> OrderStatus.COMPLETED.equals(o.getStatus_wsh()))
                .count();
        BigDecimal totalSpent = orders.stream()
                .filter(o -> OrderStatus.PAID.equals(o.getStatus_wsh()) || OrderStatus.COMPLETED.equals(o.getStatus_wsh()))
                .map(PetOrder::getFinal_amount_wsh)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.put("activeOrders", activeOrders);
        stats.put("completedOrders", completedOrders);
        stats.put("totalSpent", totalSpent);

        return stats;
    }

    @Override
    public Map<String, Object> getMerchantDashboard(Long merchantId) {
        Map<String, Object> stats = new HashMap<>();
        List<PetOrder> orders = orderService.listByMerchant(merchantId);

        stats.put("totalOrders", orders.size());
        BigDecimal revenue = orders.stream()
                .filter(o -> OrderStatus.PAID.equals(o.getStatus_wsh()) || OrderStatus.COMPLETED.equals(o.getStatus_wsh()))
                .map(PetOrder::getFinal_amount_wsh)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("revenue", revenue);

        long pending = orders.stream().filter(o -> OrderStatus.PENDING.equals(o.getStatus_wsh())).count();
        long active = orders.stream().filter(o -> OrderStatus.PAID.equals(o.getStatus_wsh()) || OrderStatus.IN_PROGRESS.equals(o.getStatus_wsh())).count();
        stats.put("pendingOrders", pending);
        stats.put("activeOrders", active);

        return stats;
    }
}
