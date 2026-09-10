package com.pet.ai.service.impl;

import com.pet.ai.dto.RagDocumentCreateRequestDTO;
import com.pet.ai.service.LlmChatService;
import com.pet.ai.service.ChromaService;
import com.pet.ai.service.ChromaService.ChromaGetResult;
import com.pet.ai.service.EmbeddingService;
import com.pet.ai.entity.KnowledgeDocument;
import com.pet.ai.service.RagService;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG 服务实现，集成 Chroma 向量数据库和 DeepSeek AI 模型。
 * <p>
 * 检索策略：双阶段降级 —— 先向量相似性搜索，未命中时使用字符级关键词评分。
 * 问答策略：三阶段降级 —— AI 在线 -> 宠物档案模板 -> 知识库模板。
 * 文档管理：支持 .txt / .docx 文件导入，HTML 转义防止 XSS，Chroma 存储含完整元数据。
 */

@Service
public class RagServiceImpl implements RagService {

    private static final Logger log = LoggerFactory.getLogger(RagServiceImpl.class);

    // Chroma数据库集合名称
    private static final String COLLECTION_NAME = "knowledge_documents";
    
    // 系统提示词模板
    private static final String SYSTEM_PROMPT;
    
    // 日期时间格式化器
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 静态代码块
     * 用于加载系统提示词模板文件
     * 如果文件加载失败则使用默认提示词
     */
    static {
        String prompt = null;
        try (InputStream is = RagServiceImpl.class.getClassLoader().getResourceAsStream("rag-system-prompt.txt")) {
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                prompt = sb.toString().trim();
            }
        } catch (Exception e) {
            log.warn("加载rag-system-prompt.txt失败，使用默认提示词", e);
        }
        SYSTEM_PROMPT = prompt;
    }

    private final ChromaService chromaService;
    private final EmbeddingService embeddingService;
    private final LlmChatService aiChatService;
    private final String chromaBaseUrl;
    private final String chromaTenant;
    private final String chromaDatabase;

    private String collectionId;

    public RagServiceImpl(ChromaService chromaService,
                          EmbeddingService embeddingService,
                          LlmChatService aiChatService,
                          @org.springframework.beans.factory.annotation.Value("${chroma.url:http://localhost:8000}") String chromaBaseUrl,
                          @org.springframework.beans.factory.annotation.Value("${chroma.tenant:default_tenant}") String chromaTenant,
                          @org.springframework.beans.factory.annotation.Value("${chroma.database:default_database}") String chromaDatabase) {
        this.chromaService = chromaService;
        this.embeddingService = embeddingService;
        this.aiChatService = aiChatService;
        this.chromaBaseUrl = chromaBaseUrl;
        this.chromaTenant = chromaTenant;
        this.chromaDatabase = chromaDatabase;
    }

    /**
     * 获取Chroma集合ID
     * 如果集合不存在则创建新集合
     * @return 集合ID
     */
    private String getCollectionId() {
        if (collectionId == null) {
            collectionId = chromaService.getOrCreateCollection(COLLECTION_NAME);
        }
        return collectionId;
    }

    /**
     * 【业务名称】获取所有知识文档实现
     * <p>业务作用：从Chroma向量库查询全部文档的元数据和文本内容，转换为KnowledgeDocument列表并按创建时间倒序排列。</p>
     * <p>调用场景：管理员查看知识库列表、搜索前获取全量数据。</p>
     * <p>调用链：Controller → listAll() → ChromaService.get(collectionId, null, null, ["metadatas","documents"]) → toDocuments()</p>
     * <p>数据处理：调用ChromaService.get()不设过滤条件获取全部文档；toDocuments()解析ids/documents/metadatas为KnowledgeDocument对象列表；按created_at_wsh倒序排序。</p>
     * <p>业务规则：数据只存储在Chroma中，无MySQL备份；Chroma为空时返回空列表。</p>
     * <p>状态影响：只读操作。</p>
     * <p>异常情况：Chroma服务不可用时返回空列表。</p>
     * <p>注意事项：文档ID解析异常时使用hashCode作为ID兜底。</p>
     */
    @Override
    public List<KnowledgeDocument> listAll() {
        log.info("调用 listAll()");
        ChromaGetResult result = chromaService.get(getCollectionId(), null, null,
                List.of("metadatas", "documents"));
        return toDocuments(result);
    }

    /**
     * 【业务名称】搜索知识文档实现
     * <p>业务作用：先获取全量文档，按分类过滤，然后双阶段检索——向量搜索优先，未命中时关键词搜索降级。</p>
     * <p>调用场景：用户搜索知识库或AI问答内部检索。</p>
     * <p>调用链：Controller或answer() → search() → listAll() → 分类过滤 → vectorSearch()向量搜索 | termSearch()关键词文本匹配</p>
     * <p>数据处理：listAll()获取全量文档；Java Stream按category过滤；query非空时先vectorSearch（EmbeddingService.embed→Chroma query API返回匹配ID列表），结果为空时termSearch（单字+词组在title/content中评分）。</p>
     * <p>业务规则：分类过滤在检索前执行；向量搜索返回空时自动降级到关键词搜索。</p>
     * <p>状态影响：只读操作。</p>
     * <p>异常情况：向量搜索异常返回空列表触发关键词降级。</p>
     * <p>注意事项：向量搜索匹配到的文档ID需与candidateDocs取交集返回。</p>
     */
    @Override
    public List<KnowledgeDocument> search(String query, String category) {
        log.info("search()开始运行");
        List<KnowledgeDocument> allDocs = listAll();

        if (category != null && !category.isEmpty()) {
            allDocs = allDocs.stream()
                    .filter(d -> category.equals(d.getCategory_wsh()))
                    .collect(Collectors.toList());
        }

        if (query == null || query.isEmpty()) {
            return allDocs;
        }

        List<KnowledgeDocument> vectorResults = vectorSearch(query, allDocs);
        if (!vectorResults.isEmpty()) {
            log.debug("向量搜索返回 {} 条结果，查询: {}", vectorResults.size(), query);
            return vectorResults;
        }

        log.debug("向量搜索未返回结果，使用术语匹配: {}", query);
        return termSearch(query, allDocs);
    }

    /**
     * 向量搜索方法
     * 使用嵌入向量在Chroma数据库中进行相似性搜索
     * @param query 查询文本
     * @param candidateDocs 候选文档集合
     * @return 匹配的知识文档列表
     */
    private List<KnowledgeDocument> vectorSearch(String query, List<KnowledgeDocument> candidateDocs) {
        try {
            float[] queryVec = embeddingService.embed(query);
            if (queryVec.length == 0) return List.of();

            int dim = queryVec.length;
            List<Double> queryEmbedding = new ArrayList<>(dim);
            for (float v : queryVec) queryEmbedding.add((double) v);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("query_embeddings", List.of(queryEmbedding));
            body.put("n_results", candidateDocs.size() > 0 ? candidateDocs.size() : 10);

            String url = chromaBaseUrl + "/api/v2/tenants/" + chromaTenant +
                    "/databases/" + chromaDatabase + "/collections/" + getCollectionId() + "/query";
            org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);
            var entity = new org.springframework.http.HttpEntity<>(json, headers);

            String response = rt.postForObject(url, entity, String.class);
            if (response == null) return List.of();

            var om = new com.fasterxml.jackson.databind.ObjectMapper();
            var tree = om.readTree(response);
            var idsNode = tree.get("ids");
            if (idsNode == null || idsNode.isEmpty()) return List.of();

            var matchedIds = new HashSet<String>();
            for (var idArr : idsNode) {
                for (var id : idArr) matchedIds.add(id.asText());
            }
            if (matchedIds.isEmpty()) return List.of();

            Set<Long> idSet = new HashSet<>();
            for (String id : matchedIds) {
                try { idSet.add(Long.parseLong(id)); } catch (Exception ignored) {}
            }

            return candidateDocs.stream()
                    .filter(d -> idSet.contains(d.getId_wsh()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("向量搜索失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 【业务名称】AI问答（无宠物档案）实现
     * <p>业务作用：不带宠物档案信息的AI问答，委托给answer(question, null)执行。</p>
     * <p>调用场景：用户在知识库页面提问但未关联宠物。</p>
     * <p>调用链：Controller → answer(question) → answer(question, null)</p>
     * <p>数据处理：petProfile传null，不加入宠物档案上下文。</p>
     * <p>业务规则：无宠物档案时AI不可用降级到知识库模板。</p>
     * <p>状态影响：只读操作。</p>
     * <p>异常情况：不直接抛出异常。</p>
     * <p>注意事项：始终委托给双参数方法执行。</p>
     */
    @Override
    public String answer(String question) {
        return answer(question, null);
    }

    /**
     * 【业务名称】AI问答（带宠物档案）实现
     * <p>业务作用：检索相关知识文档，构建系统提示词和用户提示词，调用AI模型生成回答。三阶段降级策略：AI在线→AI回答、AI不可用且有宠物档案→宠物档案模板、AI不可用且知识库非空→知识库模板。</p>
     * <p>调用场景：用户在知识库页面提问时关联了宠物，或AI报告生成等需要个性化回答的场景。</p>
     * <p>调用链：Controller → answer(question, petProfile) → search()检索 → 构建systemPrompt+userPrompt → LlmChatService.chat(systemPrompt, userPrompt) → AI成功返回 | AI失败且有宠物档案→buildPetProfileFallback() | 知识库非空→知识库模板 | 空→引导提示</p>
     * <p>数据处理：search()检索相关内容；systemPrompt优先从classpath:rag-system-prompt.txt加载，失败使用英文默认提示词；buildUserPrompt()拼接petProfile+知识库上下文（最多5条）+问题+要求中文回答；AI返回null时降级处理。</p>
     * <p>业务规则：AI返回优先；有宠物档案且AI不可用时使用5条通用护理建议模板+知识库参考（最多2条）；无宠物档案但知识库非空时使用最相关3条文档作为回答；都空时返回引导提示。</p>
     * <p>状态影响：只读操作。</p>
     * <p>异常情况：AiChatService返回null时继续降级流程，不中断。</p>
     * <p>注意事项：系统提示词文件覆盖默认英文prompt；回答控制在800字以内由AI保证。</p>
     */
    @Override
    public String answer(String question, String petProfile) {
        log.info("answer()开始执行");
        List<KnowledgeDocument> relevantDocs = search(question, null);

        String systemPrompt = SYSTEM_PROMPT != null ? SYSTEM_PROMPT
                : "You are a professional pet service customer service assistant. "
                + "Answer the user's question based on the knowledge base context provided. "
                + "Keep answers concise and helpful. "
                + "If the context doesn't contain relevant information, say so politely.";

        String userPrompt = buildUserPrompt(question, petProfile, relevantDocs);

        String aiReply = aiChatService.chat(systemPrompt, userPrompt);
        if (aiReply != null) {
            return aiReply;
        }

//        没有宠物档案,回复固定答复
        if (petProfile != null && !petProfile.isBlank()) {
            return buildPetProfileFallback(question, petProfile, relevantDocs);
        }

//        知识库没有返回数据或为空
        if (relevantDocs.isEmpty()) {
            return "暂时没有在知识库中找到足够相关的信息。可以先补充宠物的品种、年龄、体重、疫苗情况、过敏禁忌和日常习惯；如果已经出现呕吐、腹泻、精神沉郁、持续不吃不喝等异常，请及时联系兽医。";
        }

//        构建知识库上下文
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < Math.min(3, relevantDocs.size()); i++) {
            KnowledgeDocument doc = relevantDocs.get(i);
            context.append("[").append(doc.getCategory_wsh()).append("] ")
                    .append(doc.getTitle_wsh()).append(":\n")
                    .append(doc.getContent_wsh()).append("\n\n");
        }

        return "我暂时无法连接 AI 模型，先根据知识库给你这些参考信息：\n\n"
                + context.toString().trim()
                + "\n\n如果你想要更具体的护理建议，请先补充宠物档案，尤其是年龄、体重、疫苗、绝育、过敏禁忌和生活习惯。";
    }

    /**
     * 构建用户提示词
     * 组合宠物档案和知识库上下文生成AI模型输入提示
     * @param question 用户问题
     * @param petProfile 宠物档案信息
     * @param relevantDocs 相关知识文档
     * @return 构建完成的提示词字符串
     */
    private String buildUserPrompt(String question, String petProfile, List<KnowledgeDocument> relevantDocs) {
        StringBuilder prompt = new StringBuilder();
        if (petProfile != null && !petProfile.isBlank()) {
            prompt.append("Pet profile for personalization:\n")
                    .append(petProfile.trim())
                    .append("\n\n");
        }
        if (!relevantDocs.isEmpty()) {
            prompt.append("Knowledge base context:\n\n");
            for (int i = 0; i < Math.min(5, relevantDocs.size()); i++) {
                KnowledgeDocument doc = relevantDocs.get(i);
                prompt.append("[").append(doc.getCategory_wsh()).append("] ")
                        .append(doc.getTitle_wsh()).append(":\n")
                        .append(doc.getContent_wsh()).append("\n\n");
            }
            prompt.append("---\n\n");
        }
        prompt.append("User question: ").append(question).append("\n\n")
                .append("Please answer in Chinese. Use the knowledge base context first, ")
                .append("and combine the pet profile when it is provided.");
        return prompt.toString();
    }

    /**
     * 构建宠物档案回退回答
     * 当AI模型不可用时使用基础护理建议和知识库内容生成回答
     * @param question 用户问题
     * @param petProfile 宠物档案信息
     * @param relevantDocs 相关知识文档
     * @return 构建完成的回退回答
     */
    private String buildPetProfileFallback(String question, String petProfile, List<KnowledgeDocument> relevantDocs) {
        StringBuilder answer = new StringBuilder();
        answer.append("我现在没能连上 AI 模型，先根据你的宠物档案和知识库给一版基础建议：\n\n");
        answer.append("宠物档案：\n").append(petProfile.trim()).append("\n\n");
        answer.append("护理建议：\n")
                .append("1. 饮食保持稳定，不要突然换粮；如果档案里有过敏或禁忌，请严格避开。\n")
                .append("2. 每天观察食欲、饮水、排便、精神状态和活动量，发现明显变化要记录时间和表现。\n")
                .append("3. 活动量按年龄、体重和性格来安排，幼宠、老年宠或刚换环境时先降低强度。\n")
                .append("4. 保持生活区、食盆、水盆和猫砂盆/厕所清洁，减少应激和感染风险。\n")
                .append("5. 如果出现持续呕吐、腹泻、精神沉郁、呼吸异常、抽搐或长时间不吃不喝，请及时联系兽医。\n");

        if (!relevantDocs.isEmpty()) {
            answer.append("\n知识库参考：\n");
            for (int i = 0; i < Math.min(2, relevantDocs.size()); i++) {
                KnowledgeDocument doc = relevantDocs.get(i);
                answer.append("- ").append(doc.getTitle_wsh()).append("：")
                        .append(doc.getContent_wsh()).append("\n");
            }
        }

        answer.append("\n你的问题是：").append(question);
        return answer.toString();
    }

    /**
     * 【业务名称】创建知识文档实现
     * <p>业务作用：将传入的DTO转为KnowledgeDocument实体，标题和内容做HTML转义后，委托doCreateDocument存储到Chroma向量库。</p>
     * <p>调用场景：管理员在后台手动新增知识文档。</p>
     * <p>调用链：Controller → create() → HTML转义 → doCreateDocument() → storeInChroma() → EmbeddingService.embed() → ChromaService.add()</p>
     * <p>数据处理：HtmlUtils.htmlEscape()转义title和content防止XSS；计算content.length()作为word_count；doCreateDocument设置时间戳并存储。</p>
     * <p>业务规则：标题和内容必须做HTML转义；category不做转义（枚举值）。</p>
     * <p>状态影响：新增一条知识文档记录。</p>
     * <p>异常情况：Embedding失败仅记日志；Chroma存储失败抛出异常。</p>
     * <p>注意事项：此方法不校验管理员权限（由Controller层负责）。</p>
     */
    @Override
    public KnowledgeDocument create(RagDocumentCreateRequestDTO request) {
        log.info("调用 create()");
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle_wsh(HtmlUtils.htmlEscape(request.getTitle_wsh()));
        doc.setContent_wsh(HtmlUtils.htmlEscape(request.getContent_wsh()));
        doc.setCategory_wsh(request.getCategory_wsh());
        doc.setWord_count_wsh(request.getContent_wsh().length());
        return doCreateDocument(doc);
    }

    /**
     * 【业务名称】从文件导入知识文档实现
     * <p>业务作用：解析上传的文件（.txt/.docx），提取文本内容，构建KnowledgeDocument并存储到Chroma。</p>
     * <p>调用场景：管理员在后台通过文件上传导入知识文档。</p>
     * <p>调用链：Controller → createFromFile() → extractText()提取文本 → 构造KnowledgeDocument → doCreateDocument() → storeInChroma()</p>
     * <p>数据处理：extractText()根据文件后缀选择解析方式；title为空时从文件名截取（取.之前部分）；设置source_type="upload"、source_path=fileName；委托doCreateDocument存储。</p>
     * <p>业务规则：仅支持.txt和.docx格式；title和category均可为空。</p>
     * <p>状态影响：新增一条知识文档记录。</p>
     * <p>异常情况：不支持的文件格式抛出RuntimeException；docx解析异常抛出RuntimeException。</p>
     * <p>注意事项：不对此方法中的文本做HTML转义（信任上传文件）；Controller层已做文件格式的前置校验。</p>
     */
    @Override
    public KnowledgeDocument createFromFile(String fileName, byte[] fileBytes, String title, String category) {
        String content = extractText(fileName, fileBytes);
        if (title == null || title.isBlank()) {
            int dot = fileName.lastIndexOf('.');
            title = dot > 0 ? fileName.substring(0, dot) : fileName;
        }
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle_wsh(title);
        doc.setContent_wsh(content);
        doc.setCategory_wsh(category);
        doc.setSource_type_wsh("upload");
        doc.setSource_path_wsh(fileName);
        return doCreateDocument(doc);
    }

    private KnowledgeDocument doCreateDocument(KnowledgeDocument doc) {
        doc.setCreated_at_wsh(LocalDateTime.now());
        doc.setUpdated_at_wsh(LocalDateTime.now());
        storeInChroma(doc);
        return doc;
    }

    private String extractText(String fileName, byte[] fileBytes) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".txt")) {
            return new String(fileBytes, StandardCharsets.UTF_8);
        } else if (lower.endsWith(".docx")) {
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(fileBytes));
                 XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                return extractor.getText();
            } catch (Exception e) {
                log.error("从docx文件提取文本失败: {}", fileName, e);
                throw new RuntimeException("无法解析Word文档: " + fileName, e);
            }
        } else {
            throw new RuntimeException("不支持的文件格式，仅支持 .txt 和 .docx 文件");
        }
    }

    /**
     * 【业务名称】删除知识文档实现
     * <p>业务作用：根据文档ID从Chroma向量库中删除文档及其向量嵌入和元数据。</p>
     * <p>调用场景：管理员在后台删除知识文档。</p>
     * <p>调用链：Controller → delete() → ChromaService.delete(collectionId, [id], null)</p>
     * <p>数据处理：将Long id转为String单元素列表传递给ChromaService。</p>
     * <p>业务规则：Controller层控制ADMIN权限。</p>
     * <p>状态影响：从Chroma中删除一条文档记录。</p>
     * <p>异常情况：Chroma删除失败记录warn日志，不抛出异常。</p>
     * <p>注意事项：ID不存在时Chroma不报错；无MySQL备份数据，删除后不可恢复。</p>
     */
    @Override
    public void delete(Long id) {
        log.info("调用 delete()");
        chromaService.delete(getCollectionId(), List.of(String.valueOf(id)), null);
    }

    /**
     * 将知识文档存储到Chroma数据库
     * 包括生成向量嵌入和元数据处理
     * @param doc 待存储的知识文档
     */
    private void storeInChroma(KnowledgeDocument doc) {
        String docId = doc.getId_wsh() != null ? String.valueOf(doc.getId_wsh()) : String.valueOf(generateId());
        if (doc.getId_wsh() == null) doc.setId_wsh(Long.parseLong(docId));

        Map<String, Object> metadata = new LinkedHashMap<>();
        putIfNotNull(metadata, "title_wsh", doc.getTitle_wsh());
        putIfNotNull(metadata, "category_wsh", doc.getCategory_wsh());
        putIfNotNull(metadata, "source_type_wsh", doc.getSource_type_wsh());
        putIfNotNull(metadata, "source_path_wsh", doc.getSource_path_wsh());
        if (doc.getWord_count_wsh() != null) metadata.put("word_count_wsh", doc.getWord_count_wsh());
        metadata.put("created_at_wsh", doc.getCreated_at_wsh() != null
                ? doc.getCreated_at_wsh().format(DTF) : LocalDateTime.now().format(DTF));
        metadata.put("updated_at_wsh", doc.getUpdated_at_wsh() != null
                ? doc.getUpdated_at_wsh().format(DTF) : LocalDateTime.now().format(DTF));

        float[] embedding = embeddingService.embed(doc.getContent_wsh() != null ? doc.getContent_wsh() : "");
        chromaService.add(getCollectionId(),
                List.of(docId),
                List.of(embedding),
                List.of(doc.getContent_wsh()),
                List.of(metadata));
        log.info("知识文档 {} 已存储到Chroma", docId);
    }

    /**
     * 将Chroma数据库查询结果转换为知识文档列表
     * @param result Chroma查询结果
     * @return 知识文档列表
     */
    private List<KnowledgeDocument> toDocuments(ChromaGetResult result) {
        List<KnowledgeDocument> docs = new ArrayList<>();
        if (result == null || result.ids == null || result.ids.isEmpty()) return docs;

        for (int i = 0; i < result.ids.size(); i++) {
            KnowledgeDocument doc = new KnowledgeDocument();
            try {
                doc.setId_wsh(Long.parseLong(result.ids.get(i)));
            } catch (Exception e) {
                doc.setId_wsh((long) Math.abs(result.ids.get(i).hashCode()));
            }
            if (result.documents != null && i < result.documents.size()) {
                doc.setContent_wsh(result.documents.get(i));
            }
            if (result.metadatas != null && i < result.metadatas.size()) {
                Map<String, Object> meta = result.metadatas.get(i);
                doc.setTitle_wsh((String) meta.get("title_wsh"));
                doc.setCategory_wsh((String) meta.get("category_wsh"));
                doc.setSource_type_wsh((String) meta.get("source_type_wsh"));
                doc.setSource_path_wsh((String) meta.get("source_path_wsh"));
                Object wc = meta.get("word_count_wsh");
                if (wc instanceof Number n) doc.setWord_count_wsh(n.intValue());
                doc.setCreated_at_wsh(parseDateTime((String) meta.get("created_at_wsh")));
                doc.setUpdated_at_wsh(parseDateTime((String) meta.get("updated_at_wsh")));
            }
            docs.add(doc);
        }
        docs.sort((a, b) -> {
            if (a.getCreated_at_wsh() == null && b.getCreated_at_wsh() == null) return 0;
            if (a.getCreated_at_wsh() == null) return 1;
            if (b.getCreated_at_wsh() == null) return -1;
            return b.getCreated_at_wsh().compareTo(a.getCreated_at_wsh());
        });
        return docs;
    }

    /**
     * 将字符串解析为本地日期时间对象
     * @param value 待解析的日期时间字符串
     * @return 解析后的本地日期时间对象，解析失败返回null
     */
    private static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value, DTF);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 生成随机的长整型ID
     * @return 随机生成的长整型ID
     */
    private static long generateId() {
        long id = UUID.randomUUID().getLeastSignificantBits();
        return id == Long.MIN_VALUE ? 1L : Math.abs(id);
    }

    /**
     * 术语搜索方法
     * 在知识文档中进行基于关键词的文本匹配搜索
     * @param query 查询文本
     * @param allDocs 待搜索的文档集合
     * @return 匹配的知识文档列表
     */
    private List<KnowledgeDocument> termSearch(String query, List<KnowledgeDocument> allDocs) {
        Set<String> termSet = new LinkedHashSet<>();
        String[] rawTerms = query.toLowerCase().split("[\\s,]+");
        for (String term : rawTerms) {
            termSet.add(term);
            for (int i = 0; i < term.length(); i++) {
                termSet.add(String.valueOf(term.charAt(i)));
            }
        }
        String[] queryTerms = termSet.toArray(new String[0]);

        return allDocs.stream()
                .map(doc -> {
                    String content = doc.getContent_wsh().toLowerCase();
                    String title = doc.getTitle_wsh().toLowerCase();
                    int score = 0;
                    for (String term : queryTerms) {
                        if (term.isEmpty()) continue;
                        if (title.contains(term)) score += 10;
                        if (content.contains(term)) {
                            score += countOccurrences(content, term) * 2;
                        }
                    }
                    return new AbstractMap.SimpleEntry<>(doc, score);
                })
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }

    /**
     * 计算术语在文本中的出现次数
     * @param text 待搜索的文本
     * @param term 待搜索的术语
     * @return 术语在文本中的出现次数
     */
    private int countOccurrences(String text, String term) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(term, idx)) != -1) {
            count++;
            idx += term.length();
        }
        return count;
    }
}
