package com.pet.ai.service;

import com.pet.ai.dto.AiChatRequestDTO;
import com.pet.ai.dto.AiChatResponseDTO;

/**
 * 【业务模块】AI 智能客服
 * 业务作用：用户与 AI 客服对话。AI 回复前先检索知识库文档拼入提示词；
 * 检测到用户需要人工服务（关键词/异常/兜底）时返回 need_human=true 引导转人工。
 */
public interface AiChatService {

    /**
     * AI 客服对话
     *
     * 业务作用：检索知识库 → 组装提示词 → 调用 kenari 大模型 → 转人工判定。
     * 业务规则：知识库无匹配时不编造，引导转人工；kenari 失败自动转人工；频率限制。
     * 异常情况：无（所有 AI 异常降级为转人工）。
     * 注意事项：key 仅在服务端，前端不感知。
     *
     * @param request 用户消息与历史
     * @param userId  当前用户ID
     * @return AI 回复与是否需要转人工
     */
    AiChatResponseDTO chat(AiChatRequestDTO request, Long userId);

    /**
     * RAG 问答
     *
     * 业务作用：单轮问答（兼容 /api/rag/ask），检索知识库并返回 AI 回复。
     * 业务规则：同 chat()。
     */
    AiChatResponseDTO ask(String question, Long userId);
}
