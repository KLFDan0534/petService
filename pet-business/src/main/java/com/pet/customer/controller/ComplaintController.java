package com.pet.customer.controller;

import com.pet.common.PageRequestDTO;
import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.customer.dto.ComplaintCreateRequestDTO;
import com.pet.customer.dto.ComplaintDTO;
import com.pet.customer.dto.ComplaintEvidenceDTO;
import com.pet.customer.dto.ComplaintReviewRequestDTO;
import com.pet.customer.service.ComplaintService;
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
@RequestMapping("/api/complaints")
@Tag(name = "【用户端】投诉管理", description = "投诉管理（用户投诉/管理员处理）")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取我的投诉列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<ComplaintDTO>> listMyComplaints(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(complaintService.listByOwner(token.getUserId()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @Operation(summary = "管理员获取所有投诉列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<PageResult<ComplaintDTO>> listAll(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            PageRequestDTO pageParam) {
        return Result.success(new PageResult<>(complaintService.listPageForStaff(
                pageParam,
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE"))));
    }

    @GetMapping("/{id}/evidence")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @Operation(summary = "获取投诉证据")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ComplaintEvidenceDTO> evidence(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "投诉ID") Long id) {
        return Result.success(complaintService.getEvidenceForStaff(
                id,
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE")));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "创建投诉")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ComplaintDTO> create(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Valid @RequestBody ComplaintCreateRequestDTO request) {
        return Result.success(complaintService.create(request, token.getUserId()));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @Operation(summary = "处理投诉")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ComplaintDTO> resolve(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "投诉ID") Long id,
            @Valid @RequestBody ComplaintReviewRequestDTO request) {
        return Result.success(complaintService.processForStaff(
                id,
                request.getResult_wsh(),
                "resolved",
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE")));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @Operation(summary = "驳回投诉")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ComplaintDTO> reject(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "投诉ID") Long id,
            @Valid @RequestBody ComplaintReviewRequestDTO request) {
        return Result.success(complaintService.processForStaff(
                id,
                request.getResult_wsh(),
                "rejected",
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE")));
    }

    private boolean hasRole(JwtAuthenticationToken token, String role) {
        String authority = "ROLE_" + role;
        return token != null
                && token.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
