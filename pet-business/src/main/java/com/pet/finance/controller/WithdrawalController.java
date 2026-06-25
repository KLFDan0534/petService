package com.pet.finance.controller;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageParam;
import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.finance.entity.Withdrawal;
import com.pet.security.JwtAuthenticationToken;
import com.pet.finance.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/withdrawals")
@Tag(name = "财务管理", description = "提现管理")
@Slf4j
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    public WithdrawalController(WithdrawalService withdrawalService) {
        this.withdrawalService = withdrawalService;
    }

    /**
     * 获取当前用户的提现记录
     * @param token 当前用户认证信息
     * @return 提现记录列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/me")
    @Operation(summary = "获取我的提现记录")
    @PreAuthorize("isAuthenticated()")
    public Result<List<Withdrawal>> listMyWithdrawals(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 listMyWithdrawals()");
        return Result.success(withdrawalService.listByUser(token.getUserId()));
    }

    /**
     * 管理员分页查询全部提现记录
     * @param pageParam 分页参数
     * @return 分页提现记录列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @Operation(summary = "获取所有提现记录")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<Withdrawal>> listAll(PageParam pageParam) {
        log.info("调用 listAll()");
        return Result.success(new PageResult<>(withdrawalService.listPage(pageParam)));
    }

    /**
     * 申请提现
     * @param token 当前用户认证信息
     * @param body 请求体，包含amount_wsh/bank_name_wsh/bank_card_wsh/account_name_wsh
     * @return 创建的提现记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping
    @Operation(summary = "申请提现")
    @PreAuthorize("isAuthenticated()")
    public Result<Withdrawal> apply(@AuthenticationPrincipal JwtAuthenticationToken token, @RequestBody Map<String, Object> body) {
        log.info("调用 apply()");
        BigDecimal amount = new BigDecimal(body.getOrDefault("amount_wsh", "0").toString());
        String bankName = (String) body.getOrDefault("bank_name_wsh", "");
        String bankCard = (String) body.getOrDefault("bank_card_wsh", "");
        String accountName = (String) body.getOrDefault("account_name_wsh", "");
        return Result.success(withdrawalService.apply(token.getUserId(), amount, bankName, bankCard, accountName));
    }

    /**
     * 管理员审核通过提现
     * @param id 提现ID
     * @param body 请求体，可包含remark_wsh备注
     * @return 更新后的提现记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "审核通过提现")
    public Result<Withdrawal> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        log.info("调用 approve()");
        return Result.success(withdrawalService.approve(id, body != null ? body.get("remark_wsh") : null));
    }

    /**
     * 管理员驳回提现申请
     * @param id 提现ID
     * @param body 请求体，可包含remark_wsh驳回原因
     * @return 更新后的提现记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "驳回提现")
    public Result<Withdrawal> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        log.info("调用 reject()");
        return Result.success(withdrawalService.reject(id, body != null ? body.get("remark_wsh") : null));
    }
}
