package com.pet.pet.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.BusinessException;
import com.pet.common.PageResult;
import com.pet.pet.dto.PetCreateRequestDTO;
import com.pet.pet.dto.PetDTO;
import com.pet.pet.dto.PetUpdateRequestDTO;
import com.pet.pet.entity.Pet;
import com.pet.pet.mapper.PetMapper;
import com.pet.pet.service.PetService;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PetServiceImpl implements PetService {

    private final PetMapper petMapper;
    private final UserMapper userMapper;

    /**
     * 业务规则：依赖均为 final 字段。
     * 注意事项：userMapper 仅用于 toDTO 时回填主人昵称。
     */
    public PetServiceImpl(PetMapper petMapper, UserMapper userMapper) {
        this.petMapper = petMapper;
        this.userMapper = userMapper;
    }

    /**
     * 宠物全量列表查询（实现）
     *
     * 业务作用：查询未被逻辑删除的全部宠物记录，遍历填充主人姓名后返回。
     * 业务规则：MyBatis-Plus 逻辑删除自动过滤 deleted=1 记录。
     * 异常情况：无（返回空列表而非 null）。
     * 注意事项：全表扫描，大数据量时建议改造为分页查询。
     */
    @Override
    public List<PetDTO> listAll() {
        log.info("调用 listAll()");
        return toDTOList(petMapper.selectList(null));
    }

    /**
     * 分页查询全部宠物（实现）
     *
     * 业务作用：管理后台宠物列表分页，替代 listAll 的全表加载。
     * 业务规则：keyword 按宠物名模糊匹配；size 越界（<1 或 >100）回落到 20。
     * 注意事项：只改查询方式，DTO 组装仍复用 toDTOList，输出字段与 listAll 完全一致。
     */
    @Override
    public PageResult<PetDTO> pageAll(Integer page, Integer size, String keyword) {
        int current = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1 || size > 100) ? 20 : size;

        LambdaQueryWrapper<Pet> wrapper = new LambdaQueryWrapper<Pet>()
                .like(keyword != null && !keyword.isBlank(), Pet::getName_wsh,
                        keyword == null ? null : keyword.trim())
                .orderByDesc(Pet::getCreated_at_wsh);

        Page<Pet> result = petMapper.selectPage(new Page<>(current, pageSize), wrapper);
        PageResult<PetDTO> pageResult = new PageResult<>();
        pageResult.setList(toDTOList(result.getRecords()));
        pageResult.setTotal(result.getTotal());
        pageResult.setPage(current);
        pageResult.setSize(pageSize);
        pageResult.setPages(result.getPages());
        return pageResult;
    }

    /**
     * 按主人查询宠物列表（实现）
     *
     * 业务作用：根据主人 ID 查询该主人名下的所有宠物，填充主人姓名后返回。
     * 业务规则：一个用户可拥有多只宠物。
     * 注意事项：ownerId 为 null 时返回空列表。
     */
    @Override
    public List<PetDTO> getPetsByOwner(Long ownerId) {
        log.info("调用 getPetsByOwner()");
        return toDTOList(petMapper.selectList(
                new LambdaQueryWrapper<Pet>().eq(Pet::getOwner_id_wsh, ownerId)));
    }

    /**
     * 按商家查询本店服务过的宠物（实现）
     *
     * 业务作用：聚合该商家订单涉及的宠物，按 pet_id 去重后返回档案。
     * 业务规则：宠物归主人所有，商家只能查看在本店下过单的宠物，不能新增、编辑或下架。
     * 异常情况：无，无订单时返回空列表。
     * 注意事项：从未在本店下单的宠物不会出现，这是预期行为。
     */
    @Override
    public List<PetDTO> getPetsServedByMerchant(Long merchantId) {
        log.info("调用 getPetsServedByMerchant()");
        return toDTOList(petMapper.selectServedByMerchant(merchantId));
    }

    /**
     * 宠物详情查询（实现）
     *
     * 业务作用：根据宠物 ID 查询单只宠物详情，不存在则抛异常。
     * 业务规则：不存在时抛出 BusinessException。
     * 异常情况：宠物不存在时抛出 BusinessException("宠物不存在")。
     * 注意事项：toDTO 包含 owner_name 的关联查询。
     */
    @Override
    public PetDTO getPetById(Long id) {
        log.info("调用 getPetById()");
        Pet pet = petMapper.selectById(id);
        if (pet == null) {
            throw new BusinessException("宠物不存在");
        }
        return toDTO(pet);
    }

    /**
     * 创建宠物（实现）
     *
     * 业务作用：创建一只新宠物，校验主人 ID 后执行 insert。
     * 业务规则：主人 ID 不能为空。
     * 异常情况：owner_id_wsh 为空时抛出 BusinessException("未指定宠物主人")。
     * 注意事项：使用 @Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public PetDTO createPet(PetCreateRequestDTO request) {
        log.info("调用 createPet()");
        if (request.getOwner_id_wsh() == null) {
            throw new BusinessException("未指定宠物主人");
        }
        Pet pet = new Pet();
        pet.setOwner_id_wsh(request.getOwner_id_wsh());
        pet.setName_wsh(request.getName_wsh());
        pet.setType_wsh(request.getType_wsh());
        pet.setBreed_wsh(request.getBreed_wsh());
        pet.setAge_wsh(request.getAge_wsh());
        pet.setWeight_wsh(request.getWeight_wsh());
        pet.setGender_wsh(request.getGender_wsh());
        pet.setSterilized_wsh(request.getSterilized_wsh());
        pet.setVaccinated_wsh(request.getVaccinated_wsh());
        pet.setAvatar_wsh(request.getAvatar_wsh());
        pet.setDescription_wsh(request.getDescription_wsh());
        pet.setAllergies_wsh(request.getAllergies_wsh());
        pet.setHabits_wsh(request.getHabits_wsh());
        petMapper.insert(pet);
        return toDTO(pet);
    }

    /**
     * 普通用户更新宠物信息（实现）
     *
     * 业务作用：校验所有权后更新宠物信息。
     * 业务规则：当前用户必须是宠物的主人。
     * 异常情况：宠物不存在时抛异常；非主人时抛出 BusinessException("无权修改此宠物")。
     * 注意事项：@Transactional 保证更新原子性。
     */
    @Transactional
    @Override
    public PetDTO updatePet(Long userId, Long petId, PetUpdateRequestDTO request) {
        log.info("调用 updatePet()");
        Pet existing = getByIdRaw(petId);
        if (!existing.getOwner_id_wsh().equals(userId)) {
            throw new BusinessException("无权修改此宠物");
        }
        applyUpdate(existing, request);
        petMapper.updateById(existing);
        return toDTO(existing);
    }

    /**
     * 普通用户删除宠物（实现）
     *
     * 业务作用：校验所有权后逻辑删除宠物。
     * 业务规则：当前用户必须是宠物的主人。
     * 异常情况：宠物不存在时抛异常；非主人时抛出 BusinessException("无权删除此宠物")。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public void deletePet(Long userId, Long id) {
        log.info("调用 deletePet()");
        Pet existing = getByIdRaw(id);
        if (!existing.getOwner_id_wsh().equals(userId)) {
            throw new BusinessException("无权删除此宠物");
        }
        petMapper.deleteById(id);
    }

    /**
     * 管理员更新宠物信息（实现）
     *
     * 业务作用：管理员更新任意宠物信息，跳过所有权校验。
     * 业务规则：不校验宠物主人身份。
     * 异常情况：宠物不存在时抛异常。
     * 注意事项：跳过所有权校验。
     */
    @Transactional
    @Override
    public PetDTO updatePetAsAdmin(Long petId, PetUpdateRequestDTO request) {
        log.info("调用 updatePetAsAdmin()");
        Pet existing = getByIdRaw(petId);
        applyUpdate(existing, request);
        petMapper.updateById(existing);
        return toDTO(existing);
    }

    /**
     * 管理员删除宠物（实现）
     *
     * 业务作用：管理员逻辑删除任意宠物记录，跳过所有权校验。
     * 业务规则：跳过所有权校验。
     * 异常情况：无（删除不存在记录时 MyBatis-Plus 返回 0）。
     * 注意事项：跳过所有权校验。
     */
    @Transactional
    @Override
    public void deletePetAsAdmin(Long id) {
        log.info("调用 deletePetAsAdmin()");
        petMapper.deleteById(id);
    }

    /**
     * 业务作用：按主键查询宠物实体，不存在时抛业务异常。
     * 业务规则：宠物必须存在。
     * 异常情况：宠物不存在时抛 BusinessException「宠物不存在」。
     * 注意事项：返回的是实体而非 DTO，调用方可直接修改后 updateById。
     */
    private Pet getByIdRaw(Long id) {
        Pet pet = petMapper.selectById(id);
        // 查不到宠物直接抛业务异常
        if (pet == null) {
            throw new BusinessException("宠物不存在");
        }
        return pet;
    }

    /**
     * 业务作用：把请求中非 null 的字段写入已有宠物实体，实现部分更新。
     * 业务规则：null 字段保持原值，无法通过该接口把字段清空。
     * 注意事项：主人字段 owner_id_wsh 不在可更新范围内，宠物归属不可通过该接口变更。
     */
    private void applyUpdate(Pet existing, PetUpdateRequestDTO request) {
        // 只覆盖非空字段，实现部分更新
        if (request.getName_wsh() != null) existing.setName_wsh(request.getName_wsh());
        if (request.getType_wsh() != null) existing.setType_wsh(request.getType_wsh());
        if (request.getBreed_wsh() != null) existing.setBreed_wsh(request.getBreed_wsh());
        if (request.getAge_wsh() != null) existing.setAge_wsh(request.getAge_wsh());
        if (request.getWeight_wsh() != null) existing.setWeight_wsh(request.getWeight_wsh());
        if (request.getGender_wsh() != null) existing.setGender_wsh(request.getGender_wsh());
        if (request.getSterilized_wsh() != null) existing.setSterilized_wsh(request.getSterilized_wsh());
        if (request.getVaccinated_wsh() != null) existing.setVaccinated_wsh(request.getVaccinated_wsh());
        if (request.getAvatar_wsh() != null) existing.setAvatar_wsh(request.getAvatar_wsh());
        if (request.getDescription_wsh() != null) existing.setDescription_wsh(request.getDescription_wsh());
        if (request.getAllergies_wsh() != null) existing.setAllergies_wsh(request.getAllergies_wsh());
        if (request.getHabits_wsh() != null) existing.setHabits_wsh(request.getHabits_wsh());
    }

    /**
     * 业务作用：把宠物实体转换为 DTO，并回填主人展示名。
     * 业务规则：入参为 null 时返回 null；主人不存在时 owner_name_wsh 为 null。
     * 注意事项：每条记录都会触发一次用户查询，列表场景存在 N+1 问题。
     */
    private PetDTO toDTO(Pet pet) {
        if (pet == null) return null;
        PetDTO dto = new PetDTO();
        dto.setId_wsh(pet.getId_wsh());
        dto.setOwner_id_wsh(pet.getOwner_id_wsh());
        if (pet.getOwner_id_wsh() != null) {
            User owner = userMapper.selectById(pet.getOwner_id_wsh());
            // 昵称缺失时退回用户名展示
            dto.setOwner_name_wsh(owner != null ? (owner.getNickname_wsh() != null ? owner.getNickname_wsh() : owner.getUsername_wsh()) : null);
        }
        dto.setName_wsh(pet.getName_wsh());
        dto.setType_wsh(pet.getType_wsh());
        dto.setBreed_wsh(pet.getBreed_wsh());
        dto.setAge_wsh(pet.getAge_wsh());
        dto.setWeight_wsh(pet.getWeight_wsh());
        dto.setGender_wsh(pet.getGender_wsh());
        dto.setSterilized_wsh(pet.getSterilized_wsh());
        dto.setVaccinated_wsh(pet.getVaccinated_wsh());
        dto.setAvatar_wsh(pet.getAvatar_wsh());
        dto.setDescription_wsh(pet.getDescription_wsh());
        dto.setAllergies_wsh(pet.getAllergies_wsh());
        dto.setHabits_wsh(pet.getHabits_wsh());
        dto.setCreated_at_wsh(pet.getCreated_at_wsh());
        return dto;
    }

    /**
     * 业务作用：批量把宠物实体转换为 DTO。
     * 数据处理：入参为 null 时返回空列表，否则流式逐条转换。
     * 业务规则：保证不返回 null。
     * 注意事项：逐条转换会逐条查用户表，列表越大查询次数越多。
     */
    private List<PetDTO> toDTOList(List<Pet> list) {
        // 空列表兜底，避免上层判空
        if (list == null) return List.of();
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }
}
