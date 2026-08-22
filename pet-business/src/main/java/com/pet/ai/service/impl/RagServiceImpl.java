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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public RagServiceImpl(KnowledgeDocumentMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

    @Override
    public IPage<RagDocumentDTO> listDocuments(String query, String category, long page, long size) {
        LambdaQueryWrapper<KnowledgeDocument> wrapper = new LambdaQueryWrapper<KnowledgeDocument>()
                .orderByDesc(KnowledgeDocument::getCreated_at_wsh);
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

    @Override
    public RagDocumentDTO getDocument(Long id) {
        return toDTO(requireDocument(id));
    }

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

    @Override
    @Transactional
    public void deleteDocument(Long id) {
        KnowledgeDocument doc = requireDocument(id);
        documentMapper.deleteById(doc.getId_wsh());
        log.info("删除知识库文档 id={}", id);
    }

    @Override
    public List<RagDocumentDTO> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<KnowledgeDocument> docs = documentMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocument>()
                        .orderByDesc(KnowledgeDocument::getCreated_at_wsh));
        return docs.stream()
                .map(d -> new Scored(d, score(d, query)))
                .filter(s -> s.score > 0)
                .sorted(Comparator.comparingDouble((Scored s) -> s.score).reversed())
                .limit(Math.max(1, limit))
                .map(s -> toDTO(s.doc))
                .collect(Collectors.toList());
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
        for (int i = 0; i + 2 <= cn.length(); i++) {
            tokens.add(cn.substring(i, i + 2));
        }
        for (int i = 0; i + 3 <= cn.length(); i++) {
            tokens.add(cn.substring(i, i + 3));
        }
        return tokens;
    }

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

    private KnowledgeDocument requireDocument(Long id) {
        KnowledgeDocument doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(404, "文档不存在");
        }
        return doc;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

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

    private record Scored(KnowledgeDocument doc, double score) {
    }
}
