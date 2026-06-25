package com.pet.boarding.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.common.StatusCode;
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.mapper.ServiceItemMapper;
import com.pet.boarding.service.ServiceItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 服务项目服务实现
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Service
@Slf4j
public class ServiceItemServiceImpl implements ServiceItemService {

    private final ServiceItemMapper serviceItemMapper;

    public ServiceItemServiceImpl(ServiceItemMapper serviceItemMapper) {
        this.serviceItemMapper = serviceItemMapper;
    }

    /**
     * 获取所有已上架的服务项目
     * @return 服务项目列表
     */
    public List<ServiceItem> listAll() {
        log.info("listAll() called");
        return serviceItemMapper.selectList(
                new LambdaQueryWrapper<ServiceItem>()
                        .eq(ServiceItem::getStatus_wsh, StatusCode.SERVICE_ENABLED.getValue()));
    }

    /**
     * 根据商家ID获取已上架的服务项目
     * @param merchantId 商家ID
     * @return 服务项目列表
     */
    public List<ServiceItem> listByMerchant(Long merchantId) {
        log.info("listByMerchant() called");
        return serviceItemMapper.selectList(
                new LambdaQueryWrapper<ServiceItem>()
                        .eq(ServiceItem::getMerchant_id_wsh, merchantId)
                        .eq(ServiceItem::getStatus_wsh, StatusCode.SERVICE_ENABLED.getValue()));
    }

    /**
     * 根据ID获取服务项目
     * @param id 服务项目ID
     * @return 服务项目实体
     */
    public ServiceItem getById(Long id) {
        log.info("getById() called");
        ServiceItem item = serviceItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("服务项目不存在");
        }
        return item;
    }

    /**
     * 创建服务项目
     * @param item 服务项目实体
     * @return 创建后的服务项目
     */
    @Transactional
    public ServiceItem create(ServiceItem item) {
        log.info("create() called");
        serviceItemMapper.insert(item);
        return item;
    }

    /**
     * 更新服务项目（非空字段覆盖）
     * @param item 服务项目实体
     * @return 更新后的服务项目
     */
    @Transactional
    public ServiceItem update(ServiceItem item) {
        log.info("update() called");
        ServiceItem existing = getById(item.getId_wsh());
        if (item.getName_wsh() != null) existing.setName_wsh(item.getName_wsh());
        if (item.getType_wsh() != null) existing.setType_wsh(item.getType_wsh());
        if (item.getDescription_wsh() != null) existing.setDescription_wsh(item.getDescription_wsh());
        if (item.getPrice_wsh() != null) existing.setPrice_wsh(item.getPrice_wsh());
        if (item.getUnit_wsh() != null) existing.setUnit_wsh(item.getUnit_wsh());
        if (item.getImages_wsh() != null) existing.setImages_wsh(item.getImages_wsh());
        if (item.getStatus_wsh() != null) existing.setStatus_wsh(item.getStatus_wsh());
        serviceItemMapper.updateById(existing);
        return existing;
    }

    /**
     * 删除服务项目
     * @param id 服务项目ID
     */
    @Transactional
    public void delete(Long id) {
        log.info("delete() called");
        getById(id);
        serviceItemMapper.deleteById(id);
    }

    /**
     * 切换服务项目上下架状态
     * @param id 服务项目ID
     */
    @Transactional
    public void toggleStatus(Long id) {
        log.info("toggleStatus() called");
        ServiceItem item = getById(id);
        item.setStatus_wsh(item.getStatus_wsh() == null || item.getStatus_wsh() == StatusCode.SERVICE_DISABLED.getValue()
                ? StatusCode.SERVICE_ENABLED.getValue()
                : StatusCode.SERVICE_DISABLED.getValue());
        serviceItemMapper.updateById(item);
    }

    /**
     * 更新服务项目图片
     * @param id 服务项目ID
     * @param images 图片URL（多个用逗号分隔）
     * @return 更新后的服务项目
     */
    @Transactional
    public ServiceItem updateImages(Long id, String images) {
        log.info("updateImages() called");
        ServiceItem item = getById(id);
        item.setImages_wsh(images);
        serviceItemMapper.updateById(item);
        return item;
    }
}
