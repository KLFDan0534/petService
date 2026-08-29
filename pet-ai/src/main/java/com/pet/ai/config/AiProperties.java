package com.pet.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 配置属性类，从 application.yml 的 ai.deepseek 前缀读取配置。
 * <p>
 * 当前对接质谱 GLM-4.7-Flash（OpenAI 兼容格式），可配置 API Key、模型、端点、Token、温度等。
 */
@Configuration
@ConfigurationProperties(prefix = "ai.deepseek")
@Getter
@Setter
public class AiProperties {
    private String apiKey = "";
    private String model = "glm-4.7-flash";
    private String endpoint = "https://open.bigmodel.cn/api/paas/v4";
    private int maxTokens = 65536;
    private double temperature = 1.0;
    private String thinkingType = "disabled";

//    public String getApiKey() { return apiKey; }
//    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
//    public String getModel() { return model; }
//    public void setModel(String model) { this.model = model; }
//    public String getEndpoint() { return endpoint; }
//    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
//    public int getMaxTokens() { return maxTokens; }
//    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
//    public double getTemperature() { return temperature; }
//    public void setTemperature(double temperature) { this.temperature = temperature; }
//    public String getThinkingType() { return thinkingType; }
//    public void setThinkingType(String thinkingType) { this.thinkingType = thinkingType; }
}
