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
     * 【业务名称】管理员数据看板（接口）
     * <p>业务作用：获取平台全局统计数据，展示管理员后台首页的核心运营指标。</p>
     * <p>调用场景：管理员登录后台首页。</p>
     * <p>调用链：前端GET /api/statistics/admin → getAdminDashboard() → StatisticsService.getAdminDashboard() → 各Service查询聚合 → 返回AdminDashboardVO</p>
     * <p>业务规则：需要ADMIN角色权限。</p>
     * <p>状态影响：只读操作。</p>
     * <p>注意事项：TODO 前端返回数据太久了，需要优化——全量查询在大数据量下存在性能瓶颈，建议后续改为增量统计或定时缓存。</p>
     */
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
     * 【业务名称】用户个人数据看板（接口）
     * <p>业务作用：获取当前登录用户的个人数据统计（宠物数、订单数、消费金额等）。</p>
     * <p>调用场景：用户在个人中心查看自己的数据概览。</p>
     * <p>调用链：前端GET /api/statistics/user → getUserDashboard() → resolveUserId()解析用户ID → StatisticsService.getUserDashboard() → 返回UserDashboardVO</p>
     * <p>数据处理：通过反射从Authentication principal中提取userId；委托service查询。</p>
     * <p>业务规则：需登录认证；仅查询本人数据。</p>
     * <p>状态影响：只读操作。</p>
     * <p>注意事项：principal可能为JwtAuthenticationToken等不同类型，通过反射调用getUserId()获取。</p>
     */
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
     * 【业务名称】商家数据看板（接口）
     * <p>业务作用：获取当前商家/管理员的店铺经营数据统计。</p>
     * <p>调用场景：商家登录后台查看自己店铺的经营概览。</p>
     * <p>调用链：前端GET /api/statistics/merchant → getMerchantDashboard() → resolveUserId() → StatisticsService.getMerchantDashboard() → 返回MerchantDashboardVO</p>
     * <p>数据处理：从principal提取userId；委托service查询。</p>
     * <p>业务规则：需要MERCHANT或ADMIN角色权限。</p>
     * <p>状态影响：只读操作。</p>
     * <p>注意事项：商户ID通过登录用户关联查询，而非直接传入merchantId。</p>
     */
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

    /**
     * 【业务名称】信誉统计（接口）
     * <p>业务作用：查询商家或看护人的信誉数据，包括评价、订单完成率、投诉率和打赏数。</p>
     * <p>调用场景：商家/看护人查看信誉评分；管理员审核商家/看护人资质。</p>
     * <p>调用链：前端GET /api/statistics/reputation → getReputationStats() → StatisticsService.getReputationStats() → 多表统计 → 返回ReputationStatsVO</p>
     * <p>业务规则：需登录认证；targetType仅支持"merchant"或"keeper"。</p>
     * <p>状态影响：只读操作。</p>
     * <p>异常情况：targetType不合法返回400错误（Service层抛出IllegalArgumentException被全局异常处理器捕获）。</p>
     * <p>注意事项：targetType和targetId均为必填Query参数。</p>
     */
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

    /**
     * 【业务名称】解析Authentication中用户ID
     * <p>业务作用：从Spring Security的Authentication对象中通过反射提取userId（Long类型），兼容JwtAuthenticationToken等多种Principal实现。</p>
     * <p>调用场景：getUserDashboard/getMerchantDashboard中从认证信息解析用户ID。</p>
     * <p>数据处理：判断principal是否为Authentication类型；通过反射调用getUserId()方法；支持Long/Number/String返回值类型。</p>
     * <p>异常情况：principal为null或无法提取userId时抛出IllegalStateException。</p>
     * <p>注意事项：使用反射而非强转以兼容不同的认证实现；Number类型自动转Long。</p>
     */
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
