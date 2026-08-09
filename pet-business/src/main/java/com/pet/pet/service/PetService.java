package com.pet.pet.service;

import com.pet.pet.dto.PetCreateRequestDTO;
import com.pet.pet.dto.PetDTO;
import com.pet.pet.dto.PetUpdateRequestDTO;

import java.util.List;

public interface PetService {
    /**
     * 【业务名称】宠物全量列表查询
     * 业务作用：查询未被逻辑删除的全部宠物，并转换为 DTO 返回，包含主人姓名信息。
     * 调用场景：管理后台宠物列表页、API 调试接口。
     * 调用链：PetService.listAll() → PetMapper.selectList() → toDTOList()。
     * 数据处理：无查询条件，返回全部宠物记录，并从 User 表补充 owner_name。
     * 业务规则：MyBatis-Plus 逻辑删除自动过滤已删除记录。
     * 状态影响：无。
     * 异常情况：无，不会返回 null（空列表返回 List.of()）。
     * 注意事项：全量查询在数据量大时性能不佳，建议后续按分页改造。
     *
     * @return 宠物 DTO 列表，不会返回 null
     */
    List<PetDTO> listAll();
    /**
     * 【业务名称】按主人查询宠物列表
     * 业务作用：根据主人 ID 获取该用户拥有的全部宠物。
     * 调用场景：用户个人中心展示"我的宠物"列表。
     * 调用链：PetService.getPetsByOwner() → PetMapper.selectList()。
     * 数据处理：按 owner_id_wsh 精确匹配，返回 DTO 列表（含主人姓名填充）。
     * 业务规则：同一用户可以拥有多只宠物，列表按数据库默认顺序返回。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：ownerId 参数不能为空，否则返回空列表。
     *
     * @param ownerId 主人用户 ID
     * @return 该主人的宠物列表
     */
    List<PetDTO> getPetsByOwner(Long ownerId);
    /**
     * 【业务名称】宠物详情查询
     * 业务作用：根据宠物 ID 查询单只宠物的详细信息，包含主人姓名等关联信息。
     * 调用场景：宠物详情页、订单选择宠物下拉、护理记录关联查询。
     * 调用链：PetService.getPetById() → PetMapper.selectById() → toDTO()。
     * 数据处理：根据主键查询单条记录，从 User 表关联 owner_name。
     * 业务规则：不存在时抛出 BusinessException。
     * 状态影响：无。
     * 异常情况：宠物不存在时抛出 BusinessException("宠物不存在")。
     * 注意事项：仅查询单条，不会返回多条。
     *
     * @param id 宠物 ID
     * @return 宠物 DTO，不存在时抛出 BusinessException
     */
    PetDTO getPetById(Long id);
    /**
     * 【业务名称】创建宠物
     * 业务作用：创建一只新宠物，必须指定主人 ID，写入所有基础字段后返回完整 DTO。
     * 调用场景：用户添加宠物、管理员代客添加宠物。
     * 调用链：PetService.createPet() → PetMapper.insert() → toDTO()。
     * 数据处理：将请求中的全部字段复制到新实体后执行 insert（含名称、类型、品种、年龄、体重、性别、绝育状态、疫苗接种状态、头像、描述、过敏信息、生活习惯）。
     * 业务规则：主人 ID 不能为空，否则抛 BusinessException。
     * 状态影响：新增一条宠物记录。
     * 异常情况：主人 ID 为空时抛出 BusinessException("未指定宠物主人")。
     * 注意事项：插入后返回的 DTO 包含数据库自增 ID 与时间戳。
     *
     * @param request 创建请求，包含宠物基本信息及主人 ID
     * @return 创建完成后的宠物 DTO（含自动生成的 ID 和时间戳）
     */
    PetDTO createPet(PetCreateRequestDTO request);
    /**
     * 【业务名称】普通用户更新宠物信息
     * 业务作用：当前用户作为宠物主人更新宠物信息。
     * 调用场景：用户编辑自己的宠物资料页。
     * 调用链：PetService.updatePet() → 校验主人 → applyUpdate() → PetMapper.updateById()。
     * 数据处理：仅更新请求中非空的字段，其余保持不变（名称、类型、品种、年龄、体重、性别、绝育状态、疫苗接种、头像、描述、过敏信息、生活习惯）。
     * 业务规则：会校验当前用户是否为该宠物的主人：非主人调用将抛出 BusinessException("无权修改此宠物")。
     * 状态影响：更新宠物表对应记录。
     * 异常情况：宠物不存在时抛出 BusinessException("宠物不存在")；非主人时抛出 BusinessException("无权修改此宠物")。
     * 注意事项：仅更新请求中非 null 的字段。
     *
     * @param userId  当前操作用户 ID，用于所有权校验
     * @param petId   要更新的宠物 ID
     * @param request 更新的字段，null 字段不更新
     * @return 更新后的宠物 DTO
     */
    PetDTO updatePet(Long userId, Long petId, PetUpdateRequestDTO request);
    /**
     * 【业务名称】普通用户删除宠物
     * 业务作用：当前用户删除自己名下的宠物记录。
     * 调用场景：用户删除宠物、不再寄养时清理宠物列表。
     * 调用链：PetService.deletePet() → 校验主人 → PetMapper.deleteById()。
     * 数据处理：执行逻辑删除（MyBatis-Plus 自动填充 deleted 字段）。
     * 业务规则：会校验当前用户是否为该宠物的主人：非主人调用将抛出 BusinessException。
     * 状态影响：标记对应宠物记录为已删除（逻辑删除）。
     * 异常情况：宠物不存在时抛出 BusinessException("宠物不存在")；非主人时抛出 BusinessException("无权删除此宠物")。
     * 注意事项：逻辑删除，数据仍在数据库中但后续查询不可见。
     *
     * @param userId 当前操作用户 ID，用于所有权校验
     * @param id     要删除的宠物 ID
     */
    void deletePet(Long userId, Long id);
    /**
     * 【业务名称】管理员更新宠物信息
     * 业务作用：管理员更新任意宠物的信息，跳过所有权校验。
     * 调用场景：后台管理编辑宠物信息、客服协助用户修改。
     * 调用链：PetService.updatePetAsAdmin() → applyUpdate() → PetMapper.updateById()。
     * 数据处理：同普通更新，仅更新非 null 字段。
     * 业务规则：管理员可直接修改任意宠物的信息，不校验宠物主人身份。
     * 状态影响：更新宠物表对应记录。
     * 异常情况：宠物不存在时抛出 BusinessException("宠物不存在")。
     * 注意事项：跳过所有权校验，适用于管理后台和客服场景。
     *
     * @param petId   宠物 ID
     * @param request 更新的字段
     * @return 更新后的宠物 DTO
     */
    PetDTO updatePetAsAdmin(Long petId, PetUpdateRequestDTO request);
    /**
     * 【业务名称】管理员删除宠物
     * 业务作用：管理员删除任意宠物记录，跳过所有权校验。
     * 调用场景：后台管理删除违规或重复宠物记录。
     * 调用链：PetService.deletePetAsAdmin() → PetMapper.deleteById()。
     * 数据处理：逻辑删除。
     * 业务规则：管理员可直接删除任意宠物记录，不校验宠物主人身份。
     * 状态影响：标记对应宠物记录为已删除。
     * 异常情况：宠物不存在时可正常返回（MyBatis-Plus 删除不存在记录返回 0）。
     * 注意事项：跳过所有权校验，适用于后台管理场景。
     *
     * @param id 要删除的宠物 ID
     */
    void deletePetAsAdmin(Long id);
}

