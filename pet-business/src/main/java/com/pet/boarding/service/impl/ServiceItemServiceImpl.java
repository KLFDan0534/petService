package com.pet.boarding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.dto.ServiceItemCreateRequestDTO;
import com.pet.boarding.dto.ServiceItemDTO;
import com.pet.boarding.dto.ServiceItemUpdateRequestDTO;
import com.pet.boarding.entity.ServiceCategory;
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.mapper.ServiceCategoryMapper;
import com.pet.boarding.mapper.ServiceItemMapper;
import com.pet.boarding.service.ServiceItemService;
import com.pet.common.BusinessException;
import com.pet.common.StatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@Slf4j
public class ServiceItemServiceImpl implements ServiceItemService {

    private final ServiceItemMapper serviceItemMapper;
    private final ServiceCategoryMapper categoryMapper;

    public ServiceItemServiceImpl(ServiceItemMapper serviceItemMapper, ServiceCategoryMapper categoryMapper) {
        this.serviceItemMapper = serviceItemMapper;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public List<ServiceItem> listAll() {
        log.info("listAll() called");
        return serviceItemMapper.selectList(
                new LambdaQueryWrapper<ServiceItem>()
                        .eq(ServiceItem::getStatus_wsh, StatusCode.SERVICE_ENABLED.getValue()));
    }

    @Override
    public List<ServiceItem> listByMerchant(Long merchantId) {
        log.info("listByMerchant() called");
        return serviceItemMapper.selectList(
                new LambdaQueryWrapper<ServiceItem>()
                        .eq(ServiceItem::getMerchant_id_wsh, merchantId)
                        .eq(ServiceItem::getStatus_wsh, StatusCode.SERVICE_ENABLED.getValue()));
    }

    @Override
    public List<ServiceItem> listByMerchantForManage(Long merchantId) {
        log.info("listByMerchantForManage() called");
        return serviceItemMapper.selectList(
                new LambdaQueryWrapper<ServiceItem>()
                        .eq(ServiceItem::getMerchant_id_wsh, merchantId)
                        .orderByDesc(ServiceItem::getCreated_at_wsh));
    }

    @Override
    public ServiceItem getById(Long id) {
        log.info("getById() called");
        ServiceItem item = serviceItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("鏈嶅姟椤圭洰涓嶅瓨鍦?");
        }
        return item;
    }

    @Override
    public List<ServiceItem> listByIds(Collection<Long> ids) {
        log.info("listByIds() called");
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return serviceItemMapper.selectBatchIds(ids);
    }

    @Override
    @Transactional
    public ServiceItem create(ServiceItemCreateRequestDTO dto) {
        log.info("create() called");
        ServiceItem item = new ServiceItem();
        item.setMerchant_id_wsh(dto.getMerchant_id_wsh());
        item.setName_wsh(dto.getName_wsh());
        item.setCategory_id_wsh(dto.getCategory_id_wsh());
        item.setDescription_wsh(dto.getDescription_wsh());
        item.setPrice_wsh(dto.getPrice_wsh());
        item.setUnit_wsh(dto.getUnit_wsh());
        item.setImages_wsh(dto.getImages_wsh());
        if (item.getCategory_id_wsh() != null) {
            ServiceCategory cat = categoryMapper.selectById(item.getCategory_id_wsh());
            if (cat != null) {
                item.setType_wsh(cat.getCode_wsh());
            }
        }
        if (item.getStatus_wsh() == null) {
            item.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        }
        serviceItemMapper.insert(item);
        return item;
    }

    @Override
    @Transactional
    public ServiceItem update(Long id, ServiceItemUpdateRequestDTO dto) {
        log.info("update() called");
        ServiceItem existing = getById(id);
        if (dto.getName_wsh() != null) existing.setName_wsh(dto.getName_wsh());
        if (dto.getCategory_id_wsh() != null) {
            existing.setCategory_id_wsh(dto.getCategory_id_wsh());
            ServiceCategory cat = categoryMapper.selectById(dto.getCategory_id_wsh());
            if (cat != null) {
                existing.setType_wsh(cat.getCode_wsh());
            }
        }
        if (dto.getDescription_wsh() != null) existing.setDescription_wsh(dto.getDescription_wsh());
        if (dto.getPrice_wsh() != null) existing.setPrice_wsh(dto.getPrice_wsh());
        if (dto.getUnit_wsh() != null) existing.setUnit_wsh(dto.getUnit_wsh());
        if (dto.getImages_wsh() != null) existing.setImages_wsh(dto.getImages_wsh());
        if (dto.getStatus_wsh() != null) existing.setStatus_wsh(dto.getStatus_wsh());
        serviceItemMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("delete() called");
        getById(id);
        serviceItemMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        log.info("toggleStatus() called");
        ServiceItem item = getById(id);
        item.setStatus_wsh(item.getStatus_wsh() == null || item.getStatus_wsh() == StatusCode.SERVICE_DISABLED.getValue()
                ? StatusCode.SERVICE_ENABLED.getValue()
                : StatusCode.SERVICE_DISABLED.getValue());
        serviceItemMapper.updateById(item);
    }

    @Override
    @Transactional
    public ServiceItem updateImages(Long id, String images) {
        log.info("updateImages() called");
        ServiceItem item = getById(id);
        item.setImages_wsh(images);
        serviceItemMapper.updateById(item);
        return item;
    }

    @Override
    public List<ServiceItem> listByCategory(Long categoryId) {
        log.info("listByCategory() called");
        return serviceItemMapper.selectList(
                new LambdaQueryWrapper<ServiceItem>()
                        .eq(ServiceItem::getCategory_id_wsh, categoryId)
                        .eq(ServiceItem::getStatus_wsh, StatusCode.SERVICE_ENABLED.getValue()));
    }

    @Override
    public ServiceItemDTO toDTO(ServiceItem entity) {
        if (entity == null) return null;
        ServiceItemDTO dto = new ServiceItemDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setMerchant_id_wsh(entity.getMerchant_id_wsh());
        dto.setName_wsh(entity.getName_wsh());
        dto.setType_wsh(entity.getType_wsh());
        dto.setCategory_id_wsh(entity.getCategory_id_wsh());
        if (entity.getCategory_id_wsh() != null) {
            ServiceCategory cat = categoryMapper.selectById(entity.getCategory_id_wsh());
            dto.setCategory_name_wsh(cat != null ? cat.getName_wsh() : null);
        }
        dto.setDescription_wsh(entity.getDescription_wsh());
        dto.setPrice_wsh(entity.getPrice_wsh());
        dto.setUnit_wsh(entity.getUnit_wsh());
        dto.setImages_wsh(entity.getImages_wsh());
        dto.setStatus_wsh(entity.getStatus_wsh());
        return dto;
    }
}
