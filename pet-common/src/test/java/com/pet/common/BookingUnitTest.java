package com.pet.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 多计费单位注册表契约：只允许 night / session / hour。
 * <p>
 * 旧的 day / boarding 已下线：必须返回 null（调用方据此返回 400），不得静默归一成别的单位。
 */
class BookingUnitTest {

    // ============ 规范化别名 ============

    @Test
    void aliasesNormalizeToCanonicalUnits() {
        assertEquals(BookingUnit.NIGHT, BookingUnit.normalize("晚"));
        assertEquals(BookingUnit.NIGHT, BookingUnit.normalize("nights"));
        assertEquals(BookingUnit.NIGHT, BookingUnit.normalize("night"));
        assertEquals(BookingUnit.SESSION, BookingUnit.normalize("次"));
        assertEquals(BookingUnit.SESSION, BookingUnit.normalize("sessions"));
        assertEquals(BookingUnit.SESSION, BookingUnit.normalize("session"));
        assertEquals(BookingUnit.HOUR, BookingUnit.normalize("小时"));
        assertEquals(BookingUnit.HOUR, BookingUnit.normalize("hours"));
        assertEquals(BookingUnit.HOUR, BookingUnit.normalize("hour"));
    }

    @Test
    void normalizationTrimsAndLowercases() {
        assertEquals(BookingUnit.NIGHT, BookingUnit.normalize(" 夜 "));
        assertEquals(BookingUnit.NIGHT, BookingUnit.normalize("  Nights  "));
        assertEquals(BookingUnit.HOUR, BookingUnit.normalize("HOUR"));
    }

    @Test
    void lessonUnitMapsToSession() {
        // 「课时」= 课程节次，按次收费（一次订单 = 一节课），不是连续小时
        assertEquals(BookingUnit.SESSION, BookingUnit.normalize("课时"));
        assertEquals(BookingUnit.SESSION, BookingUnit.normalize("节"));
        assertEquals(BookingUnit.SESSION, BookingUnit.normalize(" 课时 "));
        assertEquals(BookingUnit.MODE_SLOT, BookingUnit.bookingMode(BookingUnit.normalize("课时")));
        assertEquals(60, BookingUnit.resolveDurationMinutes(BookingUnit.normalize("课时"), null));
    }

    @Test
    void legacyUnitsAreRejected() {
        // 旧口径彻底下线：day / boarding 返回 null，由调用方返回明确的 400
        assertNull(BookingUnit.normalize("day"));
        assertNull(BookingUnit.normalize("days"));
        assertNull(BookingUnit.normalize("天"));
        assertNull(BookingUnit.normalize("boarding"));
        assertNull(BookingUnit.normalize("寄养"));
        assertNull(BookingUnit.normalize("24小时"));
        assertFalse(BookingUnit.isSupported("day"));
        assertFalse(BookingUnit.isSupported("boarding"));
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
        assertTrue(BookingUnit.isSupported(BookingUnit.NIGHT));
        assertTrue(BookingUnit.isSupported(BookingUnit.SESSION));
        assertTrue(BookingUnit.isSupported(BookingUnit.HOUR));
        assertFalse(BookingUnit.isSupported("week"));
        assertFalse(BookingUnit.isSupported("天"));
    }

    // ============ booking mode ============

    @Test
    void bookingModeDerivedFromUnit() {
        assertEquals(BookingUnit.MODE_INTERVAL, BookingUnit.bookingMode(BookingUnit.NIGHT));
        assertEquals(BookingUnit.MODE_SLOT, BookingUnit.bookingMode(BookingUnit.SESSION));
        assertEquals(BookingUnit.MODE_SLOT, BookingUnit.bookingMode(BookingUnit.HOUR));
        assertNull(BookingUnit.bookingMode("day"));
    }

    // ============ duration 边界 ============

    @Test
    void nightDurationFixedAtOneDayMetadata() {
        assertEquals(1440, BookingUnit.resolveDurationMinutes(BookingUnit.NIGHT, null));
        assertEquals(1440, BookingUnit.resolveDurationMinutes(BookingUnit.NIGHT, 1440));
        assertThrows(BusinessException.class,
                () -> BookingUnit.resolveDurationMinutes(BookingUnit.NIGHT, 60));
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
    void hourDurationMustBePositiveMultipleOfGranularity() {
        // 默认粒度 60 分钟
        assertEquals(60, BookingUnit.resolveDurationMinutes(BookingUnit.HOUR, null));
        assertEquals(60, BookingUnit.resolveDurationMinutes(BookingUnit.HOUR, 60));
        assertEquals(120, BookingUnit.resolveDurationMinutes(BookingUnit.HOUR, 120));
        assertEquals(1440, BookingUnit.resolveDurationMinutes(BookingUnit.HOUR, 1440));
        // 支持 30 分钟粒度：一个计费单位就是半小时
        assertEquals(30, BookingUnit.resolveDurationMinutes(BookingUnit.HOUR, 30));
        assertEquals(90, BookingUnit.resolveDurationMinutes(BookingUnit.HOUR, 90));
        assertEquals(BookingUnit.HOUR_GRANULARITY_STEP_MINUTES, 30);
        // 非 30 的整数倍一律拒绝（15/10 分钟无法用半小时粒度表达）
        assertThrows(BusinessException.class,
                () -> BookingUnit.resolveDurationMinutes(BookingUnit.HOUR, 15));
        assertThrows(BusinessException.class,
                () -> BookingUnit.resolveDurationMinutes(BookingUnit.HOUR, 10));
        assertThrows(BusinessException.class,
                () -> BookingUnit.resolveDurationMinutes(BookingUnit.HOUR, 1500));
    }
}
