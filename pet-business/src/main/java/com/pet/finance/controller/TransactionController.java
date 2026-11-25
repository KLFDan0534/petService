package com.pet.finance.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.finance.dto.TransactionDTO;
import com.pet.finance.entity.Transaction;
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

    /**
     * 业务作用：注入 TransactionService 供用户侧与管理员侧流水查询使用。
     * 注意事项：本控制器只提供只读查询，不涉及资金变更。
     */
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
    @Operation(summary = "获取全部流水", description = "管理员获取全部交易流水记录（可选按类型过滤，如 recharge/admin_adjust）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回全部交易流水列表"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<TransactionDTO>> listAll(@RequestParam(required = false) String type) {
        log.info("调用 listAll(), type={}", type);
        // type 为空时返回全部类型流水
        return Result.success(transactionService.listAllByType(type).stream().map(transactionService::toDTO).collect(Collectors.toList()));
    }

    /**
     * 分页获取全部交易流水（管理员对账）
     *
     * <p>API: GET /api/transactions/page</p>
     * <p>为什么新增：流水表随时间无限增长，原 GET /api/transactions 全量返回，
     * 后台打开一次就把整表拉进内存。新端点走 SQL 分页。</p>
     *
     * @param type 交易类型，可为空
     * @param page 页码，从 1 开始
     * @param size 每页条数（建议 20/50/100）
     * @return 分页流水
     */
    @GetMapping("/page")
    @Operation(summary = "分页获取全部流水", description = "管理员对账用，支持按类型过滤，size 上限 100")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回分页流水"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<TransactionDTO>> pageAll(@RequestParam(required = false) String type,
                                                      @RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "20") Integer size) {
        PageResult<Transaction> source = transactionService.pageAllByType(type, page, size);
        PageResult<TransactionDTO> result = new PageResult<>();
        result.setList(source.getList().stream().map(transactionService::toDTO).toList());
        result.setTotal(source.getTotal());
        result.setPage(source.getPage());
        result.setSize(source.getSize());
        result.setPages(source.getPages());
        return Result.success(result);
    }
}
