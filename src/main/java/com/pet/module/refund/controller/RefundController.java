package com.pet.module.refund.controller;

import com.pet.common.Result;
import com.pet.module.refund.entity.Refund;
import com.pet.module.refund.service.RefundService;
import com.pet.security.JwtAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/refunds")
@Tag(name = "退款管理", description = "退款流程的申请、审批和管理")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @GetMapping
    @Operation(summary = "获取我的退款列表", description = "获取当前用户的所有退款记录")
    public Result<List<Refund>> listMyRefunds(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(refundService.listByOwner(token.getUserId()));
    }

    @PostMapping
    @Operation(summary = "申请退款", description = "用户发起退款申请")
    public Result<Refund> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                  @RequestBody Map<String, String> body) {
        return Result.success(refundService.createRefund(
                token.getUserId(), body.get("orderNo"), body.get("reason")));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "审核通过退款", description = "管理员批准退款申请")
    public Result<Void> approve(@PathVariable Long id) {
        refundService.approveRefund(id);
        return Result.success();
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "完成退款", description = "管理员标记退款已完成")
    public Result<Void> complete(@PathVariable Long id) {
        refundService.completeRefund(id);
        return Result.success();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "驳回退款申请", description = "管理员驳回用户的退款申请")
    public Result<Void> reject(@PathVariable Long id) {
        refundService.rejectRefund(id);
        return Result.success();
    }
}
