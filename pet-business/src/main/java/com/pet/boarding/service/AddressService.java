package com.pet.boarding.service;

import com.pet.boarding.dto.AddressCreateRequestDTO;
import com.pet.boarding.dto.AddressDTO;
import com.pet.boarding.dto.AddressUpdateRequestDTO;
import com.pet.boarding.entity.Address;

import java.util.List;

/**
 * 地址服务接口
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
public interface AddressService {
    /**
     * 根据用户ID获取地址列表
     * @param userId 用户ID
     * @return 地址列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Address> listByUser(Long userId);
    /**
     * 根据ID获取地址
     * @param id 地址ID
     * @return 地址实体
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Address getById(Long id);
    /**
     * 创建地址
     * @param userId 用户ID
     * @param dto 地址创建请求DTO
     * @return 创建后的地址
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Address create(Long userId, AddressCreateRequestDTO dto);
    /**
     * 更新地址
     * @param userId 用户ID
     * @param id 地址ID
     * @param dto 地址更新请求DTO
     * @return 更新后的地址
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Address update(Long userId, Long id, AddressUpdateRequestDTO dto);
    /**
     * 删除地址
     * @param userId 用户ID
     * @param id 地址ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void delete(Long userId, Long id);
    /**
     * 设置默认地址
     * @param userId 用户ID
     * @param id 地址ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void setDefault(Long userId, Long id);

    AddressDTO toDTO(Address entity);
}

