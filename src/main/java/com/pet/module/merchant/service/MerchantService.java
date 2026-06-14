package com.pet.module.merchant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.module.merchant.entity.Merchant;
import com.pet.module.merchant.mapper.MerchantMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MerchantService {

    private final MerchantMapper merchantMapper;

    public MerchantService(MerchantMapper merchantMapper) {
        this.merchantMapper = merchantMapper;
    }

    public List<Merchant> listAll() {
        return merchantMapper.selectList(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getStatus, 1));
    }

    public Merchant getById(Long id) {
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }
        return merchant;
    }

    public List<Merchant> searchNearby(double lat, double lng, double radius) {
        List<Merchant> merchants = merchantMapper.searchNearby(lat, lng, radius);
        for (Merchant m : merchants) {
            double d = calculateDistance(lat, lng,
                    m.getLatitude().doubleValue(), m.getLongitude().doubleValue());
            m.setDistance(Math.round(d * 100.0) / 100.0);
        }
        return merchants;
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

    @Transactional
    public Merchant create(Merchant merchant) {
        merchant.setStatus(0);
        merchantMapper.insert(merchant);
        return merchant;
    }

    @Transactional
    public Merchant update(Merchant merchant) {
        Merchant existing = getById(merchant.getId());
        if (merchant.getName() != null) existing.setName(merchant.getName());
        if (merchant.getPhone() != null) existing.setPhone(merchant.getPhone());
        if (merchant.getAddress() != null) existing.setAddress(merchant.getAddress());
        if (merchant.getLatitude() != null) existing.setLatitude(merchant.getLatitude());
        if (merchant.getLongitude() != null) existing.setLongitude(merchant.getLongitude());
        if (merchant.getDescription() != null) existing.setDescription(merchant.getDescription());
        if (merchant.getBusinessLicense() != null) existing.setBusinessLicense(merchant.getBusinessLicense());
        if (merchant.getStatus() != null) existing.setStatus(merchant.getStatus());
        merchantMapper.updateById(existing);
        return existing;
    }

    @Transactional
    public void approve(Long id) {
        Merchant merchant = getById(id);
        merchant.setStatus(1);
        merchantMapper.updateById(merchant);
    }

    @Transactional
    public void reject(Long id) {
        Merchant merchant = getById(id);
        merchant.setStatus(2);
        merchantMapper.updateById(merchant);
    }
}
