package com.pet.ai.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.ai.dto.AgentExecuteResult;
import com.pet.ai.service.AgentService;
import com.pet.ai.service.AiChatService;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@Tag(name = "AI智能体", description = "AI智能体，通过自然语言执行业务操作")
@Slf4j
public class AgentController {

    private final AgentService agentService;
    private final AiChatService aiChatService;

    private static final String SYSTEM_PROMPT = "你是\"宠物之家\"服务平台的 AI 客服助手。\n"
            + "你的名字叫\"小宠\"。\n"
            + "开场白是：\"您好！我是宠物护理 AI 助手小宠，很高兴为您服务！请问有什么可以帮您的？\"\n"
            + "请用中文回复，保持友好、专业、耐心。\n"
            + "如果用户问的问题超出宠物护理范围，请礼貌地引导回宠物相关话题。\n"
            + "回答应简洁清晰，不要过长。";

    public AgentController(AgentService agentService, AiChatService aiChatService) {
        this.agentService = agentService;
        this.aiChatService = aiChatService;
    }

    /**
     * AI聊天助手接口
     * @param body 请求体，包含question和可选的history
     * @param token 当前用户认证信息
     * @return AI回复消息
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "ai聊天助手", description = "ai宠物交流")
    public Result<Map<String, Object>> chat(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("用户id为:{}, 发送了信息: {}", token.getUserId(),body.get("question"));
        String message = (String) body.get("question");
        if (message == null || message.trim().isEmpty()) {
            return Result.error("消息不能为空哦");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) body.get("history");
        if (history == null) history = new ArrayList<>();

        List<Map<String, String>> fullMessages = new ArrayList<>();
        fullMessages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        fullMessages.addAll(history);
        fullMessages.add(Map.of("role", "user", "content", message));

        String reply = aiChatService.chat(fullMessages);

        Map<String, Object> result = new HashMap<>();
        result.put("reply", reply);
        return Result.success(result);
    }

    /**
     * AI智能体执行自然语言业务操作
     * @param body 请求体，包含input及可选的latitude/longitude
     * @param token 当前用户认证信息
     * @return Agent执行结果
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/execute")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "执行智能体", description = "通过自然语言输入执行AI智能体")
    public Result<AgentExecuteResult> execute(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 execute()");
        String userInput = (String) body.get("input");
        if (userInput == null || userInput.trim().isEmpty()) {
            return Result.error("输入内容不能为空");
        }
        if (userInput.length() > 500) {
            return Result.error(400, "输入内容过长，请控制在 500 字符以内");
        }
        Double latitude = null;
        Double longitude = null;
        try {
            latitude = body.get("latitude") != null ? Double.parseDouble(body.get("latitude").toString()) : null;
            longitude = body.get("longitude") != null ? Double.parseDouble(body.get("longitude").toString()) : null;
        } catch (NumberFormatException e) {
            return Result.error(400, "经纬度格式错误");
        }
        Map<String, Object> result = agentService.execute(token.getUserId(), userInput, latitude, longitude);
        AgentExecuteResult dto = new AgentExecuteResult();
        dto.setStatus((String) result.get("status"));
        dto.setOrderNo((String) result.get("orderNo"));
        dto.setPayNo((String) result.get("payNo"));
        dto.setSelectedKeeper(result.get("selectedKeeper"));
        dto.setPetName((String) result.get("petName"));
        dto.setDays((Integer) result.get("days"));
        dto.setLogs((java.util.List<String>) result.get("logs"));
        dto.setCurrentStep((Integer) result.get("currentStep"));
        dto.setError((String) result.get("error"));
        return Result.success(dto);
    }
}
