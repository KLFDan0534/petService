package com.pet.customer.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.customer.dto.RatingDTO;
import com.pet.customer.service.RatingService;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.pet.customer.dto.RatingCreateRequestDTO;
import com.pet.customer.dto.RatingReplyRequestDTO;
import java.util.List;

@RestController
@RequestMapping("/api/ratings")
@Tag(name = "【用户端】评价管理", description = "宠物寄养服务评价（用户评价/商家回复）")
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<RatingDTO>> getRatings(@Parameter(description = "目标ID") @RequestParam Long targetId,
                                              @Parameter(description = "目标类型（商家/寄养员）") @RequestParam String targetType) {
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<RatingDTO> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                    @RequestBody RatingCreateRequestDTO request) {
        log.info("调用 create()");
        return Result.success(ratingService.createRating(token.getUserId(), request));
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<RatingDTO> reply(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "评价ID") @PathVariable Long id,
            @Valid @RequestBody RatingReplyRequestDTO request) {
        log.info("调用 reply()");
        return Result.success(ratingService.replyRating(id, request.getReply_wsh(), token.getUserId()));
    }
}
