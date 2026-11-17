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

    /**
     * 业务作用：返回当前内部人员的待处理投诉、工单、会话等统计数字。
     * 业务规则：需 ADMIN/MERCHANT/CUSTOMER_SERVICE 之一。
     * 异常情况：未登录返回 401；角色不符返回 403。
     * 注意事项：角色由 token 权限实时解析，不依赖数据库角色表。
     */
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

    /**
     * 业务作用：返回当前客服/商家可管理的商家列表。
     * 业务规则：需 ADMIN/MERCHANT/CUSTOMER_SERVICE 之一。
     * 异常情况：未登录返回 401；角色不符返回 403。
     */
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

    /**
     * 业务作用：返回当前用户的聊天会话列表。
     * 业务规则：仅需登录，任意角色可用。
     * 异常情况：未登录返回 401。
     * 注意事项：权限比 stats/threads 更宽松，普通用户也能访问。
     */
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

    /**
     * 业务作用：按业务对象（投诉/工单）拆分返回独立会话线程。
     * 业务规则：需 ADMIN/MERCHANT/CUSTOMER_SERVICE 之一。
     * 异常情况：未登录返回 401；角色不符返回 403。
     */
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

    /**
     * 业务作用：把指定投诉/工单线程中发给当前用户的消息标记为已读，消除红点。
     * 业务规则：需 ADMIN/MERCHANT/CUSTOMER_SERVICE 之一；type 与 bizId 为必填请求参数。
     * 异常情况：缺少 type 或 bizId 由框架返回 400；未登录返回 401。
     * 注意事项：返回 Result.success(null) 而非 Result.success()，两者语义一致但写法不同。
     */
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

    /**
     * 业务作用：从登录凭证中判断当前用户是否拥有某个角色权限。
     * 业务规则：token 为空返回 false；角色名不带 ROLE_ 前缀。
     * 注意事项：与 @PreAuthorize 中的 hasAnyRole 表达式需保持角色名一致。
     */
    private boolean hasRole(JwtAuthenticationToken token, String role) {
        String authority = "ROLE_" + role;
        return token != null
                && token.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
