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
 * 地址服务实现。
 * <p>
 * 管理用户地址的增删改查以及默认地址切换。每次写操作都校验地址归属权，
 * 确保用户只能操作自己的地址。默认地址规则：每个用户最多只有一个默认地址。
 */
@Service
@Slf4j
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;

    public AddressServiceImpl(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    /**
     * 【查询用户地址列表】
     *
     * 业务作用：获取指定用户的所有地址，默认地址排在首位。
     * 调用场景：用户在地址管理页面查看地址列表时调用。
     * 调用链：AddressController → listByUser → AddressMapper.selectList（按 is_default 降序 + 创建时间降序）
     * 数据处理：按 user_id 查询，按 is_default 降序、创建时间降序排列。
     * 业务规则：排序确保默认地址始终在列表首位。
     * 状态影响：只读操作。
     */
    @Override
    public List<Address> listByUser(Long userId) {
        log.info("listByUser() 被调用");
        return addressMapper.selectList(
                new LambdaQueryWrapper<Address>()
                        .eq(Address::getUser_id_wsh, userId)
                        .orderByDesc(Address::getIs_default_wsh)
                        .orderByDesc(Address::getCreated_at_wsh));
    }

    /**
     * 【根据ID查询地址】
     *
     * 业务作用：根据主键ID查询地址信息，不存在则抛异常。
     * 调用场景：被 update、delete、setDefault 方法内部调用。
     * 调用链：上层业务方法 → getById → AddressMapper.selectById
     * 数据处理：按主键ID查询单条记录。
     * 业务规则：查询结果为 null 时抛出 BusinessException。
     * 状态影响：只读操作。
     * 异常情况：地址不存在抛 BusinessException("地址不存在")。
     */
    @Override
    public Address getById(Long id) {
        log.info("getById() 被调用");
        Address addr = addressMapper.selectById(id);
        if (addr == null) throw new BusinessException("地址不存在");
        return addr;
    }

    /**
     * 【创建地址】
     *
     * 业务作用：为用户新增一条地址记录，自动管理默认地址标志。
     * 调用场景：用户在地址管理页面添加新地址时调用。
     * 调用链：AddressController → create @Transactional → AddressMapper（批量取消默认 + insert + 检查默认）
     * 数据处理：根据 DTO 构建地址实体；若设为默认则先清除其他地址的默认标志；插入后检查用户是否还有默认地址，若无则自动设为默认。
     * 业务规则：设为默认时先清除其他地址默认标志；确保用户始终有一个默认地址。
     * 状态影响：新增地址记录；可能修改其他地址的 is_default 标志。
     * 事务边界：批量更新 + 插入 + 可能再次更新在同一事务中。
     */
    @Override
    @Transactional
    public Address create(Long userId, AddressCreateRequestDTO dto) {
        log.info("create() 被调用");
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
     * 【更新地址信息】
     *
     * 业务作用：修改用户已有的地址信息，支持默认标志变更。
     * 调用场景：用户在地址管理页面编辑地址时调用。
     * 调用链：AddressController → update @Transactional → AddressMapper.updateById
     * 数据处理：先校验地址归属权；仅更新非 null 字段；若设为默认则取消其他地址默认标志（使用 ne 排除自身）。
     * 业务规则：操作人必须是该地址的所属用户；设为默认时排除自身，避免将自己取消默认。
     * 状态影响：更新地址字段；可能修改其他地址的 is_default 标志。
     * 事务边界：权限校验 + 批量更新 + 单条更新在同一事务中。
     * 异常情况：用户无权操作时抛 BusinessException。
     */
    @Override
    @Transactional
    public Address update(Long userId, Long id, AddressUpdateRequestDTO dto) {
        log.info("update() 被调用");
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
     * 【删除地址】
     *
     * 业务作用：删除用户指定的地址记录。
     * 调用场景：用户在地址管理页面删除地址时调用。
     * 调用链：AddressController → delete @Transactional → AddressMapper.deleteById
     * 数据处理：先校验地址归属权，然后物理删除。
     * 业务规则：仅地址的所属用户可以删除。
     * 状态影响：物理删除地址记录。
     * 事务边界：权限校验 + 删除在同一事务中。
     * 异常情况：用户无权操作时抛 BusinessException。
     */
    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        log.info("delete() 被调用");
        Address existing = getById(id);
        if (!existing.getUser_id_wsh().equals(userId)) throw new BusinessException("无权操作此地址");
        addressMapper.deleteById(id);
    }

    /**
     * 【地址实体转DTO】
     *
     * 业务作用：将地址实体转换为前端展示所需的 DTO。
     * 调用场景：Controller 层返回地址信息前调用。
     * 调用链：各查询 Controller → toDTO
     * 数据处理：字段拷贝。
     * 状态影响：只读操作。
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

    /**
     * 【设置默认地址】
     *
     * 业务作用：将指定地址设为当前用户的默认地址。
     * 调用场景：用户在地址管理页面设置默认地址时调用。
     * 调用链：AddressController → setDefault @Transactional → AddressMapper（批量取消 + 单独设置）
     * 数据处理：两步更新——先将该用户所有地址的 is_default 置为 0，再将指定地址的 is_default 置为 1。
     * 业务规则：一个用户最多只有一个默认地址。
     * 状态影响：修改该用户所有地址的 is_default 标志。
     * 事务边界：两次更新在同一事务中。
     */
    @Override
    @Transactional
    public void setDefault(Long userId, Long id) {
        log.info("setDefault() 被调用");
        addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                .eq(Address::getUser_id_wsh, userId)
                .set(Address::getIs_default_wsh, 0));
        addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                .eq(Address::getId_wsh, id)
                .eq(Address::getUser_id_wsh, userId)
                .set(Address::getIs_default_wsh, 1));
    }
}
