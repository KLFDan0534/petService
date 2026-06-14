package com.pet.module.payment.controller;

import com.pet.common.Result;
import com.pet.module.payment.entity.Payment;
import com.pet.module.payment.service.PaymentService;
import com.pet.security.JwtAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    public Result<Payment> createPayment(@AuthenticationPrincipal JwtAuthenticationToken token,
                                          @RequestBody Map<String, String> body) {
        return Result.success(paymentService.createPayment(
                token.getUserId(), body.get("orderNo"), body.get("method")));
    }

    @PostMapping("/pay")
    public Result<Void> pay(@RequestBody Map<String, String> body) {
        paymentService.pay(body.get("payNo"));
        return Result.success();
    }

    @GetMapping("/order/{orderNo}")
    public Result<Payment> getByOrderNo(@PathVariable String orderNo) {
        return Result.success(paymentService.getByOrderNo(orderNo));
    }

    @GetMapping
    public Result<List<Payment>> listMyPayments(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(paymentService.listByUser(token.getUserId()));
    }
}
