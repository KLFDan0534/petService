package com.pet.order.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.order.dto.TipDTO;
import com.pet.order.dto.TipCreateRequestDTO;
import com.pet.order.service.TipService;
import com.pet.security.JwtAuthenticationToken;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 小费控制器
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@RestController
@RequestMapping("/api/tips")
@Slf4j
@Tag(name = "【用户端】小费管理", description = "订单小费管理（用户赠送/查看）")
public class TipController {

    private final TipService tipService;

    public TipController(TipService tipService) {
        this.tipService = tipService;
    }

    /**
     * 给小费
     *
     * <p>API: POST /api/tips</p>
     * <p>请求来源：前端订单完成页或评价页，用户对看护者的服务满意后可额外给予小费</p>
     * <p>权限要求：已登录用户（@PreAuthorize("isAuthenticated()")）</p>
     * <p>输入参数：@body TipCreateRequestDTO - 包含订单ID、小费金额、收款看护者/商家ID等</p>
     * <p>返回数据：无（Result.success()）</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 小费金额不合法或订单状态不支持打赏</li>
     *   <li>401 - 未登录</li>
     *   <li>403 - 无权限（非订单所属用户）</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "给小费", description = "为指定的订单添加小费")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "给小费成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                               @Valid @RequestBody TipCreateRequestDTO request) {
        log.info("create() called");
        tipService.create(token.getUserId(), request);
        return Result.success();
    }

    /**
     * 根据订单获取小费
     *
     * <p>API: GET /api/tips/order/{orderId}</p>
     * <p>请求来源：前端订单详情页，查看该订单的小费记录</p>
     * <p>权限要求：已登录用户（@PreAuthorize("isAuthenticated()")）</p>
     * <p>输入参数：@path orderId - 订单ID</p>
     * <p>返回数据：List&lt;TipDTO&gt; - 该订单的小费记录列表</p>
     * <p>异常情况：
     * <ul>
     *   <li>401 - 未登录</li>
     *   <li>403 - 无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "根据订单获取小费", description = "根据订单ID获取该订单的小费列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回小费列表"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<TipDTO>> listByOrder(@Parameter(description = "订单ID") @PathVariable Long orderId) {
        log.info("listByOrder() called");
        return Result.success(tipService.listByOrder(orderId).stream().map(tipService::toDTO).collect(Collectors.toList()));
    }

    /**
     * 获取我的小费列表
     *
     * <p>API: GET /api/tips/me</p>
     * <p>请求来源：前端个人中心-我的打赏页，用户查看自己送出的小费记录</p>
     * <p>权限要求：已登录用户（@PreAuthorize("isAuthenticated()")）</p>
     * <p>输入参数：无（从token中提取用户ID）</p>
     * <p>返回数据：List&lt;TipDTO&gt; - 当前用户送出的所有小费记录</p>
     * <p>异常情况：
     * <ul>
     *   <li>401 - 未登录</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取我的小费列表", description = "获取当前用户的所有小费记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回小费列表"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<TipDTO>> listMyTips(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("listMyTips() called");
        return Result.success(tipService.listMyTips(token.getUserId()).stream().map(tipService::toDTO).collect(Collectors.toList()));
    }
}
