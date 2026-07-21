package com.pet.operation.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.PageRequestDTO;
import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.operation.dto.ContentReviewDTO;
import com.pet.operation.entity.ContentReview;
import com.pet.operation.service.ContentReviewService;
import com.pet.security.JwtAuthenticationToken;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import com.pet.operation.dto.ContentReviewReportRequestDTO;
import com.pet.operation.dto.ContentReviewReviewRequestDTO;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "【后台管理】内容审核管理", description = "用户举报和内容审核管理（管理员审核）")
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
    @Operation(summary = "获取待审核列表", description = "分页查询待审核内容")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<PageResult<ContentReviewDTO>> listPending(PageRequestDTO pageParam) {
        log.info("listPending() called");
        return Result.success(new PageResult<>(toDTOPage(contentReviewService.listPendingPage(pageParam))));
    }

    /**
     * 分页查询所有审核记录
     * @param pageParam 分页参数
     * @return 分页审核记录列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @Operation(summary = "获取所有审核记录", description = "分页查询所有审核记录")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<PageResult<ContentReviewDTO>> listAll(PageRequestDTO pageParam) {
        log.info("listAll() called");
        return Result.success(new PageResult<>(toDTOPage(contentReviewService.listPage(pageParam))));
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
    @Operation(summary = "举报内容", description = "用户举报违规内容")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ContentReviewDTO> report(@AuthenticationPrincipal JwtAuthenticationToken token,
                                           @Valid @RequestBody ContentReviewReportRequestDTO body) {
        log.info("report() called");
        return Result.success(toDTO(contentReviewService.report(body.getTarget_type_wsh(), body.getTarget_id_wsh(), token.getUserId(), body.getReason_wsh())));
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
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "审核通过", description = "管理员审核通过内容")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "404", description = "资源不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ContentReviewDTO> approve(@AuthenticationPrincipal JwtAuthenticationToken token,
                                            @Parameter(description = "审核记录ID") @PathVariable Long id,
                                            @RequestBody(required = false) ContentReviewReviewRequestDTO body) {
        log.info("approve() called");
        return Result.success(toDTO(contentReviewService.approve(id, token.getUserId(),
                body != null ? body.getRemark_wsh() : null)));
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
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "驳回审核", description = "管理员驳回内容审核")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "404", description = "资源不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ContentReviewDTO> reject(@AuthenticationPrincipal JwtAuthenticationToken token,
                                           @Parameter(description = "审核记录ID") @PathVariable Long id,
                                           @RequestBody(required = false) ContentReviewReviewRequestDTO body) {
        log.info("reject() called");
        return Result.success(toDTO(contentReviewService.reject(id, token.getUserId(),
                body != null ? body.getRemark_wsh() : null)));
    }

    private ContentReviewDTO toDTO(ContentReview entity) {
        ContentReviewDTO dto = new ContentReviewDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setTarget_type_wsh(entity.getTarget_type_wsh());
        dto.setTarget_id_wsh(entity.getTarget_id_wsh());
        dto.setReporter_id_wsh(entity.getReporter_id_wsh());
        dto.setReason_wsh(entity.getReason_wsh());
        dto.setStatus_wsh(entity.getStatus_wsh());
        dto.setReviewer_id_wsh(entity.getReviewer_id_wsh());
        dto.setReview_remark_wsh(entity.getReview_remark_wsh());
        dto.setCreated_at_wsh(entity.getCreated_at_wsh());
        return dto;
    }

    private com.baomidou.mybatisplus.core.metadata.IPage<ContentReviewDTO> toDTOPage(com.baomidou.mybatisplus.core.metadata.IPage<ContentReview> page) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ContentReviewDTO> dtoPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        dtoPage.setRecords(page.getRecords().stream().map(this::toDTO).collect(Collectors.toList()));
        return dtoPage;
    }
}
