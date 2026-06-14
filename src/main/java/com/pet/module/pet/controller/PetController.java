package com.pet.module.pet.controller;

import com.pet.common.Result;
import com.pet.module.pet.entity.Pet;
import com.pet.module.pet.service.PetService;
import com.pet.security.JwtAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping
    public Result<List<Pet>> listMyPets(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(petService.getPetsByOwner(token.getUserId()));
    }

    @GetMapping("/{id}")
    public Result<Pet> getPet(@PathVariable Long id) {
        return Result.success(petService.getPetById(id));
    }

    @PostMapping
    public Result<Pet> createPet(@AuthenticationPrincipal JwtAuthenticationToken token,
                                  @RequestBody Pet pet) {
        pet.setOwnerId(token.getUserId());
        return Result.success(petService.createPet(pet));
    }

    @PutMapping("/{id}")
    public Result<Pet> updatePet(@PathVariable Long id, @RequestBody Pet pet) {
        pet.setId(id);
        return Result.success(petService.updatePet(pet));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deletePet(@PathVariable Long id) {
        petService.deletePet(id);
        return Result.success();
    }
}
