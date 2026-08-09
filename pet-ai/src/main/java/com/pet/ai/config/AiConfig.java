package com.pet.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * AI 模块 Spring 配置类，提供全局 RestTemplate Bean 用于 HTTP API 调用
 */
@Configuration
public class AiConfig {

    /**
     * 创建 RestTemplate Bean，用于调用 DeepSeek API 和 ChromaDB HTTP API
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
