package com.pet.customer.controller;

import com.pet.common.Result;
import com.pet.customer.dto.MerchantCustomerServiceApplyRequestDTO;
import com.pet.customer.dto.MerchantCustomerServiceDTO;
import com.pet.customer.dto.MerchantCustomerServiceReviewRequestDTO;
import com.pet.customer.service.MerchantCustomerServiceService;
import com.pet.security.JwtAuthenticationToken;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "【用户端】客服管理", description = "商家客服服务管理（用户申请/商家审核）")
@RestController
@RequestMapping("/api/merchant-customer-service")
public class MerchantCustomerServiceController {
    private final MerchantCustomerServiceService service;

    public MerchantCustomerServiceController(MerchantCustomerServiceService service) {
        this.service = service;
    }

    @Operation(summary = "申请成为客服")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "申请提交成功"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    @PostMapping("/applications")
    @PreAuthorize("isAuthenticated()")
    public Result<MerchantCustomerServiceDTO> apply(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Valid @RequestBody MerchantCustomerServiceApplyRequestDTO request) {
        return Result.success(service.apply(token.getUserId(), request));
    }

    @Operation(summary = "查看我的客服申请列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    @GetMapping("/applications/me")
    @PreAuthorize("isAuthenticated()")
    public Result<List<MerchantCustomerServiceDTO>> listMine(
            @AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(service.listMine(token.getUserId()));
    }

    @Operation(summary = "商家查看待审核的客服申请列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无商家权限")
    })
    @GetMapping("/merchant/applications/pending")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<List<MerchantCustomerServiceDTO>> listPendingForMerchant(
            @AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(service.listPendingForMerchant(token.getUserId()));
    }

    @Operation(summary = "商家查看已通过审核的客服列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无商家权限")
    })
    @GetMapping("/merchant/staff")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<List<MerchantCustomerServiceDTO>> listApprovedForMerchant(
            @AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(service.listApprovedForMerchant(token.getUserId()));
    }

    @Operation(summary = "商家审核通过客服申请")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "审核通过成功"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无商家权限"),
            @ApiResponse(responseCode = "404", description = "申请记录不存在")
    })
    @PostMapping("/merchant/applications/{id}/approve")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<MerchantCustomerServiceDTO> approve(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "客服申请记录ID") @PathVariable Long id,
            @RequestBody(required = false) MerchantCustomerServiceReviewRequestDTO request) {
        return Result.success(service.approve(id, token.getUserId(), request));
    }

    @Operation(summary = "商家驳回客服申请")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "驳回成功"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无商家权限"),
            @ApiResponse(responseCode = "404", description = "申请记录不存在")
    })
    @PostMapping("/merchant/applications/{id}/reject")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<MerchantCustomerServiceDTO> reject(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "客服申请记录ID") @PathVariable Long id,
            @RequestBody(required = false) MerchantCustomerServiceReviewRequestDTO request) {
        return Result.success(service.reject(id, token.getUserId(), request));
    }

    @Operation(summary = "客服主动辞职")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "辞职成功"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无客服权限"),
            @ApiResponse(responseCode = "404", description = "记录不存在")
    })
    @PostMapping("/applications/{id}/resign")
    @PreAuthorize("hasRole('CUSTOMER_SERVICE')")
    public Result<MerchantCustomerServiceDTO> resign(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "客服申请记录ID") @PathVariable Long id) {
        return Result.success(service.resign(id, token.getUserId()));
    }

    @Operation(summary = "商家终止客服关系")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "终止成功"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无商家权限"),
            @ApiResponse(responseCode = "404", description = "记录不存在")
    })
    @PostMapping("/merchant/staff/{id}/terminate")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<MerchantCustomerServiceDTO> terminateByMerchant(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "客服记录ID") @PathVariable Long id) {
        return Result.success(service.terminateByMerchant(id, token.getUserId()));
    }
}
