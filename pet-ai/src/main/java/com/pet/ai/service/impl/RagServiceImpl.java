package com.pet.ai.service.impl;

import com.pet.ai.dto.RagDocumentCreateRequestDTO;
import com.pet.ai.service.AiChatService;
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
 * RAG（Retrieval-Augmented Generation）服务实现类
 * 提供基于知识库的语义搜索和问答功能
 * 主要功能包括：
 * 1. 知识文档管理（增删查）
 * 2. 向量搜索和关键词搜索
 * 3. 基于AI模型的智能问答
 * 4. 宠物档案个性化处理
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
    private final AiChatService aiChatService;
    private final String chromaBaseUrl;
    private final String chromaTenant;
    private final String chromaDatabase;

    private String collectionId;

    public RagServiceImpl(ChromaService chromaService,
                          EmbeddingService embeddingService,
                          AiChatService aiChatService,
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

    @Override
    public List<KnowledgeDocument> listAll() {
        log.info("调用 listAll()");
        ChromaGetResult result = chromaService.get(getCollectionId(), null, null,
                List.of("metadatas", "documents"));
        return toDocuments(result);
    }

    /**
     * 根据查询条件和分类搜索知识文档
     * @param query 查询条件
     * @param category 分类过滤
     * @return 匹配的知识文档列表
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
     * 基于知识库的AI问答接口（无宠物档案）
     * @param question 用户问题
     * @return AI回答结果
     */
    @Override
    public String answer(String question) {
        return answer(question, null);
    }

    /**
     * 基于知识库的AI问答接口
     * @param question 用户问题
     * @param petProfile 宠物档案信息
     * @return AI回答结果
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
     * 创建新的知识文档
     * @param doc 待创建的知识文档
     * @return 创建后的知识文档
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
     * 删除知识文档
     * @param id 待删除文档的ID
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
