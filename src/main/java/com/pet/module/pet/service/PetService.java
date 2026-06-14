package com.pet.module.pet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.module.pet.entity.Pet;
import com.pet.module.pet.mapper.PetMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PetService {

    private final PetMapper petMapper;

    public PetService(PetMapper petMapper) {
        this.petMapper = petMapper;
    }

    public List<Pet> getPetsByOwner(Long ownerId) {
        return petMapper.selectList(
                new LambdaQueryWrapper<Pet>().eq(Pet::getOwnerId, ownerId));
    }

    public Pet getPetById(Long id) {
        Pet pet = petMapper.selectById(id);
        if (pet == null) {
            throw new BusinessException("宠物不存在");
        }
        return pet;
    }

    @Transactional
    public Pet createPet(Pet pet) {
        if (pet.getOwnerId() == null) {
            throw new BusinessException("未指定宠物主人");
        }
        petMapper.insert(pet);
        return pet;
    }

    @Transactional
    public Pet updatePet(Pet pet) {
        Pet existing = getPetById(pet.getId());
        if (pet.getName() != null) existing.setName(pet.getName());
        if (pet.getType() != null) existing.setType(pet.getType());
        if (pet.getBreed() != null) existing.setBreed(pet.getBreed());
        if (pet.getAge() != null) existing.setAge(pet.getAge());
        if (pet.getWeight() != null) existing.setWeight(pet.getWeight());
        if (pet.getGender() != null) existing.setGender(pet.getGender());
        if (pet.getSterilized() != null) existing.setSterilized(pet.getSterilized());
        if (pet.getVaccinated() != null) existing.setVaccinated(pet.getVaccinated());
        if (pet.getAvatar() != null) existing.setAvatar(pet.getAvatar());
        if (pet.getDescription() != null) existing.setDescription(pet.getDescription());
        if (pet.getAllergies() != null) existing.setAllergies(pet.getAllergies());
        if (pet.getHabits() != null) existing.setHabits(pet.getHabits());
        petMapper.updateById(existing);
        return existing;
    }

    @Transactional
    public void deletePet(Long id) {
        getPetById(id);
        petMapper.deleteById(id);
    }
}
