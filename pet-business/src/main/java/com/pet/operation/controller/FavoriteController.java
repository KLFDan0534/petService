package com.pet.operation.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.operation.entity.Favorite;
import com.pet.operation.service.FavoriteService;
import com.pet.security.JwtAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/favorites")
@Tag(name = "收藏管理", description = "用户收藏管理，包括添加/取消/检查")
@Slf4j
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    /**
     * 获取当前用户的收藏列表
     * @param token 当前用户认证信息
     * @param targetType 收藏目标类型（可选）
     * @return 收藏列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取收藏列表", description = "获取当前用户的收藏列表")
    public Result<List<Favorite>> list(@AuthenticationPrincipal JwtAuthenticationToken token,
                                       @RequestParam(required = false) String targetType) {
        log.info("调用 list()");
        return Result.success(favoriteService.listByUser(token.getUserId(), targetType));
    }

    /**
     * 切换收藏状态
     * @param token 当前用户认证信息
     * @param body 请求体，包含target_id_wsh和target_type_wsh
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/toggle")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "切换收藏", description = "切换目标的收藏状态")
    public Result<Void> toggle(@AuthenticationPrincipal JwtAuthenticationToken token,
                               @RequestBody Map<String, Object> body) {
        log.info("调用 toggle()");
        Long targetId = Long.valueOf(body.get("target_id_wsh").toString());
        String targetType = body.get("target_type_wsh").toString();
        favoriteService.toggle(token.getUserId(), targetId, targetType);
        return Result.success();
    }

    /**
     * 检查用户是否已收藏目标
     * @param token 当前用户认证信息
     * @param targetId 目标ID
     * @param targetType 目标类型
     * @return 是否已收藏
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/check")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "检查是否已收藏", description = "检查用户是否已收藏目标实体")
    public Result<Boolean> check(@AuthenticationPrincipal JwtAuthenticationToken token,
                                 @RequestParam Long targetId,
                                 @RequestParam String targetType) {
        log.info("调用 check()");
        return Result.success(favoriteService.isFavorited(token.getUserId(), targetId, targetType));
    }
}
