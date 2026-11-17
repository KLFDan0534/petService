package com.pet.pet.service;

import com.pet.pet.dto.PetCreateRequestDTO;
import com.pet.pet.dto.PetDTO;
import com.pet.pet.dto.PetUpdateRequestDTO;

import java.util.List;

public interface PetService {
    /**
     * 宠物全量列表查询
     *
     * 业务作用：查询未被逻辑删除的全部宠物，并转换为 DTO 返回，包含主人姓名信息。
     * 业务规则：MyBatis-Plus 逻辑删除自动过滤已删除记录。
     * 异常情况：无，不会返回 null（空列表返回 List.of()）。
     * 注意事项：全量查询在数据量大时性能不佳，建议后续按分页改造。
     *
     * @return 宠物 DTO 列表，不会返回 null
     */
    List<PetDTO> listAll();
    /**
     * 按主人查询宠物列表
     *
     * 业务作用：根据主人 ID 获取该用户拥有的全部宠物。
     * 业务规则：同一用户可以拥有多只宠物，列表按数据库默认顺序返回。
     * 注意事项：ownerId 参数不能为空，否则返回空列表。
     *
     * @param ownerId 主人用户 ID
     * @return 该主人的宠物列表
     */
    List<PetDTO> getPetsByOwner(Long ownerId);

    /**
     * 按商家查询本店服务过的宠物
     *
     * 业务作用：聚合该商家订单涉及的宠物，按 pet_id 去重后返回档案。
     * 业务规则：宠物归主人所有，商家只能查看在本店下过单的宠物，不能新增、编辑或下架。
     * 异常情况：无，无订单时返回空列表。
     * 注意事项：从未在本店下单的宠物不会出现在结果里，这是预期行为。
     *
     * @param merchantId 商家 ID
     * @return 该商家服务过的宠物列表，按宠物 ID 倒序
     */
    List<PetDTO> getPetsServedByMerchant(Long merchantId);

    /**
     * 宠物详情查询
     *
     * 业务作用：根据宠物 ID 查询单只宠物的详细信息，包含主人姓名等关联信息。
     * 业务规则：不存在时抛出 BusinessException。
     * 异常情况：宠物不存在时抛出 BusinessException("宠物不存在")。
     * 注意事项：仅查询单条，不会返回多条。
     *
     * @param id 宠物 ID
     * @return 宠物 DTO，不存在时抛出 BusinessException
     */
    PetDTO getPetById(Long id);
    /**
     * 创建宠物
     *
     * 业务作用：创建一只新宠物，必须指定主人 ID，写入所有基础字段后返回完整 DTO。
     * 业务规则：主人 ID 不能为空，否则抛 BusinessException。
     * 异常情况：主人 ID 为空时抛出 BusinessException("未指定宠物主人")。
     * 注意事项：插入后返回的 DTO 包含数据库自增 ID 与时间戳。
     *
     * @param request 创建请求，包含宠物基本信息及主人 ID
     * @return 创建完成后的宠物 DTO（含自动生成的 ID 和时间戳）
     */
    PetDTO createPet(PetCreateRequestDTO request);
    /**
     * 普通用户更新宠物信息
     *
     * 业务作用：当前用户作为宠物主人更新宠物信息。
     * 业务规则：会校验当前用户是否为该宠物的主人：非主人调用将抛出 BusinessException("无权修改此宠物")。
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
     * 普通用户删除宠物
     *
     * 业务作用：当前用户删除自己名下的宠物记录。
     * 业务规则：会校验当前用户是否为该宠物的主人：非主人调用将抛出 BusinessException。
     * 异常情况：宠物不存在时抛出 BusinessException("宠物不存在")；非主人时抛出 BusinessException("无权删除此宠物")。
     * 注意事项：逻辑删除，数据仍在数据库中但后续查询不可见。
     *
     * @param userId 当前操作用户 ID，用于所有权校验
     * @param id     要删除的宠物 ID
     */
    void deletePet(Long userId, Long id);
    /**
     * 管理员更新宠物信息
     *
     * 业务作用：管理员更新任意宠物的信息，跳过所有权校验。
     * 业务规则：管理员可直接修改任意宠物的信息，不校验宠物主人身份。
     * 异常情况：宠物不存在时抛出 BusinessException("宠物不存在")。
     * 注意事项：跳过所有权校验，适用于管理后台和客服场景。
     *
     * @param petId   宠物 ID
     * @param request 更新的字段
     * @return 更新后的宠物 DTO
     */
    PetDTO updatePetAsAdmin(Long petId, PetUpdateRequestDTO request);
    /**
     * 管理员删除宠物
     *
     * 业务作用：管理员删除任意宠物记录，跳过所有权校验。
     * 业务规则：管理员可直接删除任意宠物记录，不校验宠物主人身份。
     * 异常情况：宠物不存在时可正常返回（MyBatis-Plus 删除不存在记录返回 0）。
     * 注意事项：跳过所有权校验，适用于后台管理场景。
     *
     * @param id 要删除的宠物 ID
     */
    void deletePetAsAdmin(Long id);
}

