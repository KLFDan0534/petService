package com.pet.order.service.impl;

import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.ServiceItem;
import com.pet.common.BookingErrorCode;
import com.pet.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * U3 RED: 服务驱动下单契约 —— 服务计价单元必须是 day、客户端版本必须与当前服务版本一致
 * （否则 PRICE_CHANGED），有服务时按服务单价计价，无服务时沿用看护者单价（兼容旧客户端）。
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
    void serviceUnitNotDayRejected() {
        ServiceItem s = service("hour", "120.00", LocalDateTime.of(2026, 8, 11, 13, 45, 20));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> OrderServiceImpl.assertServiceUnitSupported(s));
        assertEquals(400, ex.getCode());
        assertEquals(BookingErrorCode.UNSUPPORTED_SERVICE_UNIT, ex.getErrorCode());
    }

    @Test
    void dayUnitAccepted() {
        assertDoesNotThrow(() -> OrderServiceImpl.assertServiceUnitSupported(service("day", "120.00", null)));
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
}
