package com.pet.order.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.BusinessException;
import com.pet.common.PageRequestDTO;
import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.order.dto.PaymentDTO;
import com.pet.security.JwtAuthenticationToken;
import com.pet.order.service.PaymentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import com.pet.order.dto.PaymentCreateRequestDTO;
import com.pet.order.dto.PaymentPayRequestDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "【用户端】支付管理", description = "支付操作，包括创建、支付、查询（用户/管理员使用）")
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<PaymentDTO> createPayment(@AuthenticationPrincipal JwtAuthenticationToken token,
                                         @RequestBody PaymentCreateRequestDTO body) {
        log.info("调用 createPayment()");
        body = requireBody(body);
        Long orderId = body.getOrder_id_wsh();
        if (orderId != null) {
            return Result.success(paymentService.toDTO(paymentService.createPaymentByOrderId(token.getUserId(), orderId, body.getMethod_wsh())));
        }
        return Result.success(paymentService.toDTO(paymentService.createPayment(token.getUserId(), body.getOrder_no_wsh(), body.getMethod_wsh())));
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> pay(@AuthenticationPrincipal JwtAuthenticationToken token,
                            @RequestBody PaymentPayRequestDTO body) {
        log.info("调用 pay()");
        body = requireBody(body);
        if (body.getPay_no_wsh() == null || body.getPay_no_wsh().isBlank()) {
            throw new BusinessException(400, "payNo cannot be empty");
        }
        paymentService.pay(token.getUserId(), body.getPay_no_wsh());
        return Result.success();
    }

    /**
     * 管理员分页查询支付记录
     * @param pageParam 分页参数
     * @param keyword 搜索关键字（可按订单号/支付编号模糊搜索）
     * @return 分页的支付记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/admin-list")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员分页查询支付记录", description = "管理员分页查询所有支付记录，支持按订单号/支付编号模糊搜索")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<PageResult<PaymentDTO>> adminList(PageRequestDTO pageParam,
                                                    @Parameter(description = "搜索关键字（按订单号/支付编号模糊搜索）")
                                                    @RequestParam(required = false) String keyword) {
        log.info("调用 adminList(), page: {}, size: {}, keyword: {}", pageParam.getPage(), pageParam.getSize(), keyword);
        var page = paymentService.pageAll(pageParam, keyword);
        var dtoList = page.getRecords()
                .stream()
                .map(paymentService::toDTO)
                .collect(Collectors.toList());
        PageResult<PaymentDTO> result = new PageResult<>();
        result.setList(dtoList);
        result.copyPageInfo(page);
        return Result.success(result);
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<PaymentDTO> getByOrderNo(@AuthenticationPrincipal JwtAuthenticationToken token,
                                           @Parameter(description = "订单号") @PathVariable String orderNo) {
        log.info("调用 getByOrderNo()");
        return Result.success(paymentService.toDTO(paymentService.getByOrderNo(token.getUserId(), orderNo)));
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<PaymentDTO>> listMyPayments(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(paymentService.listByUser(token.getUserId()).stream().map(paymentService::toDTO).collect(Collectors.toList()));
    }

    private <T> T requireBody(T body) {
        if (body == null) {
            throw new BusinessException(400, "Request body cannot be empty");
        }
        return body;
    }

}
