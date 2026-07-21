package com.pet.pet.service;

import com.pet.pet.dto.PetCreateRequestDTO;
import com.pet.pet.dto.PetDTO;
import com.pet.pet.dto.PetUpdateRequestDTO;

import java.util.List;

public interface PetService {
    /**
     * 获取所有宠物列表
     * @return 宠物列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<PetDTO> listAll();
    /**
     * 根据主人ID获取宠物列表
     * @param ownerId 主人ID
     * @return 宠物列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<PetDTO> getPetsByOwner(Long ownerId);
    /**
     * 根据ID获取宠物信息
     * @param id 宠物ID
     * @return 宠物数据传输对象
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    PetDTO getPetById(Long id);
    /**
     * 创建宠物
     * @param request 创建请求
     * @return 创建后的宠物数据传输对象
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    PetDTO createPet(PetCreateRequestDTO request);
    /**
     * 更新宠物信息
     * @param userId 用户ID
     * @param petId 宠物ID
     * @param request 更新请求
     * @return 更新后的宠物数据传输对象
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    PetDTO updatePet(Long userId, Long petId, PetUpdateRequestDTO request);
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
     * @param petId 宠物ID
     * @param request 更新请求
     * @return 更新后的宠物数据传输对象
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    PetDTO updatePetAsAdmin(Long petId, PetUpdateRequestDTO request);
    /**
     * 管理员删除宠物
     * @param id 宠物ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void deletePetAsAdmin(Long id);
}

