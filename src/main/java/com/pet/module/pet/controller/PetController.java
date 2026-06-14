package com.pet.module.pet.controller;

import com.pet.common.Result;
import com.pet.module.pet.entity.Pet;
import com.pet.module.pet.service.PetService;
import com.pet.security.JwtAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/pets")
@Tag(name = "宠物管理", description = "宠物信息管理，包括宠物的增删改查")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping
    @Operation(summary = "获取我的宠物列表", description = "获取当前用户的所有宠物")
    public Result<List<Pet>> listMyPets(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(petService.getPetsByOwner(token.getUserId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取宠物详情", description = "根据ID获取宠物详细信息")
    public Result<Pet> getPet(@PathVariable Long id) {
        return Result.success(petService.getPetById(id));
    }

    @PostMapping
    @Operation(summary = "新增宠物", description = "为当前用户新增一条宠物信息")
    public Result<Pet> createPet(@AuthenticationPrincipal JwtAuthenticationToken token,
                                  @RequestBody Pet pet) {
        pet.setOwnerId(token.getUserId());
        return Result.success(petService.createPet(pet));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新宠物信息", description = "根据ID更新宠物信息")
    public Result<Pet> updatePet(@PathVariable Long id, @RequestBody Pet pet) {
        pet.setId(id);
        return Result.success(petService.updatePet(pet));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除宠物", description = "根据ID删除宠物信息")
    public Result<Void> deletePet(@PathVariable Long id) {
        petService.deletePet(id);
        return Result.success();
    }
}
