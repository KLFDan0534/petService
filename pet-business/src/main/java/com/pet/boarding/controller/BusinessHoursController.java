package com.pet.boarding.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.boarding.dto.BusinessHoursUpsertRequestDTO;
import com.pet.boarding.dto.BusinessHoursDTO;
import com.pet.boarding.service.BusinessHoursService;
import com.pet.boarding.service.MerchantService;
import com.pet.security.JwtAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 营业时间管理控制器
 * 提供商家营业时间的查询、设置和删除功能
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@RestController
@RequestMapping("/api/merchants/{merchantId}/hours")
@Tag(name = "【用户端】营业时间管理", description = "商家营业时间查询和设置管理")
@Slf4j
public class BusinessHoursController {

    private final BusinessHoursService businessHoursService;
    private final MerchantService merchantService;

    public BusinessHoursController(BusinessHoursService businessHoursService, MerchantService merchantService) {
        this.businessHoursService = businessHoursService;
        this.merchantService = merchantService;
    }

    /**
     * 获取商家的营业时间列表
     * @param merchantId 商家ID
     * @return 营业时间列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @Operation(summary = "获取营业时间", description = "获取商家的营业时间列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<BusinessHoursDTO>> list(@Parameter(description = "商家ID") @PathVariable Long merchantId) {
        log.info("list() called");
        return Result.success(businessHoursService.getByMerchantId(merchantId).stream().map(businessHoursService::toDTO).collect(Collectors.toList()));
    }

    /**
     * 设置或更新某天的营业时间
     * @param merchantId 商家ID
     * @param hours 营业时间信息
     * @return 更新的营业时间
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    @Operation(summary = "设置营业时间", description = "设置或更新某天的营业时间")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<BusinessHoursDTO> upsert(@AuthenticationPrincipal JwtAuthenticationToken token,
                                           @Parameter(description = "商家ID") @PathVariable Long merchantId,
                                           @RequestBody BusinessHoursUpsertRequestDTO dto) {
        log.info("upsert() called");
        if (!isAdmin(token) && !merchantService.isOwner(merchantId, token.getUserId())) {
            return Result.error(403, "无权修改此商家的营业时间");
        }
        return Result.success(businessHoursService.toDTO(businessHoursService.upsert(merchantId, dto)));
    }

    /**
     * 删除某一天的营业时间记录
     * @param merchantId 商家ID
     * @param id 营业时间记录ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    @Operation(summary = "删除营业时间", description = "删除某一天的营业时间记录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> delete(@AuthenticationPrincipal JwtAuthenticationToken token,
                               @Parameter(description = "商家ID") @PathVariable Long merchantId,
                               @Parameter(description = "营业时间记录ID") @PathVariable Long id) {
        log.info("delete() called");
        if (!isAdmin(token) && !merchantService.isOwner(merchantId, token.getUserId())) {
            return Result.error(403, "无权修改此商家的营业时间");
        }
        businessHoursService.delete(merchantId, id);
        return Result.success();
    }

    private boolean isAdmin(JwtAuthenticationToken token) {
        if (token == null) return false;
        for (GrantedAuthority authority : token.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
