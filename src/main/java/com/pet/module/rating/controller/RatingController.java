package com.pet.module.rating.controller;

import com.pet.common.Result;
import com.pet.module.rating.entity.Rating;
import com.pet.module.rating.service.RatingService;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ratings")
@Tag(name = "评价管理", description = "宠物寄养服务评价，支持对商家和寄养员评分")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @GetMapping
    @Operation(summary = "获取评价列表", description = "根据目标ID和类型获取评价列表（支持商家和寄养员）")
    public Result<List<Rating>> getRatings(@RequestParam Long targetId,
                                            @RequestParam String targetType) {
        return Result.success(ratingService.getRatingsByTarget(targetId, targetType));
    }

    @PostMapping
    @Operation(summary = "创建评价", description = "用户对订单中的商家或寄养员进行评价")
    public Result<Rating> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                  @RequestBody Rating rating) {
        return Result.success(ratingService.createRating(token.getUserId(), rating));
    }
}
