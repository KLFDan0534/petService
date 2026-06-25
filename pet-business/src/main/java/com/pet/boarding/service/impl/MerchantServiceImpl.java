package com.pet.boarding.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.common.StatusCode;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.service.MerchantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商家服务实现
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Service
@Slf4j
public class MerchantServiceImpl implements MerchantService {

    private final MerchantMapper merchantMapper;

    public MerchantServiceImpl(MerchantMapper merchantMapper) {
        this.merchantMapper = merchantMapper;
    }

    /**
     * 获取所有商家列表
     * @return 商家列表
     */
    public List<Merchant> listAll() {
        log.info("listAll() called");
        return merchantMapper.selectList(
                new LambdaQueryWrapper<Merchant>().orderByDesc(Merchant::getCreated_at_wsh));
    }

    /**
     * 根据ID获取商家
     * @param id 商家ID
     * @return 商家实体
     */
    public Merchant getById(Long id) {
        log.info("getById() called");
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }
        return merchant;
    }

    /**
     * 搜索附近商家（按距离排序）
     * @param lat 纬度
     * @param lng 经度
     * @param radius 搜索半径（公里）
     * @return 附近商家列表
     */
    public List<Merchant> searchNearby(double lat, double lng, double radius) {
        log.info("searchNearby() called");
        List<Merchant> merchants = merchantMapper.searchNearby(lat, lng, radius);
        for (Merchant m : merchants) {
            double d = calculateDistance(lat, lng,
                    m.getLatitude_wsh().doubleValue(), m.getLongitude_wsh().doubleValue());
            m.setDistance_wsh(Math.round(d * 100.0) / 100.0);
        }
        return merchants;
    }

    /**
     * 根据用户ID查找商家
     * @param userId 用户ID
     * @return 商家实体
     */
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

    /**
     * 创建商家，初始状态为待审核
     * @param merchant 商家实体
     * @return 创建后的商家
     */
    @Transactional
    public Merchant create(Merchant merchant) {
        log.info("create() called");
        merchant.setStatus_wsh(StatusCode.MERCHANT_PENDING.getValue());
        merchantMapper.insert(merchant);
        return merchant;
    }

    /**
     * 更新商家信息（非空字段覆盖）
     * @param merchant 商家实体
     * @return 更新后的商家
     */
    @Transactional
    public Merchant update(Merchant merchant) {
        log.info("update() called");
        Merchant existing = getById(merchant.getId_wsh());
        if (merchant.getName_wsh() != null) existing.setName_wsh(merchant.getName_wsh());
        if (merchant.getPhone_wsh() != null) existing.setPhone_wsh(merchant.getPhone_wsh());
        if (merchant.getAddress_wsh() != null) existing.setAddress_wsh(merchant.getAddress_wsh());
        if (merchant.getLatitude_wsh() != null) existing.setLatitude_wsh(merchant.getLatitude_wsh());
        if (merchant.getLongitude_wsh() != null) existing.setLongitude_wsh(merchant.getLongitude_wsh());
        if (merchant.getDescription_wsh() != null) existing.setDescription_wsh(merchant.getDescription_wsh());
        if (merchant.getBusiness_license_wsh() != null) existing.setBusiness_license_wsh(merchant.getBusiness_license_wsh());
        if (merchant.getStatus_wsh() != null) existing.setStatus_wsh(merchant.getStatus_wsh());
        merchantMapper.updateById(existing);
        return existing;
    }

    /**
     * 审核通过商家申请
     * @param id 商家ID
     */
    @Transactional
    public void approve(Long id) {
        log.info("approve() called");
        Merchant merchant = getById(id);
        merchant.setStatus_wsh(StatusCode.MERCHANT_APPROVED.getValue());
        merchantMapper.updateById(merchant);
    }

    /**
     * 驳回商家申请
     * @param id 商家ID
     */
    @Transactional
    public void reject(Long id) {
        log.info("reject() called");
        Merchant merchant = getById(id);
        merchant.setStatus_wsh(StatusCode.MERCHANT_REJECTED.getValue());
        merchantMapper.updateById(merchant);
    }
}
