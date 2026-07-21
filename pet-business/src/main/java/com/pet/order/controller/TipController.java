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
     * @param token 当前用户认证信息
     * @param tip 小费信息
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
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
     * 根据订单ID获取小费列表
     * @param orderId 订单ID
     * @return 小费列表
     * @author: wsh
     * @date: 2026/06/24 11:05
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
     * 获取当前用户的小费列表
     * @param token 当前用户认证信息
     * @return 小费列表
     * @author: wsh
     * @date: 2026/06/24 11:05
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
