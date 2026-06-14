package com.pet.module.order.controller;

import com.pet.common.Result;
import com.pet.module.order.dto.CreateOrderRequest;
import com.pet.module.order.entity.PetOrder;
import com.pet.module.order.service.OrderService;
import com.pet.security.JwtAuthenticationToken;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "订单管理", description = "宠物服务订单的创建、查询和管理")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "获取我的订单列表", description = "获取当前用户的所有订单")
    public Result<List<PetOrder>> listMyOrders(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(orderService.listByOwner(token.getUserId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取订单详情", description = "根据ID获取订单详细信息")
    public Result<PetOrder> getById(@PathVariable Long id) {
        return Result.success(orderService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建订单", description = "创建一个新的宠物服务订单")
    public Result<PetOrder> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                    @Valid @RequestBody CreateOrderRequest request) {
        return Result.success(orderService.createOrder(token.getUserId(), request));
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消订单", description = "取消指定的订单")
    public Result<Void> cancel(@AuthenticationPrincipal JwtAuthenticationToken token,
                                @RequestBody Map<String, String> body) {
        orderService.cancelOrder(token.getUserId(), body.get("orderNo"));
        return Result.success();
    }

    @PostMapping("/complete")
    @PreAuthorize("hasRole('KEEPER') or hasRole('MERCHANT')")
    @Operation(summary = "完成订单", description = "看护人或商家完成订单")
    public Result<Void> complete(@RequestBody Map<String, String> body) {
        orderService.completeOrder(body.get("orderNo"));
        return Result.success();
    }
}
