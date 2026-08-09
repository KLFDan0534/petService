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
 * 【商家管理控制器】
 *
 * 业务作用：
 * 提供商家的全生命周期 API 接口，包括列表查询、详情查看、入驻申请、
 * 信息更新、营业模式切换、管理员审核（通过/驳回）以及 LBS 附近商家搜索。
 *
 * 权限要求：
 * - 公共接口（GET）：任意用户可访问（含未登录用户）
 * - 用户接口（POST/PUT）：需登录（isAuthenticated）
 * - 审核接口：仅 ADMIN 角色
 * - 营业模式切换：MERCHANT 或 ADMIN 角色
 *
 * API 路由前缀：/api/merchants
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
     * 【获取所有商家列表】
     *
     * API: GET /api/merchants
     *
     * 权限：公开（无需登录）
     *
     * 返回：全量商家 DTO 列表（含资质信息和实时店铺营业状态），
     * 按创建时间倒序排列。
     *
     * 调用链：
     * 前端页面加载
     *   ↓
     * MerchantController.listAll()
     *   ↓
     * MerchantService.listAll() → toDTO()
     */
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
     * 【获取商家详情】
     *
     * API: GET /api/merchants/{id}
     *
     * 权限：公开（无需登录）
     *
     * @param id 商家 ID
     * @return 商家 DTO（含资质信息、实时店铺营业状态）
     */
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
     * 【获取我的商家信息】
     *
     * API: GET /api/merchants/my
     *
     * 权限：需登录
     *
     * 场景：商家登录后在商家后台查看自己的店铺信息。
     * 如果当前用户不是商家，返回 null。
     *
     * @param token 当前用户认证信息
     * @return 当前用户的商家信息，非商家用户返回 null
     */
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
     * 【搜索附近商家】
     *
     * API: GET /api/merchants/nearby?lat=...&lng=...&radius=5
     *
     * 权限：公开（无需登录）
     *
     * 场景：用户在首页或地图模式搜索附近宠物寄养商家。
     *
     * 数据处理：
     * 使用 Haversine 公式在数据库层过滤附近商家，
     * 然后 Java 层再次计算精确距离（保留两位小数）。
     *
     * 业务规则：
     * - 仅返回 status = MERCHANT_APPROVED 的商家
     * - radius 默认 5 公里
     *
     * @param lat    用户纬度
     * @param lng    用户经度
     * @param radius 搜索半径（公里，默认 5）
     * @return 附近商家列表（含距离信息）
     */
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
     * 【商家入驻申请】
     *
     * API: POST /api/merchants
     *
     * 权限：需登录
     *
     * 场景：用户填写商家入驻信息并提交申请。
     *
     * 状态影响：
     * 创建后商家状态为 MERCHANT_PENDING（待审核），
     * 需管理员审核通过后才能正式营业。
     *
     * @param token 当前用户认证信息
     * @param dto   商家入驻申请信息
     * @return 创建后的商家信息（含资质审核记录）
     */
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
     * 【更新商家信息】
     *
     * API: PUT /api/merchants/{id}
     *
     * 权限：需登录（仅商家本人或 ADMIN 可操作）
     *
     * 业务规则：
     * - 仅更新非 null 字段
     * - 不修改商家审核状态和营业模式
     *
     * @param token 当前用户认证信息
     * @param id    商家 ID
     * @param dto   商家信息更新内容
     * @return 更新后的商家信息
     */
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
     * 【更新商家营业模式】
     *
     * API: PATCH /api/merchants/{id}/store-mode
     *
     * 权限：MERCHANT 或 ADMIN 角色
     *
     * 支持三种模式：
     * - MODE_AUTO（自动）：根据营业时间配置自动开关店
     * - MODE_MANUAL_OPEN（手动开店）：强制开门
     * - MODE_MANUAL_CLOSED（手动关店）：强制关闭
     *
     * 触发联动：
     * 切换模式后自动触发店铺状态刷新（resolveStoreStatus），
     * 并同步通知旗下看护者上线/下线。
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
     * 【审核通过商家申请】
     *
     * API: POST /api/merchants/{id}/approve
     *
     * 权限：仅 ADMIN 角色
     *
     * 状态变化：MERCHANT_PENDING → MERCHANT_APPROVED
     *
     * 触发联动：
     * - 设置营业模式为 MODE_AUTO
     * - 触发店铺状态刷新（按当前时间判断是否开门）
     */
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
     * 【驳回商家申请】
     *
     * API: POST /api/merchants/{id}/reject
     *
     * 权限：仅 ADMIN 角色
     *
     * 状态变化：MERCHANT_PENDING → MERCHANT_REJECTED
     *
     * 触发联动：
     * - 设置营业模式为 MODE_MANUAL_CLOSED
     * - 强制店铺关闭
     * - 同步通知旗下所有看护者下线
     */
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
