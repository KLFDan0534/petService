package com.pet.ai.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.ai.entity.AiReport;
import com.pet.ai.service.AiReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI报告", description = "AI报告生成和管理")
@Slf4j
public class AiReportController {

    private final AiReportService aiReportService;

    public AiReportController(AiReportService aiReportService) {
        this.aiReportService = aiReportService;
    }

    /**
     * 根据订单ID获取AI报告
     * @param orderId 订单ID
     * @return AI报告列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/reports/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "根据订单获取报告", description = "根据订单ID获取AI报告")
    public Result<List<AiReport>> getByOrder(@PathVariable Long orderId) {
        log.info("调用 getByOrder()");
        return Result.success(aiReportService.getReportsByOrder(orderId));
    }

    /**
     * 根据宠物ID获取AI报告
     * @param petId 宠物ID
     * @return AI报告列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/reports/pet/{petId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "根据宠物获取报告", description = "根据宠物ID获取AI报告")
    public Result<List<AiReport>> getByPet(@PathVariable Long petId) {
        log.info("调用 getByPet()");
        return Result.success(aiReportService.getReportsByPet(petId));
    }

    /**
     * 手动创建AI报告
     * @param report AI报告信息
     * @return 创建的AI报告
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/reports")
    @PreAuthorize("hasAnyRole('ADMIN','KEEPER')")
    @Operation(summary = "创建报告", description = "手动创建AI报告")
    public Result<AiReport> create(@RequestBody AiReport report) {
        log.info("调用 create()");
        return Result.success(aiReportService.createReport(report));
    }

    /**
     * AI生成护理建议报告
     * @param body 请求体，可包含petId/keeperId/orderId
     * @param petId 宠物ID（可选）
     * @param keeperId 看护者ID（可选）
     * @param orderId 订单ID（可选）
     * @return AI生成的护理建议报告
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/care-suggestion")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "生成护理建议", description = "AI生成护理建议报告")
    public Result<AiReport> generateCareSuggestion(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) Long keeperId,
            @RequestParam(required = false) Long orderId) {
        log.info("调用 generateCareSuggestion()");
        petId = resolveLong(petId, body, "petId", "pet_id_wsh");
        keeperId = resolveLong(keeperId, body, "keeperId", "keeper_id_wsh");
        orderId = resolveLong(orderId, body, "orderId", "order_id_wsh");
        return Result.success(aiReportService.generateCareSuggestion(petId, keeperId, orderId));
    }

    /**
     * AI生成寄养总结报告
     * @param body 请求体，可包含petId/keeperId/orderId
     * @param petId 宠物ID（可选）
     * @param keeperId 看护者ID（可选）
     * @param orderId 订单ID（可选）
     * @return AI生成的寄养总结报告
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/boarding-report")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "生成寄养报告", description = "AI生成寄养总结报告")
    public Result<AiReport> generateBoardingReport(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) Long keeperId,
            @RequestParam(required = false) Long orderId) {
        log.info("调用 generateBoardingReport()");
        petId = resolveLong(petId, body, "petId", "pet_id_wsh");
        keeperId = resolveLong(keeperId, body, "keeperId", "keeper_id_wsh");
        orderId = resolveLong(orderId, body, "orderId", "order_id_wsh");
        return Result.success(aiReportService.generateBoardingReport(petId, keeperId, orderId));
    }

    private Long resolveLong(Long queryValue, Map<String, Object> body, String camelKey, String snakeKey) {
        if (queryValue != null) {
            return queryValue;
        }
        if (body == null) {
            return null;
        }
        Object value = body.containsKey(camelKey) ? body.get(camelKey) : body.get(snakeKey);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : Long.parseLong(text);
    }
}
