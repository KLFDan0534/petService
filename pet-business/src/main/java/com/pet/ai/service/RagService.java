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
     * 【业务名称】分页查询文档
     * 业务作用：按关键字/分类分页查询知识库文档。
     * 调用场景：管理端知识库管理列表。
     * 数据处理：title/content LIKE 关键字 + category 精确过滤，按创建时间倒序。
     * 业务规则：关键字为空时返回全部。
     * 状态影响：无。
     * 异常情况：无。
     */
    IPage<RagDocumentDTO> listDocuments(String query, String category, long page, long size);

    /**
     * 【业务名称】查询文档详情
     * 业务作用：按 ID 查询单个知识库文档。
     * 调用场景：管理端编辑文档时回填。
     * 数据处理：按 ID 查询。
     * 业务规则：不存在抛 404。
     * 状态影响：无。
     * 异常情况：文档不存在抛 BusinessException(404)。
     */
    RagDocumentDTO getDocument(Long id);

    /**
     * 【业务名称】创建文档
     * 业务作用：新增知识库文档，自动统计字数。
     * 调用场景：管理端新增文档。
     * 数据处理：校验标题/内容 → 统计字数 → 插入。
     * 业务规则：标题与内容必填；标题≤200字；内容≤20000字。
     * 状态影响：新增文档。
     * 异常情况：参数不合法抛 BusinessException(400)。
     */
    RagDocumentDTO createDocument(RagDocumentUpsertRequestDTO request);

    /**
     * 【业务名称】更新文档
     * 业务作用：编辑知识库文档。
     * 调用场景：管理端编辑文档。
     * 数据处理：校验 → 更新 → 重新统计字数。
     * 业务规则：不存在抛 404。
     * 状态影响：更新文档。
     * 异常情况：文档不存在抛 BusinessException(404)。
     */
    RagDocumentDTO updateDocument(Long id, RagDocumentUpsertRequestDTO request);

    /**
     * 【业务名称】删除文档
     * 业务作用：逻辑删除知识库文档。
     * 调用场景：管理端删除文档。
     * 数据处理：逻辑删除（deleted_wsh=1）。
     * 业务规则：不存在抛 404。
     * 状态影响：删除文档。
     * 异常情况：文档不存在抛 BusinessException(404)。
     */
    void deleteDocument(Long id);

    /**
     * 【业务名称】检索文档
     * 业务作用：按相关性检索知识库文档，供 AI 回复前注入提示词。
     * 调用场景：AiChatService 问答前检索。
     * 数据处理：内存关键词打分（标题/内容加权），取 top-K。
     * 业务规则：无匹配时返回空列表。
     * 状态影响：无。
     * 异常情况：无。
     */
    List<RagDocumentDTO> search(String query, int limit);
}
