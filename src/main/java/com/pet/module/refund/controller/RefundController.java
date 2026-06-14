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

@RestController
@RequestMapping("/api/refunds")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @GetMapping
    public Result<List<Refund>> listMyRefunds(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(refundService.listByOwner(token.getUserId()));
    }

    @PostMapping
    public Result<Refund> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                  @RequestBody Map<String, String> body) {
        return Result.success(refundService.createRefund(
                token.getUserId(), body.get("orderNo"), body.get("reason")));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> approve(@PathVariable Long id) {
        refundService.approveRefund(id);
        return Result.success();
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> complete(@PathVariable Long id) {
        refundService.completeRefund(id);
        return Result.success();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> reject(@PathVariable Long id) {
        refundService.rejectRefund(id);
        return Result.success();
    }
}
