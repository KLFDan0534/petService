package com.pet.ai.service;

import com.pet.ai.dto.RagDocumentCreateRequestDTO;
import com.pet.ai.entity.KnowledgeDocument;

import java.util.List;

/**
 * RAG（Retrieval-Augmented Generation）服务接口，提供基于知识库的语义搜索和智能问答功能。
 * <p>
 * 知识文档存储在 Chroma 向量数据库中，支持向量相似性搜索和关键词文本匹配两种检索方式。
 * AI 问答结合知识库上下文和可选的宠物档案信息，生成个性化回答。
 */
public interface RagService {

    /**
     * 【业务名称】获取所有知识文档
     * <p>业务作用：从Chroma向量库查询所有知识文档列表，按创建时间倒序排列。</p>
     * <p>调用场景：管理员在后台查看知识库管理页面；前端知识库列表展示。</p>
     * <p>调用链：RagController.listDocuments() → RagService.listAll() → ChromaService.get() → toDocuments()转换</p>
     * <p>数据处理：调用ChromaService.get()获取所有文档的元数据和文本内容；toDocuments()将ChromaGetResult转换为KnowledgeDocument列表；按created_at_wsh倒序排序。</p>
     * <p>业务规则：Chroma为空时返回空列表而非null。</p>
     * <p>状态影响：只读操作。</p>
     * <p>异常情况：Chroma调用异常时返回空列表。</p>
     * <p>注意事项：文档ID在Chroma中以字符串存储，转换为Long时异常则用hashCode兜底。</p>
     *
     * @return 知识文档列表
     */
    List<KnowledgeDocument> listAll();

    /**
     * 【业务名称】搜索知识文档
     * <p>业务作用：根据关键词和分类搜索知识文档，双阶段检索策略——优先向量相似性搜索（EmbeddingService），未命中时降级为字符级关键词文本匹配（termSearch）。</p>
     * <p>调用场景：用户在知识库搜索框输入关键词搜索；AI问答时内部调用检索相关知识上下文。</p>
     * <p>调用链：RagController.search() → RagService.search() → listAll()全量获取 → 分类过滤 → vectorSearch()向量搜索 | termSearch()关键词搜索</p>
     * <p>数据处理：先获取全量文档；按category过滤；query不为空时先尝试向量搜索（调用EmbeddingService.embed()生成查询向量 → Chroma query API），返回空时降级为termSearch（拆分为单字+词组在title/content中匹配并评分）。</p>
     * <p>业务规则：分类和关键词均为可选过滤条件；向量搜索优先，未命中时关键词搜索兜底；query为空或null时返回按分类过滤后的全部文档。</p>
     * <p>状态影响：只读操作。</p>
     * <p>异常情况：向量搜索异常时记录error日志并返回空列表，触发关键词搜索降级。</p>
     * <p>注意事项：termSearch评分规则——标题匹配+10分，内容中每出现一次+2分；单字搜索用于处理中文分词不精确的场景。</p>
     *
     * @param query    查询关键词
     * @param category 文档分类过滤，传null或空表示不过滤
     * @return 匹配的知识文档列表，按相关度排序
     */
    List<KnowledgeDocument> search(String query, String category);

    /**
     * 【业务名称】AI问答（无宠物档案）
     * <p>业务作用：基于知识库检索结果，调用AI模型回答用户问题。不携带宠物档案信息。</p>
     * <p>调用场景：用户在前端知识库页面直接提问（不带宠物上下文）。</p>
     * <p>调用链：RagController.ask() → RagService.answer(question) → answer(question, null) → search()检索 → AiChatService.chat() → AI返回 | 降级模板</p>
     * <p>数据处理：委托answer(question, null)执行，petProfile传null即不加入宠物上下文。</p>
     * <p>业务规则：AI可用时返回AI回答；AI不可用且知识库不为空时返回知识库模板；知识库也为空时返回引导提示。</p>
     * <p>状态影响：只读操作。</p>
     * <p>异常情况：无抛出异常，任何失败返回降级文案。</p>
     * <p>注意事项：系统提示词优先从rag-system-prompt.txt加载，不存在时使用英文默认提示词。</p>
     *
     * @param question 用户问题
     * @return AI回答内容
     */
    String answer(String question);

    /**
     * 【业务名称】AI问答（带宠物档案）
     * <p>业务作用：基于知识库检索结果和宠物档案信息，调用AI模型生成个性化回答。三阶段降级：AI在线 → 宠物档案+知识库模板 → 知识库模板。</p>
     * <p>调用场景：用户在前端知识库页面提问时已关联宠物，或用户提供了宠物档案上下文。</p>
     * <p>调用链：RagController.ask() → RagService.answer(question, petProfile) → search()检索 → 构建systemPrompt+userPrompt → AiChatService.chat() → AI返回 | buildPetProfileFallback() | 知识库模板</p>
     * <p>数据处理：search()检索相关知识文档；构建systemPrompt（从文件加载或默认英文）；buildUserPrompt拼接宠物档案+知识库上下文+问题；调用AiChatService；AI返回null时降级——有宠物档案→buildPetProfileFallback()、知识库非空→知识库模板、都空→引导提示。</p>
     * <p>业务规则：AI返回优先；有宠物档案但AI不可用时使用宠物档案模板（含5条通用护理建议）；无宠物档案但知识库非空时使用知识库内容模板；都没数据时给出引导提示。</p>
     * <p>状态影响：只读操作。</p>
     * <p>异常情况：无抛出异常，所有失败路径返回降级文案。</p>
     * <p>注意事项：userPrompt中知识库上下文最多取5条相关文档；降级回答中知识库参考最多取2条。</p>
     *
     * @param question   用户问题
     * @param petProfile 宠物档案文本信息
     * @return AI回答内容
     */
    String answer(String question, String petProfile);

    /**
     * 【业务名称】创建知识文档
     * <p>业务作用：创建一条知识文档，标题和内容做HTML转义防止XSS攻击，存储到Chroma向量库并生成向量嵌入。</p>
     * <p>调用场景：管理员在后台知识库管理中手动新增知识文档。</p>
     * <p>调用链：RagController.create() → RagService.create() → HTML转义 → doCreateDocument() → storeInChroma() → EmbeddingService.embed() → ChromaService.add()</p>
     * <p>数据处理：HtmlUtils.htmlEscape()转义标题和内容；计算content.length()作为word_count；设置created_at/updated_at时间戳；调用EmbeddingService.embed()生成向量嵌入；构造metadata（title/category/source_type/source_path/word_count/时间戳）；调用ChromaService.add()存储。</p>
     * <p>业务规则：需要ADMIN角色权限（Controller层控制）；标题和内容会自动做HTML转义。</p>
     * <p>状态影响：新增一条知识文档记录（Chroma向量库中新增一个文档+向量嵌入+元数据）。</p>
     * <p>异常情况：EmbeddingService调用异常记录error日志；Chroma存储异常抛出RuntimeException。</p>
     * <p>注意事项：创建的文档ID由Chroma自动生成或UUID兜底；source_type默认为空（文件上传时为"upload"）。</p>
     *
     * @param request 创建请求DTO
     * @return 创建完成后的知识文档
     */
    KnowledgeDocument create(RagDocumentCreateRequestDTO request);

    /**
     * 【业务名称】从文件导入知识文档
     * <p>业务作用：上传.txt或.docx文件，自动提取文本内容创建知识文档，文件名作为默认标题。</p>
     * <p>调用场景：管理员在后台批量导入知识文档（如宠物护理手册、FAQ等）。</p>
     * <p>调用链：RagController.uploadDocument() → RagService.createFromFile() → extractText()提取 → doCreateDocument() → storeInChroma()</p>
     * <p>数据处理：extractText()根据文件后缀选择解析方式——.txt用UTF-8直接读取，.docx用Apache POI XWPFWordExtractor提取；title为空时截取文件名（去掉后缀）作为标题；设置source_type="upload"、source_path=fileName。</p>
     * <p>业务规则：仅支持.txt和.docx格式；title可省略（自动从文件名生成）；category可空。</p>
     * <p>状态影响：新增一条知识文档记录。</p>
     * <p>异常情况：不支持的文件格式抛出RuntimeException；.docx解析异常抛出RuntimeException。</p>
     * <p>注意事项：提取的文本不做HTML转义（文件内容信任上传者）；大文件可能导致内存压力。</p>
     *
     * @param fileName  文件名（用于提取标题和判断文件类型）
     * @param fileBytes 文件二进制数据
     * @param title     文档标题，传null则自动从文件名生成
     * @param category  文档分类
     * @return 创建完成后的知识文档
     */
    KnowledgeDocument createFromFile(String fileName, byte[] fileBytes, String title, String category);

    /**
     * 【业务名称】删除知识文档
     * <p>业务作用：根据文档ID从Chroma向量库中删除知识文档及其向量嵌入。</p>
     * <p>调用场景：管理员在后台知识库管理中删除不需要的知识文档。</p>
     * <p>调用链：RagController.delete() → RagService.delete() → ChromaService.delete()</p>
     * <p>数据处理：将Long id转为String列表传入ChromaService.delete()。</p>
     * <p>业务规则：需要ADMIN角色权限（Controller层控制）。</p>
     * <p>状态影响：从Chroma向量库中删除文档及对应向量和元数据。</p>
     * <p>异常情况：Chroma删除失败时记录warn日志（不抛出异常）。</p>
     * <p>注意事项：目前仅从Chroma删除，如后续增加MySQL存储需同步删除；删除不存在的ID不会报错。</p>
     *
     * @param id 文档ID
     */
    void delete(Long id);
}

