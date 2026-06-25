package com.pet.adoption.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.adoption.entity.AdoptionApplication;
import com.pet.adoption.entity.AdoptionPet;
import com.pet.adoption.mapper.AdoptionApplicationMapper;
import com.pet.adoption.service.AdoptionApplicationService;
import com.pet.adoption.service.AdoptionPetService;
import com.pet.common.AdoptionStatus;
import com.pet.common.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 领养申请服务实现
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Service
@Slf4j
public class AdoptionApplicationServiceImpl implements AdoptionApplicationService {

    private final AdoptionApplicationMapper adoptionApplicationMapper;
    private final AdoptionPetService adoptionPetService;

    public AdoptionApplicationServiceImpl(AdoptionApplicationMapper adoptionApplicationMapper,
                                          AdoptionPetService adoptionPetService) {
        this.adoptionApplicationMapper = adoptionApplicationMapper;
        this.adoptionPetService = adoptionPetService;
    }

    /**
     * 创建领养申请，校验宠物是否可领养及是否重复申请
     * @param userId 用户ID
     * @param app 领养申请实体
     * @return 创建后的领养申请
     */
    @Transactional
    @Override
    public AdoptionApplication create(Long userId, AdoptionApplication app) {
        log.info("调用 create()");
        AdoptionPet pet = adoptionPetService.getById(app.getPet_id_wsh());
        if (!statusEquals(pet.getStatus_wsh(), AdoptionStatus.PET_AVAILABLE)) {
            throw new BusinessException("该宠物暂不可领养");
        }
        long count = adoptionApplicationMapper.selectCount(
                new LambdaQueryWrapper<AdoptionApplication>()
                        .eq(AdoptionApplication::getUser_id_wsh, userId)
                        .eq(AdoptionApplication::getPet_id_wsh, app.getPet_id_wsh())
                        .notIn(AdoptionApplication::getStatus_wsh,
                                AdoptionStatus.REJECTED,
                                AdoptionStatus.REJECTED.toUpperCase()));
        if (count > 0) {
            throw new BusinessException("您已申请过此宠物");
        }
        app.setUser_id_wsh(userId);
        app.setMerchant_id_wsh(pet.getMerchant_id_wsh());
        app.setStatus_wsh(AdoptionStatus.PENDING);
        app.setMerchant_status_wsh(AdoptionStatus.MERCHANT_STATUS_PENDING);
        app.setAdmin_status_wsh(AdoptionStatus.ADMIN_STATUS_PENDING);
        adoptionApplicationMapper.insert(app);
        return app;
    }

    /**
     * 根据ID获取领养申请
     * @param id 申请ID
     * @return 领养申请实体
     */
    @Override
    public AdoptionApplication getById(Long id) {
        log.info("调用 getById()");
        AdoptionApplication app = adoptionApplicationMapper.selectById(id);
        if (app == null) {
            throw new BusinessException("申请记录不存在");
        }
        return app;
    }

    /**
     * 根据用户ID获取领养申请列表
     * @param userId 用户ID
     * @return 领养申请列表
     */
    @Override
    public List<AdoptionApplication> listByUser(Long userId) {
        log.info("调用 listByUser()");
        return adoptionApplicationMapper.selectList(
                new LambdaQueryWrapper<AdoptionApplication>()
                        .eq(AdoptionApplication::getUser_id_wsh, userId)
                        .orderByDesc(AdoptionApplication::getCreated_at_wsh));
    }

    /**
     * 根据商家ID获取领养申请列表
     * @param merchantId 商家ID
     * @return 领养申请列表
     */
    @Override
    public List<AdoptionApplication> listByMerchant(Long merchantId) {
        log.info("调用 listByMerchant()");
        return adoptionApplicationMapper.selectList(
                new LambdaQueryWrapper<AdoptionApplication>()
                        .eq(AdoptionApplication::getMerchant_id_wsh, merchantId)
                        .orderByDesc(AdoptionApplication::getCreated_at_wsh));
    }

    /**
     * 获取所有领养申请列表
     * @return 领养申请列表
     */
    @Override
    public List<AdoptionApplication> listAll() {
        log.info("调用 listAll()");
        return adoptionApplicationMapper.selectList(
                new LambdaQueryWrapper<AdoptionApplication>()
                        .orderByDesc(AdoptionApplication::getCreated_at_wsh));
    }

    /**
     * 商家审核领养申请
     * @param id 申请ID
     * @param merchantId 商家ID
     * @param approved 是否通过
     * @param remark 审核备注
     * @return 更新后的领养申请
     */
    @Transactional
    @Override
    public AdoptionApplication merchantReview(Long id, Long merchantId, boolean approved, String remark) {
        log.info("调用 merchantReview()");
        AdoptionApplication app = getById(id);
        String merchantStatus = defaultStatus(app.getMerchant_status_wsh(), app.getStatus_wsh());
        if (!statusEquals(merchantStatus, AdoptionStatus.MERCHANT_STATUS_PENDING)) {
            throw new BusinessException("商家已审核此申请");
        }
        if (!app.getMerchant_id_wsh().equals(merchantId)) {
            throw new BusinessException("无权限审核此申请");
        }
        if (approved) {
            app.setMerchant_status_wsh(AdoptionStatus.MERCHANT_STATUS_APPROVED);
            app.setAdmin_status_wsh(AdoptionStatus.ADMIN_STATUS_PENDING);
            app.setStatus_wsh(AdoptionStatus.ADMIN_REVIEWING);
        } else {
            app.setMerchant_status_wsh(AdoptionStatus.MERCHANT_STATUS_REJECTED);
            app.setAdmin_status_wsh(AdoptionStatus.ADMIN_STATUS_REJECTED);
            app.setStatus_wsh(AdoptionStatus.REJECTED);
        }
        app.setMerchant_remark_wsh(remark);
        adoptionApplicationMapper.updateById(app);
        return app;
    }

    /**
     * 管理员审核领养申请，通过后更新宠物状态为已领养
     * @param id 申请ID
     * @param approved 是否通过
     * @param remark 审核备注
     * @return 更新后的领养申请
     */
    @Transactional
    @Override
    public AdoptionApplication adminReview(Long id, boolean approved, String remark) {
        log.info("调用 adminReview()");
        AdoptionApplication app = getById(id);
        if (!statusEquals(app.getMerchant_status_wsh(), AdoptionStatus.MERCHANT_STATUS_APPROVED)) {
            throw new BusinessException("商家未批准或申请已被拒绝");
        }
        String adminStatus = defaultReviewStatus(app.getAdmin_status_wsh(), app.getStatus_wsh());
        if (!statusEquals(adminStatus, AdoptionStatus.ADMIN_STATUS_PENDING)) {
            throw new BusinessException("管理员已审核此申请");
        }
        if (approved) {
            app.setAdmin_status_wsh(AdoptionStatus.ADMIN_STATUS_APPROVED);
            app.setStatus_wsh(AdoptionStatus.APPROVED);
            adoptionPetService.updateStatus(app.getPet_id_wsh(), AdoptionStatus.PET_ADOPTED);
        } else {
            app.setAdmin_status_wsh(AdoptionStatus.ADMIN_STATUS_REJECTED);
            app.setStatus_wsh(AdoptionStatus.REJECTED);
        }
        app.setAdmin_remark_wsh(remark);
        adoptionApplicationMapper.updateById(app);
        return app;
    }

    private boolean statusEquals(String actual, String expected) {
        return actual != null && expected != null && actual.equalsIgnoreCase(expected);
    }

    private String defaultStatus(String status, String fallback) {
        if (status != null && !status.isBlank()) {
            return status;
        }
        if (statusEquals(fallback, AdoptionStatus.PENDING)) {
            return AdoptionStatus.PENDING;
        }
        return status;
    }

    private String defaultReviewStatus(String status, String fallback) {
        if (status != null && !status.isBlank()) {
            return status;
        }
        if (statusEquals(fallback, AdoptionStatus.APPROVED)) {
            return AdoptionStatus.APPROVED;
        }
        if (statusEquals(fallback, AdoptionStatus.REJECTED)) {
            return AdoptionStatus.REJECTED;
        }
        return AdoptionStatus.PENDING;
    }
}
