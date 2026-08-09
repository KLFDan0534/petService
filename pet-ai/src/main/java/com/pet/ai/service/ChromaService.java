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

    /**
     * 【业务名称】获取或创建Chroma集合
     * <p>业务作用：按集合名称查询ChromaDB中是否已存在该集合，存在则直接返回ID，不存在则创建新集合（使用余弦距离hnsw:space=cosine作为向量索引空间）。</p>
     * <p>调用场景：RagService和AiReportService初始化时调用，确保集合存在后执行后续向量操作。</p>
     * <p>调用链：调用方 → getOrCreateCollection(name) → GET /api/v2/.../collections?name=xxx 查询 → 存在返回ID | 不存在 → POST /api/v2/.../collections 创建 → 返回新集合ID</p>
     * <p>数据处理：先调用GET查询接口搜索name匹配的集合；如查询失败或未找到则调用POST创建接口。</p>
     * <p>业务规则：先查询后创建；集合名称为业务标识（如"ai_reports"、"knowledge_documents"）；新集合默认启用cosine距离。</p>
     * <p>状态影响：集合不存在时在ChromaDB中创建一个新集合。</p>
     * <p>异常情况：查询异常记录warn日志后尝试创建；创建异常记录error日志后返回null。</p>
     * <p>注意事项：返回的collectionId应缓存复用（调用方已在各自Service中缓存）；返回null时需调用方自行处理不可用场景。</p>
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
     * 【业务名称】向Chroma集合添加文档
     * <p>业务作用：向指定集合中添加文档记录，包含ID、向量嵌入、文本内容和元数据。</p>
     * <p>调用场景：RagServiceImpl创建知识文档、AiReportServiceImpl保存AI报告时调用。</p>
     * <p>调用链：调用方 → add() → POST /api/v2/.../collections/{id}/add</p>
     * <p>数据处理：将ids/embeddings/documents/metadatas封装为JSON请求体发送POST请求。</p>
     * <p>业务规则：所有参数列表必须等长；embeddings维度需与集合配置一致（创建时指定）。</p>
     * <p>状态影响：在Chroma集合中新增文档记录（含向量和元数据）。</p>
     * <p>异常情况：请求异常包装为RuntimeException抛出；集合不存在时Chroma返回错误。</p>
     * <p>注意事项：float[]数组在序列化时自动转换为List&lt;List&lt;Double&gt;&gt;格式。</p>
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
     * 【业务名称】查询Chroma集合文档
     * <p>业务作用：从指定集合中查询文档，支持按文档ID过滤、按metadata字段条件过滤和指定返回字段。</p>
     * <p>调用场景：RagService.listAll()获取全部文档、AiReportService按order_id/pet_id查询报告。</p>
     * <p>调用链：调用方 → get() → POST /api/v2/.../collections/{id}/get</p>
     * <p>数据处理：构建请求体——可选设置ids/where/include；发送POST请求；解析JSON响应映射到ChromaGetResult。</p>
     * <p>业务规则：ids和where均为可选参数，都为空时返回集合中所有文档；include控制返回字段以减少数据传输。</p>
     * <p>状态影响：只读操作。</p>
     * <p>异常情况：请求异常记录error日志，返回空ChromaGetResult（ids/documents/metadatas均为空列表）。</p>
     * <p>注意事项：where条件仅支持metadata字段的精确匹配；关联的jsonEntity方法处理body序列化。</p>
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
     * 【业务名称】删除Chroma集合文档
     * <p>业务作用：从指定集合中删除文档，支持按文档ID和/或metadata条件删除。</p>
     * <p>调用场景：RagService.delete()删除知识文档；清空集合前的部分删除操作。</p>
     * <p>调用链：调用方 → delete() → POST /api/v2/.../collections/{id}/delete</p>
     * <p>数据处理：构建请求体包含ids和/或where；POST方式调用Chroma删除接口。</p>
     * <p>业务规则：至少提供ids或where中的一个条件。</p>
     * <p>状态影响：从Chroma集合中删除匹配的文档及其向量和元数据。</p>
     * <p>异常情况：请求异常记录error日志，不抛出异常。</p>
     * <p>注意事项：删除不存在的ID不会报错（Chroma幂等）；删除不可恢复。</p>
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
     * 【业务名称】清空Chroma集合全部文档
     * <p>业务作用：先查询集合中所有文档ID，再批量删除全部文档。用于重置集合数据。</p>
     * <p>调用场景：管理员重置知识库、数据迁移后清理旧数据。</p>
     * <p>调用链：调用方 → deleteWhereAll() → GET /.../get 获取全部ID → POST /.../delete 批量删除</p>
     * <p>数据处理：先调用get接口不带过滤条件获取所有文档ID；从响应中提取ids列表；再调用delete接口批量删除。</p>
     * <p>业务规则：先查后删，两阶段操作；如果集合为空则不执行删除。</p>
     * <p>状态影响：清空指定集合中所有文档数据。</p>
     * <p>异常情况：get接口异常记录warn日志；delete接口异常记录warn日志。</p>
     * <p>注意事项：此操作不可恢复；不会删除集合本身，仅清空文档。</p>
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
     * 【业务名称】删除Chroma集合
     * <p>业务作用：从ChromaDB中删除整个集合及其包含的所有文档和向量数据。</p>
     * <p>调用场景：管理员重置向量库、集合重建操作。</p>
     * <p>调用链：调用方 → deleteCollection() → DELETE /api/v2/.../collections/{id}</p>
     * <p>数据处理：发送HTTP DELETE请求到集合URL。</p>
     * <p>业务规则：删除后集合不可恢复，后续操作需重新创建。</p>
     * <p>状态影响：删除整个集合及其中所有文档数据。</p>
     * <p>异常情况：请求异常记录warn日志，不抛出异常。</p>
     * <p>注意事项：调用方需同步清除本地缓存的collectionId引用。</p>
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
     * 【业务名称】生成零向量占位
     * <p>业务作用：生成指定维度的全零浮点数组，用于AI报告等场景中不需要真实向量搜索时的占位嵌入。</p>
     * <p>调用场景：AiReportServiceImpl.storeInChroma()中生成占位向量（4维）。</p>
     * <p>调用链：调用方 → generateDummyEmbedding() → new float[dimensions]</p>
     * <p>数据处理：直接创建指定长度的float数组，所有元素默认0.0。</p>
     * <p>业务规则：维度由调用方指定，用于填充Chroma add接口的embeddings参数。</p>
     * <p>状态影响：无状态变更。</p>
     * <p>异常情况：无异常（dimensions为负数时new float[负数]抛出NegativeArraySizeException）。</p>
     * <p>注意事项：占位向量不具备语义搜索能力，仅用于满足Chroma API的格式要求。</p>
     *
     * @param dimensions 向量维度
     * @return 全零浮点数数组
     */
    public float[] generateDummyEmbedding(int dimensions) {
        return new float[dimensions];
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
