package com.pet.finance.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.finance.entity.Wallet;
import com.pet.security.JwtAuthenticationToken;
import com.pet.finance.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@Tag(name = "财务管理", description = "钱包/余额管理")
@Slf4j
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * 获取当前用户的钱包信息
     * @param token 当前用户认证信息
     * @return 钱包信息
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/me")
    @Operation(summary = "获取我的钱包")
    @PreAuthorize("isAuthenticated()")
    public Result<Wallet> getMyWallet(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 getMyWallet()");
        return Result.success(walletService.getByUserId(token.getUserId()));
    }

    /**
     * 管理员获取所有钱包信息
     * @return 钱包列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @Operation(summary = "获取所有钱包")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Wallet>> listAll() {
        log.info("调用 listAll()");
        return Result.success(walletService.listAll());
    }
}
