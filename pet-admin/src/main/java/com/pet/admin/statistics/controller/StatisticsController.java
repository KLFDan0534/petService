package com.pet.admin.statistics.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.admin.statistics.service.StatisticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.pet.admin.statistics.vo.AdminDashboardVO;
import com.pet.admin.statistics.vo.MerchantDashboardVO;
import com.pet.admin.statistics.vo.ReputationStatsVO;
import com.pet.admin.statistics.vo.UserDashboardVO;
import java.lang.reflect.Method;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/statistics")
@Tag(name = "【后台管理】数据统计", description = "平台数据统计和看板展示（管理员/商家查看）")
@Slf4j
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * 获取管理员平台数据统计看板
     * @return 管理台数据统计
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员数据看板", description = "获取管理员平台数据统计看板")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<AdminDashboardVO> getAdminDashboard() {
        // TODO 前端返回数据太久了,需要优化
        log.info("调用 getAdminDashboard()");
        return Result.success(statisticsService.getAdminDashboard());
    }

    /**
     * 获取用户个人数据统计看板
     * @param token 当前用户认证信息
     * @return 用户个人数据统计
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "用户数据看板", description = "获取用户个人数据统计看板")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<UserDashboardVO> getUserDashboard(
            @AuthenticationPrincipal Object principal) {
        log.info("调用 getUserDashboard()");
        return Result.success(statisticsService.getUserDashboard(resolveUserId(principal)));
    }

    /**
     * 获取商家数据统计看板
     * @param token 当前用户认证信息
     * @return 商家数据统计
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/merchant")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
    @Operation(summary = "商家数据看板", description = "获取商家的数据统计看板")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<MerchantDashboardVO> getMerchantDashboard(
            @AuthenticationPrincipal Object principal) {
        log.info("调用 getMerchantDashboard()");
        return Result.success(statisticsService.getMerchantDashboard(resolveUserId(principal)));
    }

    @GetMapping("/reputation")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "信誉统计", description = "按商家或看护员统计评价、订单、投诉和打赏数据")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ReputationStatsVO> getReputationStats(@Parameter(description = "统计目标类型（merchant/keeper）") @RequestParam String targetType,
                                                        @Parameter(description = "统计目标ID") @RequestParam Long targetId) {
        return Result.success(statisticsService.getReputationStats(targetType, targetId));
    }

    private Long resolveUserId(Object principal) {
        Object candidate = principal instanceof Authentication authentication
                ? authentication.getPrincipal()
                : principal;
        if (candidate == null) {
            throw new IllegalStateException("Authenticated principal is missing");
        }
        try {
            Method method = candidate.getClass().getMethod("getUserId");
            Object value = method.invoke(candidate);
            if (value instanceof Long userId) return userId;
            if (value instanceof Number number) return number.longValue();
            if (value instanceof String text) return Long.parseLong(text);
        } catch (ReflectiveOperationException | NumberFormatException e) {
            throw new IllegalStateException("Authenticated principal does not expose a numeric userId", e);
        }
        throw new IllegalStateException("Authenticated principal does not expose a numeric userId");
    }
}
