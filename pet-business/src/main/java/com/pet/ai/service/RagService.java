package com.pet.ai.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.ai.dto.RagDocumentDTO;
import com.pet.ai.dto.RagDocumentUpsertRequestDTO;

import java.util.List;

/**
 * 【业务模块】RAG 知识库文档管理
 * 业务作用：知识库文档的增删改查与检索。AI 回复前先调用 search() 检索相关文档拼入提示词。
 */
public interface RagService {

    /**
     * 分页查询文档
     *
     * 业务作用：按关键字/分类分页查询知识库文档。
     * 业务规则：关键字为空时返回全部。
     */
    IPage<RagDocumentDTO> listDocuments(String query, String category, long page, long size);

    /**
     * 查询文档详情
     *
     * 业务作用：按 ID 查询单个知识库文档。
     * 业务规则：不存在抛 404。
     * 异常情况：文档不存在抛 BusinessException(404)。
     */
    RagDocumentDTO getDocument(Long id);

    /**
     * 创建文档
     *
     * 业务作用：新增知识库文档，自动统计字数。
     * 业务规则：标题与内容必填；标题≤200字；内容≤20000字。
     * 异常情况：参数不合法抛 BusinessException(400)。
     */
    RagDocumentDTO createDocument(RagDocumentUpsertRequestDTO request);

    /**
     * 更新文档
     *
     * 业务作用：编辑知识库文档。
     * 业务规则：不存在抛 404。
     * 异常情况：文档不存在抛 BusinessException(404)。
     */
    RagDocumentDTO updateDocument(Long id, RagDocumentUpsertRequestDTO request);

    /**
     * 删除文档
     *
     * 业务作用：逻辑删除知识库文档。
     * 业务规则：不存在抛 404。
     * 异常情况：文档不存在抛 BusinessException(404)。
     */
    void deleteDocument(Long id);

    /**
     * 检索文档
     *
     * 业务作用：按相关性检索知识库文档，供 AI 回复前注入提示词。
     * 业务规则：无匹配时返回空列表。
     */
    List<RagDocumentDTO> search(String query, int limit);

    /**
     * 从文件导入文档
     *
     * 业务作用：上传 .txt/.docx 文件，提取文本后按普通文档创建，写入 MySQL。
     * 业务规则：仅支持 .txt 和 .docx；内容与标题校验复用 createDocument。
     * 异常情况：格式不支持或解析失败抛 BusinessException(400)。
     */
    RagDocumentDTO createFromFile(String fileName, byte[] fileBytes, String title, String category);
}
