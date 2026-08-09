package com.pet.ai.service.impl;

import com.pet.ai.config.AiProperties;
import com.pet.ai.service.AiChatService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 对话服务实现，基于 LangChain4j 的 OpenAiChatModel 调用 DeepSeek API。
 * <p>
 * API Key 未配置时返回 null 而非抛出异常，由调用方决定降级策略。
 */
@Service
public class AiChatServiceImpl implements AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);

    private final OpenAiChatModel chatModel;

    public AiChatServiceImpl(AiProperties aiProperties) {
        String apiKey = aiProperties.getApiKey();
        if (apiKey != null && !apiKey.isEmpty()) {
            this.chatModel = OpenAiChatModel.builder()
                    .baseUrl(aiProperties.getEndpoint())
                    .apiKey(apiKey)
                    .modelName(aiProperties.getModel())
                    .maxTokens(aiProperties.getMaxTokens())
                    .temperature(aiProperties.getTemperature())
                    .build();
        } else {
            this.chatModel = null;
        }
    }

    /**
     * 【业务名称】AI单轮对话实现
     * <p>业务作用：将systemPrompt和userMessage组装为两元素消息列表，委托给多轮对话方法chat(List)执行。</p>
     * <p>调用场景：Controller层需要带系统提示词的AI对话时调用。</p>
     * <p>调用链：chat(String, String) → chat(List) → langchain4j → DeepSeek API</p>
     * <p>数据处理：构造ArrayList，先添加system角色消息，再添加user角色消息。</p>
     * <p>业务规则：始终添加两条消息（system + user），不做空值过滤。</p>
     * <p>状态影响：无。</p>
     * <p>异常情况：无直接异常，由底层chat(List)方法处理。</p>
     * <p>注意事项：不校验systemPrompt和userMessage是否为null或空。</p>
     */
    @Override
    public String chat(String systemPrompt, String userMessage) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userMessage));
        return chat(messages);
    }

    /**
     * 【业务名称】AI多轮对话实现
     * <p>业务作用：将Map格式消息列表转换为langchain4j的ChatMessage列表，调用OpenAiChatModel获取AI回复。</p>
     * <p>调用场景：各类需要AI对话能力的业务场景统一调用的核心方法。</p>
     * <p>调用链：调用方 → chat(List) → LangChain4j OpenAiChatModel.chat() → DeepSeek /v1/chat/completions API</p>
     * <p>数据处理：遍历messageList，按role字段分发转换为SystemMessage/AiMessage/UserMessage；构建ChatRequest；调用AI模型；从response.aiMessage()提取text()。</p>
     * <p>业务规则：chatModel为null（API未配置）时记录警告日志并返回null；消息转换不校验content是否为空。</p>
     * <p>状态影响：无状态变更。</p>
     * <p>异常情况：any Exception被捕获，记录error日志，返回null。不向上抛出异常。</p>
     * <p>注意事项：API Key在构造函数中初始化chatModel，key为空时chatModel为null；所有异常统一吞掉返回null，调用方需要感知失败场景时需额外处理。</p>
     */
    @Override
    public String chat(List<Map<String, String>> messageList) {
        if (chatModel == null) {
            log.warn("apikey没有配置,返回默认的回复");
            return null;
        }

        //  转换成langchain4j的message,存入列表中
        try {
            List<ChatMessage> langchainMessages = new ArrayList<>();
            for (Map<String, String> msg : messageList) {
                String role = msg.get("role");
                String content = msg.get("content");
                if ("system".equals(role)) {
                    langchainMessages.add(new SystemMessage(content));
                } else if ("assistant".equals(role)) {
                    langchainMessages.add(new AiMessage(content));
                } else {
                    langchainMessages.add(new UserMessage(content));
                }
            }

            // 构建请求
            ChatRequest request = ChatRequest.builder()
                    .messages(langchainMessages)
                    .build();
            var response = chatModel.chat(request);
            String content = response.aiMessage().text();
            log.debug("LangChain4j的AI的回复接收 ({} chars)", content != null ? content.length() : 0);
            return content;
        } catch (Exception e) {
            log.error("未能调用ai: {}", e.getMessage());
            return null;
        }
    }
}
