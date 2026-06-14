package com.pet.module.keeper.controller;

import com.pet.common.Result;
import com.pet.module.keeper.dto.KeeperVO;
import com.pet.module.keeper.entity.Keeper;
import com.pet.module.keeper.service.KeeperService;
import com.pet.security.JwtAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/keepers")
@Tag(name = "看护人管理", description = "看护人信息管理，包括看护人列表、搜索等功能")
public class KeeperController {

    private final KeeperService keeperService;

    public KeeperController(KeeperService keeperService) {
        this.keeperService = keeperService;
    }

    @GetMapping
    @Operation(summary = "获取所有看护人列表", description = "获取平台所有看护人信息")
    public Result<List<Keeper>> listAll() {
        return Result.success(keeperService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取看护人详情", description = "根据ID获取看护人详细信息")
    public Result<Keeper> getById(@PathVariable Long id) {
        return Result.success(keeperService.getById(id));
    }

    @GetMapping("/nearby")
    @Operation(summary = "搜索附近看护人", description = "根据经纬度和半径搜索附近的看护人")
    public Result<List<KeeperVO>> searchNearby(@RequestParam double lat,
                                                @RequestParam double lng,
                                                @RequestParam(defaultValue = "5") double radius) {
        return Result.success(keeperService.searchNearby(lat, lng, radius));
    }

    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "获取商家下的看护人", description = "根据商家ID获取该商家下的所有看护人")
    public Result<List<Keeper>> findByMerchant(@PathVariable Long merchantId) {
        return Result.success(keeperService.findByMerchantId(merchantId));
    }

    @PostMapping
    @Operation(summary = "新增看护人", description = "当前用户注册成为看护人")
    public Result<Keeper> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                  @RequestBody Keeper keeper) {
        keeper.setUserId(token.getUserId());
        return Result.success(keeperService.create(keeper));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新看护人信息", description = "根据ID更新看护人信息")
    public Result<Keeper> update(@PathVariable Long id, @RequestBody Keeper keeper) {
        keeper.setId(id);
        return Result.success(keeperService.update(keeper));
    }
}
