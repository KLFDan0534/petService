package com.pet.ai.controller;

import com.pet.ai.service.AiConfigService;
import com.pet.ai.service.AiConfigService.AiConfigSnapshot;
import com.pet.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 配置管理接口（仅管理员可用）
 */
@RestController
@RequestMapping("/api/ai/config")
@Tag(name = "【AI 配置】管理", description = "管理员配置 AI 大模型参数并测试连通性")
@PreAuthorize("hasRole('ADMIN')")
public class AiConfigController {

    private final AiConfigService aiConfigService;

    public AiConfigController(AiConfigService aiConfigService) {
        this.aiConfigService = aiConfigService;
    }

    @GetMapping
    @Operation(summary = "获取当前 AI 配置", description = "返回 API Key（脱敏）、模型、端点等配置信息")
    public Result<AiConfigSnapshot> getConfig() {
        return Result.success(aiConfigService.getSnapshot());
    }

    @PutMapping
    @Operation(summary = "更新 AI 配置", description = "管理员更新 AI 大模型的 API Key、模型、端点等参数，仅传入需要修改的字段")
    public Result<Void> updateConfig(@RequestBody AiConfigRequest request) {
        aiConfigService.updateConfig(
                request.getApiKey(),
                request.getModel(),
                request.getEndpoint(),
                request.getMaxTokens(),
                request.getTemperature()
        );
        return Result.success();
    }

    @PostMapping("/test")
    @Operation(summary = "测试 AI 连通性", description = "发送一个简单请求测试 AI 服务是否可用")
    public Result<Map<String, Object>> testConnection() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String apiKey = aiConfigService.getApiKey();
            String model = aiConfigService.getModel();
            String url = aiConfigService.getChatCompletionsUrl();

            if (apiKey == null || apiKey.isBlank()) {
                result.put("success", false);
                result.put("message", "API Key 未配置");
                return Result.success(result);
            }

            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Duration.ofSeconds(10));
            factory.setReadTimeout(Duration.ofSeconds(30));
            RestTemplate testRest = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);

            java.util.List<Map<String, String>> messages = new java.util.ArrayList<>();
            Map<String, String> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", "你好，请回复'OK'");
            messages.add(userMsg);
            body.put("messages", messages);
            body.put("max_tokens", 50);

            String raw = testRest.postForObject(url, new HttpEntity<>(body, headers), String.class);

            if (raw != null && raw.contains("choices")) {
                result.put("success", true);
                result.put("message", "AI 服务连接成功");
                result.put("endpoint", url);
                result.put("model", model);
            } else {
                result.put("success", false);
                result.put("message", "AI 服务返回异常: " + (raw != null ? raw.substring(0, Math.min(raw.length(), 200)) : "空响应"));
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "连接失败: " + e.getMessage());
        }
        return Result.success(result);
    }

    @lombok.Data
    public static class AiConfigRequest {
        private String apiKey;
        private String model;
        private String endpoint;
        private Integer maxTokens;
        private Double temperature;
    }
}
