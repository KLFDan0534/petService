package com.pet.ai.service;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AI 运行时配置管理服务。
 * 从 application.yml 读取初始值，作为 AiConfigStore（DB 配置）无启用配置时的兜底。
 */
@Service
@Slf4j
public class AiConfigService {

    @Getter
    private String apiKey;

    @Getter
    private String model;

    @Getter
    private String endpoint;

    @Getter
    private int maxTokens;

    @Getter
    private double temperature;

    /**
     * 【构造方法】从配置项初始化 AI 运行时参数
     *
     * 业务作用：由 Spring 注入 ai.kenari.* 配置（支持环境变量覆盖），写入实例字段作为运行时默认值。
     * 业务规则：
     * 支持 ${KENARI_API_KEY}、${KENARI_MODEL}、${KENARI_ENDPOINT}、${KENARI_MAX_TOKENS}、${KENARI_TEMPERATURE} 环境变量覆盖。
     * 异常情况：配置值类型不匹配（如 max-tokens 非数字）时由 Spring 抛出绑定异常。
     * 注意事项：这是 yml 兜底配置，DB 中 ai_config_wsh 有启用配置时由 AiConfigStore 优先提供；日志只打印 endpoint 与 model，不打印 key。
     */
    public AiConfigService(
            @Value("${ai.kenari.api-key:${KENARI_API_KEY:}}") String apiKey,
            @Value("${ai.kenari.model:${KENARI_MODEL:glm-5.3-flash}}") String model,
            @Value("${ai.kenari.endpoint:${KENARI_ENDPOINT:https://api.b.ai/v1}}") String endpoint,
            @Value("${ai.kenari.max-tokens:${KENARI_MAX_TOKENS:8192}}") int maxTokens,
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
     * 【API Key 脱敏】保留前 6 位与后 4 位，中间打码
     *
     * 业务作用：在配置查询接口返回时隐藏密钥明文。
     * 数据处理：长度小于 10 或为 null 时返回 "****"，否则返回 前6位 + "****" + 后4位。
     * 业务规则：短于 10 位的 Key 完全不展示。
     * 注意事项：与 AiConfigStore.maskKey（保留前后 4 位、阈值 8）规则不一致，两套脱敏长度不同；getSnapshot 同时把明文写进 apiKeyRaw 字段，若该接口对外开放会泄露密钥。
     */
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
