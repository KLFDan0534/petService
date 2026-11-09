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
import com.pet.order.occupancy.OrderOccupancy;
import com.pet.order.pricing.OrderPricing;
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
            OrderStatus.RECEIVED, OrderStatus.IN_PROGRESS,
            OrderStatus.REFUNDING);

    /** 已在店状态：逾期未完成仍占容量，候选查询不能按旧 end_date 把它们过滤掉 */
    private static final Set<String> IN_STORE_STATUSES = Set.of(
            OrderStatus.RECEIVED, OrderStatus.IN_PROGRESS);

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

    /**
     * 【构造可预约性查询服务】
     *
     * 业务作用：注入服务项、商家、分类、看护者、订单五个 Mapper，以及营业时间、看护者休假、资质三个服务，并读取槽位粒度与预约窗口配置。
     *
     * 调用场景：Spring 容器启动创建 Bean 时调用。
     *
     * 调用链：Spring 容器 → 本构造器 → 赋值各依赖字段与 slotMinutes / maxBookingDays。
     *
     * 数据处理：slotMinutes 取自 booking.slot-minutes（默认 30），maxBookingDays 取自 booking.max-booking-days（默认 91）；配置值不大于 0 时回退为 DEFAULT_MAX_BOOKING_DAYS。
     *
     * 业务规则：预约窗口天数必须为正，否则使用常量默认值 91。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：该类为只读实现，不调用任何写方法。
     */
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

    /**
     * 【查询服务动态可预约性】
     *
     * 业务作用：按日期范围逐日返回服务的可预约窗口与可选起始槽位，可叠加看护者维度。
     *
     * 调用场景：用户在服务详情页选择日期/看护员后查询档期。
     *
     * 调用链：ServiceItemController.availability() → getAvailability() → clampRange() / requireVisibleService() / requireEnabledCategory() / requireFutureBookingEligibleMerchant() / requireQualifiedKeeper() → businessHoursService.getByMerchantId() → loadCapacityCountByDate() → buildDay()。
     *
     * 数据处理：先收敛日期范围，再校验服务/分类/商家/看护者，读取营业时间后逐日生成 DayAvailabilityVO，并汇总服务版本、计费单位、时区、槽位粒度等元信息。
     *
     * 业务规则：serviceId 必须为正数；日期范围不合法直接抛异常；无营业时间配置时按 legacy_unrestricted 全天可约；看护者容量按窗口一次性加载后在内存按天累加。
     *
     * 状态影响：无，只读接口。
     *
     * 异常情况：服务ID非法、服务下架、分类下架、商家未审核、商家未开放未来预约、看护者不可接单或资质未通过，均抛 BusinessException(400/404)。
     *
     * 注意事项：容量统计口径与 OrderServiceImpl 下单校验保持一致，修改任一侧需同步。
     */
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
        // 看护人占用区间按窗口一次性加载，逐日逐槽位用同一套事件扫描判定容量，
        // 避免逐日 N+1 查询，也避免「当天曾满员就整天关闭」的粗粒度误判。
        List<OrderOccupancy.Interval> occupied = keeper == null
                ? List.of()
                : loadOccupancyIntervals(keeper, from, effectiveTo);
        for (LocalDate date = from; !date.isAfter(effectiveTo); date = date.plusDays(1)) {
            days.add(buildDay(date, hours, scheduleSource, keeper, unit, durationMinutes, occupied));
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
        if (BookingUnit.NIGHT.equals(unit)) {
            // 收费规则由服务端返回，前端只展示、不另写公式
            vo.setCheckin_anchor_time_wsh(OrderPricing.CHECKIN_ANCHOR_TIME.toString());
            vo.setCheckout_grace_minutes_wsh(OrderPricing.CHECKOUT_GRACE_MINUTES);
            vo.setEarly_limit_minutes_wsh(OrderPricing.EARLY_LIMIT_MINUTES);
            vo.setHourly_price_wsh(OrderPricing.nightHourlyPrice(service.getPrice_wsh()));
            vo.setOvertime_price_mode_wsh(OrderPricing.OVERTIME_PRICE_MODE);
        }
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

    /**
     * 【校验服务存在且上架】
     *
     * 业务作用：查询服务项并校验其处于启用状态。
     *
     * 调用场景：getAvailability 校验链第一步。
     *
     * 调用链：getAvailability() → requireVisibleService() → serviceItemMapper.selectById()。
     *
     * 数据处理：按主键查询服务项，比对 status_wsh 与 StatusCode.SERVICE_ENABLED。
     *
     * 业务规则：服务不存在或已下架都不可预约。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：服务不存在抛 BusinessException(404, SERVICE_NOT_FOUND)；已下架抛 400, SERVICE_OFF_SHELF。
     *
     * 注意事项：错误码取自 BookingErrorCode，前端据此做提示映射。
     */
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

    /**
     * 【校验服务分类上架】
     *
     * 业务作用：校验服务所属分类存在且处于启用状态。
     *
     * 调用场景：getAvailability 校验链第二步。
     *
     * 调用链：getAvailability() → requireEnabledCategory() → categoryMapper.selectById()。
     *
     * 数据处理：服务未配置分类时直接返回；否则按分类 ID 查询并比对状态。
     *
     * 业务规则：分类缺失或下架时该服务不可预约。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：分类不存在或已下架抛 BusinessException(400, SERVICE_OFF_SHELF)「服务所属分类已下架」。
     *
     * 注意事项：category_id_wsh 为空的历史服务会跳过本校验。
     */
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

    /**
     * 【校验商家可开放未来预约】
     *
     * 业务作用：校验商家存在、已审核通过且已开启未来预约开关。
     *
     * 调用场景：getAvailability 校验链第三步。
     *
     * 调用链：getAvailability() → requireFutureBookingEligibleMerchant() → merchantMapper.selectById()。
     *
     * 数据处理：按商家 ID 查询后依次校验 status_wsh 与 future_booking_enabled_wsh。
     *
     * 业务规则：状态必须为 MERCHANT_APPROVED，future_booking_enabled_wsh 必须等于 1。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：商家不存在抛 404；未通过审核抛 400, MERCHANT_NOT_APPROVED；未开放未来预约抛 400, FUTURE_BOOKING_DISABLED。
     *
     * 注意事项：OrderServiceImpl 下单时复用同一套判定，两处口径必须一致。
     */
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

    /**
     * 【校验看护者可接单】
     *
     * 业务作用：校验看护者存在、归属该商家、处于可接单状态且资质已通过。
     *
     * 调用场景：getAvailability 传入 keeperId 时。
     *
     * 调用链：getAvailability() → requireQualifiedKeeper() → keeperMapper.selectById() → qualificationService.listByOwner()。
     *
     * 数据处理：比对 keeper.merchant_id_wsh 与服务所属商家；状态需为 ACTIVE / OFFLINE / BUSY 之一；资质列表中存在 APPROVED 记录才算合格。
     *
     * 业务规则：看护者必须属于该商家，且至少有一条已通过资质。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：看护者不存在或不可接单抛 400, KEEPER_NOT_BOOKABLE；归属不符抛 400, KEEPER_MERCHANT_MISMATCH；资质未通过抛 400, KEEPER_NOT_QUALIFIED。
     *
     * 注意事项：OFFLINE 与 BUSY 也算可接单状态，只有离职/停用等状态才拒绝。
     */
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

    /**
     * 【构建单日可预约结果】
     *
     * 业务作用：计算某一天的营业窗口、是否可约以及不可约原因，并生成对应槽位。
     *
     * 调用场景：getAvailability 逐日循环。
     *
     * 调用链：getAvailability() → buildDay() → resolver.windowsForDate() / keeperLeaveService.isKeeperOnLeave() / isDayAtCapacity() → toWindowVOs()。
     *
     * 数据处理：无营业配置时构造 00:00~次日 00:00 的全天窗口；有配置时按日期解析营业窗口；再叠加看护者休假与容量判定。
     *
     * 业务规则：窗口为空即休息日；看护者休假或满容量时清空窗口并把 bookable 置为 false。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：无。
     *
     * 注意事项：reason_code 优先级为休息日 < 休假 < 容量超限，后判定者会覆盖前者。
     */
    private DayAvailabilityVO buildDay(LocalDate date, List<BusinessHours> hours, String scheduleSource,
                                       Keeper keeper, String unit, int durationMinutes,
                                       List<OrderOccupancy.Interval> occupied) {
        List<BusinessHoursTargetResolver.BusinessWindow> windows = "legacy_unrestricted".equals(scheduleSource)
                ? List.of(new BusinessHoursTargetResolver.BusinessWindow(
                        date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                : resolver.windowsForDate(hours, date);

        boolean bookable = !windows.isEmpty();
        String reasonCode = windows.isEmpty() ? BookingErrorCode.MERCHANT_REST_DAY : null;
        List<AvailabilityWindowVO> windowVOs = List.of();
        if (windows.isEmpty()) {
            // 休息日：无窗口
        } else if (keeper != null && keeperLeaveService.isKeeperOnLeave(keeper.getId_wsh(), date)) {
            bookable = false;
            reasonCode = BookingErrorCode.KEEPER_ON_LEAVE;
        } else {
            // 容量按「每个候选槽位」判定：当天早些时候满员不影响 14:30 之后的空闲窗口
            windowVOs = toWindowVOs(windows, unit, durationMinutes, keeper, occupied);
            if (keeper != null && windowVOs.isEmpty()) {
                bookable = false;
                reasonCode = BookingErrorCode.CAPACITY_EXCEEDED;
            }
        }

        DayAvailabilityVO day = new DayAvailabilityVO();
        day.setDate_wsh(date);
        day.setBookable_wsh(bookable);
        day.setReason_code_wsh(reasonCode);
        day.setWindows_wsh(bookable ? windowVOs : List.of());
        return day;
    }

    /**
     * 一次性加载看护人在查询窗口内的占用区间。
     * <p>
     * 候选集必须包含「已在店但预约终点已过」的订单（SQL 不能先用旧 end_date 把它们过滤掉），
     * 否则跨日逾期单会被当成不占容量。区间语义由 {@link OrderOccupancy} 统一推导。
     */
    private List<OrderOccupancy.Interval> loadOccupancyIntervals(Keeper keeper, LocalDate from, LocalDate to) {
        List<PetOrder> overlapping = orderMapper.selectList(new LambdaQueryWrapper<PetOrder>()
                .eq(PetOrder::getKeeper_id_wsh, keeper.getId_wsh())
                .in(PetOrder::getStatus_wsh, BOOKING_STATUSES)
                .and(w -> w
                        .and(x -> x.lt(PetOrder::getStart_date_wsh, to.plusDays(1))
                                .ge(PetOrder::getEnd_date_wsh, from.minusDays(1)))
                        .or()
                        .in(PetOrder::getStatus_wsh, IN_STORE_STATUSES)));
        List<OrderOccupancy.Interval> intervals = new ArrayList<>(overlapping.size());
        for (PetOrder order : overlapping) {
            OrderOccupancy.Interval interval = OrderOccupancy.intervalOf(order);
            if (interval != null && interval.isValid()) {
                intervals.add(interval);
            }
        }
        return intervals;
    }

    /**
     * 判断某个候选槽位是否还有容量：用事件扫描求峰值，峰值不超过 max_pets 即可约。
     */
    private boolean slotFits(Keeper keeper, List<OrderOccupancy.Interval> occupied,
                             String unit, LocalDateTime slotStart, int durationMinutes) {
        Integer maxPets = keeper.getMax_pets_wsh();
        if (maxPets == null || maxPets <= 0) {
            return false;
        }
        OrderOccupancy.Interval candidate = candidateInterval(unit, slotStart, durationMinutes);
        if (candidate == null) {
            return true;
        }
        return OrderOccupancy.peakConcurrency(occupied, candidate) <= maxPets;
    }

    /**
     * 候选占用区间：night 按「一待 + 免费宽限」估算（[A, 次日 14:30)），
     * session/hour 按实际服务时长。
     */
    private OrderOccupancy.Interval candidateInterval(String unit, LocalDateTime slotStart, int durationMinutes) {
        if (BookingUnit.NIGHT.equals(unit)) {
            LocalDateTime end = slotStart.toLocalDate().plusDays(1)
                    .atTime(OrderPricing.CHECKIN_ANCHOR_TIME)
                    .plusMinutes(OrderPricing.CHECKOUT_GRACE_MINUTES);
            return new OrderOccupancy.Interval(slotStart, end);
        }
        return new OrderOccupancy.Interval(slotStart, slotStart.plusMinutes(durationMinutes));
    }

    /**
     * 【营业窗口转VO】
     *
     * 业务作用：把内部营业窗口对象转换为前端可用的窗口 VO，并生成每个窗口的起始槽位。
     *
     * 调用场景：buildDay 生成 DayAvailabilityVO 时。
     *
     * 调用链：buildDay() → toWindowVOs() → generateSlots()。
     *
     * 数据处理：逐窗口复制起止时间，并调用 generateSlots 生成槽位字符串列表。
     *
     * 业务规则：按窗口数量预分配列表容量。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：槽位生成规则由 generateSlots 决定，本方法只做结构转换。
     */
    private List<AvailabilityWindowVO> toWindowVOs(List<BusinessHoursTargetResolver.BusinessWindow> windows,
                                                   String unit, int durationMinutes,
                                                   Keeper keeper,
                                                   List<OrderOccupancy.Interval> occupied) {
        List<AvailabilityWindowVO> vos = new ArrayList<>(windows.size());
        for (BusinessHoursTargetResolver.BusinessWindow w : windows) {
            List<String> slots = generateSlots(w.start(), w.end(), unit, durationMinutes);
            if (keeper != null) {
                List<String> available = new ArrayList<>(slots.size());
                for (String slot : slots) {
                    LocalDateTime slotStart = LocalDateTime.parse(slot, SLOT_FORMATTER);
                    if (slotFits(keeper, occupied, unit, slotStart, durationMinutes)) {
                        available.add(slot);
                    }
                }
                slots = available;
            }
            if (slots.isEmpty()) {
                continue;
            }
            AvailabilityWindowVO vo = new AvailabilityWindowVO();
            vo.setStart_wsh(w.start());
            vo.setEnd_wsh(w.end());
            vo.setSlots_wsh(slots);
            vos.add(vo);
        }
        return vos;
    }

    /**
     * 生成可选起始时间。slot 模式（session/hour）只暴露“完整服务时长能落进
     * 该窗口”的起始槽位：仅当 start + duration <= end 才可选，避免选择后才发现
     * 时长覆盖不完整。day 模式保持与旧接口一致的槽位列表。
     * hour 单位的起始槽位对齐到步长网格，步长 = min(60, 计费粒度)：
     * - 粒度 60 / 120 分钟：整点开始（与历史行为一致）
     * - 粒度 30 分钟：整点或半点开始，否则半小时粒度的服务永远选不到 :30
     */
    private List<String> generateSlots(LocalDateTime windowStart, LocalDateTime windowEnd,
                                       String unit, int durationMinutes) {
        List<String> slots = new ArrayList<>();
        boolean slotMode = BookingUnit.MODE_SLOT.equals(BookingUnit.bookingMode(unit));
        int stepMinutes = slotMinutes;
        LocalDateTime t = windowStart;
        if (BookingUnit.HOUR.equals(unit)) {
            stepMinutes = Math.min(BookingUnit.DEFAULT_HOUR_MINUTES, durationMinutes);
            // 对齐到步长网格：即使窗口开在非网格点(如 09:10)，第一个可选起始也顺延到下一个网格点。
            LocalDateTime aligned = windowStart.withSecond(0).withNano(0);
            int remainder = aligned.getMinute() % stepMinutes;
            if (remainder != 0) {
                aligned = aligned.plusMinutes(stepMinutes - remainder);
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

    /** 服务单位归属校验：未知/无法解析的单位不允许参与预约；历史空单位按按夜兜底。 */
    private String normalizeServiceUnit(ServiceItem service) {
        String unit = BookingUnit.normalize(service.getUnit_wsh());
        if (unit == null) {
            if (service.getUnit_wsh() == null || service.getUnit_wsh().isBlank()) {
                // 历史数据 unit_wsh 为空 → 按夜兜底（与 OrderServiceImpl 保持一致）
                return BookingUnit.NIGHT;
            }
            throw new BusinessException(400, BookingErrorCode.UNSUPPORTED_SERVICE_UNIT,
                    "服务计费单位不受支持，无法预约");
        }
        return unit;
    }

    /**
     * 【格式化服务版本号】
     *
     * 业务作用：把服务项的更新时间格式化为版本字符串，供前端做版本比对。
     *
     * 调用场景：getAvailability 组装返回 VO 时。
     *
     * 调用链：getAvailability() → formatVersion() → ServiceVersions.format()。
     *
     * 数据处理：直接委托 ServiceVersions.format 处理 updated_at_wsh。
     *
     * 业务规则：格式规则由 ServiceVersions 统一定义。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：下单时的服务版本校验依赖同一套格式化规则，不可单独改动。
     */
    private String formatVersion(LocalDateTime updatedAt) {
        return ServiceVersions.format(updatedAt);
    }
}
