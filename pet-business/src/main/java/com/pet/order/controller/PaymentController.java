package com.pet.order.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.order.entity.Payment;
import com.pet.security.JwtAuthenticationToken;
import com.pet.order.service.PaymentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "支付管理", description = "支付操作，包括创建、支付、查询")
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * 创建订单支付
     * @param token 当前用户认证信息
     * @param body 请求体，包含order_id_wsh/orderNo和method_wsh支付方式
     * @return 创建的支付记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "创建支付", description = "为订单创建支付")
    public Result<Payment> createPayment(@AuthenticationPrincipal JwtAuthenticationToken token,
                                         @RequestBody Map<String, Object> body) {
        log.info("调用 createPayment()");
        String method = stringValue(firstPresent(body, "method_wsh", "method"));
        if (method == null || method.isBlank()) {
            method = "online";
        }
        Long orderId = longValue(firstPresent(body, "order_id_wsh", "orderId", "order_id"));
        if (orderId != null) {
            return Result.success(paymentService.createPaymentByOrderId(token.getUserId(), orderId, method));
        }
        String orderNo = stringValue(firstPresent(body, "order_no_wsh", "orderNo", "order_no"));
        return Result.success(paymentService.createPayment(token.getUserId(), orderNo, method));
    }

    /**
     * 执行支付
     * @param token 当前用户认证信息
     * @param body 请求体，包含pay_no_wsh支付编号
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/pay")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "执行支付", description = "根据支付编号执行支付")
    public Result<Void> pay(@AuthenticationPrincipal JwtAuthenticationToken token,
                            @RequestBody Map<String, String> body) {
        log.info("调用 pay()");
        String payNo = body.get("pay_no_wsh");
        if (payNo == null || payNo.isBlank()) {
            payNo = body.get("payNo");
        }
        paymentService.pay(payNo);
        return Result.success();
    }

    /**
     * 根据订单号获取支付信息
     * @param orderNo 订单号
     * @return 支付信息
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/order/{orderNo}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "根据订单获取支付信息", description = "根据订单号获取支付信息")
    public Result<Payment> getByOrderNo(@PathVariable String orderNo) {
        log.info("调用 getByOrderNo()");
        return Result.success(paymentService.getByOrderNo(orderNo));
    }

    /**
     * 获取当前用户的支付列表
     * @param token 当前用户认证信息
     * @return 支付列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取我的支付列表", description = "获取当前用户的支付列表")
    public Result<List<Payment>> listMyPayments(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(paymentService.listByUser(token.getUserId()));
    }

    private Object firstPresent(Map<String, Object> body, String... keys) {
        if (body == null) return null;
        for (String key : keys) {
            if (body.containsKey(key)) return body.get(key);
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private Long longValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : Long.parseLong(text);
    }
}
