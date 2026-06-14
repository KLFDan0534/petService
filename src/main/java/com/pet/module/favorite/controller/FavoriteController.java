package com.pet.module.favorite.controller;

import com.pet.common.Result;
import com.pet.module.favorite.entity.Favorite;
import com.pet.module.favorite.service.FavoriteService;
import com.pet.security.JwtAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/favorites")
@Tag(name = "收藏管理", description = "用户收藏管理，包括收藏列表、添加和取消收藏")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    @Operation(summary = "获取收藏列表", description = "获取当前用户的收藏列表，可按类型筛选")
    public Result<List<Favorite>> list(@AuthenticationPrincipal JwtAuthenticationToken token,
                                        @RequestParam(required = false) String targetType) {
        return Result.success(favoriteService.listByUser(token.getUserId(), targetType));
    }

    @PostMapping("/toggle")
    @Operation(summary = "切换收藏状态", description = "添加或取消收藏（如果已收藏则取消，否则添加）")
    public Result<Void> toggle(@AuthenticationPrincipal JwtAuthenticationToken token,
                                @RequestBody Map<String, Object> body) {
        Long targetId = Long.valueOf(body.get("targetId").toString());
        String targetType = body.get("targetType").toString();
        favoriteService.toggle(token.getUserId(), targetId, targetType);
        return Result.success();
    }

    @GetMapping("/check")
    @Operation(summary = "检查是否已收藏", description = "检查当前用户是否已收藏指定目标")
    public Result<Boolean> check(@AuthenticationPrincipal JwtAuthenticationToken token,
                                  @RequestParam Long targetId,
                                  @RequestParam String targetType) {
        return Result.success(favoriteService.isFavorited(token.getUserId(), targetId, targetType));
    }
}
