package com.pet.boarding.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.common.annotation.LogOperation;
import com.pet.boarding.entity.Merchant;
import com.pet.security.JwtAuthenticationToken;
import com.pet.boarding.service.MerchantService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 商家管理控制器
 * 提供商家的列表、详情、注册、审核及附近搜索功能
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@RestController
@RequestMapping("/api/merchants")
@Tag(name = "商家管理", description = "商家注册、审核和信息管理")
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
    public Result<List<Merchant>> listAll() {
        log.info("调用 listAll()");
        return Result.success(merchantService.listAll());
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
    public Result<Merchant> getById(@PathVariable Long id) {
        log.info("调用 getById()");
        return Result.success(merchantService.getById(id));
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
    public Result<Merchant> getMyMerchant(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 getMyMerchant()");
        Merchant merchant = merchantService.findByUserId(token.getUserId());
        return Result.success(merchant);
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
    public Result<List<Merchant>> searchNearby(@RequestParam double lat,
                                                @RequestParam double lng,
                                                @RequestParam(defaultValue = "5") double radius) {
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
    public Result<Merchant> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                    @RequestBody Merchant merchant) {
        log.info("调用 create()");
        merchant.setUser_id_wsh(token.getUserId());
        return Result.success(merchantService.create(merchant));
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
    public Result<Merchant> update(@AuthenticationPrincipal JwtAuthenticationToken token,
                                    @PathVariable Long id,
                                    @RequestBody Merchant merchant) {
        log.info("调用 update()");
        Merchant existing = merchantService.getById(id);
        if (existing == null) {
            return Result.error(404, "商家不存在");
        }
        if (!existing.getUser_id_wsh().equals(token.getUserId())) {
            return Result.error(403, "无权修改此商家");
        }
        merchant.setId_wsh(id);
        return Result.success(merchantService.update(merchant));
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
    public Result<Void> approve(@PathVariable Long id) {
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
    public Result<Void> reject(@PathVariable Long id) {
        log.info("调用 reject()");
        merchantService.reject(id);
        return Result.success();
    }
}
