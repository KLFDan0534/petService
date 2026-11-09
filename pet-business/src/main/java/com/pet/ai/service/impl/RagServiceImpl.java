package com.pet.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.ai.dto.RagDocumentDTO;
import com.pet.ai.dto.RagDocumentUpsertRequestDTO;
import com.pet.ai.entity.KnowledgeDocument;
import com.pet.ai.mapper.KnowledgeDocumentMapper;
import com.pet.ai.service.RagService;
import com.pet.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RagServiceImpl implements RagService {

    private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z0-9]+");
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_CONTENT_LENGTH = 20000;

    private final KnowledgeDocumentMapper documentMapper;

    /**
     * 【知识库服务实现的构造注入】
     *
     * 业务作用：注入知识库文档 Mapper。
     *
     * 调用场景：Spring 容器创建 RagServiceImpl 时。
     *
     * 调用链：Spring 容器 → 本构造方法 → 赋值 documentMapper。
     *
     * 数据处理：仅保存依赖引用。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：无。
     */
    public RagServiceImpl(KnowledgeDocumentMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

    /**
     * 【分页查询知识库文档】
     *
     * 业务作用：按关键字与分类分页查询知识库文档，按创建时间倒序。
     *
     * 调用场景：后台知识库列表页。
     *
     * 调用链：RagController → listDocuments() → documentMapper.selectPage() → toDTO()。
     *
     * 数据处理：query 非空白时对标题或内容做 LIKE；category 非空白时等值过滤；结果按页转换 DTO。
     *
     * 业务规则：关键字与分类均为可选条件。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：无。
     */
    @Override
    public IPage<RagDocumentDTO> listDocuments(String query, String category, long page, long size) {
        LambdaQueryWrapper<KnowledgeDocument> wrapper = new LambdaQueryWrapper<KnowledgeDocument>()
                .orderByDesc(KnowledgeDocument::getCreated_at_wsh);
        // 标题或内容命中任一即可
        if (query != null && !query.isBlank()) {
            String kw = query.trim();
            wrapper.and(w -> w.like(KnowledgeDocument::getTitle_wsh, kw)
                    .or()
                    .like(KnowledgeDocument::getContent_wsh, kw));
        }
        if (category != null && !category.isBlank()) {
            wrapper.eq(KnowledgeDocument::getCategory_wsh, category.trim());
        }
        Page<KnowledgeDocument> p = new Page<>(page, size);
        IPage<KnowledgeDocument> result = documentMapper.selectPage(p, wrapper);
        return result.convert(this::toDTO);
    }

    /**
     * 【查询单个知识库文档】
     *
     * 业务作用：按 ID 获取知识库文档详情。
     *
     * 调用场景：后台查看文档详情。
     *
     * 调用链：RagController → getDocument() → requireDocument() → toDTO()。
     *
     * 数据处理：查询单条记录后转换为 DTO。
     *
     * 业务规则：文档必须存在。
     *
     * 状态影响：无。
     *
     * 异常情况：不存在抛 BusinessException(404, "文档不存在")。
     *
     * 注意事项：无。
     */
    @Override
    public RagDocumentDTO getDocument(Long id) {
        return toDTO(requireDocument(id));
    }

    /**
     * 【新增知识库文档】
     *
     * 业务作用：校验入参后插入一条知识库文档。
     *
     * 调用场景：后台手工新增知识文档。
     *
     * 调用链：RagController → createDocument() → validate() → documentMapper.insert() → toDTO()。
     *
     * 数据处理：标题与内容 trim 后落库，分类与来源类型经 trimToNull 处理，word_count_wsh 取内容 trim 后的字符长度。
     *
     * 业务规则：标题与内容必填，标题≤200 字、内容≤20000 字。
     *
     * 状态影响：新增 knowledge_document 表记录。
     *
     * 异常情况：参数不合法抛 BusinessException(400)。
     *
     * 注意事项：word_count_wsh 统计的是字符数而非分词数；@Transactional 保证单条插入。
     */
    @Override
    @Transactional
    public RagDocumentDTO createDocument(RagDocumentUpsertRequestDTO request) {
        validate(request);
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle_wsh(request.getTitle_wsh().trim());
        doc.setContent_wsh(request.getContent_wsh().trim());
        doc.setCategory_wsh(trimToNull(request.getCategory_wsh()));
        doc.setSource_type_wsh(trimToNull(request.getSource_type_wsh()));
        doc.setWord_count_wsh(request.getContent_wsh().trim().length());
        documentMapper.insert(doc);
        log.info("创建知识库文档 id={}", doc.getId_wsh());
        return toDTO(doc);
    }

    /**
     * 【更新知识库文档】
     *
     * 业务作用：覆盖式更新已有文档的标题、内容、分类与来源类型。
     *
     * 调用场景：后台编辑知识文档。
     *
     * 调用链：RagController → updateDocument() → validate() → requireDocument() → documentMapper.updateById() → toDTO()。
     *
     * 数据处理：同 createDocument()，按 id 更新全部可编辑字段并重算 word_count_wsh。
     *
     * 业务规则：文档必须存在；字段校验规则与新增一致。
     *
     * 状态影响：更新 knowledge_document 表记录。
     *
     * 异常情况：文档不存在抛 404；参数不合法抛 400。
     *
     * 注意事项：全量覆盖，未传的分类与来源类型会被置为 null。
     */
    @Override
    @Transactional
    public RagDocumentDTO updateDocument(Long id, RagDocumentUpsertRequestDTO request) {
        validate(request);
        KnowledgeDocument doc = requireDocument(id);
        doc.setTitle_wsh(request.getTitle_wsh().trim());
        doc.setContent_wsh(request.getContent_wsh().trim());
        doc.setCategory_wsh(trimToNull(request.getCategory_wsh()));
        doc.setSource_type_wsh(trimToNull(request.getSource_type_wsh()));
        doc.setWord_count_wsh(request.getContent_wsh().trim().length());
        documentMapper.updateById(doc);
        log.info("更新知识库文档 id={}", id);
        return toDTO(doc);
    }

    /**
     * 【删除知识库文档】
     *
     * 业务作用：按 ID 删除知识库文档。
     *
     * 调用场景：后台删除知识文档。
     *
     * 调用链：RagController → deleteDocument() → requireDocument() → documentMapper.deleteById()。
     *
     * 数据处理：先确认存在，再按主键删除。
     *
     * 业务规则：文档必须存在。
     *
     * 状态影响：删除 knowledge_document 表记录。
     *
     * 异常情况：文档不存在抛 BusinessException(404)。
     *
     * 注意事项：物理删除，无回收站与软删标记。
     */
    @Override
    @Transactional
    public void deleteDocument(Long id) {
        KnowledgeDocument doc = requireDocument(id);
        documentMapper.deleteById(doc.getId_wsh());
        log.info("删除知识库文档 id={}", id);
    }

    /**
     * 【知识库关键词检索】
     *
     * 业务作用：对全部知识文档做关键词打分排序，返回最相关的前 N 条。
     *
     * 调用场景：AI 对话前检索知识库上下文。
     *
     * 调用链：AiChatServiceImpl → search() → documentMapper.selectList() → score() → toDTO()。
     *
     * 数据处理：全量加载文档 → 逐条计算得分 → 过滤得分大于 0 → 按得分倒序 → 截取 limit 条 → 转 DTO。
     *
     * 业务规则：查询词为空返回空列表；limit 至少为 1。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：全表加载后在内存打分，文档量大时性能差，且不是向量检索。
     */
    @Override
    public List<RagDocumentDTO> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<KnowledgeDocument> docs = documentMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocument>()
                        .orderByDesc(KnowledgeDocument::getCreated_at_wsh));
        return docs.stream()
                // 全量加载后在内存打分排序
                .map(d -> new Scored(d, score(d, query)))
                .filter(s -> s.score > 0)
                .sorted(Comparator.comparingDouble((Scored s) -> s.score).reversed())
                .limit(Math.max(1, limit))
                .map(s -> toDTO(s.doc))
                .collect(Collectors.toList());
    }

    /**
     * 【从上传文件创建知识文档】
     *
     * 业务作用：解析 txt/docx 文件正文并创建知识库文档。
     *
     * 调用场景：后台上传知识文件。
     *
     * 调用链：RagController → createFromFile() → extractText() → titleFromFileName() → createDocument()。
     *
     * 数据处理：校验文件名 → 提取正文 → 标题为空时用文件名去后缀作为标题 → 组装请求并复用创建逻辑，来源类型固定为 upload。
     *
     * 业务规则：文件名必填；仅支持 .txt 与 .docx。
     *
     * 状态影响：新增 knowledge_document 表记录。
     *
     * 异常情况：文件名为空、格式不支持、docx 解析失败均抛 BusinessException(400)。
     *
     * 注意事项：内容长度受 createDocument() 的 20000 字上限约束，超长文件会创建失败。
     */
    @Override
    @Transactional
    public RagDocumentDTO createFromFile(String fileName, byte[] fileBytes, String title, String category) {
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException(400, "文件名不能为空");
        }
        String content = extractText(fileName, fileBytes);
        String resolvedTitle = (title == null || title.isBlank()) ? titleFromFileName(fileName) : title;
        RagDocumentUpsertRequestDTO request = new RagDocumentUpsertRequestDTO();
        request.setTitle_wsh(resolvedTitle);
        request.setContent_wsh(content);
        request.setCategory_wsh(category);
        request.setSource_type_wsh("upload");
        return createDocument(request);
    }

    /**
     * 从文件名提取默认标题（去掉最后一个 .后缀）。
     */
    private String titleFromFileName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    /**
     * 按后缀解析文件文本：.txt 直读 UTF-8；.docx 用 POI 提取。
     */
    private String extractText(String fileName, byte[] fileBytes) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".txt")) {
            return new String(fileBytes, StandardCharsets.UTF_8);
        }
        if (lower.endsWith(".docx")) {
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(fileBytes));
                 XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                return extractor.getText();
            } catch (Exception e) {
                log.error("从docx文件提取文本失败: {}", fileName, e);
                throw new BusinessException(400, "无法解析Word文档: " + fileName);
            }
        }
        throw new BusinessException(400, "不支持的文件格式，仅支持 .txt 和 .docx 文件");
    }

    /**
     * 【业务名称】文档相关性打分（实现）
     * 业务作用：计算查询词与文档标题/内容的匹配得分。
     * 数据处理：标题命中权重高（5+8），内容命中权重低（2+4），整句命中额外加权。
     * 业务规则：得分大于0才视为相关。
     * 状态影响：无。
     * 异常情况：无。
     */
    private double score(KnowledgeDocument doc, String query) {
        String title = doc.getTitle_wsh() == null ? "" : doc.getTitle_wsh().toLowerCase();
        String content = doc.getContent_wsh() == null ? "" : doc.getContent_wsh().toLowerCase();
        String q = query.toLowerCase();
        double score = 0;
        // 整句命中标题权重最高
        if (title.contains(q)) score += 8;
        if (content.contains(q)) score += 4;
        for (String token : tokenize(q)) {
            if (title.contains(token)) score += 5;
            if (content.contains(token)) score += 2;
        }
        return score;
    }

    /**
     * 【业务名称】查询词切分（实现）
     * 业务作用：把查询词切成英文单词 + 中文2/3字滑窗，用于打分。
     * 数据处理：英文单词按空白/非字母数字切；中文按连续汉字滑窗。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     */
    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        Matcher m = WORD_PATTERN.matcher(text);
        while (m.find()) {
            tokens.add(m.group().toLowerCase());
        }
        String cn = text.replaceAll("[^\\u4e00-\\u9fa5]", "");
        // 中文按 2 字滑窗切词
        for (int i = 0; i + 2 <= cn.length(); i++) {
            tokens.add(cn.substring(i, i + 2));
        }
        for (int i = 0; i + 3 <= cn.length(); i++) {
            tokens.add(cn.substring(i, i + 3));
        }
        return tokens;
    }

    /**
     * 【校验文档写入请求】
     *
     * 业务作用：校验新增/更新文档请求的必填项与长度上限。
     *
     * 调用场景：createDocument() 与 updateDocument() 的第一道校验。
     *
     * 调用链：createDocument()/updateDocument() → validate()。
     *
     * 数据处理：检查请求非空、标题非空白、内容非空白，并比对 MAX_TITLE_LENGTH(200) 与 MAX_CONTENT_LENGTH(20000)。
     *
     * 业务规则：标题与内容均按 trim 后长度判断。
     *
     * 状态影响：无。
     *
     * 异常情况：任一不满足抛 BusinessException(400)，消息分别为文档内容不能为空、文档标题不能为空、文档标题不能超过200字、文档内容不能超过20000字。
     *
     * 注意事项：不校验分类与来源类型的合法性。
     */
    private void validate(RagDocumentUpsertRequestDTO request) {
        if (request == null) {
            throw new BusinessException(400, "文档内容不能为空");
        }
        if (request.getTitle_wsh() == null || request.getTitle_wsh().trim().isEmpty()) {
            throw new BusinessException(400, "文档标题不能为空");
        }
        if (request.getContent_wsh() == null || request.getContent_wsh().trim().isEmpty()) {
            throw new BusinessException(400, "文档内容不能为空");
        }
        if (request.getTitle_wsh().trim().length() > MAX_TITLE_LENGTH) {
            throw new BusinessException(400, "文档标题不能超过" + MAX_TITLE_LENGTH + "字");
        }
        if (request.getContent_wsh().trim().length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(400, "文档内容不能超过" + MAX_CONTENT_LENGTH + "字");
        }
    }

    /**
     * 【加载文档并校验存在】
     *
     * 业务作用：按主键加载知识文档，不存在则抛异常。
     *
     * 调用场景：getDocument、updateDocument、deleteDocument。
     *
     * 调用链：各业务方法 → requireDocument() → documentMapper.selectById()。
     *
     * 数据处理：按 id 查询 knowledge_document 单条记录。
     *
     * 业务规则：查不到即视为不存在。
     *
     * 状态影响：无。
     *
     * 异常情况：记录为空抛 BusinessException(404, "文档不存在")。
     *
     * 注意事项：源码中该方法声明与第一行语句写在同一行，属既有排版问题。
     */
    private KnowledgeDocument requireDocument(Long id) {        KnowledgeDocument doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(404, "文档不存在");
        }
        return doc;
    }

    /**
     * 【去空白并转 null】
     *
     * 业务作用：把空白字符串规范化为 null，避免库里出现空串。
     *
     * 调用场景：createDocument() 与 updateDocument() 处理分类、来源类型字段。
     *
     * 调用链：createDocument()/updateDocument() → trimToNull()。
     *
     * 数据处理：null 直接返回 null，否则 trim 后为空则返回 null，否则返回 trim 结果。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：无。
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * 【知识文档实体转 DTO】
     *
     * 业务作用：把 KnowledgeDocument 实体转换为 RagDocumentDTO。
     *
     * 调用场景：列表、详情、创建、更新、检索结果返回前。
     *
     * 调用链：各业务方法 → toDTO()。
     *
     * 数据处理：逐字段拷贝 id、标题、内容、分类、来源类型、字数、创建与更新时间。
     *
     * 业务规则：实体为 null 时返回 null。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：返回完整正文内容，列表接口也携带全文，数据量较大。
     */
    private RagDocumentDTO toDTO(KnowledgeDocument doc) {
        if (doc == null) {
            return null;
        }
        RagDocumentDTO dto = new RagDocumentDTO();
        dto.setId_wsh(doc.getId_wsh());
        dto.setTitle_wsh(doc.getTitle_wsh());
        dto.setContent_wsh(doc.getContent_wsh());
        dto.setCategory_wsh(doc.getCategory_wsh());
        dto.setSource_type_wsh(doc.getSource_type_wsh());
        dto.setWord_count_wsh(doc.getWord_count_wsh());
        dto.setCreated_at_wsh(doc.getCreated_at_wsh());
        dto.setUpdated_at_wsh(doc.getUpdated_at_wsh());
        return dto;
    }

    /** 【检索打分中间结果】内部 record，临时保存文档实体与其相关性得分，仅用于 search() 内部排序，不对外暴露。 */
    private record Scored(KnowledgeDocument doc, double score) {
    }
}
