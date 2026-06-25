package com.pet.adoption.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.adoption.entity.AdoptionPet;
import com.pet.adoption.mapper.AdoptionPetMapper;
import com.pet.adoption.service.AdoptionPetService;
import com.pet.common.AdoptionStatus;
import com.pet.common.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 领养宠物服务实现
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Service
@Slf4j
public class AdoptionPetServiceImpl implements AdoptionPetService {

    private final AdoptionPetMapper adoptionPetMapper;

    public AdoptionPetServiceImpl(AdoptionPetMapper adoptionPetMapper) {
        this.adoptionPetMapper = adoptionPetMapper;
    }

    /**
     * 获取所有可领养宠物列表
     * @return 可领养宠物列表
     */
    @Override
    public List<AdoptionPet> listAvailable() {
        log.info("调用 listAvailable()");
        return adoptionPetMapper.selectList(
                new LambdaQueryWrapper<AdoptionPet>()
                        .in(AdoptionPet::getStatus_wsh,
                                AdoptionStatus.PET_AVAILABLE,
                                AdoptionStatus.PET_AVAILABLE.toUpperCase()));
    }

    /**
     * 根据ID获取领养宠物
     * @param id 宠物ID
     * @return 领养宠物实体
     */
    @Override
    public AdoptionPet getById(Long id) {
        log.info("调用 getById()");
        AdoptionPet pet = adoptionPetMapper.selectById(id);
        if (pet == null) {
            throw new BusinessException("宠物不存在");
        }
        return pet;
    }

    /**
     * 创建领养宠物信息
     * @param merchantId 商家ID
     * @param pet 领养宠物实体
     * @return 创建后的领养宠物
     */
    @Transactional
    @Override
    public AdoptionPet create(Long merchantId, AdoptionPet pet) {
        log.info("调用 create()");
        pet.setMerchant_id_wsh(merchantId);
        pet.setStatus_wsh(AdoptionStatus.PET_AVAILABLE);
        adoptionPetMapper.insert(pet);
        return pet;
    }

    /**
     * 更新领养宠物信息（校验归属权，非空字段覆盖）
     * @param merchantId 商家ID
     * @param pet 领养宠物实体
     * @return 更新后的领养宠物
     */
    @Transactional
    @Override
    public AdoptionPet update(Long merchantId, AdoptionPet pet) {
        log.info("调用 update()");
        AdoptionPet existing = getById(pet.getId_wsh());
        if (!existing.getMerchant_id_wsh().equals(merchantId)) {
            throw new BusinessException("无权修改此宠物");
        }
        if (pet.getName_wsh() != null) existing.setName_wsh(pet.getName_wsh());
        if (pet.getType_wsh() != null) existing.setType_wsh(pet.getType_wsh());
        if (pet.getBreed_wsh() != null) existing.setBreed_wsh(pet.getBreed_wsh());
        if (pet.getAge_wsh() != null) existing.setAge_wsh(pet.getAge_wsh());
        if (pet.getGender_wsh() != null) existing.setGender_wsh(pet.getGender_wsh());
        if (pet.getWeight_wsh() != null) existing.setWeight_wsh(pet.getWeight_wsh());
        if (pet.getColor_wsh() != null) existing.setColor_wsh(pet.getColor_wsh());
        if (pet.getHealth_status_wsh() != null) existing.setHealth_status_wsh(pet.getHealth_status_wsh());
        if (pet.getVaccinated_wsh() != null) existing.setVaccinated_wsh(pet.getVaccinated_wsh());
        if (pet.getSterilized_wsh() != null) existing.setSterilized_wsh(pet.getSterilized_wsh());
        if (pet.getPersonality_wsh() != null) existing.setPersonality_wsh(pet.getPersonality_wsh());
        if (pet.getStory_wsh() != null) existing.setStory_wsh(pet.getStory_wsh());
        if (pet.getAdoption_requirements_wsh() != null) existing.setAdoption_requirements_wsh(pet.getAdoption_requirements_wsh());
        if (pet.getAdoption_fee_wsh() != null) existing.setAdoption_fee_wsh(pet.getAdoption_fee_wsh());
        if (pet.getCover_image_wsh() != null) existing.setCover_image_wsh(pet.getCover_image_wsh());
        if (pet.getImages_wsh() != null) existing.setImages_wsh(pet.getImages_wsh());
        adoptionPetMapper.updateById(existing);
        return existing;
    }

    /**
     * 删除领养宠物（校验归属权）
     * @param merchantId 商家ID
     * @param id 宠物ID
     */
    @Transactional
    @Override
    public void delete(Long merchantId, Long id) {
        log.info("调用 delete()");
        AdoptionPet existing = getById(id);
        if (!existing.getMerchant_id_wsh().equals(merchantId)) {
            throw new BusinessException("无权删除此宠物");
        }
        adoptionPetMapper.deleteById(id);
    }

    /**
     * 更新领养宠物状态
     * @param id 宠物ID
     * @param status 新状态
     * @return 更新后的领养宠物
     */
    @Transactional
    @Override
    public AdoptionPet updateStatus(Long id, String status) {
        log.info("调用 updateStatus()");
        AdoptionPet existing = getById(id);
        existing.setStatus_wsh(status);
        adoptionPetMapper.updateById(existing);
        return existing;
    }
}
