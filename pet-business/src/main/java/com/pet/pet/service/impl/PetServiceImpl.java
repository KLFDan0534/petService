package com.pet.pet.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.pet.entity.Pet;
import com.pet.pet.mapper.PetMapper;
import com.pet.pet.service.PetService;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 宠物服务实现类
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Service
@Slf4j
public class PetServiceImpl implements PetService {

    private final PetMapper petMapper;
    private final UserMapper userMapper;

    public PetServiceImpl(PetMapper petMapper, UserMapper userMapper) {
        this.petMapper = petMapper;
        this.userMapper = userMapper;
    }

    /**
     * 获取所有宠物列表
     * @return 宠物列表
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Override
    public List<Pet> listAll() {
        log.info("调用 listAll()");
        List<Pet> list = petMapper.selectList(null);
        for (Pet p : list) enrichOwnerName(p);
        return list;
    }

    /**
     * 根据主人ID获取宠物列表
     * @param ownerId 主人ID
     * @return 宠物列表
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Override
    public List<Pet> getPetsByOwner(Long ownerId) {
        log.info("调用 getPetsByOwner()");
        return petMapper.selectList(
                new LambdaQueryWrapper<Pet>().eq(Pet::getOwner_id_wsh, ownerId));
    }

    /**
     * 根据ID获取宠物信息
     * @param id 宠物ID
     * @return 宠物实体
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Override
    public Pet getPetById(Long id) {
        log.info("调用 getPetById()");
        Pet pet = petMapper.selectById(id);
        if (pet == null) {
            throw new BusinessException("宠物不存在");
        }
        enrichOwnerName(pet);
        return pet;
    }

    /**
     * 创建宠物
     * @param pet 宠物实体
     * @return 创建后的宠物
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Transactional
    @Override
    public Pet createPet(Pet pet) {
        log.info("调用 createPet()");
        if (pet.getOwner_id_wsh() == null) {
            throw new BusinessException("未指定宠物主人");
        }
        petMapper.insert(pet);
        return pet;
    }

    /**
     * 更新宠物信息（普通用户）
     * @param userId 用户ID
     * @param pet 宠物实体
     * @return 更新后的宠物
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Transactional
    @Override
    public Pet updatePet(Long userId, Pet pet) {
        log.info("调用 updatePet()");
        Pet existing = getPetById(pet.getId_wsh());
        if (!existing.getOwner_id_wsh().equals(userId)) {
            throw new BusinessException("无权修改此宠物");
        }
        if (pet.getName_wsh() != null) existing.setName_wsh(pet.getName_wsh());
        if (pet.getType_wsh() != null) existing.setType_wsh(pet.getType_wsh());
        if (pet.getBreed_wsh() != null) existing.setBreed_wsh(pet.getBreed_wsh());
        if (pet.getAge_wsh() != null) existing.setAge_wsh(pet.getAge_wsh());
        if (pet.getWeight_wsh() != null) existing.setWeight_wsh(pet.getWeight_wsh());
        if (pet.getGender_wsh() != null) existing.setGender_wsh(pet.getGender_wsh());
        if (pet.getSterilized_wsh() != null) existing.setSterilized_wsh(pet.getSterilized_wsh());
        if (pet.getVaccinated_wsh() != null) existing.setVaccinated_wsh(pet.getVaccinated_wsh());
        if (pet.getAvatar_wsh() != null) existing.setAvatar_wsh(pet.getAvatar_wsh());
        if (pet.getDescription_wsh() != null) existing.setDescription_wsh(pet.getDescription_wsh());
        if (pet.getAllergies_wsh() != null) existing.setAllergies_wsh(pet.getAllergies_wsh());
        if (pet.getHabits_wsh() != null) existing.setHabits_wsh(pet.getHabits_wsh());
        petMapper.updateById(existing);
        return existing;
    }

    private void enrichOwnerName(Pet p) {
        if (p.getOwner_id_wsh() != null) {
            User user = userMapper.selectById(p.getOwner_id_wsh());
            if (user != null) p.setOwner_name_wsh(user.getNickname_wsh() != null ? user.getNickname_wsh() : user.getUsername_wsh());
        }
    }

    /**
     * 删除宠物（普通用户）
     * @param userId 用户ID
     * @param id 宠物ID
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Transactional
    @Override
    public void deletePet(Long userId, Long id) {
        log.info("调用 deletePet()");
        Pet existing = getPetById(id);
        if (!existing.getOwner_id_wsh().equals(userId)) {
            throw new BusinessException("无权删除此宠物");
        }
        petMapper.deleteById(id);
    }

    /**
     * 管理员更新宠物信息
     * @param pet 宠物实体
     * @return 更新后的宠物
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Transactional
    @Override
    public Pet updatePetAsAdmin(Pet pet) {
        log.info("调用 updatePetAsAdmin()");
        Pet existing = getPetById(pet.getId_wsh());
        if (pet.getName_wsh() != null) existing.setName_wsh(pet.getName_wsh());
        if (pet.getType_wsh() != null) existing.setType_wsh(pet.getType_wsh());
        if (pet.getBreed_wsh() != null) existing.setBreed_wsh(pet.getBreed_wsh());
        if (pet.getAge_wsh() != null) existing.setAge_wsh(pet.getAge_wsh());
        if (pet.getWeight_wsh() != null) existing.setWeight_wsh(pet.getWeight_wsh());
        if (pet.getGender_wsh() != null) existing.setGender_wsh(pet.getGender_wsh());
        if (pet.getSterilized_wsh() != null) existing.setSterilized_wsh(pet.getSterilized_wsh());
        if (pet.getVaccinated_wsh() != null) existing.setVaccinated_wsh(pet.getVaccinated_wsh());
        if (pet.getAvatar_wsh() != null) existing.setAvatar_wsh(pet.getAvatar_wsh());
        if (pet.getDescription_wsh() != null) existing.setDescription_wsh(pet.getDescription_wsh());
        if (pet.getAllergies_wsh() != null) existing.setAllergies_wsh(pet.getAllergies_wsh());
        if (pet.getHabits_wsh() != null) existing.setHabits_wsh(pet.getHabits_wsh());
        petMapper.updateById(existing);
        return existing;
    }

    /**
     * 管理员删除宠物
     * @param id 宠物ID
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Transactional
    @Override
    public void deletePetAsAdmin(Long id) {
        log.info("调用 deletePetAsAdmin()");
        petMapper.deleteById(id);
    }
}
