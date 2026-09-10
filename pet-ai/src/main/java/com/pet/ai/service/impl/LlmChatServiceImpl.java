package com.pet.ai.service.impl;

import com.pet.ai.config.AiProperties;
import com.pet.ai.entity.AiConfigWsh;
import com.pet.ai.service.AiConfigStore;
import com.pet.ai.service.LlmChatService;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 对话服务实现，基于 LangChain4j 的 OpenAiChatModel 调用 OpenAI 兼容 API。
 * <p>
 * 配置来源：优先取后台持久化的"智能下单/AI助手(agent)"生效配置（AiConfigStore），
 * 无生效配置时回退 application.yml 的 ai.deepseek 默认值。
 * 模型实例按配置指纹缓存，后台修改配置后下一次调用自动重建。
 * </p>
 */
@Service
public class LlmChatServiceImpl implements LlmChatService {

    private static final Logger log = LoggerFactory.getLogger(LlmChatServiceImpl.class);

    private final AiProperties aiProperties;
    private final AiConfigStore configStore;
    /** 配置指纹 → OpenAI 模型实例缓存 */
    private final Map<String, OpenAiChatModel> modelCache = new ConcurrentHashMap<>();

    public LlmChatServiceImpl(AiProperties aiProperties, AiConfigStore configStore) {
        this.aiProperties = aiProperties;
        this.configStore = configStore;
    }

    /** 解析当前生效配置并返回（按指纹缓存）的 OpenAI 模型；无 API Key 时返回 null。 */
    private OpenAiChatModel resolveChatModel() {
        AiConfigWsh cfg = configStore.getActiveConfig(AiConfigWsh.USAGE_AGENT);
        String endpoint = cfg != null ? cfg.getEndpoint_wsh() : aiProperties.getEndpoint();
        String apiKey = cfg != null ? cfg.getApi_key_wsh() : aiProperties.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            return null;
        }
        String model = cfg != null ? cfg.getModel_wsh() : aiProperties.getModel();
        int maxTokens = cfg != null && cfg.getMax_tokens_wsh() != null
                ? cfg.getMax_tokens_wsh()
                : aiProperties.getMaxTokens();
        double temperature = cfg != null && cfg.getTemperature_wsh() != null
                ? cfg.getTemperature_wsh().doubleValue()
                : aiProperties.getTemperature();
        String fingerprint = endpoint + "|" + apiKey + "|" + model + "|" + maxTokens + "|" + temperature;
        return modelCache.computeIfAbsent(fingerprint, key -> OpenAiChatModel.builder()
                .baseUrl(endpoint.replaceAll("/+$", ""))
                .apiKey(apiKey)
                .modelName(model)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build());
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
     * <p>注意事项：模型实例按后台生效配置动态解析并按指纹缓存；key 未配置时返回 null，所有异常统一吞掉返回 null，调用方需要感知失败场景时需额外处理。</p>
     */
    @Override
    public String chat(List<Map<String, String>> messageList) {
        OpenAiChatModel chatModel = resolveChatModel();
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
            if (content == null || content.isBlank()) {
                // 思考型模型（如 glm-5.3-flash）若 reasoning 耗尽了 max_tokens，content 可能为空
                log.warn("AI 回复内容为空（可能思考过程过长被截断，可调大 max-tokens）");
                return null;
            }
            return content;
        } catch (Exception e) {
            log.error("未能调用ai: {}", e.getMessage());
            return null;
        }
    }
}
