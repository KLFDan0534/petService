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

    /**
     * 分页查询实名认证审核列表
     *
     * <p>API: GET /api/admin/real-name-reviews</p>
     * <p>请求来源：后台管理实名审核列表页，管理员查看和筛选实名认证记录</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：
     * <ul>
     *   <li>@query PageRequestDTO - 分页参数</li>
     *   <li>@query status - 审核状态筛选（可选）</li>
     *   <li>@query q - 搜索关键词（可选），模糊匹配用户信息</li>
     * </ul>
     * </p>
     * <p>返回数据：PageResult&lt;RealNameReviewVO&gt; - 分页的实名审核记录</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 分页参数错误</li>
     *   <li>403 - 非管理员无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
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
        log.info("调用 listAll(), status={}, q={}", status, q);
        IPage<RealNameReviewVO> page = realNameReviewService.listPage(pageParam, status, q);
        PageResult<RealNameReviewVO> result = new PageResult<>();
        result.copyPageInfo(page);
        result.setList(page.getRecords());
        return Result.success(result);
    }

    /**
     * 审核通过实名认证
     *
     * <p>API: POST /api/admin/real-name-reviews/{userId}/approve</p>
     * <p>请求来源：后台管理实名审核详情页，管理员点击"通过审核"</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：
     * <ul>
     *   <li>@path userId - 申请实名认证的用户ID</li>
     *   <li>@body RealNameReviewRequestDTO - 可选，包含审核备注remark_wsh</li>
     * </ul>
     * </p>
     * <p>返回数据：无（Result.success()），用户real_name_status_wsh更新为APPROVED</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 用户不存在或申请状态异常</li>
     *   <li>403 - 非管理员无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
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
        log.info("调用 approve(), userId={}", userId);
        realNameReviewService.approve(userId, token.getUserId(), body != null ? body.getRemark_wsh() : null);
        return Result.success();
    }

    /**
     * 驳回实名认证
     *
     * <p>API: POST /api/admin/real-name-reviews/{userId}/reject</p>
     * <p>请求来源：后台管理实名审核详情页，管理员点击"驳回"</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：
     * <ul>
     *   <li>@path userId - 申请实名认证的用户ID</li>
     *   <li>@body RealNameReviewRequestDTO - 可选，包含驳回原因remark_wsh</li>
     * </ul>
     * </p>
     * <p>返回数据：无（Result.success()），用户real_name_status_wsh更新为REJECTED</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 用户不存在或申请状态异常</li>
     *   <li>403 - 非管理员无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
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
        log.info("调用 reject(), userId={}", userId);
        realNameReviewService.reject(userId, token.getUserId(), body != null ? body.getRemark_wsh() : null);
        return Result.success();
    }
}
