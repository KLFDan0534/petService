package com.pet.boarding.service;

import com.pet.boarding.dto.AddressCreateRequestDTO;
import com.pet.boarding.dto.AddressDTO;
import com.pet.boarding.dto.AddressUpdateRequestDTO;
import com.pet.boarding.entity.Address;

import java.util.List;

/**
 * 用户地址管理服务接口。
 * <p>
 * 管理用户的收货/联系地址，支持多地址管理、默认地址设置、地址归属权校验。
 * 地址包含标签（如"家"、"公司"）、联系人、电话、详细地址、地理坐标等信息。
 * 创建/更新/删除地址时都校验当前用户对地址的归属权，防止跨用户越权操作。
 */
public interface AddressService {

    /**
     * 【查询用户地址列表】
     *
     * 业务作用：获取指定用户的所有地址，默认地址排最前。
     * 调用场景：用户在地址管理页面查看地址列表时调用。
     * 调用链：AddressController → listByUser → AddressMapper.selectList
     * 数据处理：按 user_id 查询，按 is_default 降序（默认地址排最前），再按创建时间降序。
     * 业务规则：每个用户最多有一个默认地址；排序确保默认地址在列表首位。
     * 状态影响：只读操作。
     *
     * @param userId 用户ID
     * @return 地址列表
     */
    List<Address> listByUser(Long userId);

    /**
     * 【根据ID查询地址】
     *
     * 业务作用：根据主键ID查询地址信息。
     * 调用场景：被 update、delete、setDefault 方法内部调用。
     * 调用链：上层业务方法 → getById → AddressMapper.selectById
     * 数据处理：按主键ID查询单条记录。
     * 业务规则：查询结果为 null 时抛出 BusinessException。
     * 状态影响：只读操作。
     *
     * @param id 地址ID
     * @return 地址实体
     * @throws BusinessException 如果地址不存在
     */
    Address getById(Long id);

    /**
     * 【创建地址】
     *
     * 业务作用：为用户新增一条地址记录。
     * 调用场景：用户在地址管理页面添加新地址时调用。
     * 调用链：AddressController → create @Transactional → AddressMapper.insert
     * 数据处理：根据 DTO 构建地址实体；如果新地址设为默认则自动取消其他地址的默认标志。插入后检查用户是否还有默认地址，若无则自动设为默认。
     * 业务规则：如果新地址设为默认，自动取消该用户其他地址的默认标志；如果用户尚无默认地址，当前地址自动成为默认。
     * 状态影响：新增地址记录；可能修改其他地址的 is_default 标志。
     *
     * @param userId 用户ID
     * @param dto    地址创建请求DTO
     * @return 创建后的地址
     */
    Address create(Long userId, AddressCreateRequestDTO dto);

    /**
     * 【更新地址信息】
     *
     * 业务作用：修改用户已有的地址信息。
     * 调用场景：用户在地址管理页面编辑地址时调用。
     * 调用链：AddressController → update @Transactional → AddressMapper.updateById
     * 数据处理：先校验地址归属权（操作人必须是该地址的所属用户）；仅更新 DTO 中非 null 字段；如果设为默认则取消其他地址的默认标志（排除自身）。
     * 业务规则：操作人必须是该地址的所属用户；仅更新非 null 字段；设为默认时取消其他地址默认标志。
     * 状态影响：更新地址字段；可能修改其他地址的 is_default 标志。
     *
     * @param userId 用户ID（用于校验归属权）
     * @param id     地址ID
     * @param dto    地址更新请求DTO
     * @return 更新后的地址
     * @throws BusinessException 如果用户无权操作此地址
     */
    Address update(Long userId, Long id, AddressUpdateRequestDTO dto);

    /**
     * 【删除地址】
     *
     * 业务作用：删除用户指定的地址记录。
     * 调用场景：用户在地址管理页面删除地址时调用。
     * 调用链：AddressController → delete @Transactional → AddressMapper.deleteById
     * 数据处理：先校验地址归属权（操作人必须是该地址的所属用户），然后物理删除。
     * 业务规则：仅地址的所属用户可以删除。
     * 状态影响：物理删除地址记录。
     *
     * @param userId 用户ID（用于校验归属权）
     * @param id     地址ID
     * @throws BusinessException 如果用户无权操作此地址
     */
    void delete(Long userId, Long id);

    /**
     * 【设置默认地址】
     *
     * 业务作用：将指定地址设为当前用户的默认地址。
     * 调用场景：用户在地址管理页面设置默认地址时调用。
     * 调用链：AddressController → setDefault @Transactional → AddressMapper.update（批量取消 + 单独设置）
     * 数据处理：两步更新——先将该用户所有地址的 is_default 置为 0，再将指定地址的 is_default 置为 1。
     * 业务规则：一个用户最多只有一个默认地址。
     * 状态影响：修改该用户所有地址的 is_default 标志。
     *
     * @param userId 用户ID
     * @param id     地址ID
     */
    void setDefault(Long userId, Long id);

    /**
     * 【地址实体转DTO】
     *
     * 业务作用：将地址实体转换为前端展示所需的 DTO。
     * 调用场景：Controller 层返回地址信息前调用。
     * 调用链：各查询 Controller → toDTO
     * 数据处理：字段拷贝。
     * 状态影响：只读操作。
     *
     * @param entity 地址实体
     * @return 地址DTO，入参为 null 时返回 null
     */
    AddressDTO toDTO(Address entity);
}

