package com.pet.finance.controller;

import com.pet.common.BusinessException;
import com.pet.common.PageRequestDTO;
import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.finance.dto.RechargeRequestDTO;
import com.pet.finance.dto.WalletAdjustRequestDTO;
import com.pet.finance.dto.WalletDTO;
import com.pet.finance.service.AccountingService;
import com.pet.finance.service.WalletService;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/wallet")
@Tag(name = "【用户端】钱包管理", description = "用户钱包和余额管理（用户查询/管理员调整）")
@Slf4j
public class WalletController {

    private final WalletService walletService;
    private final AccountingService accountingService;

    public WalletController(WalletService walletService, AccountingService accountingService) {
        this.walletService = walletService;
        this.accountingService = accountingService;
    }

    @GetMapping("/me")
    @Operation(summary = "获取我的钱包", description = "获取当前用户的钱包信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回钱包信息"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PreAuthorize("isAuthenticated()")
    public Result<WalletDTO> getMyWallet(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 getMyWallet()");
        return Result.success(walletService.toDTO(walletService.getByUserId(token.getUserId())));
    }

    /**
     * 用户充值
     * <p>校验金额后入账到用户钱包余额，并记录充值流水。使用幂等请求 ID 防止重复提交。</p>
     *
     * @param token 当前用户认证信息
     * @param body  充值请求（金额 + 可选幂等请求 ID）
     * @return 充值后的钱包信息
     */
    @PostMapping("/recharge")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "用户充值", description = "校验金额后入账到钱包余额，支持幂等防止重复提交")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "充值成功返回更新后的钱包"),
            @ApiResponse(responseCode = "400", description = "请求参数错误或金额非法")
    })
    public Result<WalletDTO> recharge(@AuthenticationPrincipal JwtAuthenticationToken token,
                                      @RequestBody RechargeRequestDTO body) {
        log.info("调用 recharge()");
        body = requireBody(body);
        String requestId = body.getRequest_id_wsh() == null || body.getRequest_id_wsh().isBlank()
                ? "recharge:" + token.getUserId() + ":" + System.nanoTime()
                : body.getRequest_id_wsh().trim();
        return Result.success(walletService.toDTO(accountingService.credit(
                token.getUserId(), body.getAmount_wsh(), "recharge", null,
                "wallet_recharge", String.valueOf(token.getUserId()), requestId, "用户余额充值")));
    }

    private <T> T requireBody(T body) {
        if (body == null) {
            throw new BusinessException(400, "Request body cannot be empty");
        }
        return body;
    }

    @GetMapping
    @Operation(summary = "获取钱包列表", description = "管理员获取全部用户钱包信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回钱包列表"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<WalletDTO>> listAll(PageRequestDTO pageParam) {
        log.info("调用 listAll()");
        var page = walletService.listPage(pageParam);
        PageResult<WalletDTO> result = new PageResult<>();
        result.setList(page.getRecords());
        result.copyPageInfo(page);
        return Result.success(result);
    }

    @PostMapping("/admin/adjust")
    @Operation(summary = "管理员调整余额", description = "管理员调整用户钱包余额（支持设置、增加、扣减）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "调整成功返回更新后的钱包信息"),
            @ApiResponse(responseCode = "400", description = "请求参数错误或不支持的调整方式"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public Result<WalletDTO> adjustBalance(@AuthenticationPrincipal JwtAuthenticationToken token,
                                           @RequestBody WalletAdjustRequestDTO body) {
        log.info("调用 adjustBalance()");
        String requestId = body.getRequest_id_wsh() == null || body.getRequest_id_wsh().isBlank()
                ? "admin-wallet:" + token.getUserId() + ":" + body.getUser_id_wsh() + ":" + System.nanoTime()
                : body.getRequest_id_wsh().trim();
        String mode = body.getMode_wsh();
        if ("set".equals(mode)) {
            return Result.success(walletService.toDTO(accountingService.setBalanceByAdmin(
                    token.getUserId(), body.getUser_id_wsh(), body.getBalance_wsh(), requestId, body.getRemark_wsh())));
        }
        if ("add".equals(mode)) {
            return Result.success(walletService.toDTO(accountingService.credit(
                    body.getUser_id_wsh(), body.getAmount_wsh(), "admin_adjust", null,
                    "wallet_admin", String.valueOf(body.getUser_id_wsh()), requestId, body.getRemark_wsh(),
                    token.getUserId())));
        }
        if ("subtract".equals(mode)) {
            return Result.success(walletService.toDTO(accountingService.debit(
                    body.getUser_id_wsh(), body.getAmount_wsh(), "admin_adjust", null,
                    "wallet_admin", String.valueOf(body.getUser_id_wsh()), requestId, body.getRemark_wsh(),
                    token.getUserId())));
        }
        throw new BusinessException(400, "不支持的调整方式");
    }
}
