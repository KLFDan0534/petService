package com.pet.boarding.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pet.common.BusinessException;
import com.pet.boarding.dto.AddressCreateRequestDTO;
import com.pet.boarding.dto.AddressDTO;
import com.pet.boarding.dto.AddressUpdateRequestDTO;
import com.pet.boarding.entity.Address;
import com.pet.boarding.mapper.AddressMapper;
import com.pet.boarding.service.AddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 地址服务实现
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Service
@Slf4j
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;

    public AddressServiceImpl(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    /**
     * 获取用户地址列表
     * @param userId 用户ID
     * @return 地址列表
     */
    public List<Address> listByUser(Long userId) {
        log.info("listByUser() called");
        return addressMapper.selectList(
                new LambdaQueryWrapper<Address>()
                        .eq(Address::getUser_id_wsh, userId)
                        .orderByDesc(Address::getIs_default_wsh)
                        .orderByDesc(Address::getCreated_at_wsh));
    }

    /**
     * 根据ID获取地址
     * @param id 地址ID
     * @return 地址实体
     */
    public Address getById(Long id) {
        log.info("getById() called");
        Address addr = addressMapper.selectById(id);
        if (addr == null) throw new BusinessException("地址不存在");
        return addr;
    }

    /**
     * 创建地址，若设为默认则取消其他默认地址
     * @param userId 用户ID
     * @param dto 地址创建请求DTO
     * @return 创建后的地址
     */
    @Transactional
    public Address create(Long userId, AddressCreateRequestDTO dto) {
        log.info("create() called");
        Address addr = new Address();
        addr.setUser_id_wsh(userId);
        addr.setLabel_wsh(dto.getLabel_wsh());
        addr.setName_wsh(dto.getName_wsh());
        addr.setPhone_wsh(dto.getPhone_wsh());
        addr.setAddress_wsh(dto.getAddress_wsh());
        addr.setDetail_wsh(dto.getDetail_wsh());
        addr.setLatitude_wsh(dto.getLatitude_wsh());
        addr.setLongitude_wsh(dto.getLongitude_wsh());
        addr.setIs_default_wsh(dto.getIs_default_wsh());
        if (addr.getIs_default_wsh() == null) addr.setIs_default_wsh(0);
        if (addr.getIs_default_wsh() == 1) {
            addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                    .eq(Address::getUser_id_wsh, userId)
                    .set(Address::getIs_default_wsh, 0));
        }
        addressMapper.insert(addr);
        long count = addressMapper.selectCount(
                new LambdaQueryWrapper<Address>()
                        .eq(Address::getUser_id_wsh, userId)
                        .eq(Address::getIs_default_wsh, 1));
        if (count == 0) {
            addr.setIs_default_wsh(1);
            addressMapper.updateById(addr);
        }
        return addr;
    }

    /**
     * 更新地址信息，校验当前用户对地址的归属权
     * @param userId 用户ID
     * @param id 地址ID
     * @param dto 地址更新请求DTO
     * @return 更新后的地址
     */
    @Transactional
    public Address update(Long userId, Long id, AddressUpdateRequestDTO dto) {
        log.info("update() called");
        Address existing = getById(id);
        if (!existing.getUser_id_wsh().equals(userId)) throw new BusinessException("无权操作此地址");
        if (dto.getLabel_wsh() != null) existing.setLabel_wsh(dto.getLabel_wsh());
        if (dto.getName_wsh() != null) existing.setName_wsh(dto.getName_wsh());
        if (dto.getPhone_wsh() != null) existing.setPhone_wsh(dto.getPhone_wsh());
        if (dto.getAddress_wsh() != null) existing.setAddress_wsh(dto.getAddress_wsh());
        if (dto.getDetail_wsh() != null) existing.setDetail_wsh(dto.getDetail_wsh());
        if (dto.getLatitude_wsh() != null) existing.setLatitude_wsh(dto.getLatitude_wsh());
        if (dto.getLongitude_wsh() != null) existing.setLongitude_wsh(dto.getLongitude_wsh());
        if (dto.getIs_default_wsh() != null) {
            if (dto.getIs_default_wsh() == 1) {
                addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                        .eq(Address::getUser_id_wsh, userId)
                        .ne(Address::getId_wsh, id)
                        .set(Address::getIs_default_wsh, 0));
            }
            existing.setIs_default_wsh(dto.getIs_default_wsh());
        }
        addressMapper.updateById(existing);
        return existing;
    }

    /**
     * 删除地址，校验当前用户对地址的归属权
     * @param userId 用户ID
     * @param id 地址ID
     */
    @Transactional
    public void delete(Long userId, Long id) {
        log.info("delete()被调用");
        Address existing = getById(id);
        if (!existing.getUser_id_wsh().equals(userId)) throw new BusinessException("无权操作此地址");
        addressMapper.deleteById(id);
    }

    /**
     * 设置默认地址，将用户其他地址取消默认
     * @param userId 用户ID
     * @param id 地址ID
     */
    @Override
    public AddressDTO toDTO(Address entity) {
        if (entity == null) return null;
        AddressDTO dto = new AddressDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setUser_id_wsh(entity.getUser_id_wsh());
        dto.setLabel_wsh(entity.getLabel_wsh());
        dto.setName_wsh(entity.getName_wsh());
        dto.setPhone_wsh(entity.getPhone_wsh());
        dto.setAddress_wsh(entity.getAddress_wsh());
        dto.setDetail_wsh(entity.getDetail_wsh());
        dto.setLatitude_wsh(entity.getLatitude_wsh());
        dto.setLongitude_wsh(entity.getLongitude_wsh());
        dto.setIs_default_wsh(entity.getIs_default_wsh());
        return dto;
    }

    @Transactional
    public void setDefault(Long userId, Long id) {
        log.info("setDefault()被调用");
        addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                .eq(Address::getUser_id_wsh, userId)
                .set(Address::getIs_default_wsh, 0));
        addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                .eq(Address::getId_wsh, id)
                .eq(Address::getUser_id_wsh, userId)
                .set(Address::getIs_default_wsh, 1));
    }
}
