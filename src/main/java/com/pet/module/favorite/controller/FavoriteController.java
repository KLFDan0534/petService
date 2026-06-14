package com.pet.module.favorite.controller;

import com.pet.common.Result;
import com.pet.module.favorite.entity.Favorite;
import com.pet.module.favorite.service.FavoriteService;
import com.pet.security.JwtAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public Result<List<Favorite>> list(@AuthenticationPrincipal JwtAuthenticationToken token,
                                        @RequestParam(required = false) String targetType) {
        return Result.success(favoriteService.listByUser(token.getUserId(), targetType));
    }

    @PostMapping("/toggle")
    public Result<Void> toggle(@AuthenticationPrincipal JwtAuthenticationToken token,
                                @RequestBody Map<String, Object> body) {
        Long targetId = Long.valueOf(body.get("targetId").toString());
        String targetType = body.get("targetType").toString();
        favoriteService.toggle(token.getUserId(), targetId, targetType);
        return Result.success();
    }

    @GetMapping("/check")
    public Result<Boolean> check(@AuthenticationPrincipal JwtAuthenticationToken token,
                                  @RequestParam Long targetId,
                                  @RequestParam String targetType) {
        return Result.success(favoriteService.isFavorited(token.getUserId(), targetId, targetType));
    }
}
