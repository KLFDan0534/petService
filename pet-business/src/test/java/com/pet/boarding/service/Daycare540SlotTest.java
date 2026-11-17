package com.pet.boarding.service;

import com.pet.boarding.dto.DayAvailabilityVO;
import com.pet.boarding.dto.ServiceAvailabilityVO;
import com.pet.boarding.entity.BusinessHours;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.mapper.ServiceCategoryMapper;
import com.pet.boarding.mapper.ServiceItemMapper;
import com.pet.boarding.service.impl.ServiceAvailabilityServiceImpl;
import com.pet.common.BookingErrorCode;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 阶段 F / P1：日间寄养 540 分钟连续服务的预约槽位验证。
 *
 * 依据真实代码 ServiceAvailabilityServiceImpl.generateSlots（start + duration <= windowEnd）
 * 与真实配置 booking.slot-minutes=30，验证授权书 §6.2 的五组营业时间场景。
 * 不缩短服务时长、不修改营业时间、不重构 generateSlots。
 */
@ExtendWith(MockitoExtension.class)
class Daycare540SlotTest {

    private static final int DAYCARE_DURATION = 540;
    private static final int SLOT_MINUTES = 30;

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
                qualificationService, SLOT_MINUTES, 91);
    }

    private LocalDate monday() {
        return LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    }

    /** 日托服务：session / slot / 540，与 V20261110_002 迁移后的 ID 9/30 一致。 */
    private ServiceItem daycare(Long id, Long merchantId) {
        ServiceItem item = new ServiceItem();
        item.setId_wsh(id);
        item.setMerchant_id_wsh(merchantId);
        item.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        item.setUpdated_at_wsh(LocalDateTime.of(2026, 8, 11, 13, 45, 20));
        item.setUnit_wsh("session");
        item.setBooking_mode_wsh("slot");
        item.setDuration_minutes_wsh(DAYCARE_DURATION);
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

    private QualificationDTO approvedQual() {
        QualificationDTO q = new QualificationDTO();
        q.setStatus_wsh(QualificationService.STATUS_APPROVED);
        return q;
    }

    private BusinessHours hours(int dayOfWeek, LocalTime open, LocalTime close, int closed) {
        BusinessHours h = new BusinessHours();
        h.setDay_of_week_wsh(dayOfWeek);
        h.setOpen_time_wsh(open);
        h.setClose_time_wsh(close);
        h.setIs_closed_wsh(closed);
        return h;
    }

    private void stubDaycare(Long serviceId, Long merchantId) {
        when(serviceItemMapper.selectById(serviceId)).thenReturn(daycare(serviceId, merchantId));
        when(merchantMapper.selectById(merchantId)).thenReturn(merchant(merchantId));
    }

    private List<String> slotsFor(LocalTime open, LocalTime close) {
        stubDaycare(9L, 2L);
        when(businessHoursService.getByMerchantId(2L)).thenReturn(List.of(hours(1, open, close, 0)));
        LocalDate mon = monday();
        ServiceAvailabilityVO vo = service.getAvailability(9L, mon, mon, null);
        DayAvailabilityVO d = vo.getDays_wsh().get(0);
        if (d.getWindows_wsh().isEmpty()) {
            return List.of();
        }
        return d.getWindows_wsh().get(0).getSlots_wsh();
    }

    // ============ 场景 1：09:00—18:00（恰好 540 分钟）→ 1 个完整槽位 ============

    @Test
    void window0900to1800ProducesExactlyOneFullSlot() {
        List<String> slots = slotsFor(LocalTime.of(9, 0), LocalTime.of(18, 0));
        assertEquals(1, slots.size(), "09:00-18:00 恰好容纳一个 540 分钟完整服务");
        assertTrue(slots.get(0).contains("T09:00:00"), "起始应为 09:00");
    }

    // ============ 场景 2：09:00—17:59（539 分钟）→ 0 个完整槽位 ============

    @Test
    void window0900to1759ProducesZeroFullSlots() {
        List<String> slots = slotsFor(LocalTime.of(9, 0), LocalTime.of(17, 59));
        assertTrue(slots.isEmpty(), "窗口不足 540 分钟时不得生成完整槽位");
    }

    // ============ 场景 3：08:00—17:00（540 分钟）→ 1 个完整槽位 ============

    @Test
    void window0800to1700ProducesOneFullSlot() {
        List<String> slots = slotsFor(LocalTime.of(8, 0), LocalTime.of(17, 0));
        assertEquals(1, slots.size());
        assertTrue(slots.get(0).contains("T08:00:00"), "起始应为 08:00");
    }

    // ============ 场景 4：09:00—20:00（660 分钟）→ 多个不重叠的完整槽位 ============

    @Test
    void window0900to2000ProducesNonOverlappingFullSlots() {
        List<String> slots = slotsFor(LocalTime.of(9, 0), LocalTime.of(20, 0));
        assertFalse(slots.isEmpty(), "660 分钟窗口应能生成完整槽位");
        // 每个起始时刻 + 540 分钟都不得越过 20:00；相邻起始间隔应为 30 分钟步长
        LocalTime close = LocalTime.of(20, 0);
        LocalTime previous = null;
        for (String slot : slots) {
            LocalTime start = LocalTime.parse(slot.substring(slot.indexOf('T') + 1));
            assertFalse(start.plusMinutes(DAYCARE_DURATION).isAfter(close),
                    "槽位 " + slot + " 的服务时段越过营业结束时间");
            if (previous != null) {
                assertEquals(SLOT_MINUTES, (int) java.time.Duration.between(previous, start).toMinutes(),
                        "相邻起始时刻应按 30 分钟步长递增");
            }
            previous = start;
        }
        assertTrue(slots.get(0).contains("T09:00:00"));
    }

    // ============ 场景 5：09:00—18:00 已有占用 → 不生成冲突槽位 ============

    @Test
    void occupiedWindowDoesNotProduceConflictingSlots() {
        stubDaycare(9L, 2L);
        when(keeperMapper.selectById(5L)).thenReturn(keeper(5L, 2L));
        when(qualificationService.listByOwner(eq(QualificationService.OWNER_TYPE_KEEPER), eq(5L), eq(false)))
                .thenReturn(List.of(approvedQual()));
        when(businessHoursService.getByMerchantId(2L)).thenReturn(
                List.of(hours(1, LocalTime.of(9, 0), LocalTime.of(18, 0), 0)));
        LocalDate mon = monday();
        // 两条覆盖当天的有效占用订单 → 容量满，当天不可预约、不产生任何可用槽位
        PetOrder o1 = new PetOrder();
        o1.setKeeper_id_wsh(5L);
        o1.setStart_date_wsh(mon.minusDays(1));
        o1.setEnd_date_wsh(mon.plusDays(1));
        PetOrder o2 = new PetOrder();
        o2.setKeeper_id_wsh(5L);
        o2.setStart_date_wsh(mon);
        o2.setEnd_date_wsh(mon.plusDays(2));
        when(orderMapper.selectList(any())).thenReturn(List.of(o1, o2));

        ServiceAvailabilityVO vo = service.getAvailability(9L, mon, mon, 5L);
        DayAvailabilityVO d = vo.getDays_wsh().get(0);
        assertFalse(d.getBookable_wsh());
        assertEquals(BookingErrorCode.CAPACITY_EXCEEDED, d.getReason_code_wsh());
        assertTrue(d.getWindows_wsh().isEmpty(), "已有占用时不得生成冲突槽位");
    }
}
