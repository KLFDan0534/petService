package com.pet.module.statistics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.module.keeper.mapper.KeeperMapper;
import com.pet.module.merchant.mapper.MerchantMapper;
import com.pet.module.order.mapper.OrderMapper;
import com.pet.module.order.entity.PetOrder;
import com.pet.module.pet.mapper.PetMapper;
import com.pet.module.user.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsService {

    private final UserMapper userMapper;
    private final PetMapper petMapper;
    private final MerchantMapper merchantMapper;
    private final KeeperMapper keeperMapper;
    private final OrderMapper orderMapper;

    public StatisticsService(UserMapper userMapper, PetMapper petMapper,
                             MerchantMapper merchantMapper, KeeperMapper keeperMapper,
                             OrderMapper orderMapper) {
        this.userMapper = userMapper;
        this.petMapper = petMapper;
        this.merchantMapper = merchantMapper;
        this.keeperMapper = keeperMapper;
        this.orderMapper = orderMapper;
    }

    public Map<String, Object> getAdminDashboard() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userMapper.selectCount(null));
        stats.put("totalPets", petMapper.selectCount(null));
        stats.put("totalMerchants", merchantMapper.selectCount(null));
        stats.put("totalKeepers", keeperMapper.selectCount(null));

        List<PetOrder> allOrders = orderMapper.selectList(null);
        stats.put("totalOrders", allOrders.size());

        BigDecimal totalRevenue = allOrders.stream()
                .filter(o -> "paid".equals(o.getStatus()) || "completed".equals(o.getStatus()))
                .map(PetOrder::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalRevenue", totalRevenue);

        long pendingOrders = allOrders.stream()
                .filter(o -> "pending".equals(o.getStatus())).count();
        stats.put("pendingOrders", pendingOrders);

        long completedOrders = allOrders.stream()
                .filter(o -> "completed".equals(o.getStatus())).count();
        stats.put("completedOrders", completedOrders);

        return stats;
    }

    public Map<String, Object> getMerchantDashboard(Long merchantId) {
        Map<String, Object> stats = new HashMap<>();
        List<PetOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getMerchantId, merchantId));

        stats.put("totalOrders", orders.size());
        BigDecimal revenue = orders.stream()
                .filter(o -> "paid".equals(o.getStatus()) || "completed".equals(o.getStatus()))
                .map(PetOrder::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("revenue", revenue);

        long pending = orders.stream().filter(o -> "pending".equals(o.getStatus())).count();
        long active = orders.stream().filter(o -> "paid".equals(o.getStatus()) || "in_progress".equals(o.getStatus())).count();
        stats.put("pendingOrders", pending);
        stats.put("activeOrders", active);

        return stats;
    }
}
