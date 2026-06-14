package com.pet.module.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.module.service.entity.ServiceItem;
import com.pet.module.service.mapper.ServiceItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServiceItemService {

    private final ServiceItemMapper serviceItemMapper;

    public ServiceItemService(ServiceItemMapper serviceItemMapper) {
        this.serviceItemMapper = serviceItemMapper;
    }

    public List<ServiceItem> listByMerchant(Long merchantId) {
        return serviceItemMapper.selectList(
                new LambdaQueryWrapper<ServiceItem>()
                        .eq(ServiceItem::getMerchantId, merchantId)
                        .eq(ServiceItem::getStatus, 1));
    }

    public ServiceItem getById(Long id) {
        ServiceItem item = serviceItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("服务不存在");
        }
        return item;
    }

    @Transactional
    public ServiceItem create(ServiceItem item) {
        serviceItemMapper.insert(item);
        return item;
    }

    @Transactional
    public ServiceItem update(ServiceItem item) {
        ServiceItem existing = getById(item.getId());
        if (item.getName() != null) existing.setName(item.getName());
        if (item.getType() != null) existing.setType(item.getType());
        if (item.getDescription() != null) existing.setDescription(item.getDescription());
        if (item.getPrice() != null) existing.setPrice(item.getPrice());
        if (item.getUnit() != null) existing.setUnit(item.getUnit());
        if (item.getImages() != null) existing.setImages(item.getImages());
        if (item.getStatus() != null) existing.setStatus(item.getStatus());
        serviceItemMapper.updateById(existing);
        return existing;
    }

    @Transactional
    public void delete(Long id) {
        getById(id);
        serviceItemMapper.deleteById(id);
    }
}
