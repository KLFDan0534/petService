package com.pet.operation.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.PageParam;
import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.operation.entity.ContentReview;
import com.pet.operation.service.ContentReviewService;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Content Review", description = "Content review management")
@Slf4j
public class ContentReviewController {

    private final ContentReviewService contentReviewService;

    public ContentReviewController(ContentReviewService contentReviewService) {
        this.contentReviewService = contentReviewService;
    }

    /**
     * 分页查询待审核内容
     * @param pageParam 分页参数
     * @return 分页待审核内容列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/pending")
    @Operation(summary = "List pending reviews")
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER_SERVICE')")
    public Result<PageResult<ContentReview>> listPending(PageParam pageParam) {
        log.info("listPending() called");
        return Result.success(new PageResult<>(contentReviewService.listPendingPage(pageParam)));
    }

    /**
     * 分页查询所有审核记录
     * @param pageParam 分页参数
     * @return 分页审核记录列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @Operation(summary = "List all reviews")
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER_SERVICE')")
    public Result<PageResult<ContentReview>> listAll(PageParam pageParam) {
        log.info("listAll() called");
        return Result.success(new PageResult<>(contentReviewService.listPage(pageParam)));
    }

    /**
     * 举报内容
     * @param token 当前用户认证信息
     * @param body 请求体，包含target_type_wsh/target_id_wsh/reason_wsh
     * @return 创建的审核记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping
    @Operation(summary = "Report content")
    @PreAuthorize("isAuthenticated()")
    public Result<ContentReview> report(@AuthenticationPrincipal JwtAuthenticationToken token,
                                        @RequestBody Map<String, Object> body) {
        log.info("report() called");
        String targetType = (String) body.get("target_type_wsh");
        Long targetId = Long.valueOf(body.get("target_id_wsh").toString());
        String reason = (String) body.get("reason_wsh");
        return Result.success(contentReviewService.report(targetType, targetId, token.getUserId(), reason));
    }

    /**
     * 审核通过内容
     * @param token 当前用户认证信息
     * @param id 审核记录ID
     * @param body 请求体，可包含remark_wsh备注
     * @return 更新后的审核记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER_SERVICE')")
    @Operation(summary = "Approve review")
    public Result<ContentReview> approve(@AuthenticationPrincipal JwtAuthenticationToken token,
                                         @PathVariable Long id,
                                         @RequestBody(required = false) Map<String, String> body) {
        log.info("approve() called");
        return Result.success(contentReviewService.approve(id, token.getUserId(),
                body != null ? body.get("remark_wsh") : null));
    }

    /**
     * 驳回内容审核
     * @param token 当前用户认证信息
     * @param id 审核记录ID
     * @param body 请求体，可包含remark_wsh驳回原因
     * @return 更新后的审核记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER_SERVICE')")
    @Operation(summary = "Reject review")
    public Result<ContentReview> reject(@AuthenticationPrincipal JwtAuthenticationToken token,
                                        @PathVariable Long id,
                                        @RequestBody(required = false) Map<String, String> body) {
        log.info("reject() called");
        return Result.success(contentReviewService.reject(id, token.getUserId(),
                body != null ? body.get("remark_wsh") : null));
    }
}