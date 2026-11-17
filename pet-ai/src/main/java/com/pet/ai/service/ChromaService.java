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

/**
 * ChromaDB 向量数据库服务，提供集合管理、文档增删查等核心操作。
 * <p>
 * 使用 ChromaDB HTTP API (v2) 进行通信，支持多租户和多数据库隔离。
 * 集合默认使用余弦距离（hnsw:space = cosine）作为向量索引空间。
 */
@Service
public class ChromaService {

    private static final Logger log = LoggerFactory.getLogger(ChromaService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String tenant;
    private final String database;

    /**
     * 业务作用：注入 RestTemplate，并读取向量库地址、租户与数据库名。
     * 注意事项：ObjectMapper 为本类私有实例，与全局配置隔离。
     */
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

    /**
     * 业务作用：拼接 v2 版本的集合接口地址，带租户与数据库路径。
     * 注意事项：租户或数据库名变更后需重启应用。
     */
    private String collectionUrl() {
        return baseUrl + "/api/v2/tenants/" + tenant + "/databases/" + database + "/collections";
    }

    /**
     * 业务作用：在集合基础地址后追加集合 ID，用于针对单个集合的操作。
     * 注意事项：集合 ID 为空时会拼出非法地址，由调用方保证非空。
     */
    private String collectionUrl(String collectionId) {
        return collectionUrl() + "/" + collectionId;
    }

    /**
     * 业务作用：把请求体 Map 序列化为 JSON，并设置 application/json 头。
     * 业务规则：序列化失败直接抛运行时异常。
     * 异常情况：序列化失败 → RuntimeException。
     * 注意事项：异常信息固定为 JSON 序列化失败，原始异常挂在 cause 上。
     */
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

    /**
     * 获取或创建Chroma集合
     *
     * 业务作用：按集合名称查询ChromaDB中是否已存在该集合，存在则直接返回ID，不存在则创建新集合（使用余弦距离hnsw:space=cosine作为向量索引空间）。
     * 业务规则：先查询后创建；集合名称为业务标识（如"ai_reports"、"knowledge_documents"）；新集合默认启用cosine距离。
     * 异常情况：查询异常记录warn日志后尝试创建；创建异常记录error日志后返回null。
     * 注意事项：返回的collectionId应缓存复用（调用方已在各自Service中缓存）；返回null时需调用方自行处理不可用场景。
     *
     * @param name 集合名称
     * @return 集合ID，创建/查询失败时返回null
     */
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

    /**
     * 向Chroma集合添加文档
     *
     * 业务作用：向指定集合中添加文档记录，包含ID、向量嵌入、文本内容和元数据。
     * 业务规则：所有参数列表必须等长；embeddings维度需与集合配置一致（创建时指定）。
     * 异常情况：请求异常包装为RuntimeException抛出；集合不存在时Chroma返回错误。
     * 注意事项：float[]数组在序列化时自动转换为List&lt;List&lt;Double&gt;&gt;格式。
     *
     * @param collectionId 集合ID
     * @param ids          文档ID列表
     * @param embeddings   向量嵌入列表
     * @param documents    文档文本内容列表
     * @param metadatas    文档元数据列表
     * @throws RuntimeException 添加失败时抛出
     */
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

    /**
     * 查询Chroma集合文档
     *
     * 业务作用：从指定集合中查询文档，支持按文档ID过滤、按metadata字段条件过滤和指定返回字段。
     * 业务规则：ids和where均为可选参数，都为空时返回集合中所有文档；include控制返回字段以减少数据传输。
     * 异常情况：请求异常记录error日志，返回空ChromaGetResult（ids/documents/metadatas均为空列表）。
     * 注意事项：where条件仅支持metadata字段的精确匹配；关联的jsonEntity方法处理body序列化。
     *
     * @param collectionId 集合ID
     * @param ids          要查询的文档ID列表，传null查询所有
     * @param where        过滤条件（metadata字段名-值Map），传null不过滤
     * @param include      返回字段（如"documents"、"metadatas"、"embeddings"）
     * @return 查询结果，查询失败时返回空结果
     */
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

    /**
     * 删除Chroma集合文档
     *
     * 业务作用：从指定集合中删除文档，支持按文档ID和/或metadata条件删除。
     * 业务规则：至少提供ids或where中的一个条件。
     * 异常情况：请求异常记录error日志，不抛出异常。
     * 注意事项：删除不存在的ID不会报错（Chroma幂等）；删除不可恢复。
     *
     * @param collectionId 集合ID
     * @param ids          要删除的文档ID列表
     * @param where        过滤条件，传null仅按ID删除
     */
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

    /**
     * 清空Chroma集合全部文档
     *
     * 业务作用：先查询集合中所有文档ID，再批量删除全部文档。用于重置集合数据。
     * 业务规则：先查后删，两阶段操作；如果集合为空则不执行删除。
     * 异常情况：get接口异常记录warn日志；delete接口异常记录warn日志。
     * 注意事项：此操作不可恢复；不会删除集合本身，仅清空文档。
     *
     * @param collectionId 集合ID
     */
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

    /**
     * 删除Chroma集合
     *
     * 业务作用：从ChromaDB中删除整个集合及其包含的所有文档和向量数据。
     * 业务规则：删除后集合不可恢复，后续操作需重新创建。
     * 异常情况：请求异常记录warn日志，不抛出异常。
     * 注意事项：调用方需同步清除本地缓存的collectionId引用。
     *
     * @param collectionId 集合ID
     */
    public void deleteCollection(String collectionId) {
        try {
            restTemplate.exchange(collectionUrl(collectionId), HttpMethod.DELETE,
                    null, String.class);
            log.info("删除Chroma集合 {}", collectionId);
        } catch (Exception e) {
            log.warn("删除集合 {} 失败: {}", collectionId, e.getMessage());
        }
    }

    /**
     * Chroma 查询结果的数据容器
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChromaGetResult {
        public List<String> ids;
        public List<List<Double>> embeddings;
        public List<String> documents;
        public List<Map<String, Object>> metadatas;

        /**
         * 业务作用：供 Jackson 反序列化时创建空对象。
         * 注意事项：字段为 public，可直接访问。
         */
        public ChromaGetResult() {}

        /**
         * 业务作用：快速构造只含 ids、documents、metadatas 的结果对象。
         * 注意事项：embeddings 为 null，使用前需判空。
         */
        public ChromaGetResult(List<String> ids, List<String> documents,
                               List<Map<String, Object>> metadatas) {
            this.ids = ids;
            this.documents = documents;
            this.metadatas = metadatas;
        }
    }
}
