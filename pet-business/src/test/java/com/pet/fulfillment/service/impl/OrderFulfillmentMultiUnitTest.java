package com.pet.fulfillment.service.impl;

import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.service.KeeperAttendanceService;
import com.pet.customer.service.ChatEventBroadcaster;
import com.pet.customer.service.ChatService;
import com.pet.fulfillment.dto.DailyStatusDTO;
import com.pet.fulfillment.vo.OrderFulfillmentOverviewVO;
import com.pet.operation.service.FileRecordService;
import com.pet.operation.service.NotificationService;
import com.pet.operation.service.impl.MinIoService;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.pet.mapper.CareRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * U3 RED: 履约读模型 —— session/hour 订单概览必须携带计费单位/数量/单价/时长，
 * 且每日状态不得虚构多日打卡（start==end 当天仅一个必需日）。
 */
@ExtendWith(MockitoExtension.class)
class OrderFulfillmentMultiUnitTest {

    @Mock private OrderMapper orderMapper;
    @Mock private KeeperMapper keeperMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private CareRecordMapper careRecordMapper;
    @Mock private ChatService chatService;
    @Mock private ChatEventBroadcaster chatEventBroadcaster;
    @Mock private MinIoService minIoService;
    @Mock private FileRecordService fileRecordService;
    @Mock private NotificationService notificationService;
    @Mock private KeeperAttendanceService keeperAttendanceService;

    private OrderFulfillmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderFulfillmentServiceImpl(orderMapper, keeperMapper, merchantMapper,
                careRecordMapper, chatService, chatEventBroadcaster, minIoService,
                fileRecordService, notificationService, keeperAttendanceService);
    }

    @Test
    void sessionOrderOverviewCarriesBillingFields() {
        LocalDate day = LocalDate.of(2026, 8, 12);
        PetOrder order = new PetOrder();
        order.setId_wsh(7L);
        order.setOwner_id_wsh(1L);
        order.setKeeper_id_wsh(2L);
        order.setMerchant_id_wsh(3L);
        order.setBilling_unit_wsh("session");
        order.setQuantity_wsh(1);
        order.setUnit_price_wsh(new BigDecimal("88.00"));
        order.setDuration_minutes_wsh(60);
        order.setDays_wsh(1);
        order.setPrice_per_day_wsh(new BigDecimal("88.00"));
        order.setStart_date_wsh(day);
        order.setEnd_date_wsh(day.plusDays(1));
        order.setTotal_amount_wsh(new BigDecimal("88.00"));
        order.setFinal_amount_wsh(new BigDecimal("88.00"));
        order.setStatus_wsh("CONFIRMED");

        when(orderMapper.selectById(7L)).thenReturn(order);
        when(careRecordMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(java.util.List.of());

        OrderFulfillmentOverviewVO vo = service.getOverview(1L, false, 7L);

        assertEquals("session", vo.getOrder_wsh().getBilling_unit_wsh());
        assertEquals(1, vo.getOrder_wsh().getQuantity_wsh());
        assertEquals(0, vo.getOrder_wsh().getUnit_price_wsh().compareTo(new BigDecimal("88.00")));
        assertEquals(60, vo.getOrder_wsh().getDuration_minutes_wsh());
    }

    @Test
    void sessionOrderDailyStatusHasSingleRequiredDay() {
        LocalDate day = LocalDate.of(2026, 8, 12);
        PetOrder order = new PetOrder();
        order.setId_wsh(8L);
        order.setOwner_id_wsh(1L);
        order.setKeeper_id_wsh(2L);
        order.setMerchant_id_wsh(3L);
        order.setBilling_unit_wsh("hour");
        order.setQuantity_wsh(3);
        order.setUnit_price_wsh(new BigDecimal("30.00"));
        order.setDuration_minutes_wsh(60);
        order.setDays_wsh(1);
        order.setStart_date_wsh(day);
        order.setEnd_date_wsh(day.plusDays(1));

        when(orderMapper.selectById(8L)).thenReturn(order);
        when(careRecordMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(java.util.List.of());

        DailyStatusDTO status = service.getDailyUploadStatus(1L, false, 8L);

        assertEquals(1, status.getRequired_days_wsh().size());
        assertEquals(day.toString(), status.getRequired_days_wsh().get(0));
        assertTrue(status.getRequired_days_wsh().contains("2026-08-12"));
    }

    @Test
    void keeperNullDoesNotBreakReadAccess() {
        LocalDate day = LocalDate.of(2026, 8, 12);
        PetOrder order = new PetOrder();
        order.setId_wsh(9L);
        order.setOwner_id_wsh(1L);
        order.setKeeper_id_wsh(null);
        order.setMerchant_id_wsh(3L);
        order.setBilling_unit_wsh("day");
        order.setDays_wsh(2);
        order.setStart_date_wsh(day);
        order.setEnd_date_wsh(day.plusDays(2));

        when(orderMapper.selectById(9L)).thenReturn(order);

        Keeper k = new Keeper();
        k.setId_wsh(2L);
        k.setUser_id_wsh(5L);
        Merchant m = new Merchant();
        m.setId_wsh(3L);
        m.setUser_id_wsh(6L);
        when(merchantMapper.selectById(3L)).thenReturn(m);
        when(careRecordMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(java.util.List.of());

        DailyStatusDTO status = service.getDailyUploadStatus(6L, false, 9L);
        assertEquals(2, status.getRequired_days_wsh().size());
    }
}