package com.pet.order.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.entity.ServiceItem;
import com.pet.order.dto.OrderCreateRequestDTO;
import com.pet.order.entity.OrderSnapshot;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderSnapshotMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

/**
 * U3 RED: 订单快照必须冻结服务版本与计价来源 —— service_snapshot 含 version_wsh，
 * price_snapshot 含 price_per_day，确保服务改价后历史订单仍保有下单时的服务价格。
 */
@ExtendWith(MockitoExtension.class)
class OrderSnapshotServiceImplTest {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private static final ObjectMapper TEST_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock private OrderSnapshotMapper orderSnapshotMapper;

    private OrderSnapshotServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderSnapshotServiceImpl(orderSnapshotMapper,
                new ObjectMapper().registerModule(new JavaTimeModule()));
    }

    @Test
    void serviceSnapshotFreezesVersionAndPrice() throws Exception {
        ServiceItem serviceItem = new ServiceItem();
        serviceItem.setId_wsh(9L);
        serviceItem.setName_wsh("标准寄养");
        serviceItem.setPrice_wsh(new BigDecimal("120.00"));
        serviceItem.setUnit_wsh("day");
        serviceItem.setUpdated_at_wsh(LocalDateTime.of(2026, 8, 11, 13, 45, 20));

        PetOrder order = new PetOrder();
        order.setId_wsh(1L);
        order.setOrder_no_wsh("ORD-U3-1");
        order.setDays_wsh(3);
        order.setPrice_per_day_wsh(new BigDecimal("120.00"));
        order.setTotal_amount_wsh(new BigDecimal("360.00"));
        order.setStart_date_wsh(java.time.LocalDate.of(2026, 8, 12));
        order.setEnd_date_wsh(java.time.LocalDate.of(2026, 8, 15));

        service.createForOrder(order, null, null, null, null, serviceItem, new OrderCreateRequestDTO());

        ArgumentCaptor<OrderSnapshot> captor = ArgumentCaptor.forClass(OrderSnapshot.class);
        verify(orderSnapshotMapper).insert(captor.capture());
        OrderSnapshot snapshot = captor.getValue();

        Map<String, Object> svc = TEST_MAPPER.readValue(snapshot.getService_snapshot_wsh(), MAP_TYPE);
        assertEquals(9L, ((Number) svc.get("id_wsh")).longValue());
        assertEquals("标准寄养", svc.get("name_wsh"));
        assertEquals(0, new BigDecimal(String.valueOf(svc.get("price_wsh"))).compareTo(new BigDecimal("120.00")));
        assertEquals("day", svc.get("unit_wsh"));
        assertEquals("2026-08-11T13:45:20", svc.get("version_wsh"));

        Map<String, Object> price = TEST_MAPPER.readValue(snapshot.getPrice_snapshot_wsh(), MAP_TYPE);
        assertEquals(0, new BigDecimal(String.valueOf(price.get("price_per_day_wsh"))).compareTo(new BigDecimal("120.00")));
        assertEquals(0, new BigDecimal(String.valueOf(price.get("total_amount_wsh"))).compareTo(new BigDecimal("360.00")));
        assertEquals(3, ((Number) price.get("days_wsh")).intValue());
    }

    @Test
    void serviceSnapshotHandlesNullService() throws Exception {
        PetOrder order = new PetOrder();
        order.setId_wsh(2L);
        order.setOrder_no_wsh("ORD-U3-2");

        service.createForOrder(order, null, null, null, null, null, new OrderCreateRequestDTO());

        ArgumentCaptor<OrderSnapshot> captor = ArgumentCaptor.forClass(OrderSnapshot.class);
        verify(orderSnapshotMapper).insert(captor.capture());
        Map<String, Object> svc = TEST_MAPPER.readValue(captor.getValue().getService_snapshot_wsh(), MAP_TYPE);
        assertNotNull(svc);
        assertEquals(0, svc.size());
    }

    @Test
    void snapshotFreezesBillingFieldsAndMedia() throws Exception {
        ServiceItem serviceItem = new ServiceItem();
        serviceItem.setId_wsh(10L);
        serviceItem.setName_wsh("上门遛狗");
        serviceItem.setPrice_wsh(new BigDecimal("88.00"));
        serviceItem.setUnit_wsh("session");
        serviceItem.setBooking_mode_wsh("slot");
        serviceItem.setDuration_minutes_wsh(60);
        serviceItem.setImages_wsh("[\"https://cdn.example.com/a.png\"]");
        serviceItem.setUpdated_at_wsh(LocalDateTime.of(2026, 8, 12, 9, 0, 0));

        PetOrder order = new PetOrder();
        order.setId_wsh(3L);
        order.setOrder_no_wsh("ORD-MU-3");
        order.setBilling_unit_wsh("session");
        order.setQuantity_wsh(1);
        order.setUnit_price_wsh(new BigDecimal("88.00"));
        order.setDuration_minutes_wsh(60);
        order.setDays_wsh(1);
        order.setPrice_per_day_wsh(new BigDecimal("88.00"));
        order.setTotal_amount_wsh(new BigDecimal("88.00"));
        order.setStart_date_wsh(java.time.LocalDate.of(2026, 8, 12));
        order.setEnd_date_wsh(java.time.LocalDate.of(2026, 8, 12));

        service.createForOrder(order, null, null, null, null, serviceItem, new OrderCreateRequestDTO());

        ArgumentCaptor<OrderSnapshot> captor = ArgumentCaptor.forClass(OrderSnapshot.class);
        verify(orderSnapshotMapper).insert(captor.capture());
        OrderSnapshot snapshot = captor.getValue();

        Map<String, Object> svc = TEST_MAPPER.readValue(snapshot.getService_snapshot_wsh(), MAP_TYPE);
        assertEquals("slot", svc.get("booking_mode_wsh"));
        assertEquals(60, ((Number) svc.get("duration_minutes_wsh")).intValue());
        assertEquals("[\"https://cdn.example.com/a.png\"]", svc.get("images_wsh"));

        Map<String, Object> price = TEST_MAPPER.readValue(snapshot.getPrice_snapshot_wsh(), MAP_TYPE);
        assertEquals("session", price.get("billing_unit_wsh"));
        assertEquals(1, ((Number) price.get("quantity_wsh")).intValue());
        assertEquals(0, new BigDecimal(String.valueOf(price.get("unit_price_wsh"))).compareTo(new BigDecimal("88.00")));
        assertEquals(60, ((Number) price.get("duration_minutes_wsh")).intValue());
        assertEquals(0, new BigDecimal(String.valueOf(price.get("total_amount_wsh"))).compareTo(new BigDecimal("88.00")));
    }

    @SuppressWarnings("unused")
    private void unusedRefs() {
        Keeper.class.getName();
        Merchant.class.getName();
    }
}