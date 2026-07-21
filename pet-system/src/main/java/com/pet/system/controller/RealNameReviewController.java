package com.pet.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.common.annotation.LogOperation;
import com.pet.security.JwtAuthenticationToken;
import com.pet.system.dto.RealNameReviewRequestDTO;
import com.pet.system.service.RealNameReviewService;
import com.pet.system.vo.RealNameReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/real-name-reviews")
@Tag(name = "【后台管理】实名审核", description = "管理员审核用户实名认证")
@Slf4j
public class RealNameReviewController {

    private final RealNameReviewService realNameReviewService;

    public RealNameReviewController(RealNameReviewService realNameReviewService) {
        this.realNameReviewService = realNameReviewService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "分页查询实名认证审核列表", description = "管理员分页查询实名认证审核记录，可按状态和关键词筛选")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<PageResult<RealNameReviewVO>> listAll(PageRequestDTO pageParam,
                                                        @RequestParam(required = false) Integer status,
                                                        @RequestParam(required = false) String q) {
        log.info("call listAll(), status={}, q={}", status, q);
        IPage<RealNameReviewVO> page = realNameReviewService.listPage(pageParam, status, q);
        PageResult<RealNameReviewVO> result = new PageResult<>();
        result.copyPageInfo(page);
        result.setList(page.getRecords());
        return Result.success(result);
    }

    @PostMapping("/{userId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @LogOperation(module = "real-name-review", operation = "approve", description = "Approve real-name verification")
    @Operation(summary = "审核通过实名认证", description = "管理员审核通过用户的实名认证申请")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> approve(@AuthenticationPrincipal JwtAuthenticationToken token,
                                @Parameter(description = "用户ID") @PathVariable Long userId,
                                @RequestBody(required = false) RealNameReviewRequestDTO body) {
        log.info("call approve(), userId={}", userId);
        realNameReviewService.approve(userId, token.getUserId(), body != null ? body.getRemark_wsh() : null);
        return Result.success();
    }

    @PostMapping("/{userId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @LogOperation(module = "real-name-review", operation = "reject", description = "Reject real-name verification")
    @Operation(summary = "驳回实名认证", description = "管理员驳回用户的实名认证申请")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> reject(@AuthenticationPrincipal JwtAuthenticationToken token,
                               @Parameter(description = "用户ID") @PathVariable Long userId,
                               @RequestBody(required = false) RealNameReviewRequestDTO body) {
        log.info("call reject(), userId={}", userId);
        realNameReviewService.reject(userId, token.getUserId(), body != null ? body.getRemark_wsh() : null);
        return Result.success();
    }
}
