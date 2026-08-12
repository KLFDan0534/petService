package com.pet.boarding.service;

import com.pet.boarding.entity.BusinessHours;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BusinessHoursTargetResolverTest {

    private final BusinessHoursTargetResolver resolver = new BusinessHoursTargetResolver();

    private BusinessHours hours(int dayOfWeek, LocalTime open, LocalTime close, int closed) {
        BusinessHours h = new BusinessHours();
        h.setDay_of_week_wsh(dayOfWeek);
        h.setOpen_time_wsh(open);
        h.setClose_time_wsh(close);
        h.setIs_closed_wsh(closed);
        return h;
    }

    private List<BusinessHours> defaultWeek() {
        return List.of(
                hours(1, LocalTime.of(9, 0), LocalTime.of(18, 0), 0),   // Mon 09-18
                hours(2, LocalTime.of(8, 0), LocalTime.of(17, 30), 0),  // Tue 08-17:30
                hours(3, LocalTime.of(9, 0), LocalTime.of(18, 0), 1),   // Wed rest
                hours(4, LocalTime.of(9, 0), LocalTime.of(18, 0), 0),
                hours(5, LocalTime.of(9, 0), LocalTime.of(18, 0), 0),
                hours(6, LocalTime.of(10, 0), LocalTime.of(22, 0), 0),
                hours(7, LocalTime.of(10, 0), LocalTime.of(22, 0), 0));
    }

    @Test
    void normalWindowAcceptsTimesWithinHalfOpenRange() {
        List<BusinessHours> week = defaultWeek();
        assertEquals(true, resolver.isWithinBusinessHours(week,
                LocalDateTime.of(2026, 8, 10, 9, 0)));   // Mon 09:00 open boundary
        assertEquals(true, resolver.isWithinBusinessHours(week,
                LocalDateTime.of(2026, 8, 10, 17, 59))); // just before close
        assertEquals(false, resolver.isWithinBusinessHours(week,
                LocalDateTime.of(2026, 8, 10, 8, 59)));  // before open
    }

    @Test
    void closeBoundaryIsExclusive() {
        List<BusinessHours> week = defaultWeek();
        assertEquals(false, resolver.isWithinBusinessHours(week,
                LocalDateTime.of(2026, 8, 10, 18, 0)));  // Mon exactly at close
        assertEquals(false, resolver.isWithinBusinessHours(week,
                LocalDateTime.of(2026, 8, 11, 17, 30))); // Tue exactly at close
    }

    @Test
    void restDayIsNeverWithin() {
        List<BusinessHours> week = defaultWeek();
        assertEquals(false, resolver.isWithinBusinessHours(week,
                LocalDateTime.of(2026, 8, 12, 12, 0)));  // Wed rest
    }

    @Test
    void crossMidnightWindowBelongsToStartingDay() {
        List<BusinessHours> week = List.of(
                hours(1, LocalTime.of(22, 0), LocalTime.of(2, 0), 0)); // Mon 22:00 -> Tue 02:00
        LocalDateTime tue0100 = LocalDateTime.of(2026, 8, 11, 1, 0);
        assertEquals(true, resolver.isWithinBusinessHours(week, tue0100)); // belongs to Monday window
        LocalDateTime tue0230 = LocalDateTime.of(2026, 8, 11, 2, 30);
        assertEquals(false, resolver.isWithinBusinessHours(week, tue0230)); // past Monday close
        LocalDateTime tue0330 = LocalDateTime.of(2026, 8, 11, 3, 30);
        assertEquals(false, resolver.isWithinBusinessHours(week, tue0330)); // not inside Tuesday's own hours
    }

    @Test
    void crossMidnightDaytimeIsClosedUntilOpen() {
        List<BusinessHours> week = List.of(
                hours(1, LocalTime.of(22, 0), LocalTime.of(2, 0), 0));
        LocalDateTime tue1100 = LocalDateTime.of(2026, 8, 11, 11, 0);
        assertEquals(false, resolver.isWithinBusinessHours(week, tue1100));
        LocalDateTime mon2300 = LocalDateTime.of(2026, 8, 10, 23, 0);
        assertEquals(true, resolver.isWithinBusinessHours(week, mon2300));
    }

    @Test
    void previousDayRestStillAllowsMorningCoverageFromPreviousCrossMidnight() {
        List<BusinessHours> week = List.of(
                hours(1, LocalTime.of(20, 0), LocalTime.of(1, 0), 0)); // Mon 20:00 -> Tue 01:00
        LocalDateTime tue0030 = LocalDateTime.of(2026, 8, 11, 0, 30);
        assertEquals(true, resolver.isWithinBusinessHours(week, tue0030));
    }

    @Test
    void openEqualsCloseMeansOpenAllDay() {
        List<BusinessHours> week = List.of(
                hours(1, LocalTime.of(0, 0), LocalTime.of(0, 0), 0));
        assertEquals(true, resolver.isWithinBusinessHours(week, LocalDateTime.of(2026, 8, 10, 23, 30)));
        assertEquals(true, resolver.isWithinBusinessHours(week, LocalDateTime.of(2026, 8, 10, 0, 0)));
    }

    @Test
    void emptyOrNullConfigIsNotWithin() {
        assertEquals(false, resolver.isWithinBusinessHours(List.of(), LocalDateTime.of(2026, 8, 10, 10, 0)));
        assertEquals(false, resolver.isWithinBusinessHours(null, LocalDateTime.of(2026, 8, 10, 10, 0)));
    }

    @Test
    void pastAndCrossDayTimesAreHandledByTargetDateNotWeekdayOfNow() {
        List<BusinessHours> week = defaultWeek();
        // Tuesday 16:00 falls within Tue 08:00-17:30
        assertEquals(true, resolver.isWithinBusinessHours(week, LocalDateTime.of(2026, 8, 11, 16, 0)));
        // Sunday 23:00 outside Sun 10:00-22:00
        assertEquals(false, resolver.isWithinBusinessHours(week, LocalDateTime.of(2026, 8, 16, 23, 0)));
    }

    // ============ U2: windowsForDate 窗口生成 ============

    @Test
    void windowsForDateNormalWindow() {
        List<BusinessHours> week = defaultWeek();
        List<BusinessHoursTargetResolver.BusinessWindow> windows =
                resolver.windowsForDate(week, LocalDate.of(2026, 8, 10));
        assertEquals(1, windows.size());
        assertEquals(LocalDateTime.of(2026, 8, 10, 9, 0), windows.get(0).start());
        assertEquals(LocalDateTime.of(2026, 8, 10, 18, 0), windows.get(0).end());
    }

    @Test
    void windowsForDateRestDayIsEmpty() {
        List<BusinessHours> week = defaultWeek();
        assertEquals(List.of(), resolver.windowsForDate(week, LocalDate.of(2026, 8, 12)));
    }

    @Test
    void windowsForDateCrossMidnightOwnPartAndTail() {
        List<BusinessHours> week = List.of(
                hours(3, LocalTime.of(22, 0), LocalTime.of(2, 0), 0));
        // Wednesday own part [22:00, 24:00)
        List<BusinessHoursTargetResolver.BusinessWindow> wed =
                resolver.windowsForDate(week, LocalDate.of(2026, 8, 12));
        assertEquals(1, wed.size());
        assertEquals(LocalDateTime.of(2026, 8, 12, 22, 0), wed.get(0).start());
        assertEquals(LocalDateTime.of(2026, 8, 13, 0, 0), wed.get(0).end());
        // Thursday morning tail [00:00, 02:00) from Wednesday's window
        List<BusinessHoursTargetResolver.BusinessWindow> thu =
                resolver.windowsForDate(week, LocalDate.of(2026, 8, 13));
        assertEquals(1, thu.size());
        assertEquals(LocalDateTime.of(2026, 8, 13, 0, 0), thu.get(0).start());
        assertEquals(LocalDateTime.of(2026, 8, 13, 2, 0), thu.get(0).end());
    }

    @Test
    void windowsForDateWholeDay() {
        List<BusinessHours> week = List.of(
                hours(1, LocalTime.of(0, 0), LocalTime.of(0, 0), 0));
        List<BusinessHoursTargetResolver.BusinessWindow> windows =
                resolver.windowsForDate(week, LocalDate.of(2026, 8, 10));
        assertEquals(1, windows.size());
        assertEquals(LocalDateTime.of(2026, 8, 10, 0, 0), windows.get(0).start());
        assertEquals(LocalDateTime.of(2026, 8, 11, 0, 0), windows.get(0).end());
    }

    @Test
    void windowsForDateOverlappingWindowsAreMerged() {
        // Monday own 00:00-08:00 window plus previous-day cross-midnight tail [00:00, 04:00)
        List<BusinessHours> week = List.of(
                hours(7, LocalTime.of(20, 0), LocalTime.of(4, 0), 0),   // Sun 20:00 -> Mon 04:00
                hours(1, LocalTime.of(0, 0), LocalTime.of(8, 0), 0));   // Mon 00:00-08:00
        List<BusinessHoursTargetResolver.BusinessWindow> windows =
                resolver.windowsForDate(week, LocalDate.of(2026, 8, 10));
        assertEquals(1, windows.size());
        assertEquals(LocalDateTime.of(2026, 8, 10, 0, 0), windows.get(0).start());
        assertEquals(LocalDateTime.of(2026, 8, 10, 8, 0), windows.get(0).end());
    }
}