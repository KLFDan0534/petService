package com.pet.customer.controller;

import com.pet.common.PageRequestDTO;
import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.customer.dto.TicketAddMessageRequestDTO;
import com.pet.customer.dto.TicketAssignRequestDTO;
import com.pet.customer.dto.TicketCreateRequestDTO;
import com.pet.customer.dto.TicketDTO;
import com.pet.customer.dto.TicketMessageDTO;
import com.pet.customer.dto.TicketResolveRequestDTO;
import com.pet.customer.service.TicketService;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "【用户端】工单管理", description = "客服工单管理（用户/管理员/商户/客服使用）")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/me")
    @Operation(summary = "获取我的工单列表")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<TicketDTO>> listMyTickets(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(ticketService.listByUser(token.getUserId()));
    }

    @GetMapping
    @Operation(summary = "管理员、商户、客服获取工单列表")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<PageResult<TicketDTO>> listAll(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "分页请求") PageRequestDTO pageParam) {
        return Result.success(new PageResult<>(ticketService.listPageForStaff(
                pageParam,
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE"))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取工单详情")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "404", description = "资源不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<TicketDTO> getById(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "工单ID") @PathVariable Long id) {
        return Result.success(ticketService.getByIdForUser(
                id,
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE")));
    }

    @PostMapping
    @Operation(summary = "创建工单")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<TicketDTO> create(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Valid @RequestBody TicketCreateRequestDTO request) {
        return Result.success(ticketService.create(token.getUserId(), request));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @Operation(summary = "指派工单")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "404", description = "资源不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<TicketDTO> assign(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "工单ID") @PathVariable Long id,
            @RequestBody(required = false) TicketAssignRequestDTO request) {
        Long assigneeId = request != null && request.getAssignee_id_wsh() != null
                ? request.getAssignee_id_wsh()
                : token.getUserId();
        return Result.success(ticketService.assignForStaff(
                id,
                assigneeId,
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE")));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @Operation(summary = "解决工单")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "404", description = "资源不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<TicketDTO> resolve(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "工单ID") @PathVariable Long id,
            @RequestBody(required = false) TicketResolveRequestDTO request) {
        return Result.success(ticketService.resolveForStaff(
                id,
                request != null ? request.getResult_wsh() : null,
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE")));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @Operation(summary = "关闭工单")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "404", description = "资源不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<TicketDTO> close(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "工单ID") @PathVariable Long id) {
        return Result.success(ticketService.closeForStaff(
                id,
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE")));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "获取工单消息列表")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "404", description = "资源不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<TicketMessageDTO>> listMessages(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "工单ID") @PathVariable Long id) {
        return Result.success(ticketService.listMessagesForUser(
                id,
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE")));
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "添加工单消息")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "404", description = "资源不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<TicketMessageDTO> addMessage(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "工单ID") @PathVariable Long id,
            @Valid @RequestBody TicketAddMessageRequestDTO request) {
        return Result.success(ticketService.addMessageForUser(
                id,
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE"),
                request.getContent_wsh()));
    }

    private boolean hasRole(JwtAuthenticationToken token, String role) {
        String authority = "ROLE_" + role;
        return token != null
                && token.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
