package com.pet.module.ai.controller;

import com.pet.common.Result;
import com.pet.module.ai.entity.AiReport;
import com.pet.module.ai.service.AiReportService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiReportController {

    private final AiReportService aiReportService;

    public AiReportController(AiReportService aiReportService) {
        this.aiReportService = aiReportService;
    }

    @GetMapping("/reports/order/{orderId}")
    public Result<List<AiReport>> getByOrder(@PathVariable Long orderId) {
        return Result.success(aiReportService.getReportsByOrder(orderId));
    }

    @GetMapping("/reports/pet/{petId}")
    public Result<List<AiReport>> getByPet(@PathVariable Long petId) {
        return Result.success(aiReportService.getReportsByPet(petId));
    }

    @PostMapping("/reports")
    public Result<AiReport> create(@RequestBody AiReport report) {
        return Result.success(aiReportService.createReport(report));
    }

    @PostMapping("/care-suggestion")
    public Result<AiReport> generateCareSuggestion(
            @RequestParam Long petId,
            @RequestParam Long keeperId,
            @RequestParam Long orderId) {
        return Result.success(aiReportService.generateCareSuggestion(petId, keeperId, orderId));
    }

    @PostMapping("/boarding-report")
    public Result<AiReport> generateBoardingReport(
            @RequestParam Long petId,
            @RequestParam Long keeperId,
            @RequestParam Long orderId) {
        return Result.success(aiReportService.generateBoardingReport(petId, keeperId, orderId));
    }
}
