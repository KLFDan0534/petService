package com.pet.finance.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.finance.entity.Transaction;
import com.pet.security.JwtAuthenticationToken;
import com.pet.finance.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "财务管理", description = "交易流水")
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * 获取当前用户的交易流水
     * @param token 当前用户认证信息
     * @return 交易流水列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/me")
    @Operation(summary = "获取我的流水")
    @PreAuthorize("isAuthenticated()")
    public Result<List<Transaction>> listMyTransactions(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 listMyTransactions()");
        return Result.success(transactionService.listByUser(token.getUserId()));
    }

    /**
     * 管理员获取全部交易流水
     * @return 交易流水列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @Operation(summary = "获取全部流水")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Transaction>> listAll() {
        log.info("调用 listAll()");
        return Result.success(transactionService.listAll());
    }
}
