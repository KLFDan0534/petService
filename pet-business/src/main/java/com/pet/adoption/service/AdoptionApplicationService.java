package com.pet.adoption.service;

import com.pet.adoption.entity.AdoptionApplication;

import java.util.List;

/**
 * 领养申请服务接口
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
public interface AdoptionApplicationService {
    /**
     * 创建领养申请
     * @param userId 用户ID
     * @param app 领养申请实体
     * @return 创建后的领养申请
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    AdoptionApplication create(Long userId, AdoptionApplication app);
    /**
     * 根据ID获取领养申请
     * @param id 申请ID
     * @return 领养申请实体
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    AdoptionApplication getById(Long id);
    /**
     * 根据用户ID获取领养申请列表
     * @param userId 用户ID
     * @return 领养申请列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<AdoptionApplication> listByUser(Long userId);
    /**
     * 根据商家ID获取领养申请列表
     * @param merchantId 商家ID
     * @return 领养申请列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<AdoptionApplication> listByMerchant(Long merchantId);
    /**
     * 获取所有领养申请列表
     * @return 领养申请列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<AdoptionApplication> listAll();
    /**
     * 商家审核领养申请
     * @param id 申请ID
     * @param merchantId 商家ID
     * @param approved 是否通过
     * @param remark 审核备注
     * @return 更新后的领养申请
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    AdoptionApplication merchantReview(Long id, Long merchantId, boolean approved, String remark);
    /**
     * 管理员审核领养申请
     * @param id 申请ID
     * @param approved 是否通过
     * @param remark 审核备注
     * @return 更新后的领养申请
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    AdoptionApplication adminReview(Long id, boolean approved, String remark);
}

