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

    /**
     * 【钱包控制器构造注入】
     *
     * 业务作用：注入钱包服务与记账服务。
     *
     * 调用场景：Spring 容器创建 WalletController 时。
     *
     * 调用链：Spring 容器 → 本构造方法 → 赋值 walletService 与 accountingService。
     *
     * 数据处理：仅保存依赖引用。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：无。
     */
    public WalletController(WalletService walletService, AccountingService accountingService) {
        this.walletService = walletService;
        this.accountingService = accountingService;
    }

    /**
     * 【查询我的钱包】
     *
     * 业务作用：返回当前登录用户的钱包余额信息。
     *
     * 调用场景：用户进入钱包页面。
     *
     * 调用链：GET /api/wallet/me → getMyWallet() → walletService.getByUserId() → walletService.toDTO()。
     *
     * 数据处理：按登录用户 ID 查询钱包并转换为 DTO。
     *
     * 业务规则：需登录；钱包不存在时由服务层决定是否自动创建。
     *
     * 状态影响：无（是否初始化钱包由服务层决定）。
     *
     * 异常情况：未登录返回 401。
     *
     * 注意事项：无。
     */
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
        // 未传幂等键时按用户与时间戳兜底生成
        String requestId = body.getRequest_id_wsh() == null || body.getRequest_id_wsh().isBlank()
                ? "recharge:" + token.getUserId() + ":" + System.nanoTime()
                : body.getRequest_id_wsh().trim();
        return Result.success(walletService.toDTO(accountingService.credit(
                token.getUserId(), body.getAmount_wsh(), "recharge", null,
                "wallet_recharge", String.valueOf(token.getUserId()), requestId, "用户余额充值")));
    }

    /**
     * 【校验请求体非空】
     *
     * 业务作用：请求体为 null 时抛 400，避免后续空指针。
     *
     * 调用场景：recharge() 解析充值参数前。
     *
     * 调用链：recharge() → requireBody()。
     *
     * 数据处理：原样返回非空请求体。
     *
     * 业务规则：请求体必填。
     *
     * 状态影响：无。
     *
     * 异常情况：body 为 null 抛 BusinessException(400, "Request body cannot be empty")。
     *
     * 注意事项：泛型方法，当前仅被 recharge() 使用；adjustBalance() 未做同样的空体校验。
     */
    private <T> T requireBody(T body) {
        if (body == null) {
            throw new BusinessException(400, "Request body cannot be empty");
        }
        return body;
    }

    /**
     * 【管理员分页查询全部钱包】
     *
     * 业务作用：分页返回所有用户的钱包信息。
     *
     * 调用场景：后台钱包管理列表。
     *
     * 调用链：GET /api/wallet → listAll() → walletService.listPage() → PageResult 组装。
     *
     * 数据处理：服务层返回 IPage，取出记录并复制分页信息到 PageResult。
     *
     * 业务规则：需 ADMIN 角色。
     *
     * 状态影响：无。
     *
     * 异常情况：无权限由 @PreAuthorize 拦截返回 403。
     *
     * 注意事项：无。
     */
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

    /**
     * 【管理员调整用户余额】
     *
     * 业务作用：按 set/add/subtract 三种方式调整指定用户的钱包余额。
     *
     * 调用场景：后台人工补偿或扣减用户余额。
     *
     * 调用链：POST /api/wallet/admin/adjust → adjustBalance() → accountingService.setBalanceByAdmin() / credit() / debit() → walletService.toDTO()。
     *
     * 数据处理：优先使用请求中的 request_id_wsh 作为幂等键，为空时用操作人+目标用户+纳秒时间戳生成；按 mode 分流到对应记账方法。
     *
     * 业务规则：mode 仅支持 set、add、subtract，其余抛 400；需 ADMIN 角色。
     *
     * 状态影响：更新钱包余额并写入资金流水（由 AccountingService 完成）。
     *
     * 异常情况：mode 不支持抛 BusinessException(400, "不支持的调整方式")；余额不足等由记账服务抛出。
     *
     * 注意事项：未对请求体做 requireBody 空校验，body 为 null 会抛 NullPointerException；自动生成的幂等键含时间戳，重试时无法真正幂等。
     */
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
        // 按调整方式分流到设置、入账、扣减
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
