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
     * 业务作用：获取指定用户的所有地址，默认地址排最前。
     * 业务规则：每个用户最多有一个默认地址；排序确保默认地址在列表首位。
     *
     * @param userId 用户ID
     * @return 地址列表
     */
    List<Address> listByUser(Long userId);

    /**
     * 业务作用：根据主键ID查询地址信息。
     * 业务规则：查询结果为 null 时抛出 BusinessException。
     *
     * @param id 地址ID
     * @return 地址实体
     * @throws BusinessException 如果地址不存在
     */
    Address getById(Long id);

    /**
     * 业务作用：为用户新增一条地址记录。
     * 业务规则：如果新地址设为默认，自动取消该用户其他地址的默认标志；如果用户尚无默认地址，当前地址自动成为默认。
     *
     * @param userId 用户ID
     * @param dto    地址创建请求DTO
     * @return 创建后的地址
     */
    Address create(Long userId, AddressCreateRequestDTO dto);

    /**
     * 业务作用：修改用户已有的地址信息。
     * 数据处理：先校验地址归属权（操作人必须是该地址的所属用户）；仅更新 DTO 中非 null 字段；如果设为默认则取消其他地址的默认标志（排除自身）。
     * 业务规则：操作人必须是该地址的所属用户；仅更新非 null 字段；设为默认时取消其他地址默认标志。
     *
     * @param userId 用户ID（用于校验归属权）
     * @param id     地址ID
     * @param dto    地址更新请求DTO
     * @return 更新后的地址
     * @throws BusinessException 如果用户无权操作此地址
     */
    Address update(Long userId, Long id, AddressUpdateRequestDTO dto);

    /**
     * 业务作用：删除用户指定的地址记录。
     * 数据处理：先校验地址归属权（操作人必须是该地址的所属用户），然后物理删除。
     * 业务规则：仅地址的所属用户可以删除。
     *
     * @param userId 用户ID（用于校验归属权）
     * @param id     地址ID
     * @throws BusinessException 如果用户无权操作此地址
     */
    void delete(Long userId, Long id);

    /**
     * 业务作用：将指定地址设为当前用户的默认地址。
     * 业务规则：一个用户最多只有一个默认地址。
     *
     * @param userId 用户ID
     * @param id     地址ID
     */
    void setDefault(Long userId, Long id);

    /**
     * 业务作用：将地址实体转换为前端展示所需的 DTO。
     *
     * @param entity 地址实体
     * @return 地址DTO，入参为 null 时返回 null
     */
    AddressDTO toDTO(Address entity);
}

