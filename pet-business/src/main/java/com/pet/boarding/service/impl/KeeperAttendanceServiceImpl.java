package com.pet.boarding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.dto.AttendanceCheckRequestDTO;
import com.pet.boarding.dto.KeeperAttendanceDTO;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.KeeperAttendance;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.KeeperAttendanceMapper;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.service.KeeperAttendanceService;
import com.pet.boarding.service.KeeperLeaveService;
import com.pet.common.BusinessException;
import com.pet.common.StatusCode;
import com.pet.common.geo.GeoDistanceUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 看护者签到/签退服务实现。
 * <p>
 * 基于地理围栏（Geo-fence）技术的考勤系统，看护者必须在商家附近指定半径内才能完成打卡。
 * 打卡半径通过配置 {@code gao.map.attendance-radius-meters} 控制，默认 300 米。
 * 签到前会校验看护者状态、商家状态、当日是否休假、是否存在未签退班次等业务规则。
 */
@Service
public class KeeperAttendanceServiceImpl implements KeeperAttendanceService {

    private final KeeperAttendanceMapper attendanceMapper;
    private final KeeperMapper keeperMapper;
    private final MerchantMapper merchantMapper;
    private final KeeperLeaveService keeperLeaveService;

    @Value("${gao.map.attendance-radius-meters:${GAO_MAP_ATTENDANCE_RADIUS_METERS:300}}")
    private int attendanceRadiusMeters;

    public KeeperAttendanceServiceImpl(KeeperAttendanceMapper attendanceMapper,
                                       KeeperMapper keeperMapper,
                                       MerchantMapper merchantMapper,
                                       KeeperLeaveService keeperLeaveService) {
        this.attendanceMapper = attendanceMapper;
        this.keeperMapper = keeperMapper;
        this.merchantMapper = merchantMapper;
        this.keeperLeaveService = keeperLeaveService;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>事务边界：</b>签到记录插入在事务中。
     * <p>
     * <b>校验顺序：</b>看护者状态 → 商家状态 → 休假校验 → 重复签到校验 → 位置校验
     */
    @Transactional
    @Override
    public KeeperAttendanceDTO checkIn(Long userId, AttendanceCheckRequestDTO request) {
        Keeper keeper = requireKeeperByUser(userId);
        Merchant merchant = requireMerchantForKeeper(keeper);
        if (keeperLeaveService.isKeeperOnLeave(keeper.getId_wsh(), LocalDate.now())) {
            throw new BusinessException(400, "今天已被商家设置为休假，不能上班打卡");
        }
        if (findActiveShift(keeper.getId_wsh()) != null) {
            throw new BusinessException(400, "当前已有上班打卡记录，请先下班打卡");
        }
        BigDecimal distance = validateLocation(request, merchant);

        KeeperAttendance attendance = new KeeperAttendance();
        attendance.setKeeper_id_wsh(keeper.getId_wsh());
        attendance.setMerchant_id_wsh(merchant.getId_wsh());
        attendance.setCheck_in_at_wsh(LocalDateTime.now());
        attendance.setCheck_in_latitude_wsh(request.getLatitude_wsh());
        attendance.setCheck_in_longitude_wsh(request.getLongitude_wsh());
        attendance.setCheck_in_address_wsh(trimToNull(request.getAddress_wsh()));
        attendance.setCheck_in_accuracy_wsh(scale(request.getAccuracy_wsh(), 2));
        attendance.setCheck_in_distance_wsh(distance);
        attendance.setRadius_meters_wsh(currentRadius());
        attendanceMapper.insert(attendance);
        return toDTO(attendance, keeper, merchant);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>事务边界：</b>签退记录更新在事务中。
     * <p>
     * <b>校验顺序：</b>看护者状态 → 商家状态 → 活跃班次存在性校验 → 位置校验
     */
    @Transactional
    @Override
    public KeeperAttendanceDTO checkOut(Long userId, AttendanceCheckRequestDTO request) {
        Keeper keeper = requireKeeperByUser(userId);
        Merchant merchant = requireMerchantForKeeper(keeper);
        KeeperAttendance attendance = findActiveShift(keeper.getId_wsh());
        if (attendance == null) {
            throw new BusinessException(400, "当前没有上班打卡记录");
        }
        BigDecimal distance = validateLocation(request, merchant);
        attendance.setCheck_out_at_wsh(LocalDateTime.now());
        attendance.setCheck_out_latitude_wsh(request.getLatitude_wsh());
        attendance.setCheck_out_longitude_wsh(request.getLongitude_wsh());
        attendance.setCheck_out_address_wsh(trimToNull(request.getAddress_wsh()));
        attendance.setCheck_out_accuracy_wsh(scale(request.getAccuracy_wsh(), 2));
        attendance.setCheck_out_distance_wsh(distance);
        attendanceMapper.updateById(attendance);
        return toDTO(attendance, keeper, merchant);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>实现细节：</b>通过查询 check_out_at IS NULL 的记录定位当前活跃班次。
     */
    @Override
    public KeeperAttendanceDTO current(Long userId) {
        Keeper keeper = requireKeeperByUser(userId);
        KeeperAttendance attendance = findActiveShift(keeper.getId_wsh());
        if (attendance == null) {
            return null;
        }
        Merchant merchant = merchantMapper.selectById(attendance.getMerchant_id_wsh());
        return toDTO(attendance, keeper, merchant);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>实现细节：</b>时间范围：[当天00:00, 次日00:00)，确保覆盖当天所有签到记录。
     */
    @Override
    public List<KeeperAttendanceDTO> today(Long userId) {
        Keeper keeper = requireKeeperByUser(userId);
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<KeeperAttendance> records = attendanceMapper.selectList(
                new LambdaQueryWrapper<KeeperAttendance>()
                        .eq(KeeperAttendance::getKeeper_id_wsh, keeper.getId_wsh())
                        .ge(KeeperAttendance::getCheck_in_at_wsh, start)
                        .lt(KeeperAttendance::getCheck_in_at_wsh, end)
                        .orderByDesc(KeeperAttendance::getCheck_in_at_wsh));
        Merchant merchant = keeper.getMerchant_id_wsh() == null ? null : merchantMapper.selectById(keeper.getMerchant_id_wsh());
        return records.stream().map(record -> toDTO(record, keeper, merchant)).toList();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>实现细节：</b>先查出勤记录，再批量加载看护者信息到 Map 中提高性能。
     */
    @Override
    public List<KeeperAttendanceDTO> listMerchantToday(Long merchantUserId) {
        Merchant merchant = requireMerchantByUser(merchantUserId);
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<KeeperAttendance> records = attendanceMapper.selectList(
                new LambdaQueryWrapper<KeeperAttendance>()
                        .eq(KeeperAttendance::getMerchant_id_wsh, merchant.getId_wsh())
                        .ge(KeeperAttendance::getCheck_in_at_wsh, start)
                        .lt(KeeperAttendance::getCheck_in_at_wsh, end)
                        .orderByDesc(KeeperAttendance::getCheck_in_at_wsh));
        Map<Long, Keeper> keeperMap = loadKeeperMap(records);
        return records.stream()
                .map(record -> toDTO(record, keeperMap.get(record.getKeeper_id_wsh()), merchant))
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasActiveShift(Long keeperId, Long merchantId) {
        if (keeperId == null || merchantId == null) {
            return false;
        }
        KeeperAttendance attendance = attendanceMapper.selectOne(
                new LambdaQueryWrapper<KeeperAttendance>()
                        .eq(KeeperAttendance::getKeeper_id_wsh, keeperId)
                        .eq(KeeperAttendance::getMerchant_id_wsh, merchantId)
                        .isNull(KeeperAttendance::getCheck_out_at_wsh)
                        .orderByDesc(KeeperAttendance::getCheck_in_at_wsh)
                        .last("LIMIT 1"));
        return attendance != null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>校验顺序：</b>先查活跃班次（在岗），再查休假状态。
     */
    @Override
    public void requireKeeperOnDuty(Long keeperId, Long merchantId) {
        if (!hasActiveShift(keeperId, merchantId)) {
            throw new BusinessException(403, "看护者未上班打卡，不能进行当前操作");
        }
        if (keeperLeaveService.isKeeperOnLeave(keeperId, LocalDate.now())) {
            throw new BusinessException(403, "看护者今天处于休假，不能进行当前操作");
        }
    }

    /**
     * 根据用户ID查询看护者，并校验其状态是否允许打卡。
     *
     * @param userId 用户ID
     * @return 看护者实体
     * @throws BusinessException 如果用户未登录、看护者不存在或未通过审核
     */
    private Keeper requireKeeperByUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        Keeper keeper = keeperMapper.selectOne(
                new LambdaQueryWrapper<Keeper>()
                        .eq(Keeper::getUser_id_wsh, userId)
                        .last("LIMIT 1"));
        if (keeper == null) {
            throw new BusinessException(404, "看护者不存在");
        }
        if (keeper.getStatus_wsh() == null || keeper.getStatus_wsh() == StatusCode.KEEPER_PENDING.getValue()
                || keeper.getStatus_wsh() == StatusCode.KEEPER_REJECTED.getValue()) {
            throw new BusinessException(400, "看护者未通过审核，不能打卡");
        }
        return keeper;
    }

    /**
     * 根据用户ID查询商家并校验存在性。
     *
     * @param userId 用户ID（商家用户）
     * @return 商家实体
     * @throws BusinessException 如果用户未登录或商家不存在
     */
    private Merchant requireMerchantByUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        Merchant merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getUser_id_wsh, userId)
                        .last("LIMIT 1"));
        if (merchant == null) {
            throw new BusinessException(404, "商家不存在");
        }
        return merchant;
    }

    /**
     * 根据看护者查询其所属商家，并校验商家状态是否允许打卡。
     *
     * @param keeper 看护者实体
     * @return 商家实体
     * @throws BusinessException 如果看护者未绑定商家、商家不存在或未通过审核
     */
    private Merchant requireMerchantForKeeper(Keeper keeper) {
        if (keeper.getMerchant_id_wsh() == null) {
            throw new BusinessException(400, "看护者未绑定商家，不能打卡");
        }
        Merchant merchant = merchantMapper.selectById(keeper.getMerchant_id_wsh());
        if (merchant == null) {
            throw new BusinessException(404, "商家不存在");
        }
        if (merchant.getStatus_wsh() == null || merchant.getStatus_wsh() != StatusCode.MERCHANT_APPROVED.getValue()) {
            throw new BusinessException(400, "商家未通过审核，不能打卡");
        }
        return merchant;
    }

    /**
     * 查找看护者当前活跃的班次（已签到但未签退）。
     *
     * @param keeperId 看护者ID
     * @return 活跃的签到记录，不存在时返回 null
     */
    private KeeperAttendance findActiveShift(Long keeperId) {
        return attendanceMapper.selectOne(
                new LambdaQueryWrapper<KeeperAttendance>()
                        .eq(KeeperAttendance::getKeeper_id_wsh, keeperId)
                        .isNull(KeeperAttendance::getCheck_out_at_wsh)
                        .orderByDesc(KeeperAttendance::getCheck_in_at_wsh)
                        .last("LIMIT 1"));
    }

    /**
     * 校验打卡位置是否在商家指定的打卡范围内。
     * <p>
     * <b>校验流程：</b>
     * <ol>
     *   <li>检查请求中的定位坐标是否合法</li>
     *   <li>检查商家的坐标是否已配置</li>
     *   <li>计算两者之间的球面距离</li>
     *   <li>判断是否在打卡半径内</li>
     * </ol>
     *
     * @param request 打卡请求（含用户经纬度）
     * @param merchant 商家实体（含商家坐标）
     * @return 用户与商家的距离（米，保留两位小数）
     * @throws BusinessException 如果定位为空、坐标无效或超出打卡范围
     */
    private BigDecimal validateLocation(AttendanceCheckRequestDTO request, Merchant merchant) {
        if (request == null) {
            throw new BusinessException(400, "定位信息不能为空");
        }
        if (!GeoDistanceUtils.isValidCoordinate(request.getLatitude_wsh(), request.getLongitude_wsh())) {
            throw new BusinessException(400, "定位坐标无效");
        }
        if (!GeoDistanceUtils.isValidCoordinate(merchant.getLatitude_wsh(), merchant.getLongitude_wsh())) {
            throw new BusinessException(400, "商家未配置有效地址坐标，不能打卡");
        }
        double meters = GeoDistanceUtils.distanceMeters(
                request.getLatitude_wsh(),
                request.getLongitude_wsh(),
                merchant.getLatitude_wsh(),
                merchant.getLongitude_wsh());
        if (meters > currentRadius()) {
            throw new BusinessException(400, "当前位置距离商家约 " + Math.round(meters) + " 米，超出打卡范围");
        }
        return BigDecimal.valueOf(meters).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 获取当前配置的打卡半径（优先取配置值，默认 300 米）。
     *
     * @return 打卡半径（米）
     */
    private int currentRadius() {
        return attendanceRadiusMeters > 0 ? attendanceRadiusMeters : 300;
    }

    /**
     * 对 BigDecimal 值进行指定精度缩放。
     *
     * @param value 原始值
     * @param scale 精度位数
     * @return 缩放后的值，入参为 null 时返回 null
     */
    private BigDecimal scale(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * 去掉字符串首尾空格，超长时截断到 500 字符，空字符串转为 null。
     *
     * @param value 原始字符串
     * @return 处理后的字符串或 null
     */
    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }

    /**
     * 根据考勤记录集合批量加载看护者信息到 Map。
     *
     * @param records 考勤记录列表
     * @return keeperId → Keeper 实体的 Map
     */
    private Map<Long, Keeper> loadKeeperMap(List<KeeperAttendance> records) {
        Set<Long> keeperIds = records.stream()
                .map(KeeperAttendance::getKeeper_id_wsh)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (keeperIds.isEmpty()) {
            return Map.of();
        }
        return keeperMapper.selectList(new LambdaQueryWrapper<Keeper>().in(Keeper::getId_wsh, keeperIds))
                .stream()
                .collect(Collectors.toMap(Keeper::getId_wsh, Function.identity()));
    }

    /**
     * 将考勤实体转换为 DTO（含看护者名称和商家名称以及是否在岗标志）。
     *
     * @param entity   考勤实体
     * @param keeper   看护者实体（可为 null）
     * @param merchant 商家实体（可为 null）
     * @return 考勤DTO
     */
    private KeeperAttendanceDTO toDTO(KeeperAttendance entity, Keeper keeper, Merchant merchant) {
        if (entity == null) {
            return null;
        }
        KeeperAttendanceDTO dto = new KeeperAttendanceDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setKeeper_id_wsh(entity.getKeeper_id_wsh());
        dto.setMerchant_id_wsh(entity.getMerchant_id_wsh());
        dto.setKeeper_name_wsh(keeper == null ? null : keeper.getName_wsh());
        dto.setMerchant_name_wsh(merchant == null ? null : merchant.getName_wsh());
        dto.setCheck_in_at_wsh(entity.getCheck_in_at_wsh());
        dto.setCheck_in_latitude_wsh(entity.getCheck_in_latitude_wsh());
        dto.setCheck_in_longitude_wsh(entity.getCheck_in_longitude_wsh());
        dto.setCheck_in_address_wsh(entity.getCheck_in_address_wsh());
        dto.setCheck_in_accuracy_wsh(entity.getCheck_in_accuracy_wsh());
        dto.setCheck_in_distance_wsh(entity.getCheck_in_distance_wsh());
        dto.setCheck_out_at_wsh(entity.getCheck_out_at_wsh());
        dto.setCheck_out_latitude_wsh(entity.getCheck_out_latitude_wsh());
        dto.setCheck_out_longitude_wsh(entity.getCheck_out_longitude_wsh());
        dto.setCheck_out_address_wsh(entity.getCheck_out_address_wsh());
        dto.setCheck_out_accuracy_wsh(entity.getCheck_out_accuracy_wsh());
        dto.setCheck_out_distance_wsh(entity.getCheck_out_distance_wsh());
        dto.setRadius_meters_wsh(entity.getRadius_meters_wsh());
        dto.setOn_duty_wsh(entity.getCheck_out_at_wsh() == null);
        dto.setCreated_at_wsh(entity.getCreated_at_wsh());
        dto.setUpdated_at_wsh(entity.getUpdated_at_wsh());
        return dto;
    }
}
