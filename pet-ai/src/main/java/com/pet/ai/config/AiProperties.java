package com.pet.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * DeepSeek AI 配置属性类，从 application.yml 的 ai.deepseek 前缀读取配置。
 * <p>
 * 可配置项包括 API Key、模型名称、端点地址、最大 Token 数、温度参数和思考类型。
 */
@Configuration
@ConfigurationProperties(prefix = "ai.deepseek")
@Getter
@Setter
public class AiProperties {
    private String apiKey = "";
    private String model = "deepseek-v4-flash";
    private String endpoint = "https://api.deepseek.com";
    private int maxTokens = 8192;
    private double temperature = 0.7;
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
