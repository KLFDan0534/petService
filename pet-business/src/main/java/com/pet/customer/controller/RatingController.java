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
     * 获取评价列表
     *
     * <p>API: GET /api/ratings</p>
     * <p>请求来源：前端商家详情页、看护者详情页，用户浏览评价</p>
     * <p>权限要求：无需登录，公开接口</p>
     * <p>输入参数：
     * <ul>
     *   <li>@query targetId - 目标ID（商家或看护者）</li>
     *   <li>@query targetType - 目标类型（"merchant"或"keeper"）</li>
     * </ul>
     * </p>
     * <p>返回数据：List&lt;RatingDTO&gt; - 评价列表，包含评分、内容、回复等</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 参数错误</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
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
     * 获取当前用户对指定订单的评价（用于评价面板已评价状态）
     * <p>API: GET /api/ratings/my?orderId=xxx</p>
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取我的订单评价", description = "返回当前用户对指定订单已提交的评价维度")
    public Result<List<RatingDTO>> getMyRatings(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "订单ID") @RequestParam Long orderId) {
        log.info("调用 getMyRatings()");
        return Result.success(ratingService.getMyRatingsByOrder(token.getUserId(), orderId));
    }

    /**
     * 创建评价
     *
     * <p>API: POST /api/ratings</p>
     * <p>请求来源：前端订单完成页，用户对订单中的商家或看护者进行评价和打分</p>
     * <p>权限要求：已登录用户（@PreAuthorize("isAuthenticated()")）</p>
     * <p>输入参数：@body RatingCreateRequestDTO - 包含订单ID、目标类型、目标ID、评分分数、评价内容等</p>
     * <p>返回数据：RatingDTO - 创建的评价记录</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 参数校验失败（如评分超出范围、重复评价）</li>
     *   <li>401 - 未登录</li>
     *   <li>403 - 无权限（非订单参与者）</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
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
     * 回复评价
     *
     * <p>API: PUT /api/ratings/{id}/reply</p>
     * <p>请求来源：商家/看护者端评价管理页，商家或看护者回复用户的评价</p>
     * <p>权限要求：MERCHANT或KEEPER角色（@PreAuthorize("hasAnyRole('MERCHANT','KEEPER')")）</p>
     * <p>输入参数：
     * <ul>
     *   <li>@path id - 评价ID</li>
     *   <li>@body RatingReplyRequestDTO - 包含回复内容reply_wsh</li>
     * </ul>
     * </p>
     * <p>返回数据：RatingDTO - 更新后的评价记录（含回复内容）</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 评价不存在或已被回复过</li>
     *   <li>401 - 未登录</li>
     *   <li>403 - 无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
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
