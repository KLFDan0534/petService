package com.pet.pet.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.common.annotation.LogOperation;
import com.pet.pet.dto.PetDTO;
import com.pet.pet.dto.PetCreateRequestDTO;
import com.pet.pet.dto.PetUpdateRequestDTO;
import com.pet.security.JwtAuthenticationToken;
import com.pet.pet.service.PetService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import com.pet.common.PageResult;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/pets")
@Tag(name = "【用户端】宠物管理", description = "宠物信息管理（用户/管理员使用）")
@Slf4j
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    /**
     * 获取当前用户的宠物列表
     * @param token 当前用户认证信息
     * @return 宠物列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取我的宠物", description = "获取当前用户的宠物列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<PetDTO>> listMyPets(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 listMyPets()");
        return Result.success(petService.getPetsByOwner(token.getUserId()));
    }

    /**
     * 管理员获取所有宠物
     * @return 全量宠物列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员获取所有宠物", description = "管理员获取全量宠物列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<PetDTO>> listAllPets() {
        log.info("调用 listAllPets()");
        return Result.success(petService.listAll());
    }

    /**
     * 获取商户的宠物列表（用于管理）
     * @param token 当前用户认证信息
     * @param page 页码（默认1）
     * @param size 每页大小（默认10）
     * @return 宠物列表及分页信息
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/merchant")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取商户宠物", description = "获取商户的宠物管理列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<PageResult<PetDTO>> listMerchantPets(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                        @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
                                                        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size) {
        log.info("调用 listMerchantPets()");
        List<PetDTO> list = petService.getPetsByOwner(token.getUserId());
        PageResult<PetDTO> result = new PageResult<>();
        result.setList(list);
        result.setTotal(list.size());
        result.setPage(page);
        result.setSize(size);
        return Result.success(result);
    }

    /**
     * 根据ID获取宠物详情
     * @param token 当前用户认证信息
     * @param id 宠物ID
     * @return 宠物详情
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "根据ID获取宠物", description = "根据ID获取宠物详情")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<PetDTO> getPet(@AuthenticationPrincipal JwtAuthenticationToken token,
                                @Parameter(description = "宠物ID") @PathVariable Long id) {
        log.info("调用 getPet()");
        PetDTO pet = petService.getPetById(id);
        if (pet == null) {
            return Result.error(404, "宠物不存在");
        }
        if (!pet.getOwner_id_wsh().equals(token.getUserId())) {
            return Result.error(403, "无权访问此宠物");
        }
        return Result.success(pet);
    }

    /**
     * 新增宠物
     * @param token 当前用户认证信息
     * @param pet 宠物信息
     * @return 创建的宠物
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @LogOperation(module = "pet", operation = "create", description = "Create pet")
    @Operation(summary = "新增宠物", description = "用户添加新宠物")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<PetDTO> createPet(@AuthenticationPrincipal JwtAuthenticationToken token,
                                    @Valid @RequestBody PetCreateRequestDTO request) {
        log.info("调用 createPet()");
        request.setOwner_id_wsh(token.getUserId());
        return Result.success(petService.createPet(request));
    }

    /**
     * 更新宠物信息
     * @param token 当前用户认证信息
     * @param id 宠物ID
     * @param pet 宠物信息
     * @return 更新后的宠物
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(module = "pet", operation = "update", description = "Update pet")
    @Operation(summary = "更新宠物信息", description = "根据ID更新宠物信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<PetDTO> updatePet(@AuthenticationPrincipal JwtAuthenticationToken token,
                                    @Parameter(description = "宠物ID") @PathVariable Long id,
                                    @Valid @RequestBody PetUpdateRequestDTO request) {
        log.info("调用 updatePet()");
        boolean isAdmin = token.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return Result.success(petService.updatePetAsAdmin(id, request));
        }
        return Result.success(petService.updatePet(token.getUserId(), id, request));
    }

    /**
     * 删除宠物
     * @param token 当前用户认证信息
     * @param id 宠物ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(module = "pet", operation = "delete", description = "Delete pet")
    @Operation(summary = "删除宠物", description = "根据ID删除宠物")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> deletePet(@AuthenticationPrincipal JwtAuthenticationToken token,
                                  @Parameter(description = "宠物ID") @PathVariable Long id) {
        log.info("调用 deletePet()");
        boolean isAdmin = token.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            petService.deletePetAsAdmin(id);
        } else {
            petService.deletePet(token.getUserId(), id);
        }
        return Result.success();
    }
}
