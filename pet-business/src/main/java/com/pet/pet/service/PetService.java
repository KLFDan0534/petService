package com.pet.pet.service;

import com.pet.pet.entity.Pet;

import java.util.List;

public interface PetService {
    /**
     * 获取所有宠物列表
     * @return 宠物列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Pet> listAll();
    /**
     * 根据主人ID获取宠物列表
     * @param ownerId 主人ID
     * @return 宠物列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Pet> getPetsByOwner(Long ownerId);
    /**
     * 根据ID获取宠物信息
     * @param id 宠物ID
     * @return 宠物实体
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Pet getPetById(Long id);
    /**
     * 创建宠物
     * @param pet 宠物实体
     * @return 创建后的宠物
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Pet createPet(Pet pet);
    /**
     * 更新宠物信息
     * @param userId 用户ID
     * @param pet 宠物实体
     * @return 更新后的宠物
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Pet updatePet(Long userId, Pet pet);
    /**
     * 删除宠物
     * @param userId 用户ID
     * @param id 宠物ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void deletePet(Long userId, Long id);
    /**
     * 管理员更新宠物信息
     * @param pet 宠物实体
     * @return 更新后的宠物
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Pet updatePetAsAdmin(Pet pet);
    /**
     * 管理员删除宠物
     * @param id 宠物ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void deletePetAsAdmin(Long id);
}

