package com.pet.ai.service;

import java.util.List;
import java.util.Map;

/**
 * AI 对话服务接口，封装与 DeepSeek (OpenAI 兼容) 大语言模型的交互。
 * <p>
 * 支持单轮对话（系统提示词 + 用户消息）和多轮对话（消息列表）两种调用方式。
 * 底层使用 LangChain4j 的 OpenAiChatModel 实现，通过 AiProperties 配置。
 */
public interface LlmChatService {
    /**
     * 【业务名称】AI单轮对话（带系统提示词）
     * <p>业务作用：向DeepSeek大语言模型发送带系统提示词的单轮对话请求，获取AI回复内容。系统提示词用于设定AI角色和行为规范，用户消息为具体问题或指令。</p>
     * <p>调用场景：AI客服聊天（AgentController.chat）、知识库问答（RagServiceImpl.answer）、AI报告生成（AiReportServiceImpl）等需要AI对话能力的场景。</p>
     * <p>调用链：Controller层接收请求 → LlmChatService.chat() → 组装为多轮消息列表 → chat(List) → OpenAiChatModel.chat() → DeepSeek API</p>
     * <p>数据处理：将systemPrompt和userMessage转换为List&lt;Map&lt;String,String&gt;&gt;格式（role+content），委托给多轮对话方法处理。</p>
     * <p>业务规则：systemPrompt为空时仍作为有效消息发送；chatModel为null时（API未配置）直接返回null。</p>
     * <p>状态影响：无状态变更，仅返回AI回复文本。</p>
     * <p>异常情况：AI API调用异常时记录错误日志并返回null，不影响主流程。</p>
     * <p>注意事项：调用方需自行处理null返回值做降级方案；API密钥未配置时不报错仅返回null。</p>
     *
     * @param systemPrompt 系统提示词，用于设定AI角色和行为规范
     * @param userMessage  用户消息，即具体的问题或指令
     * @return AI回复内容，API不可用时返回null
     */
    String chat(String systemPrompt, String userMessage);

    /**
     * 【业务名称】AI多轮对话
     * <p>业务作用：向DeepSeek大语言模型发送多轮对话消息列表（支持system/assistant/user角色），获取AI回复内容。支持上下文记忆的多轮交互。</p>
     * <p>调用场景：AI客服多轮对话（传递历史消息）、AI报告生成的prompt构造、Agent执行时的信息收集等需要多轮消息的场景。</p>
     * <p>调用链：Controller层接收请求 → LlmChatService.chat() → 转换为langchain4j.ChatMessage → 构建ChatRequest → OpenAiChatModel.chat() → DeepSeek API</p>
     * <p>数据处理：将Map格式消息按role字段转换为langchain4j的SystemMessage/AiMessage/UserMessage；构建ChatRequest对象调用AI模型；解析响应提取aiMessage.text()。</p>
     * <p>业务规则：chatModel为null时直接返回null；role="system"→SystemMessage，role="assistant"→AiMessage，其他角色→UserMessage。</p>
     * <p>状态影响：无状态变更，仅返回AI回复文本。</p>
     * <p>异常情况：消息转换或API调用异常时捕获Exception记录错误日志并返回null。</p>
     * <p>注意事项：调用方需确保messages列表不为空且至少包含一条user消息；返回null时调用方应提供降级文案。</p>
     *
     * @param messages 消息列表，每项包含role（system/assistant/user）和content字段
     * @return AI回复内容，API不可用时返回null
     */
    String chat(List<Map<String, String>> messages);
}
