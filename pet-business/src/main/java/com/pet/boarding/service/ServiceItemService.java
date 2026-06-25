package com.pet.boarding.service;

import com.pet.boarding.entity.ServiceItem;

import java.util.List;

/**
 * 服务项目服务接口
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
public interface ServiceItemService {
    /**
     * 获取所有服务项目列表
     * @return 服务项目列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<ServiceItem> listAll();
    /**
     * 根据商家ID获取服务项目列表
     * @param merchantId 商家ID
     * @return 服务项目列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<ServiceItem> listByMerchant(Long merchantId);
    /**
     * 根据ID获取服务项目
     * @param id 服务项目ID
     * @return 服务项目实体
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    ServiceItem getById(Long id);
    /**
     * 创建服务项目
     * @param item 服务项目实体
     * @return 创建后的服务项目
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    ServiceItem create(ServiceItem item);
    /**
     * 更新服务项目
     * @param item 服务项目实体
     * @return 更新后的服务项目
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    ServiceItem update(ServiceItem item);
    /**
     * 删除服务项目
     * @param id 服务项目ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void delete(Long id);
    /**
     * 切换服务项目上下架状态
     * @param id 服务项目ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void toggleStatus(Long id);
    /**
     * 更新服务项目图片
     * @param id 服务项目ID
     * @param images 图片地址（多个用逗号分隔）
     * @return 更新后的服务项目
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    ServiceItem updateImages(Long id, String images);
}

