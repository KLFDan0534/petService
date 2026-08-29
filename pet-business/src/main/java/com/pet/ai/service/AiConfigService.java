package com.pet.ai.service;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AI 运行时配置管理服务。
 * 从 application.yml 读取初始值，支持管理员在运行时动态修改。
 */
@Service
@Slf4j
public class AiConfigService {

    @Getter @Setter
    private String apiKey;

    @Getter @Setter
    private String model;

    @Getter @Setter
    private String endpoint;

    @Getter @Setter
    private int maxTokens;

    @Getter @Setter
    private double temperature;

    public AiConfigService(
            @Value("${ai.kenari.api-key:${KENARI_API_KEY:ff49794d56204c31a281c695b97d4875.t5t1ymE6wUwY4gHa}}") String apiKey,
            @Value("${ai.kenari.model:${KENARI_MODEL:glm-4.7-flash}}") String model,
            @Value("${ai.kenari.endpoint:${KENARI_ENDPOINT:https://open.bigmodel.cn/api/paas/v4}}") String endpoint,
            @Value("${ai.kenari.max-tokens:${KENARI_MAX_TOKENS:65536}}") int maxTokens,
            @Value("${ai.kenari.temperature:${KENARI_TEMPERATURE:1.0}}") double temperature) {
        this.apiKey = apiKey;
        this.model = model;
        this.endpoint = endpoint;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        log.info("AI 配置已初始化: endpoint={}, model={}", endpoint, model);
    }

    /**
     * 获取当前 AI 配置（用于 API 返回）
     */
    public AiConfigSnapshot getSnapshot() {
        AiConfigSnapshot snapshot = new AiConfigSnapshot();
        snapshot.setApiKey(maskApiKey(this.apiKey));
        snapshot.setApiKeyRaw(this.apiKey);
        snapshot.setModel(this.model);
        snapshot.setEndpoint(this.endpoint);
        snapshot.setMaxTokens(this.maxTokens);
        snapshot.setTemperature(this.temperature);
        return snapshot;
    }

    /**
     * 更新 AI 配置
     */
    public void updateConfig(String apiKey, String model, String endpoint, Integer maxTokens, Double temperature) {
        if (apiKey != null && !apiKey.isBlank()) {
            this.apiKey = apiKey;
        }
        if (model != null && !model.isBlank()) {
            this.model = model;
        }
        if (endpoint != null && !endpoint.isBlank()) {
            // 去除末尾斜杠
            this.endpoint = endpoint.replaceAll("/+$", "");
        }
        if (maxTokens != null && maxTokens > 0) {
            this.maxTokens = maxTokens;
        }
        if (temperature != null) {
            this.temperature = temperature;
        }
        log.info("AI 配置已更新: endpoint={}, model={}", this.endpoint, this.model);
    }

    /**
     * 构建 chat/completions 完整 URL
     */
    public String getChatCompletionsUrl() {
        return this.endpoint + (this.endpoint.endsWith("/") ? "" : "/") + "chat/completions";
    }

    private String maskApiKey(String key) {
        if (key == null || key.length() < 10) return "****";
        return key.substring(0, 6) + "****" + key.substring(key.length() - 4);
    }

    @Getter
    @Setter
    public static class AiConfigSnapshot {
        private String apiKey;
        private String apiKeyRaw;
        private String model;
        private String endpoint;
        private int maxTokens;
        private double temperature;
    }
}
