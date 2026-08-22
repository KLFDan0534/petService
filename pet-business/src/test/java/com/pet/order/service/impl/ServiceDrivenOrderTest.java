package com.pet.order.service.impl;

import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.ServiceItem;
import com.pet.common.BookingErrorCode;
import com.pet.common.BusinessException;
import com.pet.order.dto.OrderDTO;
import com.pet.order.entity.PetOrder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * U3 RED: 服务驱动下单契约 —— 计费单位 day/session/hour 均可下单（未知单位拒绝）、
 * 客户端版本必须与当前服务版本一致（否则 PRICE_CHANGED）、
 * 有服务时按服务单价计价，无服务时沿用看护者单价（兼容旧客户端）。
 */
class ServiceDrivenOrderTest {

    private ServiceItem service(String unit, String price, LocalDateTime updatedAt) {
        ServiceItem s = new ServiceItem();
        s.setId_wsh(9L);
        s.setUnit_wsh(unit);
        s.setPrice_wsh(new BigDecimal(price));
        s.setUpdated_at_wsh(updatedAt);
        return s;
    }

    private Keeper keeper() {
        Keeper k = new Keeper();
        k.setPrice_per_day_wsh(new BigDecimal("100.00"));
        return k;
    }

    @Test
    void allCanonicalUnitsAccepted() {
        assertDoesNotThrow(() -> OrderServiceImpl.assertServiceUnitSupported(service("day", "120.00", null)));
        assertDoesNotThrow(() -> OrderServiceImpl.assertServiceUnitSupported(service("session", "120.00", null)));
        assertDoesNotThrow(() -> OrderServiceImpl.assertServiceUnitSupported(service("hour", "120.00", null)));
    }

    @Test
    void unknownUnitRejected() {
        ServiceItem s = service("week", "120.00", LocalDateTime.of(2026, 8, 11, 13, 45, 20));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> OrderServiceImpl.assertServiceUnitSupported(s));
        assertEquals(400, ex.getCode());
        assertEquals(BookingErrorCode.UNSUPPORTED_SERVICE_UNIT, ex.getErrorCode());
    }

    @Test
    void versionMismatchRejectedAsPriceChanged() {
        ServiceItem s = service("day", "120.00", LocalDateTime.of(2026, 8, 11, 13, 45, 20));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> OrderServiceImpl.assertServiceVersionMatches(s, "2026-08-01T10:00:00"));
        assertEquals(400, ex.getCode());
        assertEquals(BookingErrorCode.PRICE_CHANGED, ex.getErrorCode());
    }

    @Test
    void matchingVersionAccepted() {
        ServiceItem s = service("day", "120.00", LocalDateTime.of(2026, 8, 11, 13, 45, 20));
        assertDoesNotThrow(() -> OrderServiceImpl.assertServiceVersionMatches(s, "2026-08-11T13:45:20"));
    }

    @Test
    void nullClientVersionIsBackwardCompatible() {
        ServiceItem s = service("day", "120.00", LocalDateTime.of(2026, 8, 11, 13, 45, 20));
        assertDoesNotThrow(() -> OrderServiceImpl.assertServiceVersionMatches(s, null));
        assertDoesNotThrow(() -> OrderServiceImpl.assertServiceVersionMatches(s, "  "));
    }

    @Test
    void servicePricingUsedWhenServiceProvided() {
        ServiceItem s = service("day", "120.00", null);
        assertEquals(0, new BigDecimal("120.00").compareTo(OrderServiceImpl.resolvePricePerDay(s, keeper())));
    }

    @Test
    void legacyKeeperPricingWhenNoService() {
        assertEquals(0, new BigDecimal("100.00").compareTo(OrderServiceImpl.resolvePricePerDay(null, keeper())));
    }

    @Test
    void legacyDayOrderReadDerivesNewFields() {
        PetOrder legacy = new PetOrder();
        legacy.setId_wsh(1L);
        legacy.setDays_wsh(3);
        legacy.setPrice_per_day_wsh(new BigDecimal("120.00"));
        legacy.setTotal_amount_wsh(new BigDecimal("360.00"));
        legacy.setStart_date_wsh(LocalDate.of(2026, 8, 12));
        legacy.setEnd_date_wsh(LocalDate.of(2026, 8, 15));

        OrderDTO dto = new OrderDTO();
        OrderServiceImpl.applyBillingReadCompat(dto, legacy);

        assertEquals("day", dto.getBilling_unit_wsh());
        assertEquals(3, dto.getQuantity_wsh());
        assertEquals(0, dto.getUnit_price_wsh().compareTo(new BigDecimal("120.00")));
        assertEquals(1440, dto.getDuration_minutes_wsh());
    }

    @Test
    void legacyDayOrderReadKeepsNewFieldsWhenStored() {
        PetOrder legacy = new PetOrder();
        legacy.setId_wsh(2L);
        legacy.setBilling_unit_wsh("hour");
        legacy.setQuantity_wsh(4);
        legacy.setUnit_price_wsh(new BigDecimal("30.00"));
        legacy.setDuration_minutes_wsh(90);
        legacy.setDays_wsh(1);
        legacy.setPrice_per_day_wsh(new BigDecimal("30.00"));

        OrderDTO dto = new OrderDTO();
        dto.setBilling_unit_wsh(legacy.getBilling_unit_wsh());
        dto.setQuantity_wsh(legacy.getQuantity_wsh());
        dto.setUnit_price_wsh(legacy.getUnit_price_wsh());
        dto.setDuration_minutes_wsh(legacy.getDuration_minutes_wsh());
        dto.setDays_wsh(legacy.getDays_wsh());
        dto.setPrice_per_day_wsh(legacy.getPrice_per_day_wsh());
        OrderServiceImpl.applyBillingReadCompat(dto, legacy);

        assertEquals("hour", dto.getBilling_unit_wsh());
        assertEquals(4, dto.getQuantity_wsh());
        assertEquals(0, dto.getUnit_price_wsh().compareTo(new BigDecimal("30.00")));
        assertEquals(90, dto.getDuration_minutes_wsh());
    }

    @Test
    void nullOrderReadReturnsNullAndNoDerivation() {
        OrderDTO dto = new OrderDTO();
        OrderServiceImpl.applyBillingReadCompat(dto, null);
        assertNull(dto.getBilling_unit_wsh());
    }

    @Test
    void baseAmountIsQuantityTimesUnitPriceForAllUnits() {
        assertEquals(0, new BigDecimal("360.00").compareTo(
                OrderServiceImpl.resolveBaseAmount(new BigDecimal("120.00"), 3)));
        assertEquals(0, new BigDecimal("88.00").compareTo(
                OrderServiceImpl.resolveBaseAmount(new BigDecimal("88.00"), 1)));
        assertEquals(0, new BigDecimal("120.00").compareTo(
                OrderServiceImpl.resolveBaseAmount(new BigDecimal("30.00"), 4)));
    }

    @Test
    void discountDaysIsQuantityOnlyForDayUnit() {
        assertEquals(3, OrderServiceImpl.resolveDiscountDays("day", 3));
        assertEquals(1, OrderServiceImpl.resolveDiscountDays("session", 1));
        assertEquals(1, OrderServiceImpl.resolveDiscountDays("hour", 5));
        assertEquals(1, OrderServiceImpl.resolveDiscountDays("hour", 12));
    }
}
