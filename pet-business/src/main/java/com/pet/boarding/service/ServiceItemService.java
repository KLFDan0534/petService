package com.pet.boarding.service;

import com.pet.boarding.dto.ServiceItemCreateRequestDTO;
import com.pet.boarding.dto.ServiceItemDTO;
import com.pet.boarding.dto.ServiceItemUpdateRequestDTO;
import com.pet.boarding.entity.ServiceItem;

import java.util.Collection;
import java.util.List;

public interface ServiceItemService {
    List<ServiceItem> listAll();

    List<ServiceItem> listByMerchant(Long merchantId);

    List<ServiceItem> listByMerchantForManage(Long merchantId);

    ServiceItem getById(Long id);

    List<ServiceItem> listByIds(Collection<Long> ids);

    ServiceItem create(ServiceItemCreateRequestDTO dto);

    ServiceItem update(Long id, ServiceItemUpdateRequestDTO dto);

    void delete(Long id);

    void toggleStatus(Long id);

    ServiceItem updateImages(Long id, String images);

    List<ServiceItem> listByCategory(Long categoryId);

    ServiceItemDTO toDTO(ServiceItem entity);
}
