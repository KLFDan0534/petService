package com.pet.boarding.service;

import com.pet.boarding.dto.BusinessHoursDTO;
import com.pet.boarding.dto.BusinessHoursUpsertRequestDTO;
import com.pet.boarding.entity.BusinessHours;
import java.util.List;

/**
 * 营业时间服务接口
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
public interface BusinessHoursService {
    /**
     * 根据商家ID获取营业时间列表
     * @param merchantId 商家ID
     * @return 营业时间列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<BusinessHours> getByMerchantId(Long merchantId);
    /**
     * 新增或更新营业时间
     * @param merchantId 商家ID
     * @param dto 营业时间请求DTO
     * @return 更新后的营业时间
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    BusinessHours upsert(Long merchantId, BusinessHoursUpsertRequestDTO dto);
    /**
     * 删除营业时间
     * @param id 营业时间ID
     * @author: wsh
     * @date: 2026/6/24 11:05
    **/
    void delete(Long id);

    void delete(Long merchantId, Long id);

    BusinessHoursDTO toDTO(BusinessHours entity);
}

