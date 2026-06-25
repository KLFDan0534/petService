package com.pet.customer.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.customer.entity.Rating;
import com.pet.customer.service.RatingService;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ratings")
@Tag(name = "评价管理", description = "宠物寄养服务评价，支持对商家和寄养员评价")
@Slf4j
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    /**
     * 获取评价列表（支持商家和寄养员）
     * @param targetId 目标ID
     * @param targetType 目标类型（商家/寄养员）
     * @return 评价列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @Operation(summary = "获取评价列表", description = "根据目标ID和类型获取评价列表（支持商家和寄养员）")
    public Result<List<Rating>> getRatings(@RequestParam Long targetId,
                                            @RequestParam String targetType) {
        log.info("调用 getRatings()");
        return Result.success(ratingService.getRatingsByTarget(targetId, targetType));
    }

    /**
     * 创建评价（对订单中的商家或寄养员）
     * @param token 当前用户认证信息
     * @param rating 评价信息
     * @return 创建的评价
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "创建评价", description = "对订单中的商家或寄养员进行评价")
    public Result<Rating> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                  @RequestBody Rating rating) {
        log.info("调用 create()");
        return Result.success(ratingService.createRating(token.getUserId(), rating));
    }

    /**
     * 商家或看护人回复评价
     * @param id 评价ID
     * @param body 请求体，包含reply_wsh回复内容
     * @return 更新后的评价
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PutMapping("/{id}/reply")
    @PreAuthorize("hasAnyRole('MERCHANT','KEEPER')")
    @Operation(summary = "回复评价", description = "商家或看护人回复评价")
    public Result<Rating> reply(@PathVariable Long id,
                                 @RequestBody Map<String, String> body) {
        log.info("调用 reply()");
        String reply = body == null ? null : body.get("reply_wsh");
        if ((reply == null || reply.isBlank()) && body != null) {
            reply = body.get("reply");
        }
        return Result.success(ratingService.replyRating(id, reply, null));
    }
}
