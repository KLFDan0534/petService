package com.pet.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 文本向量嵌入服务，调用 DeepSeek Embedding API 将文本转为浮点数向量。
 * <p>
 * 返回的向量会进行 L2 归一化处理，可直接用于余弦相似度计算。
 * API 密钥未配置时返回零向量（256 维），不影响业务流程。
 */
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

    /**
     * 【业务名称】文本向量化（Embedding）
     * <p>业务作用：调用DeepSeek Embedding API将文本转换为浮点数向量，并进行L2归一化处理，归一化后的向量可直接用于余弦相似度计算。</p>
     * <p>调用场景：RagServiceImpl.storeInChroma()中生成知识文档的向量嵌入；RagServiceImpl.vectorSearch()中生成查询文本的向量。</p>
     * <p>调用链：调用方 → embed(text) → POST DeepSeek /v1/embeddings → 解析响应JSON → 提取embedding数组 → L2归一化 → 返回float[]</p>
     * <p>数据处理：构造请求体{model:"deepseek-embedding", input:text}；Bearer Token认证；解析响应JSON的data[0].embedding数组；计算L2范数并逐元素归一化。</p>
     * <p>业务规则：API未配置时返回256维零向量（不影响业务流程）；text为空时仍发送请求由API处理。</p>
     * <p>状态影响：无状态变更，仅返回向量数据。</p>
     * <p>异常情况：请求异常/空响应/数据格式异常均记录error日志，返回256维零向量。</p>
     * <p>注意事项：向量维度由DeepSeek API决定，目前为256维；归一化后向量点积即等于余弦相似度。</p>
     *
     * @param text 待嵌入的文本
     * @return 归一化后的浮点数向量，API不可用时返回256维零向量
     */
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

    /**
     * 计算两个归一化向量的余弦相似度（即向量点积）
     * <p>
     * 两个向量的长度必须相等且非空，否则返回 0。
     *
     * @param vecA 向量 A
     * @param vecB 向量 B
     * @return 余弦相似度，取值范围 [0, 1]
     */
    /**
     * 【业务名称】计算余弦相似度
     * <p>业务作用：计算两个已归一化向量的余弦相似度（向量点积），值越大表示语义越相似。</p>
     * <p>调用场景：文本语义相似度比较、向量搜索结果排序等需要度量相似度的场景。</p>
     * <p>调用链：调用方 → cosineSimilarity(vecA, vecB) → 逐元素点积求和 → 返回double</p>
     * <p>数据处理：校验向量长度相等且非空；逐元素相乘并累加。</p>
     * <p>业务规则：输入向量需已L2归一化（embed()返回向量已归一化），否则结果非真实余弦相似度。</p>
     * <p>状态影响：无状态变更。</p>
     * <p>异常情况：向量长度不等或任一为空时返回0。</p>
     * <p>注意事项：使用double精度累加避免float误差。</p>
     *
     * @param vecA 向量A
     * @param vecB 向量B
     * @return 余弦相似度，取值范围[0, 1]
     */
    public double cosineSimilarity(float[] vecA, float[] vecB) {
        if (vecA.length == 0 || vecB.length == 0 || vecA.length != vecB.length) return 0;
        double dot = 0;
        for (int i = 0; i < vecA.length; i++) {
            dot += (double) vecA[i] * vecB[i];
        }
        return dot;
    }
}