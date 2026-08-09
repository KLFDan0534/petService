package com.pet.pet.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
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

    public PetServiceImpl(PetMapper petMapper, UserMapper userMapper) {
        this.petMapper = petMapper;
        this.userMapper = userMapper;
    }

    /**
     * 【业务名称】宠物全量列表查询（实现）
     * 业务作用：查询未被逻辑删除的全部宠物记录，遍历填充主人姓名后返回。
     * 调用场景：管理后台宠物列表页、API调试。
     * 调用链：listAll() → PetMapper.selectList(null) → toDTOList() → toDTO()。
     * 数据处理：selectList(null) 无条件查询全部，toDTO 时从 User 表关联 owner_name。
     * 业务规则：MyBatis-Plus 逻辑删除自动过滤 deleted=1 记录。
     * 状态影响：无。
     * 异常情况：无（返回空列表而非 null）。
     * 注意事项：全表扫描，大数据量时建议改造为分页查询。
     */
    @Override
    public List<PetDTO> listAll() {
        log.info("调用 listAll()");
        return toDTOList(petMapper.selectList(null));
    }

    /**
     * 【业务名称】按主人查询宠物列表（实现）
     * 业务作用：根据主人 ID 查询该主人名下的所有宠物，填充主人姓名后返回。
     * 调用场景：用户个人中心"我的宠物"列表。
     * 调用链：getPetsByOwner() → PetMapper.selectList(LambdaQueryWrapper) → toDTOList()。
     * 数据处理：按 owner_id_wsh 精确匹配过滤。
     * 业务规则：一个用户可拥有多只宠物。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：ownerId 为 null 时返回空列表。
     */
    @Override
    public List<PetDTO> getPetsByOwner(Long ownerId) {
        log.info("调用 getPetsByOwner()");
        return toDTOList(petMapper.selectList(
                new LambdaQueryWrapper<Pet>().eq(Pet::getOwner_id_wsh, ownerId)));
    }

    /**
     * 【业务名称】宠物详情查询（实现）
     * 业务作用：根据宠物 ID 查询单只宠物详情，不存在则抛异常。
     * 调用场景：宠物详情页、订单选择宠物。
     * 调用链：getPetById() → PetMapper.selectById() → toDTO()。
     * 数据处理：主键查询，填充主人姓名。
     * 业务规则：不存在时抛出 BusinessException。
     * 状态影响：无。
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
     * 【业务名称】创建宠物（实现）
     * 业务作用：创建一只新宠物，校验主人 ID 后执行 insert。
     * 调用场景：用户添加宠物、管理员代客添加。
     * 调用链：createPet() → PetMapper.insert() → toDTO()。
     * 数据处理：复制请求中所有字段到新 Pet 实体（含 13 个业务字段）。
     * 业务规则：主人 ID 不能为空。
     * 状态影响：新增一条宠物记录。
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
     * 【业务名称】普通用户更新宠物信息（实现）
     * 业务作用：校验所有权后更新宠物信息。
     * 调用场景：用户编辑自己的宠物资料。
     * 调用链：updatePet() → getByIdRaw() → 校验主人 → applyUpdate() → PetMapper.updateById()。
     * 数据处理：仅更新请求中非 null 字段，不更新的保持原值。
     * 业务规则：当前用户必须是宠物的主人。
     * 状态影响：更新宠物表对应记录。
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
     * 【业务名称】普通用户删除宠物（实现）
     * 业务作用：校验所有权后逻辑删除宠物。
     * 调用场景：用户删除自己的宠物。
     * 调用链：deletePet() → getByIdRaw() → 校验主人 → PetMapper.deleteById()。
     * 数据处理：逻辑删除（MyBatis-Plus 自动填充 deleted 字段）。
     * 业务规则：当前用户必须是宠物的主人。
     * 状态影响：标记宠物记录为已删除。
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
     * 【业务名称】管理员更新宠物信息（实现）
     * 业务作用：管理员更新任意宠物信息，跳过所有权校验。
     * 调用场景：后台管理编辑宠物。
     * 调用链：updatePetAsAdmin() → getByIdRaw() → applyUpdate() → PetMapper.updateById()。
     * 数据处理：仅更新非 null 字段。
     * 业务规则：不校验宠物主人身份。
     * 状态影响：更新宠物表对应记录。
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
     * 【业务名称】管理员删除宠物（实现）
     * 业务作用：管理员逻辑删除任意宠物记录，跳过所有权校验。
     * 调用场景：后台管理删除宠物。
     * 调用链：deletePetAsAdmin() → PetMapper.deleteById()。
     * 数据处理：逻辑删除。
     * 业务规则：跳过所有权校验。
     * 状态影响：标记宠物记录为已删除。
     * 异常情况：无（删除不存在记录时 MyBatis-Plus 返回 0）。
     * 注意事项：跳过所有权校验。
     */
    @Transactional
    @Override
    public void deletePetAsAdmin(Long id) {
        log.info("调用 deletePetAsAdmin()");
        petMapper.deleteById(id);
    }

    private Pet getByIdRaw(Long id) {
        Pet pet = petMapper.selectById(id);
        if (pet == null) {
            throw new BusinessException("宠物不存在");
        }
        return pet;
    }

    private void applyUpdate(Pet existing, PetUpdateRequestDTO request) {
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

    private PetDTO toDTO(Pet pet) {
        if (pet == null) return null;
        PetDTO dto = new PetDTO();
        dto.setId_wsh(pet.getId_wsh());
        dto.setOwner_id_wsh(pet.getOwner_id_wsh());
        if (pet.getOwner_id_wsh() != null) {
            User owner = userMapper.selectById(pet.getOwner_id_wsh());
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

    private List<PetDTO> toDTOList(List<Pet> list) {
        if (list == null) return List.of();
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }
}
