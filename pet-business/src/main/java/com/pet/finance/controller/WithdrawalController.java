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

    public WithdrawalController(WithdrawalService withdrawalService) {
        this.withdrawalService = withdrawalService;
    }

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
        log.info("Calling listMyWithdrawals()");
        return Result.success(withdrawalService.listByUser(token.getUserId())
                .stream()
                .map(withdrawalService::toDTO)
                .collect(Collectors.toList()));
    }

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
        log.info("Calling listAll()");
        var page = withdrawalService.listPage(pageParam);
        var dtoList = page.getRecords()
                .stream()
                .map(withdrawalService::toDTO)
                .collect(Collectors.toList());
        PageResult<WithdrawalDTO> result = new PageResult<>();
        result.setList(dtoList);
        result.copyPageInfo(page);
        return Result.success(result);
    }

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
        log.info("Calling apply()");
        return Result.success(withdrawalService.toDTO(withdrawalService.apply(
                token.getUserId(),
                body.getAmount_wsh(),
                body.getBank_name_wsh(),
                body.getBank_card_wsh(),
                body.getAccount_name_wsh())));
    }

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
        log.info("Calling approve()");
        return Result.success(withdrawalService.toDTO(
                withdrawalService.approve(id, body != null ? body.getRemark_wsh() : null)));
    }

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
        log.info("Calling reject()");
        return Result.success(withdrawalService.toDTO(
                withdrawalService.reject(id, body != null ? body.getRemark_wsh() : null)));
    }

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
        log.info("Calling complete()");
        return Result.success(withdrawalService.toDTO(withdrawalService.complete(id)));
    }
}
