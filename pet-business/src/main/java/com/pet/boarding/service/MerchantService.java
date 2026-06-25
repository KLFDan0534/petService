package com.pet.boarding.service;

import com.pet.boarding.entity.Merchant;

import java.util.List;

/**
 * 商家服务接口
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
public interface MerchantService {
    /**
     * 获取所有商家列表
     * @return 商家列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Merchant> listAll();
    /**
     * 根据ID获取商家信息
     * @param id 商家ID
     * @return 商家实体
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Merchant getById(Long id);
    /**
     * 搜索附近的商家
     * @param lat 纬度
     * @param lng 经度
     * @param radius 搜索半径
     * @return 商家列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Merchant> searchNearby(double lat, double lng, double radius);
    /**
     * 根据用户ID查找商家
     * @param userId 用户ID
     * @return 商家实体
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Merchant findByUserId(Long userId);
    /**
     * 创建商家
     * @param merchant 商家实体
     * @return 创建后的商家
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Merchant create(Merchant merchant);
    /**
     * 更新商家信息
     * @param merchant 商家实体
     * @return 更新后的商家
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Merchant update(Merchant merchant);
    /**
     * 审核通过商家
     * @param id 商家ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void approve(Long id);
    /**
     * 驳回商家审核
     * @param id 商家ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void reject(Long id);
}

