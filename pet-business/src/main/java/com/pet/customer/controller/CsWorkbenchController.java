package com.pet.customer.controller;

import com.pet.common.Result;
import com.pet.customer.dto.CsConversationDTO;
import com.pet.customer.dto.CsThreadDTO;
import com.pet.customer.dto.CsMerchantDTO;
import com.pet.customer.dto.CsWorkbenchStatsDTO;
import com.pet.customer.service.CsWorkbenchService;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 【客服工作台】客服角色专属工作台：统计概览、服务商家、会话列表。
 */
@RestController
@RequestMapping("/api/cs-workbench")
@Tag(name = "【客服端】客服工作台", description = "客服工作台（统计/服务商家/会话）")
public class CsWorkbenchController {

    private final CsWorkbenchService csWorkbenchService;

    public CsWorkbenchController(CsWorkbenchService csWorkbenchService) {
        this.csWorkbenchService = csWorkbenchService;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @Operation(summary = "工作台统计")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问")
    })
    public Result<CsWorkbenchStatsDTO> stats(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(csWorkbenchService.stats(
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE")));
    }

    @GetMapping("/merchants")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @Operation(summary = "我服务的商家列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问")
    })
    public Result<List<CsMerchantDTO>> merchants(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(csWorkbenchService.merchants(token.getUserId()));
    }

    @GetMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "会话列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    public Result<List<CsConversationDTO>> conversations(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(csWorkbenchService.conversations(token.getUserId()));
    }

    @GetMapping("/threads")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @Operation(summary = "业务线程列表（投诉/工单独立会话）", description = "同一用户的不同投诉/工单各自成一条线程，便于分对象沟通")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问")
    })
    public Result<List<CsThreadDTO>> threads(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(csWorkbenchService.threads(
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE")));
    }

    @PostMapping("/threads/read")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @Operation(summary = "标记投诉/工单线程已读", description = "把指定投诉/工单线程中发给当前用户的消息全部标记为已读（红点消除）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问")
    })
    public Result<Void> markThreadRead(@AuthenticationPrincipal JwtAuthenticationToken token,
                                       @RequestParam String type,
                                       @RequestParam Long bizId) {
        csWorkbenchService.markThreadRead(token.getUserId(), type, bizId);
        return Result.success(null);
    }

    private boolean hasRole(JwtAuthenticationToken token, String role) {
        String authority = "ROLE_" + role;
        return token != null
                && token.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
