package com.pet.membership.controller;

import com.pet.common.Result;
import com.pet.membership.dto.MembershipOrderCreateRequestDTO;
import com.pet.membership.dto.MembershipOrderDTO;
import com.pet.membership.service.MembershipOrderService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/membership/orders")
@Tag(name = "【用户端】会员订单管理", description = "会员购买和续费订单管理（创建待支付订单/查询/取消）")
public class MembershipOrderController {
    private final MembershipOrderService membershipOrderService;

    public MembershipOrderController(MembershipOrderService membershipOrderService) {
        this.membershipOrderService = membershipOrderService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "创建会员订单", description = "基于启用会员套餐创建待支付会员订单，不执行扣款和开通")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<MembershipOrderDTO> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                             @Valid @RequestBody MembershipOrderCreateRequestDTO request) {
        return Result.success(membershipOrderService.createOrder(token.getUserId(), request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取我的会员订单", description = "获取当前用户的会员购买和续费订单")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<MembershipOrderDTO>> listMyOrders(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(membershipOrderService.listMyOrders(token.getUserId()));
    }

    @GetMapping("/{orderNo}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取会员订单详情", description = "根据会员订单号获取当前用户自己的会员订单")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<MembershipOrderDTO> getMyOrder(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                 @Parameter(description = "会员订单号") @PathVariable String orderNo) {
        return Result.success(membershipOrderService.getMyOrder(token.getUserId(), orderNo));
    }

    @PostMapping("/{orderNo}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "取消会员订单", description = "取消当前用户自己的待支付会员订单")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<MembershipOrderDTO> cancel(@AuthenticationPrincipal JwtAuthenticationToken token,
                                             @Parameter(description = "会员订单号") @PathVariable String orderNo) {
        return Result.success(membershipOrderService.cancelPendingOrder(token.getUserId(), orderNo));
    }

    @PostMapping("/{orderNo}/pay")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "支付会员订单", description = "使用余额支付当前用户自己的待支付会员订单，并激活或续费会员；外部支付方式需等待平台确认")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<MembershipOrderDTO> pay(@AuthenticationPrincipal JwtAuthenticationToken token,
                                          @Parameter(description = "会员订单号") @PathVariable String orderNo) {
        return Result.success(membershipOrderService.payOrder(token.getUserId(), orderNo));
    }

    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员获取会员订单", description = "管理员按状态查询会员购买和续费订单")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<MembershipOrderDTO>> listOrdersForAdmin(@Parameter(description = "会员订单状态")
                                                               @RequestParam(required = false) String status_wsh) {
        return Result.success(membershipOrderService.listOrdersForAdmin(status_wsh));
    }

    @PostMapping("/admin/{orderNo}/pay")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员确认会员订单支付", description = "管理员手动确认会员订单支付成功，并激活或续费会员")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<MembershipOrderDTO> confirmPaidForAdmin(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                          @Parameter(description = "会员订单号") @PathVariable String orderNo) {
        return Result.success(membershipOrderService.confirmPaidForAdmin(token.getUserId(), orderNo));
    }
}
