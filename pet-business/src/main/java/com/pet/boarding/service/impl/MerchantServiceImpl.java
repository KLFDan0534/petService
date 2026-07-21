package com.pet.boarding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.dto.MerchantCreateRequestDTO;
import com.pet.boarding.dto.MerchantDTO;
import com.pet.boarding.dto.MerchantUpdateRequestDTO;
import com.pet.boarding.entity.BusinessHours;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.BusinessHoursMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.service.MerchantService;
import com.pet.boarding.service.KeeperService;
import com.pet.boarding.constant.MerchantStoreConstants;
import com.pet.common.BusinessException;
import com.pet.common.StatusCode;
import com.pet.qualification.service.QualificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Slf4j
public class MerchantServiceImpl implements MerchantService {

    private final MerchantMapper merchantMapper;
    private final QualificationService qualificationService;
    private final BusinessHoursMapper businessHoursMapper;
    private final KeeperService keeperService;

    public MerchantServiceImpl(MerchantMapper merchantMapper,
                               QualificationService qualificationService,
                               BusinessHoursMapper businessHoursMapper,
                               KeeperService keeperService) {
        this.merchantMapper = merchantMapper;
        this.qualificationService = qualificationService;
        this.businessHoursMapper = businessHoursMapper;
        this.keeperService = keeperService;
    }

    @Override
    public List<Merchant> listAll() {
        log.info("listAll() called");
        return merchantMapper.selectList(
                new LambdaQueryWrapper<Merchant>().orderByDesc(Merchant::getCreated_at_wsh));
    }

    @Override
    public Merchant getById(Long id) {
        log.info("getById() called");
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant == null) {
            throw new BusinessException("鍟嗗涓嶅瓨鍦?");
        }
        return merchant;
    }

    @Override
    public List<Merchant> listByIds(Collection<Long> ids) {
        log.info("listByIds() called");
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return merchantMapper.selectBatchIds(ids);
    }

    @Override
    public List<MerchantDTO> searchNearby(double lat, double lng, double radius) {
        log.info("searchNearby() called");
        List<Merchant> merchants = merchantMapper.searchNearby(lat, lng, radius);
        return merchants.stream().map(m -> {
            MerchantDTO dto = toDTO(m);
            double d = calculateDistance(lat, lng,
                    m.getLatitude_wsh().doubleValue(), m.getLongitude_wsh().doubleValue());
            dto.setDistance_wsh(Math.round(d * 100.0) / 100.0);
            return dto;
        }).toList();
    }

    @Override
    public Merchant findByUserId(Long userId) {
        log.info("findByUserId() called");
        return merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getUser_id_wsh, userId));
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double a = radLat1 - radLat2;
        double b = Math.toRadians(lng1) - Math.toRadians(lng2);
        double s = 2 * Math.asin(Math.sqrt(
                Math.pow(Math.sin(a / 2), 2) +
                Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
        return s * 6371;
    }

    @Override
    @Transactional
    public Merchant create(MerchantCreateRequestDTO dto, Long userId) {
        log.info("create() called");
        Merchant merchant = new Merchant();
        merchant.setUser_id_wsh(userId);
        merchant.setName_wsh(dto.getName_wsh());
        merchant.setPhone_wsh(dto.getPhone_wsh());
        merchant.setAddress_wsh(dto.getAddress_wsh());
        merchant.setLatitude_wsh(dto.getLatitude_wsh());
        merchant.setLongitude_wsh(dto.getLongitude_wsh());
        merchant.setDescription_wsh(dto.getDescription_wsh());
        merchant.setBusiness_license_wsh(firstText(dto.getBusiness_license_wsh(), dto.getQualification_image_wsh()));
        merchant.setStatus_wsh(StatusCode.MERCHANT_PENDING.getValue());
        merchant.setStore_mode_wsh(MerchantStoreConstants.MODE_AUTO);
        merchant.setStore_status_wsh(MerchantStoreConstants.STATUS_CLOSED);
        merchantMapper.insert(merchant);
        qualificationService.createPending(
                QualificationService.OWNER_TYPE_MERCHANT,
                merchant.getId_wsh(),
                userId,
                QualificationService.QUAL_TYPE_BUSINESS_LICENSE,
                "钀ヤ笟鎵х収",
                merchant.getBusiness_license_wsh(),
                "鍟嗘埛鍏ラ┗璧勮川");
        return merchant;
    }

    @Override
    @Transactional
    public Merchant update(Long id, MerchantUpdateRequestDTO dto) {
        log.info("update() called");
        Merchant existing = getById(id);
        if (dto.getName_wsh() != null) existing.setName_wsh(dto.getName_wsh());
        if (dto.getPhone_wsh() != null) existing.setPhone_wsh(dto.getPhone_wsh());
        if (dto.getAddress_wsh() != null) existing.setAddress_wsh(dto.getAddress_wsh());
        if (dto.getLatitude_wsh() != null) existing.setLatitude_wsh(dto.getLatitude_wsh());
        if (dto.getLongitude_wsh() != null) existing.setLongitude_wsh(dto.getLongitude_wsh());
        if (dto.getDescription_wsh() != null) existing.setDescription_wsh(dto.getDescription_wsh());
        merchantMapper.updateById(existing);
        return existing;
    }

    @Override
    public MerchantDTO toDTO(Merchant entity) {
        if (entity == null) return null;
        MerchantDTO dto = new MerchantDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setUser_id_wsh(entity.getUser_id_wsh());
        dto.setName_wsh(entity.getName_wsh());
        dto.setPhone_wsh(entity.getPhone_wsh());
        dto.setAddress_wsh(entity.getAddress_wsh());
        dto.setLatitude_wsh(entity.getLatitude_wsh());
        dto.setLongitude_wsh(entity.getLongitude_wsh());
        dto.setDescription_wsh(entity.getDescription_wsh());
        dto.setBusiness_license_wsh(entity.getBusiness_license_wsh());
        dto.setRating_wsh(entity.getRating_wsh());
        dto.setStatus_wsh(entity.getStatus_wsh());
        dto.setStore_mode_wsh(entity.getStore_mode_wsh());
        dto.setStore_status_wsh(resolveStoreStatus(entity));
        dto.setQualifications_wsh(qualificationService.listByOwner(
                QualificationService.OWNER_TYPE_MERCHANT, entity.getId_wsh(), true));
        dto.setCreated_at_wsh(entity.getCreated_at_wsh());
        return dto;
    }

    private String firstText(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    @Override
    @Transactional
    public void approve(Long id) {
        log.info("approve() called");
        Merchant merchant = getById(id);
        merchant.setStatus_wsh(StatusCode.MERCHANT_APPROVED.getValue());
        merchant.setStore_mode_wsh(MerchantStoreConstants.MODE_AUTO);
        merchantMapper.updateById(merchant);
        refreshStoreState(merchant, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void reject(Long id) {
        log.info("reject() called");
        Merchant merchant = getById(id);
        merchant.setStatus_wsh(StatusCode.MERCHANT_REJECTED.getValue());
        merchant.setStore_mode_wsh(MerchantStoreConstants.MODE_MANUAL_CLOSED);
        merchant.setStore_status_wsh(MerchantStoreConstants.STATUS_CLOSED);
        merchantMapper.updateById(merchant);
        keeperService.syncMerchantStoreStatus(merchant.getId_wsh(), false);
    }

    @Override
    @Transactional
    public Merchant updateStoreMode(Long id, Integer storeMode) {
        log.info("updateStoreMode() called");
        Merchant merchant = getById(id);
        if (storeMode == null) {
            throw new BusinessException(400, "营业模式不能为空");
        }
        if (storeMode != MerchantStoreConstants.MODE_AUTO
                && storeMode != MerchantStoreConstants.MODE_MANUAL_OPEN
                && storeMode != MerchantStoreConstants.MODE_MANUAL_CLOSED) {
            throw new BusinessException(400, "营业模式无效");
        }
        merchant.setStore_mode_wsh(storeMode);
        merchantMapper.updateById(merchant);
        refreshStoreState(merchant, LocalDateTime.now());
        return merchant;
    }

    @Override
    @Transactional
    public void refreshStoreState(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        refreshStoreState(merchant, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void refreshAllStoreStates() {
        List<Merchant> merchants = merchantMapper.selectList(
                new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getStatus_wsh, StatusCode.MERCHANT_APPROVED.getValue())
                        .eq(Merchant::getDeleted_wsh, 0));
        LocalDateTime now = LocalDateTime.now();
        for (Merchant merchant : merchants) {
            refreshStoreState(merchant, now);
        }
    }

    @Override
    public boolean isOwner(Long merchantId, Long userId) {
        Merchant merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getId_wsh, merchantId)
                        .eq(Merchant::getUser_id_wsh, userId)
                        .last("LIMIT 1"));
        return merchant != null;
    }

    private void refreshStoreState(Merchant merchant, LocalDateTime now) {
        if (merchant == null) {
            return;
        }
        int resolved = resolveStoreStatus(merchant, now);
        Integer current = merchant.getStore_status_wsh();
        if (current == null || current != resolved) {
            merchant.setStore_status_wsh(resolved);
            merchantMapper.updateById(merchant);
            keeperService.syncMerchantStoreStatus(merchant.getId_wsh(), resolved == MerchantStoreConstants.STATUS_OPEN);
        }
    }

    private int resolveStoreStatus(Merchant merchant) {
        return resolveStoreStatus(merchant, LocalDateTime.now());
    }

    private int resolveStoreStatus(Merchant merchant, LocalDateTime now) {
        if (merchant == null) {
            return MerchantStoreConstants.STATUS_CLOSED;
        }
        if (merchant.getStatus_wsh() == null
                || merchant.getStatus_wsh() != StatusCode.MERCHANT_APPROVED.getValue()) {
            return MerchantStoreConstants.STATUS_CLOSED;
        }
        Integer mode = merchant.getStore_mode_wsh();
        if (mode == null) {
            mode = MerchantStoreConstants.MODE_AUTO;
        }
        if (mode == MerchantStoreConstants.MODE_MANUAL_OPEN) {
            return MerchantStoreConstants.STATUS_OPEN;
        }
        if (mode == MerchantStoreConstants.MODE_MANUAL_CLOSED) {
            return MerchantStoreConstants.STATUS_CLOSED;
        }
        if (now == null) {
            return MerchantStoreConstants.STATUS_CLOSED;
        }
        List<BusinessHours> hours = businessHoursMapper.selectList(
                new LambdaQueryWrapper<BusinessHours>()
                        .eq(BusinessHours::getMerchant_id_wsh, merchant.getId_wsh())
                        .eq(BusinessHours::getDay_of_week_wsh, now.getDayOfWeek().getValue())
                        .eq(BusinessHours::getDeleted_wsh, 0));
        if (hours == null || hours.isEmpty()) {
            return MerchantStoreConstants.STATUS_CLOSED;
        }
        return hours.stream()
                .filter(h -> h.getIs_closed_wsh() == null || h.getIs_closed_wsh() == 0)
                .anyMatch(h -> isWithinRange(now.toLocalTime(), h.getOpen_time_wsh(), h.getClose_time_wsh()))
                ? MerchantStoreConstants.STATUS_OPEN
                : MerchantStoreConstants.STATUS_CLOSED;
    }

    private boolean isWithinRange(LocalTime now, LocalTime open, LocalTime close) {
        if (open == null || close == null) {
            return false;
        }
        if (open.equals(close)) {
            return true;
        }
        if (open.isBefore(close)) {
            return !now.isBefore(open) && !now.isAfter(close);
        }
        return !now.isBefore(open) || !now.isAfter(close);
    }
}
