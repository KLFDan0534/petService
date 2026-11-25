package com.pet.admin;

import com.pet.admin.statistics.service.impl.StatisticsServiceImpl;
import com.pet.admin.statistics.vo.AdminDashboardVO;
import com.pet.admin.statistics.vo.MerchantDashboardVO;
import com.pet.admin.statistics.vo.UserDashboardVO;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.service.KeeperService;
import com.pet.boarding.service.MerchantService;
import com.pet.customer.mapper.ComplaintMapper;
import com.pet.customer.mapper.RatingMapper;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.mapper.TipMapper;
import com.pet.order.service.OrderService;
import com.pet.pet.mapper.PetMapper;
import com.pet.pet.service.PetService;
import com.pet.system.mapper.UserMapper;
import com.pet.system.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 看板统计的映射测试。
 *
 * <p>P0-04 之后，订单指标由 {@link OrderMapper} 的三条聚合 SQL 一次算出，
 * 不再把订单全量加载进内存再 stream 统计。因此本测试的职责是：
 * ① 聚合 SQL 返回的单行 Map 被正确映射到三个 VO；
 * ② 计数类指标走 {@code selectCount} 而不是 listAll。
 *
 * <p>「哪些状态计入营收/进行中」的口径已下沉到 SQL
 * （{@code selectPlatformStats} / {@code selectOwnerStats} / {@code selectMerchantStats}），
 * 需要连库验证，不在本单测覆盖范围内。
 */
@ExtendWith(MockitoExtension.class)
class OrderStatusStatisticsTest {

    @Mock private UserService userService;
    @Mock private PetService petService;
    @Mock private MerchantService merchantService;
    @Mock private KeeperService keeperService;
    @Mock private OrderService orderService;
    @Mock private RatingMapper ratingMapper;
    @Mock private ComplaintMapper complaintMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private TipMapper tipMapper;
    @Mock private UserMapper userMapper;
    @Mock private PetMapper petMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private KeeperMapper keeperMapper;

    @Test
    void adminDashboardMapsAggregatedPlatformStats() {
        when(userMapper.selectCount(any())).thenReturn(12L);
        when(petMapper.selectCount(any())).thenReturn(30L);
        when(merchantMapper.selectCount(any())).thenReturn(4L);
        when(keeperMapper.selectCount(any())).thenReturn(9L);
        when(orderMapper.selectPlatformStats()).thenReturn(Map.of(
                "total_orders_wsh", 7L,
                "total_revenue_wsh", new BigDecimal("180.00"),
                "pending_orders_wsh", 1L,
                "completed_orders_wsh", 1L));

        StatisticsServiceImpl service = newService();

        AdminDashboardVO admin = service.getAdminDashboard();
        assertEquals(12, admin.getTotal_users_wsh());
        assertEquals(30, admin.getTotal_pets_wsh());
        assertEquals(4, admin.getTotal_merchants_wsh());
        assertEquals(9, admin.getTotal_keepers_wsh());
        assertEquals(7, admin.getTotal_orders_wsh());
        assertEquals(new BigDecimal("180.00"), admin.getTotal_revenue_wsh());
        assertEquals(1L, admin.getPending_orders_wsh());
        assertEquals(1L, admin.getCompleted_orders_wsh());
    }

    @Test
    void userDashboardMapsAggregatedOwnerStats() {
        when(petMapper.selectCount(any())).thenReturn(2L);
        when(orderMapper.selectOwnerStats(7L)).thenReturn(Map.of(
                "active_orders_wsh", 4L,
                "completed_orders_wsh", 1L,
                "total_spent_wsh", new BigDecimal("180.00")));

        StatisticsServiceImpl service = newService();

        UserDashboardVO user = service.getUserDashboard(7L);
        assertEquals(2, user.getPets_wsh());
        assertEquals(4L, user.getActive_orders_wsh());
        assertEquals(1L, user.getCompleted_orders_wsh());
        assertEquals(new BigDecimal("180.00"), user.getTotal_spent_wsh());
    }

    @Test
    void merchantDashboardMapsAggregatedMerchantStats() {
        Map<String, Object> row = new HashMap<>();
        row.put("total_orders_wsh", 7L);
        row.put("total_revenue_wsh", new BigDecimal("180.00"));
        row.put("pending_orders_wsh", 1L);
        row.put("active_orders_wsh", 4L);
        row.put("completed_orders_wsh", 1L);
        row.put("serving_pets_wsh", 2L);
        when(orderMapper.selectMerchantStats(8L)).thenReturn(row);

        StatisticsServiceImpl service = newService();

        MerchantDashboardVO merchant = service.getMerchantDashboard(8L);
        assertEquals(7, merchant.getTotal_orders_wsh());
        assertEquals(new BigDecimal("180.00"), merchant.getTotal_revenue_wsh());
        assertEquals(1L, merchant.getPending_orders_wsh());
        assertEquals(4L, merchant.getActive_orders_wsh());
        assertEquals(1L, merchant.getCompleted_orders_wsh());
        assertEquals(2, merchant.getPets_wsh());
    }

    /** 聚合结果缺键（例如库为空、SQL 返回 null 列）时按 0 处理，不得 NPE。 */
    @Test
    void missingAggregateKeysFallBackToZero() {
        when(orderMapper.selectPlatformStats()).thenReturn(Map.of());
        when(orderMapper.selectOwnerStats(7L)).thenReturn(Map.of());
        when(orderMapper.selectMerchantStats(8L)).thenReturn(Map.of());

        StatisticsServiceImpl service = newService();

        AdminDashboardVO admin = service.getAdminDashboard();
        assertEquals(0, admin.getTotal_orders_wsh());
        assertEquals(0, admin.getTotal_revenue_wsh().compareTo(BigDecimal.ZERO));

        UserDashboardVO user = service.getUserDashboard(7L);
        assertEquals(0L, user.getActive_orders_wsh());
        assertEquals(0, user.getTotal_spent_wsh().compareTo(BigDecimal.ZERO));

        MerchantDashboardVO merchant = service.getMerchantDashboard(8L);
        assertEquals(0, merchant.getTotal_orders_wsh());
        assertEquals(0, merchant.getPets_wsh());
    }

    private StatisticsServiceImpl newService() {
        return new StatisticsServiceImpl(
                userService,
                petService,
                merchantService,
                keeperService,
                orderService,
                ratingMapper,
                complaintMapper,
                orderMapper,
                tipMapper,
                userMapper,
                petMapper,
                merchantMapper,
                keeperMapper);
    }
}
