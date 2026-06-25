package com.pet.order.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.order.entity.Refund;
import com.pet.security.JwtAuthenticationToken;
import com.pet.order.service.RefundService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/refunds")
@Tag(name = "退款管理", description = "订单退款管理")
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
    public Result<List<Refund>> listMyRefunds(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 listMyRefunds()");
        return Result.success(refundService.listByOwner(token.getUserId()));
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
    public Result<Refund> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                @RequestBody Map<String, Object> body) {
        log.info("调用 create()");
        Long orderId = longValue(firstPresent(body, "order_id_wsh", "orderId", "order_id"));
        String reason = stringValue(firstPresent(body, "reason_wsh", "reason"));
        return Result.success(refundService.createRefundByOrderId(token.getUserId(), orderId, reason));
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
    public Result<List<Refund>> listAll() {
        return Result.success(refundService.listAll());
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
    public Result<Void> approve(@PathVariable Long id) {
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
    public Result<Void> complete(@PathVariable Long id) {
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
    public Result<Void> reject(@PathVariable Long id) {
        refundService.rejectRefund(id);
        return Result.success();
    }

    private Object firstPresent(Map<String, Object> body, String... keys) {
        if (body == null) {
            return null;
        }
        for (String key : keys) {
            if (body.containsKey(key)) {
                return body.get(key);
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : Long.parseLong(text);
    }
}
