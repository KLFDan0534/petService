package com.pet.module.statistics.controller;

import com.pet.common.Result;
import com.pet.module.statistics.service.StatisticsService;
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
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员数据看板", description = "获取管理员平台数据统计看板")
    public Result<Map<String, Object>> getAdminDashboard() {
        return Result.success(statisticsService.getAdminDashboard());
    }

    @GetMapping("/merchant")
    @PreAuthorize("hasRole('MERCHANT')")
    @Operation(summary = "商家数据看板", description = "获取商家的数据统计看板")
    public Result<Map<String, Object>> getMerchantDashboard(
            @AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(statisticsService.getMerchantDashboard(token.getUserId()));
    }
}
