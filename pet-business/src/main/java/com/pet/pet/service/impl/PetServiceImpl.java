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

    @Override
    public List<PetDTO> listAll() {
        log.info("调用 listAll()");
        return toDTOList(petMapper.selectList(null));
    }

    @Override
    public List<PetDTO> getPetsByOwner(Long ownerId) {
        log.info("调用 getPetsByOwner()");
        return toDTOList(petMapper.selectList(
                new LambdaQueryWrapper<Pet>().eq(Pet::getOwner_id_wsh, ownerId)));
    }

    @Override
    public PetDTO getPetById(Long id) {
        log.info("调用 getPetById()");
        Pet pet = petMapper.selectById(id);
        if (pet == null) {
            throw new BusinessException("宠物不存在");
        }
        return toDTO(pet);
    }

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

    @Transactional
    @Override
    public PetDTO updatePetAsAdmin(Long petId, PetUpdateRequestDTO request) {
        log.info("调用 updatePetAsAdmin()");
        Pet existing = getByIdRaw(petId);
        applyUpdate(existing, request);
        petMapper.updateById(existing);
        return toDTO(existing);
    }

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
