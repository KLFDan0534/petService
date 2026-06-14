package com.pet.module.statistics.controller;

import com.pet.common.Result;
import com.pet.module.statistics.service.StatisticsService;
import com.pet.security.JwtAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getAdminDashboard() {
        return Result.success(statisticsService.getAdminDashboard());
    }

    @GetMapping("/merchant")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<Map<String, Object>> getMerchantDashboard(
            @AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(statisticsService.getMerchantDashboard(token.getUserId()));
    }
}
