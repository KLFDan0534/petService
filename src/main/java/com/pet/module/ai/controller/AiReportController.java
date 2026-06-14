package com.pet.module.ai.controller;

import com.pet.common.Result;
import com.pet.module.ai.entity.AiReport;
import com.pet.module.ai.service.AiReportService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI报告管理", description = "AI生成的宠物看护报告和护理建议管理")
public class AiReportController {

    private final AiReportService aiReportService;

    public AiReportController(AiReportService aiReportService) {
        this.aiReportService = aiReportService;
    }

    @GetMapping("/reports/order/{orderId}")
    @Operation(summary = "获取订单AI报告", description = "根据订单ID获取AI生成的报告列表")
    public Result<List<AiReport>> getByOrder(@PathVariable Long orderId) {
        return Result.success(aiReportService.getReportsByOrder(orderId));
    }

    @GetMapping("/reports/pet/{petId}")
    @Operation(summary = "获取宠物AI报告", description = "根据宠物ID获取AI生成的报告列表")
    public Result<List<AiReport>> getByPet(@PathVariable Long petId) {
        return Result.success(aiReportService.getReportsByPet(petId));
    }

    @PostMapping("/reports")
    @Operation(summary = "创建AI报告", description = "手动创建一条AI报告")
    public Result<AiReport> create(@RequestBody AiReport report) {
        return Result.success(aiReportService.createReport(report));
    }

    @PostMapping("/care-suggestion")
    @Operation(summary = "生成护理建议", description = "AI根据宠物和订单信息生成护理建议")
    public Result<AiReport> generateCareSuggestion(
            @RequestParam Long petId,
            @RequestParam Long keeperId,
            @RequestParam Long orderId) {
        return Result.success(aiReportService.generateCareSuggestion(petId, keeperId, orderId));
    }

    @PostMapping("/boarding-report")
    @Operation(summary = "生成寄养报告", description = "AI根据宠物和订单信息生成寄养报告")
    public Result<AiReport> generateBoardingReport(
            @RequestParam Long petId,
            @RequestParam Long keeperId,
            @RequestParam Long orderId) {
        return Result.success(aiReportService.generateBoardingReport(petId, keeperId, orderId));
    }
}
