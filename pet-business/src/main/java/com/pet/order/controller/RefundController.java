package com.pet.order.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.order.dto.RefundDTO;
import com.pet.security.JwtAuthenticationToken;
import com.pet.order.service.RefundService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import com.pet.order.dto.RefundCreateRequestDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/refunds")
@Tag(name = "【用户端】退款管理", description = "订单退款管理（用户申请/管理员审核）")
@Slf4j
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    /**
     * 获取当前用户的退款列表
     * @param token 当前用户认证信息
     * @return 退款列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取我的退款列表", description = "获取当前用户的退款列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<RefundDTO>> listMyRefunds(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 listMyRefunds()");
        return Result.success(refundService.listByOwner(token.getUserId()).stream().map(refundService::toDTO).collect(Collectors.toList()));
    }

    /**
     * 创建退款申请
     * @param token 当前用户认证信息
     * @param body 请求体，包含order_id_wsh/orderId和reason_wsh/reason
     * @return 创建的退款
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "创建退款", description = "创建退款申请")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<RefundDTO> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                @RequestBody RefundCreateRequestDTO body) {
        log.info("调用 create()");
        return Result.success(refundService.toDTO(refundService.createRefundByOrderId(token.getUserId(), body.getOrder_id_wsh(), body.getReason_wsh())));
    }

    /**
     * 管理员获取所有退款列表
     * @return 退款列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取所有退款列表", description = "管理员获取所有退款列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<RefundDTO>> listAll() {
        return Result.success(refundService.listAll().stream().map(refundService::toDTO).collect(Collectors.toList()));
    }

    /**
     * 管理员审核通过退款
     * @param id 退款ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "审核通过退款", description = "管理员审核通过退款")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> approve(@Parameter(description = "退款ID") @PathVariable Long id) {
        refundService.approveRefund(id);
        return Result.success();
    }

    /**
     * 管理员完成退款
     * @param id 退款ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "完成退款", description = "管理员完成退款")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> complete(@Parameter(description = "退款ID") @PathVariable Long id) {
        refundService.completeRefund(id);
        return Result.success();
    }

    /**
     * 管理员驳回退款
     * @param id 退款ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "驳回退款", description = "管理员驳回退款")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> reject(@Parameter(description = "退款ID") @PathVariable Long id) {
        refundService.rejectRefund(id);
        return Result.success();
    }

}
