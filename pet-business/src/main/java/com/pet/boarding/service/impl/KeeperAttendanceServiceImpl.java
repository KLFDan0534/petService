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

    @Override
    public void requireKeeperOnDuty(Long keeperId, Long merchantId) {
        if (!hasActiveShift(keeperId, merchantId)) {
            throw new BusinessException(403, "看护者未上班打卡，不能进行当前操作");
        }
        if (keeperLeaveService.isKeeperOnLeave(keeperId, LocalDate.now())) {
            throw new BusinessException(403, "看护者今天处于休假，不能进行当前操作");
        }
    }

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

    private KeeperAttendance findActiveShift(Long keeperId) {
        return attendanceMapper.selectOne(
                new LambdaQueryWrapper<KeeperAttendance>()
                        .eq(KeeperAttendance::getKeeper_id_wsh, keeperId)
                        .isNull(KeeperAttendance::getCheck_out_at_wsh)
                        .orderByDesc(KeeperAttendance::getCheck_in_at_wsh)
                        .last("LIMIT 1"));
    }

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

    private int currentRadius() {
        return attendanceRadiusMeters > 0 ? attendanceRadiusMeters : 300;
    }

    private BigDecimal scale(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }

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
