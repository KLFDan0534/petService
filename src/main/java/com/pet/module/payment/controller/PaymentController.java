package com.pet.module.payment.controller;

import com.pet.common.Result;
import com.pet.module.payment.entity.Payment;
import com.pet.module.payment.service.PaymentService;
import com.pet.security.JwtAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "支付管理", description = "支付记录的创建和查询管理")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    @Operation(summary = "创建支付", description = "根据订单号创建支付记录")
    public Result<Payment> createPayment(@AuthenticationPrincipal JwtAuthenticationToken token,
                                          @RequestBody Map<String, String> body) {
        return Result.success(paymentService.createPayment(
                token.getUserId(), body.get("orderNo"), body.get("method")));
    }

    @PostMapping("/pay")
    @Operation(summary = "执行支付", description = "根据支付编号执行支付操作")
    public Result<Void> pay(@RequestBody Map<String, String> body) {
        paymentService.pay(body.get("payNo"));
        return Result.success();
    }

    @GetMapping("/order/{orderNo}")
    @Operation(summary = "查询订单支付记录", description = "根据订单号查询支付记录")
    public Result<Payment> getByOrderNo(@PathVariable String orderNo) {
        return Result.success(paymentService.getByOrderNo(orderNo));
    }

    @GetMapping
    @Operation(summary = "获取我的支付记录", description = "获取当前用户的所有支付记录")
    public Result<List<Payment>> listMyPayments(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(paymentService.listByUser(token.getUserId()));
    }
}
