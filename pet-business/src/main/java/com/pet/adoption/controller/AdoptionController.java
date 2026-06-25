package com.pet.adoption.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.service.MerchantService;
import com.pet.adoption.entity.AdoptionApplication;
import com.pet.adoption.entity.AdoptionPet;
import com.pet.adoption.service.AdoptionApplicationService;
import com.pet.adoption.service.AdoptionPetService;
import com.pet.common.BusinessException;
import com.pet.common.Result;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 领养管理控制器
 * 提供可领养宠物管理及领养申请审核功能
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@RestController
@RequestMapping("/api/adoptions")
@Tag(name = "领养管理", description = "宠物领养管理")
@Slf4j
public class AdoptionController {

    private final AdoptionPetService adoptionPetService;
    private final AdoptionApplicationService adoptionApplicationService;
    private final MerchantService merchantService;

    public AdoptionController(AdoptionPetService adoptionPetService,
                              AdoptionApplicationService adoptionApplicationService,
                              MerchantService merchantService) {
        this.adoptionPetService = adoptionPetService;
        this.adoptionApplicationService = adoptionApplicationService;
        this.merchantService = merchantService;
    }

    /**
     * 获取可领养宠物列表
     * @return 可领养宠物列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/pets")
    @Operation(summary = "获取可领养宠物列表")
    public Result<List<AdoptionPet>> listAvailablePets() {
        log.info("调用 listAvailablePets()");
        return Result.success(adoptionPetService.listAvailable());
    }

    /**
     * 获取领养宠物详情
     * @param id 宠物ID
     * @return 领养宠物详情
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/pets/{id}")
    @Operation(summary = "获取宠物详情")
    public Result<AdoptionPet> getPet(@PathVariable Long id) {
        log.info("调用 getPet()");
        return Result.success(adoptionPetService.getById(id));
    }

    /**
     * 创建领养宠物信息
     * @param token 当前用户认证信息
     * @param pet 领养宠物信息
     * @return 创建的领养宠物
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/pets")
    @PreAuthorize("hasRole('MERCHANT')")
    @Operation(summary = "创建领养宠物")
    public Result<AdoptionPet> createPet(@AuthenticationPrincipal JwtAuthenticationToken token,
                                         @RequestBody @Valid AdoptionPet pet) {
        log.info("调用 createPet()");
        return Result.success(adoptionPetService.create(getCurrentMerchantId(token), pet));
    }

    /**
     * 更新领养宠物信息
     * @param token 当前用户认证信息
     * @param id 领养宠物ID
     * @param pet 领养宠物信息
     * @return 更新后的领养宠物
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PutMapping("/pets/{id}")
    @PreAuthorize("hasRole('MERCHANT')")
    @Operation(summary = "更新领养宠物")
    public Result<AdoptionPet> updatePet(@AuthenticationPrincipal JwtAuthenticationToken token,
                                         @PathVariable Long id,
                                         @RequestBody @Valid AdoptionPet pet) {
        log.info("调用 updatePet()");
        pet.setId_wsh(id);
        return Result.success(adoptionPetService.update(getCurrentMerchantId(token), pet));
    }

    /**
     * 删除领养宠物信息
     * @param token 当前用户认证信息
     * @param id 领养宠物ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @DeleteMapping("/pets/{id}")
    @PreAuthorize("hasRole('MERCHANT')")
    @Operation(summary = "删除领养宠物")
    public Result<Void> deletePet(@AuthenticationPrincipal JwtAuthenticationToken token,
                                  @PathVariable Long id) {
        log.info("调用 deletePet()");
        adoptionPetService.delete(getCurrentMerchantId(token), id);
        return Result.success();
    }

    /**
     * 创建领养申请
     * @param token 当前用户认证信息
     * @param app 领养申请信息
     * @return 创建的领养申请
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "创建领养申请")
    public Result<AdoptionApplication> createApplication(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                         @RequestBody @Valid AdoptionApplication app) {
        log.info("调用 createApplication()");
        return Result.success(adoptionApplicationService.create(token.getUserId(), app));
    }

    /**
     * 获取当前用户的领养申请列表
     * @param token 当前用户认证信息
     * @return 领养申请列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取我的申请列表")
    public Result<List<AdoptionApplication>> listMyApplications(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(adoptionApplicationService.listByUser(token.getUserId()));
    }

    /**
     * 获取领养申请详情
     * @param id 领养申请ID
     * @return 领养申请详情
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取申请详情")
    public Result<AdoptionApplication> getApplication(@PathVariable Long id) {
        return Result.success(adoptionApplicationService.getById(id));
    }

    /**
     * 管理员获取所有领养申请
     * @return 领养申请列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取所有申请（管理员）")
    public Result<List<AdoptionApplication>> listAllApplications() {
        return Result.success(adoptionApplicationService.listAll());
    }

    /**
     * 商家获取其收到的领养申请列表
     * @param token 当前用户认证信息
     * @return 领养申请列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/merchant")
    @PreAuthorize("hasRole('MERCHANT')")
    @Operation(summary = "获取商家收到的申请列表")
    public Result<List<AdoptionApplication>> listMerchantApplications(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(adoptionApplicationService.listByMerchant(getCurrentMerchantId(token)));
    }

    /**
     * 商家审核领养申请
     * @param token 当前用户认证信息
     * @param id 领养申请ID
     * @param body 请求体，包含approved_wsh和remark_wsh
     * @return 更新后的领养申请
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}/merchant-review")
    @PreAuthorize("hasRole('MERCHANT')")
    @Operation(summary = "商家审核申请")
    public Result<AdoptionApplication> merchantReview(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                      @PathVariable Long id,
                                                      @RequestBody Map<String, Object> body) {
        log.info("调用 merchantReview()");
        boolean approved = readBoolean(body, "approved_wsh", "approved");
        String remark = readString(body, "remark_wsh", "remark");
        return Result.success(adoptionApplicationService.merchantReview(id, getCurrentMerchantId(token), approved, remark));
    }

    /**
     * 管理员审核领养申请
     * @param id 领养申请ID
     * @param body 请求体，包含approved_wsh和remark_wsh
     * @return 更新后的领养申请
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}/admin-review")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员审核申请")
    public Result<AdoptionApplication> adminReview(@PathVariable Long id,
                                                   @RequestBody Map<String, Object> body) {
        log.info("调用 adminReview()");
        boolean approved = readBoolean(body, "approved_wsh", "approved");
        String remark = readString(body, "remark_wsh", "remark");
        return Result.success(adoptionApplicationService.adminReview(id, approved, remark));
    }

    private Long getCurrentMerchantId(JwtAuthenticationToken token) {
        Merchant merchant = merchantService.findByUserId(token.getUserId());
        if (merchant == null) {
            throw new BusinessException(404, "当前用户尚未注册商户");
        }
        return merchant.getId_wsh();
    }

    private boolean readBoolean(Map<String, Object> body, String primaryKey, String fallbackKey) {
        Object value = body.getOrDefault(primaryKey, body.get(fallbackKey));
        return value instanceof Boolean ? (Boolean) value : Boolean.parseBoolean(String.valueOf(value));
    }

    private String readString(Map<String, Object> body, String primaryKey, String fallbackKey) {
        Object value = body.getOrDefault(primaryKey, body.get(fallbackKey));
        return value == null ? "" : String.valueOf(value);
    }
}
