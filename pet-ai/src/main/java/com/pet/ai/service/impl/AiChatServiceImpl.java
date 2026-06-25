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

    @Override
    public String chat(String systemPrompt, String userMessage) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userMessage));
        return chat(messages);
    }

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
