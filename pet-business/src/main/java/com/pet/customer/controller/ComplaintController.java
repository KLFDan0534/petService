package com.pet.customer.controller;

import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.customer.dto.ComplaintCreateRequestDTO;
import com.pet.customer.dto.ComplaintDTO;
import com.pet.customer.dto.ComplaintEvidenceDTO;
import com.pet.customer.dto.ComplaintListRequestDTO;
import com.pet.customer.dto.ComplaintMessageDTO;
import com.pet.customer.dto.ComplaintMessageSendRequestDTO;
import com.pet.customer.dto.ComplaintReviewRequestDTO;
import com.pet.customer.dto.ComplaintTargetsDTO;
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

    @GetMapping("/targets")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取可投诉目标列表", description = "返回当前用户可投诉/举报的对象（订单、消费过的商家、服务过的寄养师），前端渲染为选择列表，用户无需手填ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ComplaintTargetsDTO> targets(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(complaintService.getTargets(token.getUserId()));
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
            ComplaintListRequestDTO pageParam) {
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

    @GetMapping("/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "查询投诉沟通消息", description = "投诉人本人或可管理该投诉的内部人员查看往来消息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<ComplaintMessageDTO>> listMessages(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "投诉ID") Long id) {
        return Result.success(complaintService.listMessages(
                id,
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE")));
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "发送投诉沟通消息", description = "用户追问或客服回复，站内通知接收方")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ComplaintMessageDTO> sendMessage(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "投诉ID") Long id,
            @Valid @RequestBody ComplaintMessageSendRequestDTO request) {
        return Result.success(complaintService.sendMessage(
                id,
                token.getUserId(),
                request.getContent_wsh(),
                request.getFile_url_wsh(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE")));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @Operation(summary = "受理投诉", description = "受理投诉进入处理中状态(pending -> processing)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ComplaintDTO> accept(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "投诉ID") Long id) {
        return Result.success(complaintService.accept(
                id,
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE")));
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
