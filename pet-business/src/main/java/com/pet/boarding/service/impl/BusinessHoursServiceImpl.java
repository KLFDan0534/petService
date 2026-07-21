package com.pet.boarding.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.dto.BusinessHoursDTO;
import com.pet.boarding.dto.BusinessHoursUpsertRequestDTO;
import com.pet.boarding.entity.BusinessHours;
import com.pet.boarding.mapper.BusinessHoursMapper;
import com.pet.boarding.service.BusinessHoursService;
import com.pet.boarding.service.MerchantService;
import com.pet.common.BusinessException;
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
    private final MerchantService merchantService;

    public BusinessHoursServiceImpl(BusinessHoursMapper businessHoursMapper, MerchantService merchantService) {
        this.businessHoursMapper = businessHoursMapper;
        this.merchantService = merchantService;
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
                        .eq(BusinessHours::getMerchant_id_wsh, merchantId)
                        .orderByAsc(BusinessHours::getDay_of_week_wsh));
    }

    /**
     * 新增或更新营业时间
     * @param merchantId 商家ID
     * @param dto 营业时间请求DTO
     * @return 更新后的营业时间
     */
    @Transactional
    public BusinessHours upsert(Long merchantId, BusinessHoursUpsertRequestDTO dto) {
        log.info("upsert() called");
        BusinessHours hours = new BusinessHours();
        hours.setMerchant_id_wsh(merchantId);
        hours.setDay_of_week_wsh(dto.getDay_of_week_wsh());
        hours.setOpen_time_wsh(dto.getOpen_time_wsh());
        hours.setClose_time_wsh(dto.getClose_time_wsh());
        hours.setIs_closed_wsh(dto.getIs_closed_wsh());
        LambdaQueryWrapper<BusinessHours> wrapper = new LambdaQueryWrapper<BusinessHours>()
                .eq(BusinessHours::getMerchant_id_wsh, merchantId)
                .eq(BusinessHours::getDay_of_week_wsh, dto.getDay_of_week_wsh());
        BusinessHours existing = businessHoursMapper.selectOne(wrapper);
        if (existing != null) {
            hours.setId_wsh(existing.getId_wsh());
            businessHoursMapper.updateById(hours);
            merchantService.refreshStoreState(merchantId);
            return hours;
        }
        businessHoursMapper.insert(hours);
        merchantService.refreshStoreState(merchantId);
        return hours;
    }

    /**
     * 删除营业时间
     * @param id 营业时间ID
     */
    @Override
    public BusinessHoursDTO toDTO(BusinessHours entity) {
        if (entity == null) return null;
        BusinessHoursDTO dto = new BusinessHoursDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setMerchant_id_wsh(entity.getMerchant_id_wsh());
        dto.setDay_of_week_wsh(entity.getDay_of_week_wsh());
        dto.setOpen_time_wsh(entity.getOpen_time_wsh());
        dto.setClose_time_wsh(entity.getClose_time_wsh());
        dto.setIs_closed_wsh(entity.getIs_closed_wsh());
        return dto;
    }

    @Transactional
    public void delete(Long id) {
        log.info("delete() called");
        BusinessHours existing = businessHoursMapper.selectById(id);
        businessHoursMapper.deleteById(id);
        if (existing != null && existing.getMerchant_id_wsh() != null) {
            merchantService.refreshStoreState(existing.getMerchant_id_wsh());
        }
    }

    @Override
    @Transactional
    public void delete(Long merchantId, Long id) {
        log.info("delete(merchantId, id) called");
        BusinessHours existing = businessHoursMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "Business hours not found");
        }
        if (!merchantId.equals(existing.getMerchant_id_wsh())) {
            throw new BusinessException(403, "No permission to delete this business hours record");
        }
        delete(id);
    }
}
