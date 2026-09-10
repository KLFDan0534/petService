package com.pet.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.ai.dto.AiChatRequestDTO;
import com.pet.ai.dto.AiChatResponseDTO;
import com.pet.ai.dto.RagDocumentDTO;
import com.pet.ai.entity.AiConfigWsh;
import com.pet.ai.service.AiChatHistoryService;
import com.pet.ai.service.AiChatService;
import com.pet.ai.service.AiConfigService;
import com.pet.ai.service.AiConfigStore;
import com.pet.ai.service.RagService;
import com.pet.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiChatServiceImpl implements AiChatService {

    private static final int TOP_K = 3;
    private static final int MAX_HISTORY = 10;
    private static final int RATE_LIMIT_PER_MINUTE = 20;
    private static final int MAX_INPUT_LENGTH = 500;

    /** 用户输入命中即转人工的强关键词 */
    private static final List<String> HANDOFF_KEYWORDS = List.of(
            "人工", "客服", "转接", "电话", "联系", "human", "agent", "真人", "专员", "转人工");

    /** AI 回复命中即转人工的信号词 */
    private static final List<String> AI_HANDOFF_MARKERS = List.of(
            "人工客服", "转人工", "请联系客服", "联系人工", "需要人工", "无法处理", "建议联系");

    private final RagService ragService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final AiConfigService aiConfigService;
    private final AiConfigStore aiConfigStore;
    private final AiChatHistoryService historyService;

    /** 每用户滑动窗口限流：userId → 最近时间戳列表 */
    private final Map<Long, List<Long>> rateBuckets = new ConcurrentHashMap<>();

    public AiChatServiceImpl(RagService ragService, ObjectMapper objectMapper, AiConfigService aiConfigService,
                             AiConfigStore aiConfigStore, AiChatHistoryService historyService) {
        this.ragService = ragService;
        this.objectMapper = objectMapper;
        this.aiConfigService = aiConfigService;
        this.aiConfigStore = aiConfigStore;
        this.historyService = historyService;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(20));
        this.restTemplate = new RestTemplate(requestFactory);
    }

    @Override
    public AiChatResponseDTO chat(AiChatRequestDTO request, Long userId) {
        if (request == null || request.getMessage_wsh() == null || request.getMessage_wsh().isBlank()) {
            throw new BusinessException(400, "消息不能为空");
        }
        String message = request.getMessage_wsh().trim();
        if (message.length() > MAX_INPUT_LENGTH) {
            throw new BusinessException(400, "消息不能超过" + MAX_INPUT_LENGTH + "字");
        }
        assertNotRateLimited(userId);

        // 0) 确定会话ID并保存用户消息
        String sessionId = request.getSession_id_wsh();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = AiChatHistoryService.newSessionId();
        }
        final String sid = sessionId;
        try {
            historyService.saveSimple(userId, sid, "user", message);
        } catch (Exception e) {
            log.warn("保存用户消息到历史记录失败: {}", e.getMessage());
        }

        // 1) 用户明确要求人工服务 → 直接转人工
        if (containsAny(message, HANDOFF_KEYWORDS)) {
            AiChatResponseDTO response = new AiChatResponseDTO();
            response.setReply_wsh("您需要人工服务，正在为您转接。您可以点击下方按钮创建客服工单，客服将尽快与您联系。");
            response.setNeed_human_wsh(true);
            response.setSources_wsh(List.of());
            response.setSession_id_wsh(sid);
            return response;
        }

        // 2) 检索知识库
        List<RagDocumentDTO> sources = ragService.search(message, TOP_K);
        List<String> sourceTitles = sources.stream()
                .map(RagDocumentDTO::getTitle_wsh)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.toList());

        // 3) 组装提示词并调用 kenari
        String systemPrompt = buildSystemPrompt(sources);
        String reply;
        boolean aiOk;
        try {
            reply = callKenari(systemPrompt, toMessages(request, message));
            aiOk = reply != null && !reply.isBlank();
        } catch (Exception e) {
            log.warn("kenari 调用失败，自动转人工: {}", e.getMessage());
            aiOk = false;
            reply = null;
        }

        // 4) 转人工判定
        AiChatResponseDTO response = new AiChatResponseDTO();
        response.setSources_wsh(sourceTitles);
        response.setSession_id_wsh(sid);
        if (!aiOk) {
            response.setReply_wsh("AI 服务暂时不可用，已为您转接人工客服。您可以点击下方按钮创建客服工单，客服将尽快处理您的问题。");
            response.setNeed_human_wsh(true);
            return response;
        }
        if (sources.isEmpty()) {
            response.setReply_wsh(reply + "\n\n（该问题知识库暂无记载，如需人工协助请点击下方按钮转人工客服。）");
            response.setNeed_human_wsh(true);
            return response;
        }
        boolean aiAsksHuman = containsAny(reply, AI_HANDOFF_MARKERS);
        response.setReply_wsh(reply);
        response.setNeed_human_wsh(aiAsksHuman);

        // 5) 保存 AI 回复到历史记录
        try {
            historyService.save(userId, sid, "assistant", reply, sourceTitles, aiAsksHuman);
        } catch (Exception e) {
            log.warn("保存AI回复到历史记录失败: {}", e.getMessage());
        }

        return response;
    }

    @Override
    public AiChatResponseDTO ask(String question, Long userId) {
        AiChatRequestDTO request = new AiChatRequestDTO();
        request.setMessage_wsh(question);
        request.setHistory_wsh(List.of());
        return chat(request, userId);
    }

    /**
     * 【业务名称】组装系统提示词（实现）
     * 业务作用：把检索到的知识库文档注入 system 提示词，约束 AI 只依据知识库回答。
     * 数据处理：无命中时仅保留角色与不编造规则。
     * 业务规则：知识库内容原样拼接；要求 AI 不知道时引导人工。
     * 状态影响：无。
     * 异常情况：无。
     */
    private String buildSystemPrompt(List<RagDocumentDTO> sources) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是「宠物寄养平台」的智能客服助手。请用简洁、友好的中文回答用户问题。\n");
        sb.append("回答规则：\n");
        sb.append("1. 只能依据下面提供的【知识库内容】回答，不得编造平台不存在的规则或承诺。\n");
        sb.append("2. 如果知识库内容无法回答用户问题，请明确告知\"该问题需要人工客服协助\"，并建议用户转人工。\n");
        sb.append("3. 涉及退款、投诉、订单异常等需要人工处理的问题，请引导用户转人工客服。\n");
        sb.append("4. 回答控制在 300 字以内。\n");
        if (!sources.isEmpty()) {
            sb.append("\n【知识库内容】\n");
            for (int i = 0; i < sources.size(); i++) {
                RagDocumentDTO doc = sources.get(i);
                sb.append("文档").append(i + 1).append("《").append(doc.getTitle_wsh()).append("》：\n");
                sb.append(doc.getContent_wsh()).append("\n");
            }
        } else {
            sb.append("\n【知识库内容】\n（当前知识库暂无与该问题相关的内容）\n");
        }
        return sb.toString();
    }

    /**
     * 【业务名称】组装对话消息（实现）
     * 业务作用：把历史对话与当前消息转成 OpenAI 格式的 messages 数组。
     * 数据处理：仅保留最近 MAX_HISTORY 条历史，追加当前用户消息。
     * 业务规则：历史角色只接受 user/assistant。
     * 状态影响：无。
     * 异常情况：无。
     */
    private List<Map<String, String>> toMessages(AiChatRequestDTO request, String message) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (request.getHistory_wsh() != null) {
            int from = Math.max(0, request.getHistory_wsh().size() - MAX_HISTORY);
            for (int i = from; i < request.getHistory_wsh().size(); i++) {
                AiChatRequestDTO.ChatTurn turn = request.getHistory_wsh().get(i);
                if (turn == null || turn.getContent() == null || turn.getContent().isBlank()) {
                    continue;
                }
                String role = "assistant".equals(turn.getRole()) ? "assistant" : "user";
                Map<String, String> m = new LinkedHashMap<>();
                m.put("role", role);
                m.put("content", turn.getContent().trim());
                messages.add(m);
            }
        }
        Map<String, String> current = new LinkedHashMap<>();
        current.put("role", "user");
        current.put("content", message);
        messages.add(current);
        return messages;
    }

    /**
     * 【业务名称】调用 kenari 大模型（实现）
     * 业务作用：调用 OpenAI 兼容的 /chat/completions 接口获取回复。
     * 调用链：callKenari() → restTemplate.postForObject()。
     * 数据处理：POST JSON，解析 choices[0].message.content。
     * 业务规则：key 缺失抛异常；网络异常抛异常（由上层转人工）。
     * 状态影响：无。
     * 异常情况：抛 RestClientException / IOException。
     * 注意事项：endpoint 为 https://kenari.id/v1，走标准 OpenAI 协议。
     */
    private String callKenari(String systemPrompt, List<Map<String, String>> messages) throws Exception {
        // 优先使用后台持久化的智能客服(cs)生效配置，无则回退 yml 默认
        AiConfigWsh cfg = aiConfigStore.getActiveConfig(AiConfigWsh.USAGE_CS);
        String apiKey = cfg != null ? cfg.getApi_key_wsh() : aiConfigService.getApiKey();
        String model = cfg != null ? cfg.getModel_wsh() : aiConfigService.getModel();
        int maxTokens = cfg != null && cfg.getMax_tokens_wsh() != null
                ? cfg.getMax_tokens_wsh()
                : aiConfigService.getMaxTokens();
        double temperature = cfg != null && cfg.getTemperature_wsh() != null
                ? cfg.getTemperature_wsh().doubleValue()
                : aiConfigService.getTemperature();

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AI API Key 未配置，请在管理后台 → AI 配置中设置");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        List<Map<String, String>> payloadMessages = new ArrayList<>();
        Map<String, String> system = new LinkedHashMap<>();
        system.put("role", "system");
        system.put("content", systemPrompt);
        payloadMessages.add(system);
        payloadMessages.addAll(messages);
        body.put("messages", payloadMessages);
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);

        String url = cfg != null
                ? aiConfigStore.chatCompletionsUrl(cfg.getEndpoint_wsh())
                : aiConfigService.getChatCompletionsUrl();
        String raw;
        try {
            raw = restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
        } catch (RestClientException e) {
            throw new RuntimeException("kenari request failed", e);
        }
        if (raw == null || raw.isBlank()) {
            throw new RuntimeException("kenari empty response");
        }
        JsonNode root = objectMapper.readTree(raw);
        JsonNode choice = root.path("choices").path(0);
        String content = extractReplyText(choice.path("message"));
        return content.trim();
    }

    /**
     * 【业务名称】抽取 OpenAI 兼容回复正文（适配思考型模型响应）
     * 业务作用：兼容 content 为字符串或部分数组两种 OpenAI 格式；content 为空时回退 reasoning_content（思考型模型，如 glm-5.3-flash）。
     * 数据处理：message.content → 文本；数组则拼接各 part.text / 文本节点。
     * 业务规则：content 与 reasoning_content 都为空时返回空串。
     * 状态影响：无。
     */
    private String extractReplyText(JsonNode message) {
        String content = nodeToText(message.path("content"));
        if (content == null || content.isBlank()) {
            content = nodeToText(message.path("reasoning_content"));
        }
        return content == null ? "" : content.trim();
    }

    /** 将 JSON 节点转为文本：字符串原样返回；数组按 part.text / 文本节点拼接；其他返回 null。 */
    private String nodeToText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : node) {
                if (part == null || part.isNull()) continue;
                if (part.isTextual()) {
                    sb.append(part.asText());
                } else {
                    String text = part.path("text").asText(null);
                    if (text != null) sb.append(text);
                }
            }
            String joined = sb.toString();
            return joined.isEmpty() ? null : joined;
        }
        return node.asText(null);
    }

    /**
     * 【业务名称】频率限制（实现）
     * 业务作用：限制单用户每分钟请求次数，防止刷 AI 接口。
     * 数据处理：内存滑动窗口（userId → 最近1分钟时间戳）。
     * 业务规则：超过 RATE_LIMIT_PER_MINUTE 抛 429。
     * 状态影响：无。
     * 异常情况：超限抛 BusinessException(429)。
     * 注意事项：单机内存实现，多实例部署时建议换 Redis。
     */
    private void assertNotRateLimited(Long userId) {
        if (userId == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long windowStart = now - 60_000L;
        List<Long> timestamps = rateBuckets.computeIfAbsent(userId, k -> new ArrayList<>());
        synchronized (timestamps) {
            timestamps.removeIf(t -> t < windowStart);
            if (timestamps.size() >= RATE_LIMIT_PER_MINUTE) {
                throw new BusinessException(429, "提问过于频繁，请稍后再试");
            }
            timestamps.add(now);
        }
    }

    private boolean containsAny(String text, List<String> keywords) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String lower = text.toLowerCase();
        for (String keyword : keywords) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
