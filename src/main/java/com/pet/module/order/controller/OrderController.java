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

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public Result<List<PetOrder>> listMyOrders(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(orderService.listByOwner(token.getUserId()));
    }

    @GetMapping("/{id}")
    public Result<PetOrder> getById(@PathVariable Long id) {
        return Result.success(orderService.getById(id));
    }

    @PostMapping
    public Result<PetOrder> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                    @Valid @RequestBody CreateOrderRequest request) {
        return Result.success(orderService.createOrder(token.getUserId(), request));
    }

    @PostMapping("/cancel")
    public Result<Void> cancel(@AuthenticationPrincipal JwtAuthenticationToken token,
                                @RequestBody Map<String, String> body) {
        orderService.cancelOrder(token.getUserId(), body.get("orderNo"));
        return Result.success();
    }

    @PostMapping("/complete")
    @PreAuthorize("hasRole('KEEPER') or hasRole('MERCHANT')")
    public Result<Void> complete(@RequestBody Map<String, String> body) {
        orderService.completeOrder(body.get("orderNo"));
        return Result.success();
    }
}
