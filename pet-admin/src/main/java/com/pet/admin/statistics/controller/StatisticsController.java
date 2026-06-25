package com.pet.admin.statistics.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.admin.statistics.service.StatisticsService;
import com.pet.security.JwtAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/statistics")
@Tag(name = "数据统计", description = "平台数据统计和看板展示")
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
    public Result<Map<String, Object>> getAdminDashboard() {
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
    public Result<Map<String, Object>> getUserDashboard(
            @AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 getUserDashboard()");
        return Result.success(statisticsService.getUserDashboard(token.getUserId()));
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
    public Result<Map<String, Object>> getMerchantDashboard(
            @AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 getMerchantDashboard()");
        return Result.success(statisticsService.getMerchantDashboard(token.getUserId()));
    }
}