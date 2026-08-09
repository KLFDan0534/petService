package com.pet.finance.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.finance.dto.TransactionDTO;
import com.pet.security.JwtAuthenticationToken;
import com.pet.finance.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "【用户端】交易流水", description = "交易流水查询（用户查看/管理员查询）")
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * 获取我的交易流水
     *
     * <p>API: GET /api/transactions/me</p>
     * <p>请求来源：前端个人中心-钱包页，用户查看自己的交易流水记录</p>
     * <p>权限要求：已登录用户（@PreAuthorize("isAuthenticated()")）</p>
     * <p>输入参数：无（从token中提取用户ID）</p>
     * <p>返回数据：List&lt;TransactionDTO&gt; - 当前用户的交易流水列表，包含金额、类型、时间等</p>
     * <p>异常情况：
     * <ul>
     *   <li>401 - 未登录</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @GetMapping("/me")
    @Operation(summary = "获取我的流水", description = "获取当前用户的交易流水列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回交易流水列表"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PreAuthorize("isAuthenticated()")
    public Result<List<TransactionDTO>> listMyTransactions(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 listMyTransactions()");
        return Result.success(transactionService.listByUser(token.getUserId()).stream().map(transactionService::toDTO).collect(Collectors.toList()));
    }

    /**
     * 管理员获取全部交易流水
     *
     * <p>API: GET /api/transactions</p>
     * <p>请求来源：后台管理财务页，管理员查看所有用户的交易流水</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：无</p>
     * <p>返回数据：List&lt;TransactionDTO&gt; - 全平台交易流水列表</p>
     * <p>异常情况：
     * <ul>
     *   <li>403 - 非管理员无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @GetMapping
    @Operation(summary = "获取全部流水", description = "管理员获取全部交易流水记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回全部交易流水列表"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<TransactionDTO>> listAll() {
        log.info("调用 listAll()");
        return Result.success(transactionService.listAll().stream().map(transactionService::toDTO).collect(Collectors.toList()));
    }
}
