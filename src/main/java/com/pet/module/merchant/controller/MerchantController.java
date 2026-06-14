package com.pet.module.merchant.controller;

import com.pet.common.Result;
import com.pet.module.merchant.entity.Merchant;
import com.pet.module.merchant.service.MerchantService;
import com.pet.security.JwtAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/merchants")
@Tag(name = "商家管理", description = "商家注册、审核、信息管理等功能")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @GetMapping
    @Operation(summary = "获取所有商家列表", description = "获取平台所有商家信息")
    public Result<List<Merchant>> listAll() {
        return Result.success(merchantService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取商家详情", description = "根据ID获取商家详细信息")
    public Result<Merchant> getById(@PathVariable Long id) {
        return Result.success(merchantService.getById(id));
    }

    @GetMapping("/nearby")
    @Operation(summary = "搜索附近商家", description = "根据经纬度和半径搜索附近的商家")
    public Result<List<Merchant>> searchNearby(@RequestParam double lat,
                                                @RequestParam double lng,
                                                @RequestParam(defaultValue = "5") double radius) {
        return Result.success(merchantService.searchNearby(lat, lng, radius));
    }

    @PostMapping
    @Operation(summary = "新增商家", description = "当前用户注册成为商家")
    public Result<Merchant> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                    @RequestBody Merchant merchant) {
        merchant.setUserId(token.getUserId());
        return Result.success(merchantService.create(merchant));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新商家信息", description = "根据ID更新商家信息")
    public Result<Merchant> update(@PathVariable Long id, @RequestBody Merchant merchant) {
        merchant.setId(id);
        return Result.success(merchantService.update(merchant));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "审核通过商家", description = "管理员审核通过商家入驻申请")
    public Result<Void> approve(@PathVariable Long id) {
        merchantService.approve(id);
        return Result.success();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "驳回商家申请", description = "管理员驳回商家的入驻申请")
    public Result<Void> reject(@PathVariable Long id) {
        merchantService.reject(id);
        return Result.success();
    }
}
