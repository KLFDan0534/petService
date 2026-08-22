package com.pet.boarding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.dto.AvailabilityWindowVO;
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
import com.pet.boarding.service.BusinessHoursService;
import com.pet.boarding.service.BusinessHoursTargetResolver;
import com.pet.boarding.service.KeeperLeaveService;
import com.pet.boarding.service.ServiceAvailabilityService;
import com.pet.common.BookingErrorCode;
import com.pet.common.BookingUnit;
import com.pet.common.BusinessException;
import com.pet.common.OrderStatus;
import com.pet.common.ServiceVersions;
import com.pet.common.StatusCode;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.qualification.dto.QualificationDTO;
import com.pet.qualification.service.QualificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * U2: 服务动态可预约性查询服务实现。
 *
 * <p>校验链：日期范围 → 服务（存在/上架）→ 商家（审核通过/开放未来预约）→
 * 可选看护员（归属/状态/资质）→ 按日生成窗口与槽位（营业时间配置 + 请假/容量叠加）。
 *
 * <p>只读契约：本实现不调用任何写方法，容量统计与看护员判定与
 * {@link com.pet.order.service.impl.OrderServiceImpl} 的下单校验保持一致。
 */
@Service
@Slf4j
public class ServiceAvailabilityServiceImpl implements ServiceAvailabilityService {

    private static final DateTimeFormatter SLOT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /** 预约窗口默认天数（含首尾，与 application.yml booking.max-booking-days 默认值一致） */
    static final int DEFAULT_MAX_BOOKING_DAYS = 91;

    /** 与下单容量校验一致的进行中订单状态集合 */
    private static final Set<String> BOOKING_STATUSES = Set.of(
            OrderStatus.PENDING, OrderStatus.PAID, OrderStatus.CONFIRMED,
            OrderStatus.DELIVERED, OrderStatus.RECEIVED, OrderStatus.IN_PROGRESS,
            OrderStatus.REFUNDING);

    private final ServiceItemMapper serviceItemMapper;
    private final MerchantMapper merchantMapper;
    private final ServiceCategoryMapper categoryMapper;
    private final KeeperMapper keeperMapper;
    private final OrderMapper orderMapper;
    private final BusinessHoursService businessHoursService;
    private final BusinessHoursTargetResolver resolver;
    private final KeeperLeaveService keeperLeaveService;
    private final QualificationService qualificationService;
    private final int slotMinutes;
    private final int maxBookingDays;

    public ServiceAvailabilityServiceImpl(ServiceItemMapper serviceItemMapper,
                                          MerchantMapper merchantMapper,
                                          ServiceCategoryMapper categoryMapper,
                                          KeeperMapper keeperMapper,
                                          OrderMapper orderMapper,
                                          BusinessHoursService businessHoursService,
                                          BusinessHoursTargetResolver resolver,
                                          KeeperLeaveService keeperLeaveService,
                                          QualificationService qualificationService,
                                          @Value("${booking.slot-minutes:30}") int slotMinutes,
                                          @Value("${booking.max-booking-days:91}") int maxBookingDays) {
        this.serviceItemMapper = serviceItemMapper;
        this.merchantMapper = merchantMapper;
        this.categoryMapper = categoryMapper;
        this.keeperMapper = keeperMapper;
        this.orderMapper = orderMapper;
        this.businessHoursService = businessHoursService;
        this.resolver = resolver;
        this.keeperLeaveService = keeperLeaveService;
        this.qualificationService = qualificationService;
        this.slotMinutes = slotMinutes;
        this.maxBookingDays = maxBookingDays > 0 ? maxBookingDays : DEFAULT_MAX_BOOKING_DAYS;
    }

    @Override
    public ServiceAvailabilityVO getAvailability(Long serviceId, LocalDate from, LocalDate to, Long keeperId) {
        LocalDate effectiveTo = clampRange(from, to);
        if (serviceId == null || serviceId <= 0) {
            throw new BusinessException(400, BookingErrorCode.INVALID_PRODUCT_ID, "服务ID必须为正数");
        }

        ServiceItem service = requireVisibleService(serviceId);
        requireEnabledCategory(service);
        Merchant merchant = requireFutureBookingEligibleMerchant(service.getMerchant_id_wsh());
        Keeper keeper = keeperId == null ? null : requireQualifiedKeeper(keeperId, service.getMerchant_id_wsh());

        List<BusinessHours> hours = businessHoursService.getByMerchantId(merchant.getId_wsh());
        String scheduleSource = hours == null || hours.isEmpty() ? "legacy_unrestricted" : "business_hours";

        List<DayAvailabilityVO> days = new ArrayList<>();
        String unit = normalizeServiceUnit(service);
        int durationMinutes = BookingUnit.resolveDurationMinutes(unit, service.getDuration_minutes_wsh());
        // 看护人容量按窗口一次性加载并内存计数，避免逐日 N+1 查询（窗口放大后查询次数保持恒定）。
        Map<LocalDate, Integer> capacityByDate = keeper == null
                ? Map.of()
                : loadCapacityCountByDate(keeper, from, effectiveTo);
        for (LocalDate date = from; !date.isAfter(effectiveTo); date = date.plusDays(1)) {
            days.add(buildDay(date, hours, scheduleSource, keeper, unit, durationMinutes, capacityByDate));
        }

        ServiceAvailabilityVO vo = new ServiceAvailabilityVO();
        vo.setService_id_wsh(service.getId_wsh());
        vo.setMerchant_id_wsh(merchant.getId_wsh());
        vo.setService_version_wsh(formatVersion(service.getUpdated_at_wsh()));
        vo.setUnit_wsh(unit);
        vo.setBooking_mode_wsh(BookingUnit.bookingMode(unit));
        vo.setDuration_minutes_wsh(durationMinutes);
        vo.setPrice_wsh(service.getPrice_wsh());
        vo.setTimezone_wsh(BusinessHoursTargetResolver.ZONE.getId());
        vo.setSlot_minutes_wsh(slotMinutes);
        vo.setSchedule_source_wsh(scheduleSource);
        vo.setGenerated_at_wsh(LocalDateTime.now(BusinessHoursTargetResolver.ZONE));
        vo.setBooking_window_days_wsh(maxBookingDays);
        vo.setDays_wsh(days);
        return vo;
    }

    /**
     * 校验并收敛查询范围：拒绝空/过去/倒置日期；请求范围超过预约窗口时
     * 按 booking.max-booking-days 收敛到窗口内（响应通过 booking_window_days_wsh 告知窗口）。
     */
    private LocalDate clampRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessException(400, BookingErrorCode.AVAILABILITY_RANGE_INVALID, "可用性查询日期不能为空");
        }
        if (from.isBefore(LocalDate.now())) {
            throw new BusinessException(400, BookingErrorCode.AVAILABILITY_RANGE_INVALID, "起始日期不能早于今天");
        }
        if (to.isBefore(from)) {
            throw new BusinessException(400, BookingErrorCode.AVAILABILITY_RANGE_INVALID, "结束日期不能早于起始日期");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > maxBookingDays) {
            return from.plusDays(maxBookingDays - 1);
        }
        return to;
    }

    private ServiceItem requireVisibleService(Long serviceId) {
        ServiceItem service = serviceItemMapper.selectById(serviceId);
        if (service == null) {
            throw new BusinessException(404, BookingErrorCode.SERVICE_NOT_FOUND, "服务不存在");
        }
        if (service.getStatus_wsh() == null
                || service.getStatus_wsh() != StatusCode.SERVICE_ENABLED.getValue()) {
            throw new BusinessException(400, BookingErrorCode.SERVICE_OFF_SHELF, "服务已下架");
        }
        return service;
    }

    private void requireEnabledCategory(ServiceItem service) {
        if (service.getCategory_id_wsh() == null) {
            return;
        }
        ServiceCategory category = categoryMapper.selectById(service.getCategory_id_wsh());
        if (category == null || category.getStatus_wsh() == null
                || category.getStatus_wsh() != StatusCode.SERVICE_ENABLED.getValue()) {
            throw new BusinessException(400, BookingErrorCode.SERVICE_OFF_SHELF, "服务所属分类已下架");
        }
    }

    private Merchant requireFutureBookingEligibleMerchant(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(404, BookingErrorCode.SERVICE_NOT_FOUND, "商户不存在");
        }
        if (merchant.getStatus_wsh() == null
                || merchant.getStatus_wsh() != StatusCode.MERCHANT_APPROVED.getValue()) {
            throw new BusinessException(400, BookingErrorCode.MERCHANT_NOT_APPROVED, "商家未通过审核");
        }
        if (merchant.getFuture_booking_enabled_wsh() == null
                || merchant.getFuture_booking_enabled_wsh() != 1) {
            throw new BusinessException(400, BookingErrorCode.FUTURE_BOOKING_DISABLED, "商家未开放未来预约");
        }
        return merchant;
    }

    private Keeper requireQualifiedKeeper(Long keeperId, Long merchantId) {
        Keeper keeper = keeperMapper.selectById(keeperId);
        if (keeper == null) {
            throw new BusinessException(400, BookingErrorCode.KEEPER_NOT_BOOKABLE, "看护者不存在");
        }
        if (!merchantId.equals(keeper.getMerchant_id_wsh())) {
            throw new BusinessException(400, BookingErrorCode.KEEPER_MERCHANT_MISMATCH, "看护者不属于服务所属商家");
        }
        Integer status = keeper.getStatus_wsh();
        boolean inService = status != null
                && (status == StatusCode.KEEPER_ACTIVE.getValue()
                || status == StatusCode.KEEPER_OFFLINE.getValue()
                || status == StatusCode.KEEPER_BUSY.getValue());
        if (!inService) {
            throw new BusinessException(400, BookingErrorCode.KEEPER_NOT_BOOKABLE, "看护者当前不可接单");
        }
        List<QualificationDTO> quals = qualificationService.listByOwner(
                QualificationService.OWNER_TYPE_KEEPER, keeperId, false);
        boolean qualified = quals.stream()
                .anyMatch(q -> QualificationService.STATUS_APPROVED.equals(q.getStatus_wsh()));
        if (!qualified) {
            throw new BusinessException(400, BookingErrorCode.KEEPER_NOT_QUALIFIED, "看护者资质未通过");
        }
        return keeper;
    }

    private DayAvailabilityVO buildDay(LocalDate date, List<BusinessHours> hours, String scheduleSource,
                                       Keeper keeper, String unit, int durationMinutes,
                                       Map<LocalDate, Integer> capacityByDate) {
        List<BusinessHoursTargetResolver.BusinessWindow> windows = "legacy_unrestricted".equals(scheduleSource)
                ? List.of(new BusinessHoursTargetResolver.BusinessWindow(
                        date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                : resolver.windowsForDate(hours, date);

        boolean bookable = !windows.isEmpty();
        String reasonCode = null;
        if (windows.isEmpty()) {
            reasonCode = BookingErrorCode.MERCHANT_REST_DAY;
        }
        if (keeper != null) {
            if (keeperLeaveService.isKeeperOnLeave(keeper.getId_wsh(), date)) {
                bookable = false;
                reasonCode = BookingErrorCode.KEEPER_ON_LEAVE;
                windows = List.of();
            } else if (isDayAtCapacity(keeper, date, capacityByDate)) {
                bookable = false;
                reasonCode = BookingErrorCode.CAPACITY_EXCEEDED;
                windows = List.of();
            }
        }

        DayAvailabilityVO day = new DayAvailabilityVO();
        day.setDate_wsh(date);
        day.setBookable_wsh(bookable);
        day.setReason_code_wsh(reasonCode);
        day.setWindows_wsh(toWindowVOs(windows, unit, durationMinutes));
        return day;
    }

    /**
     * 一次性加载看护人在查询窗口内的全部有效订单，按天累加占用数（半开区间 [start,end)）。
     * 与原逐日 selectCount 判定等价，但查询次数与窗口天数解耦。
     */
    private Map<LocalDate, Integer> loadCapacityCountByDate(Keeper keeper, LocalDate from, LocalDate to) {
        List<PetOrder> overlapping = orderMapper.selectList(new LambdaQueryWrapper<PetOrder>()
                .eq(PetOrder::getKeeper_id_wsh, keeper.getId_wsh())
                .in(PetOrder::getStatus_wsh, BOOKING_STATUSES)
                .lt(PetOrder::getStart_date_wsh, to.plusDays(1))
                .gt(PetOrder::getEnd_date_wsh, from.minusDays(1)));
        Map<LocalDate, Integer> counts = new HashMap<>();
        for (PetOrder order : overlapping) {
            LocalDate start = order.getStart_date_wsh();
            LocalDate end = order.getEnd_date_wsh();
            if (start == null || end == null) {
                continue;
            }
            LocalDate d = start.isAfter(from) ? start : from;
            LocalDate limit = end.isBefore(to.plusDays(1)) ? end : to.plusDays(1);
            while (d.isBefore(limit)) {
                counts.merge(d, 1, Integer::sum);
                d = d.plusDays(1);
            }
        }
        return counts;
    }

    private boolean isDayAtCapacity(Keeper keeper, LocalDate date, Map<LocalDate, Integer> capacityByDate) {
        Integer maxPets = keeper.getMax_pets_wsh();
        if (maxPets == null || maxPets <= 0) {
            return true;
        }
        return capacityByDate.getOrDefault(date, 0) >= maxPets;
    }

    private List<AvailabilityWindowVO> toWindowVOs(List<BusinessHoursTargetResolver.BusinessWindow> windows,
                                                   String unit, int durationMinutes) {
        List<AvailabilityWindowVO> vos = new ArrayList<>(windows.size());
        for (BusinessHoursTargetResolver.BusinessWindow w : windows) {
            AvailabilityWindowVO vo = new AvailabilityWindowVO();
            vo.setStart_wsh(w.start());
            vo.setEnd_wsh(w.end());
            vo.setSlots_wsh(generateSlots(w.start(), w.end(), unit, durationMinutes));
            vos.add(vo);
        }
        return vos;
    }

    /**
     * 生成可选起始时间。slot 模式（session/hour）只暴露“完整服务时长能落进
     * 该窗口”的起始槽位：仅当 start + duration <= end 才可选，避免选择后才发现
     * 时长覆盖不完整。day 模式保持与旧接口一致的槽位列表。
     * hour 单位的起始槽位必须对齐整点（按 60 分钟步进），保证“连续整数小时”。
     */
    private List<String> generateSlots(LocalDateTime windowStart, LocalDateTime windowEnd,
                                       String unit, int durationMinutes) {
        List<String> slots = new ArrayList<>();
        boolean slotMode = BookingUnit.MODE_SLOT.equals(BookingUnit.bookingMode(unit));
        int stepMinutes = BookingUnit.HOUR.equals(unit) ? 60 : slotMinutes;
        LocalDateTime t = windowStart;
        if (BookingUnit.HOUR.equals(unit)) {
            // 整点对齐：即使窗口开在非整点(如 09:30)，第一个可选起始也顺延到下一个整点。
            LocalDateTime aligned = windowStart.withMinute(0).withSecond(0).withNano(0);
            if (aligned.isBefore(windowStart)) {
                aligned = aligned.plusHours(1);
            }
            t = aligned;
        }
        while (t.isBefore(windowEnd)) {
            if (!slotMode || !t.plusMinutes(durationMinutes).isAfter(windowEnd)) {
                slots.add(t.format(SLOT_FORMATTER));
            }
            t = t.plusMinutes(stepMinutes);
        }
        return slots;
    }

    /** 服务单位归属校验：未知/无法解析的单位不允许参与预约；历史空单位按 day 兼容。 */
    private String normalizeServiceUnit(ServiceItem service) {
        String unit = BookingUnit.normalize(service.getUnit_wsh());
        if (unit == null) {
            if (service.getUnit_wsh() == null || service.getUnit_wsh().isBlank()) {
                // 历史数据 unit_wsh 为空 → 按 day 语义兼容（与 OrderServiceImpl 保持一致）
                return BookingUnit.DAY;
            }
            throw new BusinessException(400, BookingErrorCode.UNSUPPORTED_SERVICE_UNIT,
                    "服务计费单位不受支持，无法预约");
        }
        return unit;
    }

    private String formatVersion(LocalDateTime updatedAt) {
        return ServiceVersions.format(updatedAt);
    }
}
