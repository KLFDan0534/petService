package com.pet.boarding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.dto.AvailabilityWindowVO;
import com.pet.boarding.dto.DayAvailabilityVO;
import com.pet.boarding.dto.ServiceAvailabilityVO;
import com.pet.boarding.entity.BusinessHours;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.mapper.ServiceItemMapper;
import com.pet.boarding.service.BusinessHoursService;
import com.pet.boarding.service.BusinessHoursTargetResolver;
import com.pet.boarding.service.KeeperLeaveService;
import com.pet.boarding.service.ServiceAvailabilityService;
import com.pet.common.BookingErrorCode;
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
import java.util.List;
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

    /** 单次查询最大天数（含首尾） */
    public static final int MAX_RANGE_DAYS = 31;

    private static final DateTimeFormatter SLOT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /** 与下单容量校验一致的进行中订单状态集合 */
    private static final Set<String> BOOKING_STATUSES = Set.of(
            OrderStatus.PENDING, OrderStatus.PAID, OrderStatus.CONFIRMED,
            OrderStatus.DELIVERED, OrderStatus.RECEIVED, OrderStatus.IN_PROGRESS,
            OrderStatus.REFUNDING);

    private final ServiceItemMapper serviceItemMapper;
    private final MerchantMapper merchantMapper;
    private final KeeperMapper keeperMapper;
    private final OrderMapper orderMapper;
    private final BusinessHoursService businessHoursService;
    private final BusinessHoursTargetResolver resolver;
    private final KeeperLeaveService keeperLeaveService;
    private final QualificationService qualificationService;
    private final int slotMinutes;

    public ServiceAvailabilityServiceImpl(ServiceItemMapper serviceItemMapper,
                                          MerchantMapper merchantMapper,
                                          KeeperMapper keeperMapper,
                                          OrderMapper orderMapper,
                                          BusinessHoursService businessHoursService,
                                          BusinessHoursTargetResolver resolver,
                                          KeeperLeaveService keeperLeaveService,
                                          QualificationService qualificationService,
                                          @Value("${booking.slot-minutes:30}") int slotMinutes) {
        this.serviceItemMapper = serviceItemMapper;
        this.merchantMapper = merchantMapper;
        this.keeperMapper = keeperMapper;
        this.orderMapper = orderMapper;
        this.businessHoursService = businessHoursService;
        this.resolver = resolver;
        this.keeperLeaveService = keeperLeaveService;
        this.qualificationService = qualificationService;
        this.slotMinutes = slotMinutes;
    }

    @Override
    public ServiceAvailabilityVO getAvailability(Long serviceId, LocalDate from, LocalDate to, Long keeperId) {
        validateRange(from, to);

        ServiceItem service = requireVisibleService(serviceId);
        Merchant merchant = requireFutureBookingEligibleMerchant(service.getMerchant_id_wsh());
        Keeper keeper = keeperId == null ? null : requireQualifiedKeeper(keeperId, service.getMerchant_id_wsh());

        List<BusinessHours> hours = businessHoursService.getByMerchantId(merchant.getId_wsh());
        String scheduleSource = hours == null || hours.isEmpty() ? "legacy_unrestricted" : "business_hours";

        List<DayAvailabilityVO> days = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            days.add(buildDay(date, hours, scheduleSource, keeper));
        }

        ServiceAvailabilityVO vo = new ServiceAvailabilityVO();
        vo.setService_id_wsh(service.getId_wsh());
        vo.setMerchant_id_wsh(merchant.getId_wsh());
        vo.setService_version_wsh(formatVersion(service.getUpdated_at_wsh()));
        vo.setTimezone_wsh(BusinessHoursTargetResolver.ZONE.getId());
        vo.setSlot_minutes_wsh(slotMinutes);
        vo.setSchedule_source_wsh(scheduleSource);
        vo.setGenerated_at_wsh(LocalDateTime.now(BusinessHoursTargetResolver.ZONE));
        vo.setDays_wsh(days);
        return vo;
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessException(400, BookingErrorCode.AVAILABILITY_RANGE_INVALID, "可用性查询日期不能为空");
        }
        if (from.isBefore(LocalDate.now())) {
            throw new BusinessException(400, BookingErrorCode.AVAILABILITY_RANGE_INVALID, "起始日期不能早于今天");
        }
        if (to.isBefore(from)) {
            throw new BusinessException(400, BookingErrorCode.AVAILABILITY_RANGE_INVALID, "结束日期不能早于起始日期");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_RANGE_DAYS) {
            throw new BusinessException(400, BookingErrorCode.AVAILABILITY_RANGE_INVALID, "查询范围不能超过" + MAX_RANGE_DAYS + "天");
        }
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

    private DayAvailabilityVO buildDay(LocalDate date, List<BusinessHours> hours, String scheduleSource, Keeper keeper) {
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
            } else if (keeperCapacityFull(keeper, date)) {
                bookable = false;
                reasonCode = BookingErrorCode.CAPACITY_EXCEEDED;
                windows = List.of();
            }
        }

        DayAvailabilityVO day = new DayAvailabilityVO();
        day.setDate_wsh(date);
        day.setBookable_wsh(bookable);
        day.setReason_code_wsh(reasonCode);
        day.setWindows_wsh(toWindowVOs(windows));
        return day;
    }

    private boolean keeperCapacityFull(Keeper keeper, LocalDate date) {
        Integer maxPets = keeper.getMax_pets_wsh();
        if (maxPets == null || maxPets <= 0) {
            return true;
        }
        Long count = orderMapper.selectCount(new LambdaQueryWrapper<PetOrder>()
                .eq(PetOrder::getKeeper_id_wsh, keeper.getId_wsh())
                .in(PetOrder::getStatus_wsh, BOOKING_STATUSES)
                .lt(PetOrder::getStart_date_wsh, date.plusDays(1))
                .gt(PetOrder::getEnd_date_wsh, date));
        return count != null && count >= maxPets;
    }

    private List<AvailabilityWindowVO> toWindowVOs(List<BusinessHoursTargetResolver.BusinessWindow> windows) {
        List<AvailabilityWindowVO> vos = new ArrayList<>(windows.size());
        for (BusinessHoursTargetResolver.BusinessWindow w : windows) {
            AvailabilityWindowVO vo = new AvailabilityWindowVO();
            vo.setStart_wsh(w.start());
            vo.setEnd_wsh(w.end());
            vo.setSlots_wsh(generateSlots(w.start(), w.end()));
            vos.add(vo);
        }
        return vos;
    }

    private List<String> generateSlots(LocalDateTime windowStart, LocalDateTime windowEnd) {
        List<String> slots = new ArrayList<>();
        LocalDateTime t = windowStart;
        while (t.isBefore(windowEnd)) {
            slots.add(t.format(SLOT_FORMATTER));
            t = t.plusMinutes(slotMinutes);
        }
        return slots;
    }

    private String formatVersion(LocalDateTime updatedAt) {
        return ServiceVersions.format(updatedAt);
    }
}
