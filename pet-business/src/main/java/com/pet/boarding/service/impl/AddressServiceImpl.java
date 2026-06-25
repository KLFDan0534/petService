package com.pet.boarding.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pet.common.BusinessException;
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
     * @param addr 地址实体
     * @return 创建后的地址
     */
    @Transactional
    public Address create(Long userId, Address addr) {
        log.info("create() called");
        addr.setUser_id_wsh(userId);
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
     * @param addr 地址实体
     * @return 更新后的地址
     */
    @Transactional
    public Address update(Long userId, Long id, Address addr) {
        log.info("update() called");
        Address existing = getById(id);
        if (!existing.getUser_id_wsh().equals(userId)) throw new BusinessException("无权操作此地址");
        if (addr.getLabel_wsh() != null) existing.setLabel_wsh(addr.getLabel_wsh());
        if (addr.getName_wsh() != null) existing.setName_wsh(addr.getName_wsh());
        if (addr.getPhone_wsh() != null) existing.setPhone_wsh(addr.getPhone_wsh());
        if (addr.getAddress_wsh() != null) existing.setAddress_wsh(addr.getAddress_wsh());
        if (addr.getDetail_wsh() != null) existing.setDetail_wsh(addr.getDetail_wsh());
        if (addr.getLatitude_wsh() != null) existing.setLatitude_wsh(addr.getLatitude_wsh());
        if (addr.getLongitude_wsh() != null) existing.setLongitude_wsh(addr.getLongitude_wsh());
        if (addr.getIs_default_wsh() != null) {
            if (addr.getIs_default_wsh() == 1) {
                addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                        .eq(Address::getUser_id_wsh, userId)
                        .ne(Address::getId_wsh, id)
                        .set(Address::getIs_default_wsh, 0));
            }
            existing.setIs_default_wsh(addr.getIs_default_wsh());
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
