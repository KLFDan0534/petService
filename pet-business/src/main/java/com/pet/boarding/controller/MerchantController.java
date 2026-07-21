package com.pet.boarding.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.common.annotation.LogOperation;
import com.pet.boarding.dto.MerchantCreateRequestDTO;
import com.pet.boarding.dto.MerchantDTO;
import com.pet.boarding.dto.MerchantStoreModeRequestDTO;
import com.pet.boarding.dto.MerchantUpdateRequestDTO;
import com.pet.security.JwtAuthenticationToken;
import com.pet.boarding.service.MerchantService;
import jakarta.validation.Valid;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 商家管理控制器
 * 提供商家的列表、详情、注册、审核及附近搜索功能
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@RestController
@RequestMapping("/api/merchants")
@Tag(name = "【用户端】商家管理", description = "商家注册、审核和信息管理（用户/商家/管理员使用）")
@Slf4j
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    /**
     * 获取所有商家列表
     * @return 商家列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @Operation(summary = "获取所有商家", description = "获取所有商家列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<MerchantDTO>> listAll() {
        log.info("调用 listAll()");
        return Result.success(merchantService.listAll().stream().map(merchantService::toDTO).collect(Collectors.toList()));
    }

    /**
     * 根据ID获取商家详情
     * @param id 商家ID
     * @return 商家详情
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/{id}")
    @Operation(summary = "获取商家详情", description = "根据ID获取商家详情")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<MerchantDTO> getById(@Parameter(description = "商家ID") @PathVariable Long id) {
        log.info("调用 getById()");
        return Result.success(merchantService.toDTO(merchantService.getById(id)));
    }

    /**
     * 获取当前用户的商家信息
     * @param token 当前用户认证信息
     * @return 商家信息
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取我的商家信息", description = "获取当前用户的商家信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<MerchantDTO> getMyMerchant(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 getMyMerchant()");
        return Result.success(merchantService.toDTO(merchantService.findByUserId(token.getUserId())));
    }

    /**
     * 搜索附近的商家
     * @param lat 纬度
     * @param lng 经度
     * @param radius 搜索半径（公里，默认5）
     * @return 附近商家列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/nearby")
    @Operation(summary = "搜索附近商家", description = "根据经纬度和半径搜索附近商家")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<MerchantDTO>> searchNearby(@Parameter(description = "纬度") @RequestParam double lat,
                                                 @Parameter(description = "经度") @RequestParam double lng,
                                                 @Parameter(description = "搜索半径（公里，默认5）") @RequestParam(defaultValue = "5") double radius) {
        log.info("调用 searchNearby()");
        return Result.success(merchantService.searchNearby(lat, lng, radius));
    }

    /**
     * 当前用户注册为商家
     * @param token 当前用户认证信息
     * @param merchant 商家信息
     * @return 创建的商家
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @LogOperation(module = "merchant", operation = "create", description = "新增商家")
    @Operation(summary = "新增商家", description = "当前用户注册为商家")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<MerchantDTO> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                    @RequestBody MerchantCreateRequestDTO dto) {
        log.info("调用 create()");
        return Result.success(merchantService.toDTO(merchantService.create(dto, token.getUserId())));
    }

    /**
     * 更新商家信息
     * @param token 当前用户认证信息
     * @param id 商家ID
     * @param merchant 商家信息
     * @return 更新后的商家
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(module = "merchant", operation = "update", description = "更新商家信息")
    @Operation(summary = "更新商家信息", description = "根据ID更新商家信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<MerchantDTO> update(@AuthenticationPrincipal JwtAuthenticationToken token,
                                    @Parameter(description = "商家ID") @PathVariable Long id,
                                    @RequestBody MerchantUpdateRequestDTO dto) {
        log.info("调用 update()");
        if (!isAdmin(token) && !merchantService.isOwner(id, token.getUserId())) {
            return Result.error(403, "无权修改此商家");
        }
        return Result.success(merchantService.toDTO(merchantService.update(id, dto)));
    }

    /**
     * 更新商家营业模式（自动/开店/关店）
     */
    @PatchMapping("/{id}/store-mode")
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    @LogOperation(module = "merchant", operation = "update-store-mode", description = "Update merchant store mode")
    @Operation(summary = "更新商家营业模式", description = "自动、开店、关店三种模式切换")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<MerchantDTO> updateStoreMode(@AuthenticationPrincipal JwtAuthenticationToken token,
                                               @Parameter(description = "商家ID") @PathVariable Long id,
                                               @RequestBody MerchantStoreModeRequestDTO dto) {
        log.info("调用 updateStoreMode()");
        if (!isAdmin(token) && !merchantService.isOwner(id, token.getUserId())) {
            return Result.error(403, "无权修改此商家");
        }
        return Result.success(merchantService.toDTO(merchantService.updateStoreMode(id, dto.getStore_mode_wsh())));
    }

    /**
     * 管理员审核通过商家申请
     * @param id 商家ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @LogOperation(module = "merchant", operation = "approve", description = "审核通过商家")
    @Operation(summary = "审核通过商家", description = "管理员审核通过商家申请")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> approve(@Parameter(description = "商家ID") @PathVariable Long id) {
        log.info("调用 approve()");
        merchantService.approve(id);
        return Result.success();
    }

    /**
     * 管理员驳回商家申请
     * @param id 商家ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @LogOperation(module = "merchant", operation = "reject", description = "驳回商家申请")
    @Operation(summary = "驳回商家", description = "管理员驳回商家申请")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> reject(@Parameter(description = "商家ID") @PathVariable Long id) {
        log.info("调用 reject()");
        merchantService.reject(id);
        return Result.success();
    }

    private boolean isAdmin(JwtAuthenticationToken token) {
        if (token == null) return false;
        for (GrantedAuthority authority : token.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
