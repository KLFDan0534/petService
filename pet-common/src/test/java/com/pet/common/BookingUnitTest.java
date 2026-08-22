package com.pet.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RED: 多计费单位注册表契约 —— 规范化别名、未知/空白/超长拒绝、时长边界、booking mode 推导。
 */
class BookingUnitTest {

    // ============ 规范化别名 ============

    @Test
    void aliasesNormalizeToCanonicalUnits() {
        assertEquals(BookingUnit.DAY, BookingUnit.normalize("天"));
        assertEquals(BookingUnit.DAY, BookingUnit.normalize("days"));
        assertEquals(BookingUnit.DAY, BookingUnit.normalize("day"));
        assertEquals(BookingUnit.SESSION, BookingUnit.normalize("次"));
        assertEquals(BookingUnit.SESSION, BookingUnit.normalize("sessions"));
        assertEquals(BookingUnit.SESSION, BookingUnit.normalize("session"));
        assertEquals(BookingUnit.HOUR, BookingUnit.normalize("小时"));
        assertEquals(BookingUnit.HOUR, BookingUnit.normalize("hours"));
        assertEquals(BookingUnit.HOUR, BookingUnit.normalize("hour"));
    }

    @Test
    void normalizationTrimsAndLowercases() {
        assertEquals(BookingUnit.DAY, BookingUnit.normalize(" 天 "));
        assertEquals(BookingUnit.DAY, BookingUnit.normalize("  Days  "));
        assertEquals(BookingUnit.HOUR, BookingUnit.normalize("HOUR"));
    }

    @Test
    void unknownBlankAndOversizedRejected() {
        assertNull(BookingUnit.normalize(null));
        assertNull(BookingUnit.normalize(""));
        assertNull(BookingUnit.normalize("  "));
        assertNull(BookingUnit.normalize("week"));
        assertNull(BookingUnit.normalize("perWeek"));
    }

    @Test
    void canonicalUnitsAreSupported() {
        assertTrue(BookingUnit.isSupported(BookingUnit.DAY));
        assertTrue(BookingUnit.isSupported(BookingUnit.SESSION));
        assertTrue(BookingUnit.isSupported(BookingUnit.HOUR));
        assertFalse(BookingUnit.isSupported("week"));
        assertFalse(BookingUnit.isSupported("天"));
    }

    // ============ booking mode ============

    @Test
    void bookingModeDerivedFromUnit() {
        assertEquals(BookingUnit.MODE_DATE_RANGE, BookingUnit.bookingMode(BookingUnit.DAY));
        assertEquals(BookingUnit.MODE_SLOT, BookingUnit.bookingMode(BookingUnit.SESSION));
        assertEquals(BookingUnit.MODE_SLOT, BookingUnit.bookingMode(BookingUnit.HOUR));
    }

    // ============ duration 边界 ============

    @Test
    void dayDurationFixedAt1440() {
        assertEquals(1440, BookingUnit.resolveDurationMinutes(BookingUnit.DAY, null));
        assertEquals(1440, BookingUnit.resolveDurationMinutes(BookingUnit.DAY, 1440));
        assertThrows(BusinessException.class,
                () -> BookingUnit.resolveDurationMinutes(BookingUnit.DAY, 60));
        assertThrows(BusinessException.class,
                () -> BookingUnit.resolveDurationMinutes(BookingUnit.DAY, 120));
    }

    @Test
    void sessionDurationWithin15To1440() {
        assertEquals(60, BookingUnit.resolveDurationMinutes(BookingUnit.SESSION, null));
        assertEquals(60, BookingUnit.resolveDurationMinutes(BookingUnit.SESSION, 60));
        assertEquals(120, BookingUnit.resolveDurationMinutes(BookingUnit.SESSION, 120));
        assertEquals(15, BookingUnit.resolveDurationMinutes(BookingUnit.SESSION, 15));
        assertEquals(1440, BookingUnit.resolveDurationMinutes(BookingUnit.SESSION, 1440));
        assertThrows(BusinessException.class,
                () -> BookingUnit.resolveDurationMinutes(BookingUnit.SESSION, 10));
        assertThrows(BusinessException.class,
                () -> BookingUnit.resolveDurationMinutes(BookingUnit.SESSION, 1441));
    }

    @Test
    void hourDurationMustBePositiveMultipleOf60() {
        assertEquals(60, BookingUnit.resolveDurationMinutes(BookingUnit.HOUR, null));
        assertEquals(60, BookingUnit.resolveDurationMinutes(BookingUnit.HOUR, 60));
        assertEquals(120, BookingUnit.resolveDurationMinutes(BookingUnit.HOUR, 120));
        assertEquals(1440, BookingUnit.resolveDurationMinutes(BookingUnit.HOUR, 1440));
        assertThrows(BusinessException.class,
                () -> BookingUnit.resolveDurationMinutes(BookingUnit.HOUR, 15));
        assertThrows(BusinessException.class,
                () -> BookingUnit.resolveDurationMinutes(BookingUnit.HOUR, 90));
        assertThrows(BusinessException.class,
                () -> BookingUnit.resolveDurationMinutes(BookingUnit.HOUR, 10));
        assertThrows(BusinessException.class,
                () -> BookingUnit.resolveDurationMinutes(BookingUnit.HOUR, 1500));
    }
}