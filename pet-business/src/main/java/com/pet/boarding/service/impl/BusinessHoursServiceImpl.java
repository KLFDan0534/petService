package com.pet.boarding.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.entity.BusinessHours;
import com.pet.boarding.mapper.BusinessHoursMapper;
import com.pet.boarding.service.BusinessHoursService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 营业时间服务实现
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Service
@Slf4j
public class BusinessHoursServiceImpl implements BusinessHoursService {

    private final BusinessHoursMapper businessHoursMapper;

    public BusinessHoursServiceImpl(BusinessHoursMapper businessHoursMapper) {
        this.businessHoursMapper = businessHoursMapper;
    }

    /**
     * 根据商家ID获取营业时间列表
     * @param merchantId 商家ID
     * @return 营业时间列表
     */
    public List<BusinessHours> getByMerchantId(Long merchantId) {
        log.info("getByMerchantId() called");
        return businessHoursMapper.selectList(
                new LambdaQueryWrapper<BusinessHours>()
                        .eq(BusinessHours::getMerchant_id_wsh, merchantId));
    }

    /**
     * 新增或更新营业时间
     * @param hours 营业时间实体
     * @return 更新后的营业时间
     */
    @Transactional
    public BusinessHours upsert(BusinessHours hours) {
        log.info("upsert() called");
        LambdaQueryWrapper<BusinessHours> wrapper = new LambdaQueryWrapper<BusinessHours>()
                .eq(BusinessHours::getMerchant_id_wsh, hours.getMerchant_id_wsh())
                .eq(BusinessHours::getDay_of_week_wsh, hours.getDay_of_week_wsh());
        BusinessHours existing = businessHoursMapper.selectOne(wrapper);
        if (existing != null) {
            hours.setId_wsh(existing.getId_wsh());
            businessHoursMapper.updateById(hours);
            return hours;
        }
        businessHoursMapper.insert(hours);
        return hours;
    }

    /**
     * 删除营业时间
     * @param id 营业时间ID
     */
    @Transactional
    public void delete(Long id) {
        log.info("delete() called");
        businessHoursMapper.deleteById(id);
    }
}
