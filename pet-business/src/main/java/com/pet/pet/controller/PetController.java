package com.pet.pet.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.common.annotation.LogOperation;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.service.MerchantService;
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
    private final MerchantService merchantService;

    /**
     * 【构造方法】注入宠物服务
     *
     * 业务作用：由 Spring 注入 PetService，供宠物查询、新增、更新与删除接口使用。
     * 异常情况：依赖缺失时由 Spring 抛出 Bean 创建异常。
     * 注意事项：依赖为 final 字段。
     */
    public PetController(PetService petService, MerchantService merchantService) {
        this.petService = petService;
        this.merchantService = merchantService;
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
     * 管理员分页获取宠物
     *
     * API: GET /api/pets/admin/page
     * 为什么新增：原 /admin/all 全量返回，宠物表增长后会把整表拉进内存。
     * 只改查询方式，字段与 /admin/all 完全一致。
     */
    @GetMapping("/admin/page")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员分页获取宠物", description = "按创建时间倒序，支持宠物名搜索，size 上限 100")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<PageResult<PetDTO>> pageAllPets(
            @Parameter(description = "页码，从 1 开始") @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "每页条数 20/50/100") @RequestParam(required = false, defaultValue = "20") Integer size,
            @Parameter(description = "宠物名关键词") @RequestParam(required = false) String keyword) {
        log.info("调用 pageAllPets(), page={}, size={}", page, size);
        return Result.success(petService.pageAll(page, size, keyword));
    }

    /**
     * 获取本店服务过的宠物列表（商家后台只读视图）
     * <p>
     * 宠物归主人所有，商家没有宠物池，因此这里返回的是「在本商家下过单的宠物」去重档案，
     * 而不是当前登录账号自己作为宠物主人添加的宠物。
     *
     * @param token 当前用户认证信息
     * @param page 页码（默认1）
     * @param size 每页大小（默认10）
     * @return 宠物列表及分页信息
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/merchant")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取本店服务过的宠物", description = "商家后台宠物管理：按订单聚合本店服务过的宠物档案")
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
        Merchant merchant = merchantService.findByUserId(token.getUserId());
        // 账号未绑定商家资料时给空列表，由前端提示，不抛异常
        List<PetDTO> list = merchant == null
                ? List.of()
                : petService.getPetsServedByMerchant(merchant.getId_wsh());
        // 先全量取出再在内存里组装分页
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
        // 只能查看本人名下的宠物
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
        // 管理员走跳过归属校验的更新分支
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
        // 管理员可删除任意宠物
        if (isAdmin) {
            petService.deletePetAsAdmin(id);
        } else {
            petService.deletePet(token.getUserId(), id);
        }
        return Result.success();
    }
}
