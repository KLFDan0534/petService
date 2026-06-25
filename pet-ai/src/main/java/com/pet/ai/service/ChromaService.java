package com.pet.ai.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class ChromaService {

    private static final Logger log = LoggerFactory.getLogger(ChromaService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String tenant;
    private final String database;

    public ChromaService(RestTemplate restTemplate,
                         @org.springframework.beans.factory.annotation.Value("${chroma.url:http://localhost:8000}") String baseUrl,
                         @org.springframework.beans.factory.annotation.Value("${chroma.tenant:default_tenant}") String tenant,
                         @org.springframework.beans.factory.annotation.Value("${chroma.database:default_database}") String database) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        this.baseUrl = baseUrl;
        this.tenant = tenant;
        this.database = database;
    }

    private String collectionUrl() {
        return baseUrl + "/api/v2/tenants/" + tenant + "/databases/" + database + "/collections";
    }

    private String collectionUrl(String collectionId) {
        return collectionUrl() + "/" + collectionId;
    }

    private HttpEntity<String> jsonEntity(Map<String, Object> body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return new HttpEntity<>(json, headers);
        } catch (Exception e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }

    public String getOrCreateCollection(String name) {
        try {
            String listUrl = collectionUrl() + "?name=" + name;
            String json = restTemplate.getForObject(listUrl, String.class);
            if (json != null) {
                var list = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
                for (var coll : list) {
                    if (name.equals(coll.get("name"))) {
                        String id = (String) coll.get("id");
                        log.info("Chroma集合 '{}' 已存在，id={}", name, id);
                        return id;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取集合 '{}' 失败，将创建: {}", name, e.getMessage());
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("name", name);
            body.put("metadata", Map.of("hnsw:space", "cosine"));
            ResponseEntity<String> response = restTemplate.exchange(
                    collectionUrl(), HttpMethod.POST, jsonEntity(body), String.class);
            if (response.getBody() != null) {
                var result = objectMapper.readValue(response.getBody(),
                        new TypeReference<Map<String, Object>>() {});
                String id = (String) result.get("id");
                log.info("创建Chroma集合 '{}'，id={}", name, id);
                return id;
            }
        } catch (Exception e) {
            log.error("创建集合 '{}' 失败: {}", name, e.getMessage());
        }
        return null;
    }

    public void add(String collectionId, List<String> ids, List<float[]> embeddings,
                    List<String> documents, List<Map<String, Object>> metadatas) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ids", ids);
            body.put("embeddings", embeddings);
            body.put("documents", documents);
            body.put("metadatas", metadatas);

            log.debug("Chroma添加到 {}: {} 个文档", collectionId, ids.size());
            restTemplate.exchange(collectionUrl(collectionId) + "/add",
                    HttpMethod.POST, jsonEntity(body), String.class);
        } catch (Exception e) {
            log.error("向集合 {} 添加文档失败: {}", collectionId, e.getMessage());
            throw new RuntimeException("Chroma 添加失败", e);
        }
    }

    public ChromaGetResult get(String collectionId, List<String> ids,
                                Map<String, Object> where, List<String> include) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            if (ids != null && !ids.isEmpty()) body.put("ids", ids);
            if (where != null && !where.isEmpty()) body.put("where", where);
            if (include != null && !include.isEmpty()) body.put("include", include);

            ResponseEntity<String> response = restTemplate.exchange(
                    collectionUrl(collectionId) + "/get",
                    HttpMethod.POST, jsonEntity(body), String.class);
            if (response.getBody() == null) return new ChromaGetResult(List.of(), List.of(), List.of());

            return objectMapper.readValue(response.getBody(), ChromaGetResult.class);
        } catch (Exception e) {
            log.error("从集合 {} 获取文档失败: {}", collectionId, e.getMessage());
            return new ChromaGetResult(List.of(), List.of(), List.of());
        }
    }

    public void delete(String collectionId, List<String> ids, Map<String, Object> where) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            if (ids != null && !ids.isEmpty()) body.put("ids", ids);
            if (where != null && !where.isEmpty()) body.put("where", where);

            restTemplate.exchange(collectionUrl(collectionId) + "/delete",
                    HttpMethod.POST, jsonEntity(body), String.class);
            log.debug("从Chroma集合 {} 删除", collectionId);
        } catch (Exception e) {
            log.error("从集合 {} 删除失败: {}", collectionId, e.getMessage());
        }
    }

    public void deleteWhereAll(String collectionId) {
        // Get all document IDs first, then delete them
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("include", List.of());
            ResponseEntity<String> response = restTemplate.exchange(
                    collectionUrl(collectionId) + "/get",
                    HttpMethod.POST, jsonEntity(body), String.class);
            if (response.getBody() != null) {
                var result = objectMapper.readValue(response.getBody(),
                        new TypeReference<Map<String, Object>>() {});
                @SuppressWarnings("unchecked")
                List<String> allIds = (List<String>) result.get("ids");
                if (allIds != null && !allIds.isEmpty()) {
                    Map<String, Object> delBody = new LinkedHashMap<>();
                    delBody.put("ids", allIds);
                    restTemplate.exchange(collectionUrl(collectionId) + "/delete",
                            HttpMethod.POST, jsonEntity(delBody), String.class);
                    log.info("从Chroma集合 {} 删除了 {} 个文档", collectionId, allIds.size());
                }
            }
        } catch (Exception e) {
            log.warn("从集合 {} 删除所有文档失败: {}", collectionId, e.getMessage());
        }
    }

    public void deleteCollection(String collectionId) {
        try {
            restTemplate.exchange(collectionUrl(collectionId), HttpMethod.DELETE,
                    null, String.class);
            log.info("删除Chroma集合 {}", collectionId);
        } catch (Exception e) {
            log.warn("删除集合 {} 失败: {}", collectionId, e.getMessage());
        }
    }

    public float[] generateDummyEmbedding(int dimensions) {
        return new float[dimensions];
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChromaGetResult {
        public List<String> ids;
        public List<List<Double>> embeddings;
        public List<String> documents;
        public List<Map<String, Object>> metadatas;

        public ChromaGetResult() {}

        public ChromaGetResult(List<String> ids, List<String> documents,
                               List<Map<String, Object>> metadatas) {
            this.ids = ids;
            this.documents = documents;
            this.metadatas = metadatas;
        }

        public List<String> getIds() { return ids; }
        public void setIds(List<String> ids) { this.ids = ids; }
        public List<List<Double>> getEmbeddings() { return embeddings; }
        public void setEmbeddings(List<List<Double>> embeddings) { this.embeddings = embeddings; }
        public List<String> getDocuments() { return documents; }
        public void setDocuments(List<String> documents) { this.documents = documents; }
        public List<Map<String, Object>> getMetadatas() { return metadatas; }
        public void setMetadatas(List<Map<String, Object>> metadatas) { this.metadatas = metadatas; }
    }
}
