package com.pet.finance.controller;

import com.pet.common.PageRequestDTO;
import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.finance.dto.WithdrawalApplyRequestDTO;
import com.pet.finance.dto.WithdrawalDTO;
import com.pet.finance.dto.WithdrawalReviewRequestDTO;
import com.pet.finance.service.WithdrawalService;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/withdrawals")
@Tag(name = "【用户端】提现管理", description = "用户提现申请和审核管理（用户申请/管理员审核）")
@Slf4j
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    /**
     * 【构造提现控制器】
     *
     * 业务作用：注入提现业务服务，供各 REST 接口调用。
     *
     * 调用场景：Spring 容器启动创建 Bean 时调用。
     *
     * 调用链：Spring 容器 → 本构造器 → 赋值 withdrawalService 字段。
     *
     * 数据处理：无。
     *
     * 业务规则：依赖为 final 字段。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：无。
     */
    public WithdrawalController(WithdrawalService withdrawalService) {
        this.withdrawalService = withdrawalService;
    }

    /**
     * 【查询我的提现记录】
     *
     * 业务作用：返回当前登录用户的提现申请记录。
     *
     * 调用场景：用户在钱包页查看提现历史。
     *
     * 调用链：GET /api/withdrawals/me → listMyWithdrawals() → withdrawalService.listByUser() → withdrawalService.toDTO()。
     *
     * 数据处理：以当前用户 ID 查询提现实体列表，逐条转换为 DTO 后返回。
     *
     * 业务规则：需登录（isAuthenticated）。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：未登录返回 401。
     *
     * 注意事项：不做分页，返回该用户全部提现记录。
     */
    @GetMapping("/me")
    @Operation(summary = "获取我的提现记录", description = "获取当前用户的提现申请记录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PreAuthorize("isAuthenticated()")
    public Result<List<WithdrawalDTO>> listMyWithdrawals(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 listMyWithdrawals()");
        return Result.success(withdrawalService.listByUser(token.getUserId())
                .stream()
                .map(withdrawalService::toDTO)
                .collect(Collectors.toList()));
    }

    /**
     * 【管理端分页查询提现记录】
     *
     * 业务作用：管理员分页查看所有提现申请。
     *
     * 调用场景：后台提现审核列表页。
     *
     * 调用链：GET /api/withdrawals → listAll() → withdrawalService.listPage() → withdrawalService.toDTO() → PageResult.copyPageInfo()。
     *
     * 数据处理：先按分页参数查询实体分页对象，把记录逐条转 DTO 后组装成 PageResult 并复制分页信息。
     *
     * 业务规则：仅 ADMIN 角色可访问。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：非管理员由 @PreAuthorize 拦截返回 403。
     *
     * 注意事项：分页信息通过 copyPageInfo 从原分页对象复制，不要手工赋值总数。
     */
    @GetMapping
    @Operation(summary = "获取所有提现记录", description = "管理员获取所有提现申请记录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<WithdrawalDTO>> listAll(PageRequestDTO pageParam) {
        log.info("调用 listAll()");
        var page = withdrawalService.listPage(pageParam);
        var dtoList = page.getRecords()
                .stream()
                .map(withdrawalService::toDTO)
                .collect(Collectors.toList());
        // 分页信息从原分页对象复制，不手工算总数
        PageResult<WithdrawalDTO> result = new PageResult<>();
        result.setList(dtoList);
        result.copyPageInfo(page);
        return Result.success(result);
    }

    /**
     * 【申请提现】
     *
     * 业务作用：用户提交一笔提现申请，指定金额与收款银行卡信息。
     *
     * 调用场景：用户在钱包页发起提现。
     *
     * 调用链：POST /api/withdrawals → apply() → withdrawalService.apply() → withdrawalService.toDTO()。
     *
     * 数据处理：取请求体中的金额、开户行、卡号、户名，连同当前用户 ID 一起交给服务层。
     *
     * 业务规则：需登录；金额与卡信息由服务层校验。
     *
     * 状态影响：由服务层新增提现记录并冻结相应金额。
     *
     * 异常情况：金额非法、可用余额不足等由服务层抛 BusinessException(400)。
     *
     * 注意事项：申请人身份取自 token，不接受请求体传入。
     */
    @PostMapping
    @Operation(summary = "申请提现", description = "用户提交提现申请")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PreAuthorize("isAuthenticated()")
    public Result<WithdrawalDTO> apply(@AuthenticationPrincipal JwtAuthenticationToken token,
                                       @RequestBody WithdrawalApplyRequestDTO body) {
        log.info("调用 apply()");
        return Result.success(withdrawalService.toDTO(withdrawalService.apply(
                token.getUserId(),
                body.getAmount_wsh(),
                body.getBank_name_wsh(),
                body.getBank_card_wsh(),
                body.getAccount_name_wsh())));
    }

    /**
     * 【审批通过提现】
     *
     * 业务作用：管理员把提现申请审核为通过。
     *
     * 调用场景：财务核对收款信息后在后台点击通过。
     *
     * 调用链：POST /api/withdrawals/{id}/approve → approve() → withdrawalService.approve() → withdrawalService.toDTO()。
     *
     * 数据处理：请求体可选，存在时取其 remark_wsh 作为审核备注。
     *
     * 业务规则：仅 ADMIN 可操作；状态机由服务层校验。
     *
     * 状态影响：提现记录状态推进为已通过。
     *
     * 异常情况：提现不存在抛 404；状态不允许由服务层抛 400。
     *
     * 注意事项：请求体为 null 时备注传 null，不会报错。
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "审批通过提现", description = "管理员审批通过提现申请")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<WithdrawalDTO> approve(@PathVariable @Parameter(description = "提现ID") Long id,
                                         @RequestBody(required = false) WithdrawalReviewRequestDTO body) {
        log.info("调用 approve()");
        return Result.success(withdrawalService.toDTO(
                withdrawalService.approve(id, body != null ? body.getRemark_wsh() : null)));
    }

    /**
     * 【驳回提现】
     *
     * 业务作用：管理员驳回提现申请。
     *
     * 调用场景：收款信息有误或风控拦截时。
     *
     * 调用链：POST /api/withdrawals/{id}/reject → reject() → withdrawalService.reject() → withdrawalService.toDTO()。
     *
     * 数据处理：请求体可选，存在时取其 remark_wsh 作为驳回原因。
     *
     * 业务规则：仅 ADMIN 可操作。
     *
     * 状态影响：提现记录状态变为已驳回，服务层会解冻对应金额。
     *
     * 异常情况：提现不存在抛 404；状态不允许由服务层抛 400。
     *
     * 注意事项：与 approve 结构完全对称，仅调用的服务方法不同。
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "拒绝提现", description = "管理员拒绝提现申请")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<WithdrawalDTO> reject(@PathVariable @Parameter(description = "提现ID") Long id,
                                        @RequestBody(required = false) WithdrawalReviewRequestDTO body) {
        log.info("调用 reject()");
        return Result.success(withdrawalService.toDTO(
                withdrawalService.reject(id, body != null ? body.getRemark_wsh() : null)));
    }

    /**
     * 【完成提现】
     *
     * 业务作用：管理员把已通过的提现标记为已完成（打款结束）。
     *
     * 调用场景：线下转账完成后在后台确认。
     *
     * 调用链：POST /api/withdrawals/{id}/complete → complete() → withdrawalService.complete() → withdrawalService.toDTO()。
     *
     * 数据处理：仅需提现 ID，无请求体。
     *
     * 业务规则：仅 ADMIN 可操作。
     *
     * 状态影响：提现记录状态变为已完成，服务层扣减冻结金额。
     *
     * 异常情况：提现不存在抛 404；状态不允许由服务层抛 400。
     *
     * 注意事项：该方法不做实际打款，仅做账务状态流转。
     */
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "完成提现", description = "管理员标记提现为已完成")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<WithdrawalDTO> complete(@PathVariable @Parameter(description = "提现ID") Long id) {
        log.info("调用 complete()");
        return Result.success(withdrawalService.toDTO(withdrawalService.complete(id)));
    }
}
