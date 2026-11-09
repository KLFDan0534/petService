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

    /**
     * 【客服工作台控制器构造注入】
     *
     * 业务作用：注入客服工作台业务服务。
     *
     * 调用场景：Spring 容器创建 CsWorkbenchController 时。
     *
     * 调用链：Spring 容器 → 本构造方法 → 赋值 csWorkbenchService。
     *
     * 数据处理：仅保存依赖引用。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：无。
     */
    public CsWorkbenchController(CsWorkbenchService csWorkbenchService) {
        this.csWorkbenchService = csWorkbenchService;
    }

    /**
     * 【工作台统计概览】
     *
     * 业务作用：返回当前内部人员的待处理投诉、工单、会话等统计数字。
     *
     * 调用场景：客服/商家/管理员打开工作台首页。
     *
     * 调用链：GET /api/cs-workbench/stats → stats() → hasRole() → csWorkbenchService.stats()。
     *
     * 数据处理：解析 token 得到用户 ID 与三种角色标记，交给服务层按角色聚合统计。
     *
     * 业务规则：需 ADMIN/MERCHANT/CUSTOMER_SERVICE 之一。
     *
     * 状态影响：无。
     *
     * 异常情况：未登录返回 401；角色不符返回 403。
     *
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
     * 【查询我服务的商家列表】
     *
     * 业务作用：返回当前客服/商家可管理的商家列表。
     *
     * 调用场景：工作台左侧商家切换。
     *
     * 调用链：GET /api/cs-workbench/merchants → merchants() → csWorkbenchService.merchants()。
     *
     * 数据处理：仅传用户 ID，具体范围由服务层按角色与授权关系计算。
     *
     * 业务规则：需 ADMIN/MERCHANT/CUSTOMER_SERVICE 之一。
     *
     * 状态影响：无。
     *
     * 异常情况：未登录返回 401；角色不符返回 403。
     *
     * 注意事项：无。
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
     * 【查询会话列表】
     *
     * 业务作用：返回当前用户的聊天会话列表。
     *
     * 调用场景：工作台会话面板与用户端消息中心。
     *
     * 调用链：GET /api/cs-workbench/conversations → conversations() → csWorkbenchService.conversations()。
     *
     * 数据处理：以登录用户 ID 查询会话摘要。
     *
     * 业务规则：仅需登录，任意角色可用。
     *
     * 状态影响：无。
     *
     * 异常情况：未登录返回 401。
     *
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
     * 【查询投诉与工单线程列表】
     *
     * 业务作用：按业务对象（投诉/工单）拆分返回独立会话线程。
     *
     * 调用场景：客服工作台线程列表，同一用户的不同投诉各自成一条。
     *
     * 调用链：GET /api/cs-workbench/threads → threads() → hasRole() → csWorkbenchService.threads()。
     *
     * 数据处理：传入用户 ID 与三种角色标记，由服务层按权限聚合线程。
     *
     * 业务规则：需 ADMIN/MERCHANT/CUSTOMER_SERVICE 之一。
     *
     * 状态影响：无。
     *
     * 异常情况：未登录返回 401；角色不符返回 403。
     *
     * 注意事项：无。
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
     * 【标记线程已读】
     *
     * 业务作用：把指定投诉/工单线程中发给当前用户的消息标记为已读，消除红点。
     *
     * 调用场景：客服或用户点开某个投诉/工单会话。
     *
     * 调用链：POST /api/cs-workbench/threads/read → markThreadRead() → csWorkbenchService.markThreadRead()。
     *
     * 数据处理：透传用户 ID、业务类型（complaint/ticket）与业务 ID。
     *
     * 业务规则：需 ADMIN/MERCHANT/CUSTOMER_SERVICE 之一；type 与 bizId 为必填请求参数。
     *
     * 状态影响：更新消息已读标记（由服务层写入）。
     *
     * 异常情况：缺少 type 或 bizId 由框架返回 400；未登录返回 401。
     *
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
     * 【判断用户是否具备指定角色】
     *
     * 业务作用：从登录凭证中判断当前用户是否拥有某个角色权限。
     *
     * 调用场景：stats() 与 threads() 组装角色标记。
     *
     * 调用链：stats()/threads() → hasRole()。
     *
     * 数据处理：把传入角色名拼接为 ROLE_ 前缀，遍历 token 权限比对。
     *
     * 业务规则：token 为空返回 false；角色名不带 ROLE_ 前缀。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：与 @PreAuthorize 中的 hasAnyRole 表达式需保持角色名一致。
     */
    private boolean hasRole(JwtAuthenticationToken token, String role) {
        String authority = "ROLE_" + role;
        return token != null
                && token.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
