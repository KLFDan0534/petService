package com.pet.module.keeper.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.module.keeper.dto.KeeperVO;
import com.pet.module.keeper.entity.Keeper;
import com.pet.module.keeper.mapper.KeeperMapper;
import com.pet.module.merchant.entity.Merchant;
import com.pet.module.merchant.mapper.MerchantMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KeeperService {

    private final KeeperMapper keeperMapper;
    private final MerchantMapper merchantMapper;

    public KeeperService(KeeperMapper keeperMapper, MerchantMapper merchantMapper) {
        this.keeperMapper = keeperMapper;
        this.merchantMapper = merchantMapper;
    }

    public List<Keeper> listAll() {
        return keeperMapper.selectList(
                new LambdaQueryWrapper<Keeper>().eq(Keeper::getStatus, 1));
    }

    public Keeper getById(Long id) {
        Keeper keeper = keeperMapper.selectById(id);
        if (keeper == null) {
            throw new BusinessException("寄养员不存在");
        }
        return keeper;
    }

    public List<Keeper> findByMerchantId(Long merchantId) {
        return keeperMapper.findByMerchantId(merchantId);
    }

    public List<KeeperVO> searchNearby(double lat, double lng, double radius) {
        List<Merchant> nearbyMerchants = merchantMapper.searchNearby(lat, lng, radius);
        if (nearbyMerchants.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Merchant> merchantMap = nearbyMerchants.stream()
                .collect(Collectors.toMap(Merchant::getId, m -> m));
        List<Long> merchantIds = nearbyMerchants.stream()
                .map(Merchant::getId).collect(Collectors.toList());

        List<Keeper> keepers = keeperMapper.selectList(
                new LambdaQueryWrapper<Keeper>()
                        .in(Keeper::getMerchantId, merchantIds)
                        .eq(Keeper::getStatus, 1));

        return keepers.stream().map(k -> {
            KeeperVO vo = toKeeperVO(k);
            Merchant m = merchantMap.get(k.getMerchantId());
            if (m != null) {
                vo.setMerchantName(m.getName());
                vo.setMerchantLatitude(m.getLatitude());
                vo.setMerchantLongitude(m.getLongitude());
                vo.setDistance(calculateDistance(lat, lng,
                        m.getLatitude().doubleValue(), m.getLongitude().doubleValue()));
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Keeper create(Keeper keeper) {
        keeperMapper.insert(keeper);
        return keeper;
    }

    @Transactional
    public Keeper update(Keeper keeper) {
        Keeper existing = getById(keeper.getId());
        if (keeper.getName() != null) existing.setName(keeper.getName());
        if (keeper.getPhone() != null) existing.setPhone(keeper.getPhone());
        if (keeper.getAvatar() != null) existing.setAvatar(keeper.getAvatar());
        if (keeper.getExperienceYears() != null) existing.setExperienceYears(keeper.getExperienceYears());
        if (keeper.getPricePerDay() != null) existing.setPricePerDay(keeper.getPricePerDay());
        if (keeper.getMaxPets() != null) existing.setMaxPets(keeper.getMaxPets());
        if (keeper.getStatus() != null) existing.setStatus(keeper.getStatus());
        keeperMapper.updateById(existing);
        return existing;
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

    private KeeperVO toKeeperVO(Keeper k) {
        KeeperVO vo = new KeeperVO();
        vo.setId(k.getId());
        vo.setMerchantId(k.getMerchantId());
        vo.setUserId(k.getUserId());
        vo.setName(k.getName());
        vo.setPhone(k.getPhone());
        vo.setAvatar(k.getAvatar());
        vo.setExperienceYears(k.getExperienceYears());
        vo.setRating(k.getRating());
        vo.setCompletionRate(k.getCompletionRate());
        vo.setComplaintRate(k.getComplaintRate());
        vo.setPricePerDay(k.getPricePerDay());
        vo.setMaxPets(k.getMaxPets());
        vo.setCurrentPets(k.getCurrentPets());
        vo.setStatus(k.getStatus());
        return vo;
    }
}
