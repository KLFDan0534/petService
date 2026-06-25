package com.pet.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String endpoint;
    private final String model;

    public EmbeddingService(RestTemplate restTemplate,
                            @org.springframework.beans.factory.annotation.Value("${ai.deepseek.api-key:}") String apiKey,
                            @org.springframework.beans.factory.annotation.Value("${ai.deepseek.endpoint:https://api.deepseek.com}") String endpoint) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.model = "deepseek-embedding";
    }

    public float[] embed(String text) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("DeepSeek API密钥未配置，返回零向量");
            return new float[256];
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("input", text);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String json = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint + "/v1/embeddings",
                    HttpMethod.POST, entity, String.class);

            if (response.getBody() == null) {
                log.warn("嵌入API返回空响应");
                return new float[256];
            }

            var tree = objectMapper.readTree(response.getBody());
            var data = tree.get("data");
            if (data == null || data.isEmpty()) {
                log.warn("响应中没有嵌入数据");
                return new float[256];
            }

            var embedding = data.get(0).get("embedding");
            if (embedding == null) {
                log.warn("响应数据中没有嵌入字段");
                return new float[256];
            }

            int dimension = embedding.size();
            float[] vector = new float[dimension];
            for (int i = 0; i < dimension; i++) {
                vector[i] = (float) embedding.get(i).asDouble();
            }

            double magnitude = 0;
            for (float v : vector) magnitude += (double) v * v;
            magnitude = Math.sqrt(magnitude);
            if (magnitude > 0) {
                for (int i = 0; i < vector.length; i++) {
                    vector[i] /= magnitude;
                }
            }

            return vector;
        } catch (Exception e) {
            log.error("从DeepSeek API获取嵌入失败: {}", e.getMessage());
            return new float[256];
        }
    }

    public double cosineSimilarity(float[] vecA, float[] vecB) {
        if (vecA.length == 0 || vecB.length == 0 || vecA.length != vecB.length) return 0;
        double dot = 0;
        for (int i = 0; i < vecA.length; i++) {
            dot += (double) vecA[i] * vecB[i];
        }
        return dot;
    }
}