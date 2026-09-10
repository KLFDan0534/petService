package com.pet.admin;

import com.pet.admin.statistics.service.impl.StatisticsServiceImpl;
import com.pet.admin.statistics.vo.AdminDashboardVO;
import com.pet.admin.statistics.vo.MerchantDashboardVO;
import com.pet.admin.statistics.vo.UserDashboardVO;
import com.pet.boarding.service.KeeperService;
import com.pet.boarding.service.MerchantService;
import com.pet.common.OrderStatus;
import com.pet.customer.mapper.ComplaintMapper;
import com.pet.customer.mapper.RatingMapper;
import com.pet.order.dto.OrderDTO;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.mapper.TipMapper;
import com.pet.order.service.OrderService;
import com.pet.pet.service.PetService;
import com.pet.system.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

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

    @Test
    void dashboardsCountAutoAcceptedOrdersAsRevenueAndActive() {
        OrderDTO paid = order(OrderStatus.PAID, "10.00");
        OrderDTO confirmed = order(OrderStatus.CONFIRMED, "20.00");
        OrderDTO delivered = order(OrderStatus.DELIVERED, "30.00");
        OrderDTO received = order(OrderStatus.RECEIVED, "40.00");
        OrderDTO inProgress = order(OrderStatus.IN_PROGRESS, "50.00");
        OrderDTO completed = order(OrderStatus.COMPLETED, "60.00");
        OrderDTO pending = order(OrderStatus.PENDING, "70.00");
        OrderDTO cancelled = order(OrderStatus.CANCELLED, "80.00");
        List<OrderDTO> orders = List.of(paid, confirmed, delivered, received, inProgress, completed, pending, cancelled);

        when(userService.listAll()).thenReturn(List.of());
        when(petService.listAll()).thenReturn(List.of());
        when(merchantService.listAll()).thenReturn(List.of());
        when(keeperService.listAll()).thenReturn(List.of());
        when(orderService.listAll()).thenReturn(orders);
        when(petService.getPetsByOwner(7L)).thenReturn(List.of());
        when(orderService.listByOwner(7L)).thenReturn(orders);
        when(orderService.listByMerchant(8L)).thenReturn(orders);

        StatisticsServiceImpl service = new StatisticsServiceImpl(
                userService,
                petService,
                merchantService,
                keeperService,
                orderService,
                ratingMapper,
                complaintMapper,
                orderMapper,
                tipMapper);

        AdminDashboardVO admin = service.getAdminDashboard();
        UserDashboardVO user = service.getUserDashboard(7L);
        MerchantDashboardVO merchant = service.getMerchantDashboard(8L);

        assertEquals(new BigDecimal("210.00"), admin.getTotal_revenue_wsh());
        assertEquals(new BigDecimal("210.00"), user.getTotal_spent_wsh());
        assertEquals(new BigDecimal("210.00"), merchant.getTotal_revenue_wsh());
        assertEquals(5, user.getActive_orders_wsh());
        assertEquals(5, merchant.getActive_orders_wsh());
        assertEquals(1, admin.getPending_orders_wsh());
        assertEquals(1, merchant.getPending_orders_wsh());
        assertEquals(1, admin.getCompleted_orders_wsh());
        assertEquals(1, user.getCompleted_orders_wsh());
        assertEquals(1, merchant.getCompleted_orders_wsh());
    }

    private OrderDTO order(String status, String amount) {
        OrderDTO order = new OrderDTO();
        order.setStatus_wsh(status);
        order.setFinal_amount_wsh(new BigDecimal(amount));
        return order;
    }
}
