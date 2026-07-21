package com.pet.marketing.controller;

import com.pet.common.Result;
import com.pet.marketing.dto.CouponGrantRequestDTO;
import com.pet.marketing.dto.CouponQuoteDTO;
import com.pet.marketing.dto.CouponQuoteRequestDTO;
import com.pet.marketing.dto.CouponTemplateCreateRequestDTO;
import com.pet.marketing.dto.CouponTemplateDTO;
import com.pet.marketing.dto.CouponTemplateStatusRequestDTO;
import com.pet.marketing.dto.UserCouponDTO;
import com.pet.marketing.entity.CouponTemplate;
import com.pet.marketing.entity.UserCoupon;
import com.pet.marketing.service.CouponService;
import com.pet.security.JwtAuthenticationToken;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@Tag(name = "【用户端】优惠券管理", description = "优惠券模板和用户优惠券管理（用户领取/管理员发放）")
public class CouponController {
    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/templates")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取优惠券模板列表", description = "管理员获取所有优惠券模板列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回模板列表"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<CouponTemplateDTO>> listTemplates() {
        return Result.success(couponService.listTemplates(false));
    }

    @GetMapping("/templates/active")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取活跃优惠券模板", description = "获取当前可领取的优惠券模板列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回活跃模板列表"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<CouponTemplateDTO>> listActiveTemplates() {
        return Result.success(couponService.listTemplates(true));
    }

    @PostMapping("/templates")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "创建优惠券模板", description = "管理员创建新的优惠券模板")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功返回模板信息"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<CouponTemplateDTO> createTemplate(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                    @Valid @RequestBody CouponTemplateCreateRequestDTO request) {
        return Result.success(toTemplateDTO(couponService.createTemplate(token.getUserId(), request)));
    }

    @PostMapping("/templates/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新模板状态", description = "管理员更新优惠券模板的启用/禁用状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "状态更新成功返回模板信息"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<CouponTemplateDTO> updateStatus(@Parameter(description = "模板ID") @PathVariable Long id,
                                                  @RequestBody CouponTemplateStatusRequestDTO request) {
        couponService.updateTemplateStatus(id, request != null ? request.getStatus_wsh() : null);
        return Result.success(couponService.listTemplates(false).stream()
                .filter(item -> item.getId_wsh().equals(id))
                .findFirst()
                .orElse(null));
    }

    @PostMapping("/templates/{id}/grant")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "发放优惠券", description = "管理员向指定用户发放优惠券")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "发放成功返回发放数量"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Integer> grantToUser(@AuthenticationPrincipal JwtAuthenticationToken token,
                                       @Parameter(description = "模板ID") @PathVariable Long id,
                                       @RequestBody CouponGrantRequestDTO request) {
        return Result.success(couponService.grantToUser(token.getUserId(), id, request));
    }

    @PostMapping("/templates/{id}/grant-all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "发放给所有用户", description = "管理员向所有用户发放优惠券")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "发放成功返回发放数量"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Integer> grantToAll(@AuthenticationPrincipal JwtAuthenticationToken token,
                                      @Parameter(description = "模板ID") @PathVariable Long id,
                                      @RequestBody(required = false) CouponGrantRequestDTO request) {
        return Result.success(couponService.grantToAllUsers(token.getUserId(), id, request));
    }

    @PostMapping("/templates/{id}/grant-condition")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "条件发放优惠券", description = "管理员按注册时间、消费、订单、宠物数等条件定向发放优惠券")
    public Result<Integer> grantByCondition(@AuthenticationPrincipal JwtAuthenticationToken token,
                                            @Parameter(description = "模板ID") @PathVariable Long id,
                                            @RequestBody(required = false) CouponGrantRequestDTO request) {
        return Result.success(couponService.grantByCondition(token.getUserId(), id, request));
    }

    @PostMapping("/templates/{id}/claim")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "领取优惠券", description = "用户领取指定的优惠券")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "领取成功返回优惠券信息"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<UserCouponDTO> claim(@AuthenticationPrincipal JwtAuthenticationToken token,
                                       @Parameter(description = "模板ID") @PathVariable Long id) {
        UserCoupon coupon = couponService.claim(token.getUserId(), id);
        return Result.success(couponService.listMyCoupons(token.getUserId()).stream()
                .filter(item -> item.getId_wsh().equals(coupon.getId_wsh()))
                .findFirst()
                .orElse(null));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取我的优惠券", description = "获取当前用户拥有的所有优惠券")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回优惠券列表"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<UserCouponDTO>> myCoupons(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(couponService.listMyCoupons(token.getUserId()));
    }

    @GetMapping("/available")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取可用优惠券", description = "获取当前用户可用的优惠券列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回可用优惠券列表"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<UserCouponDTO>> availableCoupons(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                        @Parameter(description = "商家ID") @RequestParam(required = false) Long merchant_id_wsh,
                                                        @Parameter(description = "订单金额") @RequestParam(required = false) BigDecimal order_amount_wsh) {
        return Result.success(couponService.listAvailableCoupons(token.getUserId(), merchant_id_wsh, order_amount_wsh));
    }

    @PostMapping("/quote")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "优惠券报价", description = "根据订单金额计算最优优惠券折扣")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回报价信息"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<CouponQuoteDTO> quote(@AuthenticationPrincipal JwtAuthenticationToken token,
                                        @RequestBody CouponQuoteRequestDTO request) {
        return Result.success(couponService.quote(token.getUserId(), request));
    }

    private CouponTemplateDTO toTemplateDTO(CouponTemplate template) {
        CouponTemplateDTO dto = new CouponTemplateDTO();
        BeanUtils.copyProperties(template, dto);
        return dto;
    }
}
