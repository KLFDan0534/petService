package com.pet.boarding.service;

import com.pet.boarding.dto.DayAvailabilityVO;
import com.pet.boarding.dto.ServiceAvailabilityVO;
import com.pet.boarding.entity.BusinessHours;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.entity.ServiceCategory;
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.mapper.ServiceCategoryMapper;
import com.pet.boarding.mapper.ServiceItemMapper;
import com.pet.boarding.service.impl.ServiceAvailabilityServiceImpl;
import com.pet.common.BookingErrorCode;
import com.pet.common.BookingUnit;
import com.pet.common.BusinessException;
import com.pet.common.StatusCode;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.qualification.dto.QualificationDTO;
import com.pet.qualification.service.QualificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * U2 RED: 动态可预约性契约 —— 营业窗口 → 槽位生成、休息日/跨午夜/全天营业、
 * 31 天上限、legacy_unrestricted 兜底、商家/看护员校验、请假/容量按日标记。
 */
@ExtendWith(MockitoExtension.class)
class ServiceAvailabilityServiceTest {

    @Mock private ServiceItemMapper serviceItemMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private ServiceCategoryMapper categoryMapper;
    @Mock private KeeperMapper keeperMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private BusinessHoursService businessHoursService;
    @Mock private KeeperLeaveService keeperLeaveService;
    @Mock private QualificationService qualificationService;

    private final BusinessHoursTargetResolver resolver = new BusinessHoursTargetResolver();

    private ServiceAvailabilityServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ServiceAvailabilityServiceImpl(serviceItemMapper, merchantMapper, categoryMapper,
                keeperMapper, orderMapper, businessHoursService, resolver, keeperLeaveService,
                qualificationService, 30, 91);
    }

    private LocalDate nextOrSame(DayOfWeek day) {
        return LocalDate.now().with(TemporalAdjusters.nextOrSame(day));
    }

    private ServiceItem serviceItem(Long id, Long merchantId) {
        ServiceItem item = new ServiceItem();
        item.setId_wsh(id);
        item.setMerchant_id_wsh(merchantId);
        item.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        item.setUpdated_at_wsh(LocalDateTime.of(2026, 8, 11, 13, 45, 20));
        return item;
    }

    private Merchant merchant(Long id) {
        Merchant m = new Merchant();
        m.setId_wsh(id);
        m.setStatus_wsh(StatusCode.MERCHANT_APPROVED.getValue());
        m.setFuture_booking_enabled_wsh(1);
        m.setStore_status_wsh(1);
        return m;
    }

    private Keeper keeper(Long id, Long merchantId) {
        Keeper k = new Keeper();
        k.setId_wsh(id);
        k.setMerchant_id_wsh(merchantId);
        k.setStatus_wsh(StatusCode.KEEPER_ACTIVE.getValue());
        k.setMax_pets_wsh(2);
        return k;
    }

    private BusinessHours hours(int dayOfWeek, LocalTime open, LocalTime close, int closed) {
        BusinessHours h = new BusinessHours();
        h.setDay_of_week_wsh(dayOfWeek);
        h.setOpen_time_wsh(open);
        h.setClose_time_wsh(close);
        h.setIs_closed_wsh(closed);
        return h;
    }

    private void defaultStubs() {
        when(serviceItemMapper.selectById(11L)).thenReturn(serviceItem(11L, 7L));
        when(merchantMapper.selectById(7L)).thenReturn(merchant(7L));
    }

    private QualificationDTO approvedQual() {
        QualificationDTO q = new QualificationDTO();
        q.setStatus_wsh(QualificationService.STATUS_APPROVED);
        return q;
    }

    private DayAvailabilityVO day(ServiceAvailabilityVO vo, int index) {
        return vo.getDays_wsh().get(index);
    }

    // ============ 营业窗口与槽位 ============

    @Test
    void mondayWindowStartsWithOpenBoundarySlot() {
        defaultStubs();
        when(businessHoursService.getByMerchantId(7L)).thenReturn(
                List.of(hours(1, LocalTime.of(9, 0), LocalTime.of(18, 0), 0)));
        LocalDate mon = nextOrSame(DayOfWeek.MONDAY);

        ServiceAvailabilityVO vo = service.getAvailability(11L, mon, mon, null);

        assertEquals(11L, vo.getService_id_wsh());
        assertEquals(7L, vo.getMerchant_id_wsh());
        assertEquals("Asia/Shanghai", vo.getTimezone_wsh());
        assertEquals(30, vo.getSlot_minutes_wsh());
        assertEquals("business_hours", vo.getSchedule_source_wsh());
        assertNotNull(vo.getService_version_wsh());
        assertNotNull(vo.getGenerated_at_wsh());
        DayAvailabilityVO d = day(vo, 0);
        assertTrue(d.getBookable_wsh());
        assertNull(d.getReason_code_wsh());
        assertEquals(1, d.getWindows_wsh().size());
        assertEquals(mon.atTime(9, 0), d.getWindows_wsh().get(0).getStart_wsh());
        assertEquals(mon.atTime(18, 0), d.getWindows_wsh().get(0).getEnd_wsh());
        assertTrue(d.getWindows_wsh().get(0).getSlots_wsh().get(0).startsWith(mon.toString() + "T09:00"));
    }

    @Test
    void slotsStopBeforeCloseBoundary() {
        defaultStubs();
        when(businessHoursService.getByMerchantId(7L)).thenReturn(
                List.of(hours(1, LocalTime.of(9, 0), LocalTime.of(18, 0), 0)));
        LocalDate mon = nextOrSame(DayOfWeek.MONDAY);

        ServiceAvailabilityVO vo = service.getAvailability(11L, mon, mon, null);
        List<String> slots = day(vo, 0).getWindows_wsh().get(0).getSlots_wsh();

        assertEquals(18, slots.size());
        assertEquals(17, slots.stream().map(s -> s.substring(11, 16)).toList().indexOf("17:30"));
        assertFalse(slots.stream().anyMatch(s -> s.endsWith("T18:00:00")));
    }

    @Test
    void crossMidnightWindowSplitsAcrossDates() {
        defaultStubs();
        when(businessHoursService.getByMerchantId(7L)).thenReturn(
                List.of(hours(3, LocalTime.of(22, 0), LocalTime.of(2, 0), 0)));
        LocalDate wed = nextOrSame(DayOfWeek.WEDNESDAY);
        LocalDate thu = wed.plusDays(1);

        ServiceAvailabilityVO vo = service.getAvailability(11L, wed, thu, null);

        DayAvailabilityVO dWed = day(vo, 0);
        assertTrue(dWed.getBookable_wsh());
        assertEquals(1, dWed.getWindows_wsh().size());
        assertEquals(wed.atTime(22, 0), dWed.getWindows_wsh().get(0).getStart_wsh());
        assertEquals(wed.plusDays(1).atStartOfDay(), dWed.getWindows_wsh().get(0).getEnd_wsh());
        assertEquals(4, dWed.getWindows_wsh().get(0).getSlots_wsh().size());
        assertTrue(dWed.getWindows_wsh().get(0).getSlots_wsh().stream()
                .anyMatch(s -> s.endsWith("T23:30:00")));

        DayAvailabilityVO dThu = day(vo, 1);
        assertTrue(dThu.getBookable_wsh());
        assertEquals(1, dThu.getWindows_wsh().size());
        assertEquals(thu.atStartOfDay(), dThu.getWindows_wsh().get(0).getStart_wsh());
        assertEquals(thu.atTime(2, 0), dThu.getWindows_wsh().get(0).getEnd_wsh());
        assertEquals(4, dThu.getWindows_wsh().get(0).getSlots_wsh().size());
        assertTrue(dThu.getWindows_wsh().get(0).getSlots_wsh().stream()
                .anyMatch(s -> s.endsWith("T01:30:00")));
        assertFalse(dThu.getWindows_wsh().get(0).getSlots_wsh().stream()
                .anyMatch(s -> s.endsWith("T02:00:00")));
    }

    @Test
    void restDayIsMarkedNotBookable() {
        defaultStubs();
        when(businessHoursService.getByMerchantId(7L)).thenReturn(
                List.of(hours(1, LocalTime.of(9, 0), LocalTime.of(18, 0), 0),
                        hours(2, LocalTime.of(9, 0), LocalTime.of(18, 0), 1)));
        LocalDate tue = nextOrSame(DayOfWeek.TUESDAY);

        ServiceAvailabilityVO vo = service.getAvailability(11L, tue, tue, null);
        DayAvailabilityVO d = day(vo, 0);

        assertFalse(d.getBookable_wsh());
        assertEquals(BookingErrorCode.MERCHANT_REST_DAY, d.getReason_code_wsh());
        assertTrue(d.getWindows_wsh().isEmpty());
    }

    @Test
    void wholeDayWindowGenerates48Slots() {
        defaultStubs();
        when(businessHoursService.getByMerchantId(7L)).thenReturn(
                List.of(hours(1, LocalTime.of(0, 0), LocalTime.of(0, 0), 0)));
        LocalDate mon = nextOrSame(DayOfWeek.MONDAY);

        ServiceAvailabilityVO vo = service.getAvailability(11L, mon, mon, null);
        DayAvailabilityVO d = day(vo, 0);

        assertTrue(d.getBookable_wsh());
        assertEquals(1, d.getWindows_wsh().size());
        assertEquals(mon.atStartOfDay(), d.getWindows_wsh().get(0).getStart_wsh());
        assertEquals(mon.plusDays(1).atStartOfDay(), d.getWindows_wsh().get(0).getEnd_wsh());
        assertEquals(48, d.getWindows_wsh().get(0).getSlots_wsh().size());
        assertTrue(d.getWindows_wsh().get(0).getSlots_wsh().get(0).contains("T00:00:00"));
        assertTrue(d.getWindows_wsh().get(0).getSlots_wsh().get(47).contains("T23:30:00"));
    }

    @Test
    void slotGranularityIsConfigurable() {
        defaultStubs();
        service = new ServiceAvailabilityServiceImpl(serviceItemMapper, merchantMapper, categoryMapper,
                keeperMapper, orderMapper, businessHoursService, resolver, keeperLeaveService,
                qualificationService, 15, 91);
        when(businessHoursService.getByMerchantId(7L)).thenReturn(
                List.of(hours(1, LocalTime.of(9, 0), LocalTime.of(10, 0), 0)));
        LocalDate mon = nextOrSame(DayOfWeek.MONDAY);

        ServiceAvailabilityVO vo = service.getAvailability(11L, mon, mon, null);
        List<String> slots = day(vo, 0).getWindows_wsh().get(0).getSlots_wsh();

        assertEquals(4, slots.size());
        assertTrue(slots.get(0).contains("T09:00:00"));
        assertTrue(slots.get(1).contains("T09:15:00"));
        assertTrue(slots.get(3).contains("T09:45:00"));
    }

    // ============ 多单位槽位（session/hour） ============

    @Test
    void sessionUnitExposesOnlyFullCoverageStarts() {
        ServiceItem item = serviceItem(12L, 7L);
        item.setUnit_wsh("session");
        when(serviceItemMapper.selectById(12L)).thenReturn(item);
        when(merchantMapper.selectById(7L)).thenReturn(merchant(7L));
        when(businessHoursService.getByMerchantId(7L)).thenReturn(
                List.of(hours(1, LocalTime.of(9, 0), LocalTime.of(10, 30), 0)));
        LocalDate mon = nextOrSame(DayOfWeek.MONDAY);

        ServiceAvailabilityVO vo = service.getAvailability(12L, mon, mon, null);

        assertEquals("session", vo.getUnit_wsh());
        assertEquals(BookingUnit.MODE_SLOT, vo.getBooking_mode_wsh());
        assertEquals(60, vo.getDuration_minutes_wsh());
        List<String> slots = day(vo, 0).getWindows_wsh().get(0).getSlots_wsh();
        // 窗口 09:00..10:30，时长 60 分钟整点覆盖 => 09:00、09:30 可选；10:00+60 超过窗口不可选。
        assertTrue(slots.stream().anyMatch(s -> s.endsWith("T09:00:00")));
        assertTrue(slots.stream().anyMatch(s -> s.endsWith("T09:30:00")));
        assertFalse(slots.stream().anyMatch(s -> s.endsWith("T10:00:00")));
    }

    @Test
    void hourUnitGeneratesWholeHourSlotsOnly() {
        ServiceItem item = serviceItem(13L, 7L);
        item.setUnit_wsh("hour");
        item.setDuration_minutes_wsh(120);
        when(serviceItemMapper.selectById(13L)).thenReturn(item);
        when(merchantMapper.selectById(7L)).thenReturn(merchant(7L));
        when(businessHoursService.getByMerchantId(7L)).thenReturn(
                List.of(hours(1, LocalTime.of(9, 0), LocalTime.of(12, 30), 0)));
        LocalDate mon = nextOrSame(DayOfWeek.MONDAY);

        ServiceAvailabilityVO vo = service.getAvailability(13L, mon, mon, null);

        assertEquals("hour", vo.getUnit_wsh());
        assertEquals(BookingUnit.MODE_SLOT, vo.getBooking_mode_wsh());
        assertEquals(120, vo.getDuration_minutes_wsh());
        List<String> slots = day(vo, 0).getWindows_wsh().get(0).getSlots_wsh();
        // 整点起始 + 完整 120 分钟覆盖（窗口 09:00..12:30）：09:00、10:00 可选；
        // 11:00+120=13:00 超出窗口、09:30/10:30 非整点，均不可选。
        assertTrue(slots.stream().anyMatch(s -> s.endsWith("T09:00:00")));
        assertTrue(slots.stream().anyMatch(s -> s.endsWith("T10:00:00")));
        assertFalse(slots.stream().anyMatch(s -> s.endsWith("T11:00:00")));
        assertFalse(slots.stream().anyMatch(s -> s.endsWith("T09:30:00")));
        assertFalse(slots.stream().anyMatch(s -> s.endsWith("T10:30:00")));
    }

    @Test
    void hourUnitAlignsFirstSlotToNextWholeHour() {
        ServiceItem item = serviceItem(14L, 7L);
        item.setUnit_wsh("hour");
        item.setDuration_minutes_wsh(60);
        when(serviceItemMapper.selectById(14L)).thenReturn(item);
        when(merchantMapper.selectById(7L)).thenReturn(merchant(7L));
        when(businessHoursService.getByMerchantId(7L)).thenReturn(
                List.of(hours(1, LocalTime.of(9, 30), LocalTime.of(11, 30), 0)));
        LocalDate mon = nextOrSame(DayOfWeek.MONDAY);

        ServiceAvailabilityVO vo = service.getAvailability(14L, mon, mon, null);

        List<String> slots = day(vo, 0).getWindows_wsh().get(0).getSlots_wsh();
        // 窗口从 09:30 开始，整点对齐后第一个可选起始为 10:00（10:00+60=11:00 <= 11:30 覆盖完整）。
        assertFalse(slots.stream().anyMatch(s -> s.endsWith("T09:30:00")));
        assertTrue(slots.stream().anyMatch(s -> s.endsWith("T10:00:00")));
        assertFalse(slots.stream().anyMatch(s -> s.endsWith("T11:00:00")));
    }

    // ============ 日期范围校验 ============

    @Test
    void rangeBeyondWindowClampedToWindow() {
        defaultStubs();
        when(businessHoursService.getByMerchantId(7L)).thenReturn(
                List.of(hours(1, LocalTime.of(9, 0), LocalTime.of(18, 0), 0)));
        LocalDate from = nextOrSame(DayOfWeek.MONDAY);

        ServiceAvailabilityVO vo = service.getAvailability(11L, from, from.plusDays(120), null);

        // 请求 121 天，按默认窗口 91 天收敛；响应告知实际窗口
        assertEquals(91, vo.getBooking_window_days_wsh());
        assertEquals(91, vo.getDays_wsh().size());
        assertEquals(from.plusDays(90), day(vo, 90).getDate_wsh());
    }

    @Test
    void bookingWindowDaysIsConfigurable() {
        service = new ServiceAvailabilityServiceImpl(serviceItemMapper, merchantMapper, categoryMapper,
                keeperMapper, orderMapper, businessHoursService, resolver, keeperLeaveService,
                qualificationService, 30, 31);
        defaultStubs();
        when(businessHoursService.getByMerchantId(7L)).thenReturn(
                List.of(hours(1, LocalTime.of(9, 0), LocalTime.of(18, 0), 0)));
        LocalDate from = nextOrSame(DayOfWeek.MONDAY);

        ServiceAvailabilityVO vo = service.getAvailability(11L, from, from.plusDays(60), null);

        assertEquals(31, vo.getBooking_window_days_wsh());
        assertEquals(31, vo.getDays_wsh().size());
    }

    @Test
    void pastDateRejected() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getAvailability(11L, yesterday, LocalDate.now().plusDays(30), null));
        assertEquals(400, ex.getCode());
        assertEquals(BookingErrorCode.AVAILABILITY_RANGE_INVALID, ex.getErrorCode());
    }

    // ============ 兜底与商家校验 ============

    @Test
    void legacyUnrestrictedWhenNoHoursConfigured() {
        defaultStubs();
        when(businessHoursService.getByMerchantId(7L)).thenReturn(List.of());
        LocalDate mon = nextOrSame(DayOfWeek.MONDAY);

        ServiceAvailabilityVO vo = service.getAvailability(11L, mon, mon, null);

        assertEquals("legacy_unrestricted", vo.getSchedule_source_wsh());
        DayAvailabilityVO d = day(vo, 0);
        assertTrue(d.getBookable_wsh());
        assertEquals(1, d.getWindows_wsh().size());
        assertEquals(mon.atStartOfDay(), d.getWindows_wsh().get(0).getStart_wsh());
    }

    @Test
    void closedStoreStillBookableWhenPolicyOn() {
        defaultStubs();
        Merchant m = merchant(7L);
        m.setStore_status_wsh(0);
        when(merchantMapper.selectById(7L)).thenReturn(m);
        when(businessHoursService.getByMerchantId(7L)).thenReturn(
                List.of(hours(1, LocalTime.of(9, 0), LocalTime.of(18, 0), 0)));
        LocalDate mon = nextOrSame(DayOfWeek.MONDAY);

        ServiceAvailabilityVO vo = service.getAvailability(11L, mon, mon, null);
        assertTrue(day(vo, 0).getBookable_wsh());
    }

    @Test
    void futureBookingDisabledRejected() {
        Merchant m = merchant(7L);
        m.setFuture_booking_enabled_wsh(0);
        when(serviceItemMapper.selectById(11L)).thenReturn(serviceItem(11L, 7L));
        when(merchantMapper.selectById(7L)).thenReturn(m);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getAvailability(11L, nextOrSame(DayOfWeek.MONDAY), nextOrSame(DayOfWeek.MONDAY), null));
        assertEquals(400, ex.getCode());
        assertEquals(BookingErrorCode.FUTURE_BOOKING_DISABLED, ex.getErrorCode());
    }

    @Test
    void missingServiceIsNotFound() {
        when(serviceItemMapper.selectById(11L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getAvailability(11L, nextOrSame(DayOfWeek.MONDAY), nextOrSame(DayOfWeek.MONDAY), null));
        assertEquals(404, ex.getCode());
        assertEquals(BookingErrorCode.SERVICE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void offShelfServiceRejected() {
        ServiceItem item = serviceItem(11L, 7L);
        item.setStatus_wsh(StatusCode.SERVICE_DISABLED.getValue());
        when(serviceItemMapper.selectById(11L)).thenReturn(item);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getAvailability(11L, nextOrSame(DayOfWeek.MONDAY), nextOrSame(DayOfWeek.MONDAY), null));
        assertEquals(400, ex.getCode());
        assertEquals(BookingErrorCode.SERVICE_OFF_SHELF, ex.getErrorCode());
    }

    @Test
    void merchantNotApprovedRejected() {
        Merchant m = merchant(7L);
        m.setStatus_wsh(StatusCode.MERCHANT_PENDING.getValue());
        when(serviceItemMapper.selectById(11L)).thenReturn(serviceItem(11L, 7L));
        when(merchantMapper.selectById(7L)).thenReturn(m);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getAvailability(11L, nextOrSame(DayOfWeek.MONDAY), nextOrSame(DayOfWeek.MONDAY), null));
        assertEquals(400, ex.getCode());
        assertEquals(BookingErrorCode.MERCHANT_NOT_APPROVED, ex.getErrorCode());
    }

    @Test
    void disabledCategoryRejected() {
        ServiceItem item = serviceItem(11L, 7L);
        item.setCategory_id_wsh(1L);
        when(serviceItemMapper.selectById(11L)).thenReturn(item);
        ServiceCategory category = new ServiceCategory();
        category.setId_wsh(1L);
        category.setStatus_wsh(StatusCode.SERVICE_DISABLED.getValue());
        when(categoryMapper.selectById(1L)).thenReturn(category);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getAvailability(11L, nextOrSame(DayOfWeek.MONDAY), nextOrSame(DayOfWeek.MONDAY), null));
        assertEquals(400, ex.getCode());
        assertEquals(BookingErrorCode.SERVICE_OFF_SHELF, ex.getErrorCode());
    }

    // ============ 看护员筛选 ============

    @Test
    void keeperMerchantMismatchRejected() {
        defaultStubs();
        when(keeperMapper.selectById(5L)).thenReturn(keeper(5L, 99L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getAvailability(11L, nextOrSame(DayOfWeek.MONDAY), nextOrSame(DayOfWeek.MONDAY), 5L));
        assertEquals(400, ex.getCode());
        assertEquals(BookingErrorCode.KEEPER_MERCHANT_MISMATCH, ex.getErrorCode());
    }

    @Test
    void keeperNotBookableRejected() {
        defaultStubs();
        Keeper k = keeper(5L, 7L);
        k.setStatus_wsh(StatusCode.KEEPER_RESIGNED.getValue());
        when(keeperMapper.selectById(5L)).thenReturn(k);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getAvailability(11L, nextOrSame(DayOfWeek.MONDAY), nextOrSame(DayOfWeek.MONDAY), 5L));
        assertEquals(400, ex.getCode());
        assertEquals(BookingErrorCode.KEEPER_NOT_BOOKABLE, ex.getErrorCode());
    }

    @Test
    void keeperNotQualifiedRejected() {
        defaultStubs();
        when(keeperMapper.selectById(5L)).thenReturn(keeper(5L, 7L));
        when(qualificationService.listByOwner(eq(QualificationService.OWNER_TYPE_KEEPER), eq(5L), eq(false)))
                .thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getAvailability(11L, nextOrSame(DayOfWeek.MONDAY), nextOrSame(DayOfWeek.MONDAY), 5L));
        assertEquals(400, ex.getCode());
        assertEquals(BookingErrorCode.KEEPER_NOT_QUALIFIED, ex.getErrorCode());
    }

    @Test
    void keeperOnLeaveMarksDayNotBookable() {
        defaultStubs();
        when(keeperMapper.selectById(5L)).thenReturn(keeper(5L, 7L));
        when(qualificationService.listByOwner(eq(QualificationService.OWNER_TYPE_KEEPER), eq(5L), eq(false)))
                .thenReturn(List.of(approvedQual()));
        when(businessHoursService.getByMerchantId(7L)).thenReturn(
                List.of(hours(1, LocalTime.of(9, 0), LocalTime.of(18, 0), 0),
                        hours(2, LocalTime.of(9, 0), LocalTime.of(18, 0), 0)));
        LocalDate mon = nextOrSame(DayOfWeek.MONDAY);
        LocalDate tue = mon.plusDays(1);
        when(keeperLeaveService.isKeeperOnLeave(5L, mon)).thenReturn(false);
        when(keeperLeaveService.isKeeperOnLeave(5L, tue)).thenReturn(true);

        ServiceAvailabilityVO vo = service.getAvailability(11L, mon, tue, 5L);

        assertTrue(day(vo, 0).getBookable_wsh());
        assertNull(day(vo, 0).getReason_code_wsh());
        DayAvailabilityVO dTue = day(vo, 1);
        assertFalse(dTue.getBookable_wsh());
        assertEquals(BookingErrorCode.KEEPER_ON_LEAVE, dTue.getReason_code_wsh());
        assertTrue(dTue.getWindows_wsh().isEmpty());
    }

    @Test
    void capacityFullMarksDayNotBookable() {
        defaultStubs();
        when(keeperMapper.selectById(5L)).thenReturn(keeper(5L, 7L));
        when(qualificationService.listByOwner(eq(QualificationService.OWNER_TYPE_KEEPER), eq(5L), eq(false)))
                .thenReturn(List.of(approvedQual()));
        when(businessHoursService.getByMerchantId(7L)).thenReturn(
                List.of(hours(1, LocalTime.of(9, 0), LocalTime.of(18, 0), 0)));
        LocalDate mon = nextOrSame(DayOfWeek.MONDAY);
        // 看护人容量 2：两条覆盖当天的有效订单 => 当天满
        PetOrder o1 = new PetOrder();
        o1.setKeeper_id_wsh(5L);
        o1.setStart_date_wsh(mon.minusDays(1));
        o1.setEnd_date_wsh(mon.plusDays(1));
        PetOrder o2 = new PetOrder();
        o2.setKeeper_id_wsh(5L);
        o2.setStart_date_wsh(mon);
        o2.setEnd_date_wsh(mon.plusDays(2));
        when(orderMapper.selectList(any())).thenReturn(List.of(o1, o2));

        ServiceAvailabilityVO vo = service.getAvailability(11L, mon, mon, 5L);

        DayAvailabilityVO d = day(vo, 0);
        assertFalse(d.getBookable_wsh());
        assertEquals(BookingErrorCode.CAPACITY_EXCEEDED, d.getReason_code_wsh());
        assertTrue(d.getWindows_wsh().isEmpty());
        // 容量判定改为窗口内一次性加载：单次 selectList，不逐日 selectCount
        verify(orderMapper, times(1)).selectList(any());
    }

    @Test
    void capacityCountsOnceForWindowInsteadOfPerDay() {
        defaultStubs();
        when(keeperMapper.selectById(5L)).thenReturn(keeper(5L, 7L));
        when(qualificationService.listByOwner(eq(QualificationService.OWNER_TYPE_KEEPER), eq(5L), eq(false)))
                .thenReturn(List.of(approvedQual()));
        when(businessHoursService.getByMerchantId(7L)).thenReturn(List.of());
        when(orderMapper.selectList(any())).thenReturn(List.of());
        LocalDate from = nextOrSame(DayOfWeek.MONDAY);

        service.getAvailability(11L, from, from.plusDays(90), 5L);

        // 91 天窗口只查询一次容量，而不是逐日 N+1
        verify(orderMapper, times(1)).selectList(any());
    }

    // ============ 只读契约 ============

    @Test
    void availabilityQueryIsReadOnly() {
        defaultStubs();
        when(businessHoursService.getByMerchantId(7L)).thenReturn(
                List.of(hours(1, LocalTime.of(9, 0), LocalTime.of(18, 0), 0)));
        LocalDate mon = nextOrSame(DayOfWeek.MONDAY);

        service.getAvailability(11L, mon, mon, null);

        verify(serviceItemMapper, never()).insert(org.mockito.ArgumentMatchers.any(ServiceItem.class));
        verify(serviceItemMapper, never()).updateById(org.mockito.ArgumentMatchers.any(ServiceItem.class));
        verify(serviceItemMapper, never()).deleteById(org.mockito.ArgumentMatchers.<ServiceItem>any());
        verify(merchantMapper, never()).updateById(org.mockito.ArgumentMatchers.any(Merchant.class));
        verify(keeperMapper, never()).updateById(org.mockito.ArgumentMatchers.any(Keeper.class));
        verify(orderMapper, never()).insert(org.mockito.ArgumentMatchers.any(PetOrder.class));
        verify(orderMapper, never()).updateById(org.mockito.ArgumentMatchers.any(PetOrder.class));
    }
}