package com.pet.adoption.service;

import com.pet.adoption.entity.AdoptionPet;

import java.util.List;

/**
 * 领养宠物服务接口
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
public interface AdoptionPetService {
    /**
     * 获取所有可领养宠物列表
     * @return 可领养宠物列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<AdoptionPet> listAvailable();
    /**
     * 根据ID获取领养宠物
     * @param id 宠物ID
     * @return 领养宠物实体
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    AdoptionPet getById(Long id);
    /**
     * 创建领养宠物信息
     * @param merchantId 商家ID
     * @param pet 领养宠物实体
     * @return 创建后的领养宠物
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    AdoptionPet create(Long merchantId, AdoptionPet pet);
    /**
     * 更新领养宠物信息
     * @param merchantId 商家ID
     * @param pet 领养宠物实体
     * @return 更新后的领养宠物
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    AdoptionPet update(Long merchantId, AdoptionPet pet);
    /**
     * 删除领养宠物
     * @param merchantId 商家ID
     * @param id 宠物ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void delete(Long merchantId, Long id);
    /**
     * 更新领养宠物状态
     * @param id 宠物ID
     * @param status 新状态
     * @return 更新后的领养宠物
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    AdoptionPet updateStatus(Long id, String status);
}

