package com.pet.ai.controller;

import com.pet.ai.entity.AiChatHistory;
import com.pet.ai.service.AiChatHistoryService;
import com.pet.common.Result;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 聊天记录管理接口
 */
@RestController
@RequestMapping("/api/ai/history")
@Tag(name = "【智能客服】聊天记录", description = "AI 聊天记录持久化，支持会话列表、历史消息、清空")
@PreAuthorize("isAuthenticated()")
public class AiChatHistoryController {

    private final AiChatHistoryService historyService;

    public AiChatHistoryController(AiChatHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/sessions")
    @Operation(summary = "获取会话列表", description = "返回用户所有 AI 聊天会话，按最新消息时间倒序")
    public Result<List<Map<String, Object>>> listSessions(@AuthenticationPrincipal JwtAuthenticationToken token) {
        List<AiChatHistory> all = historyService.listSessions(token.getUserId());
        // 按 session_id 分组，每个 session 取第一条消息作为摘要 + 最后一条消息时间
        Map<String, List<AiChatHistory>> grouped = all.stream()
                .collect(Collectors.groupingBy(AiChatHistory::getSession_id_wsh));

        List<Map<String, Object>> sessions = grouped.entrySet().stream()
                .map(entry -> {
                    List<AiChatHistory> msgs = entry.getValue();
                    AiChatHistory first = msgs.get(0);
                    AiChatHistory last = msgs.get(msgs.size() - 1);
                    Map<String, Object> summary = new java.util.LinkedHashMap<>();
                    summary.put("session_id_wsh", entry.getKey());
                    summary.put("first_message_wsh", first.getContent_wsh());
                    summary.put("message_count_wsh", msgs.size());
                    summary.put("created_at_wsh", first.getCreated_at_wsh());
                    summary.put("updated_at_wsh", last.getCreated_at_wsh());
                    return summary;
                })
                .sorted((a, b) -> {
                    var ta = (java.time.LocalDateTime) a.get("updated_at_wsh");
                    var tb = (java.time.LocalDateTime) b.get("updated_at_wsh");
                    return tb.compareTo(ta);
                })
                .collect(Collectors.toList());

        return Result.success(sessions);
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "获取会话消息", description = "返回某个会话的所有聊天消息")
    public Result<List<AiChatHistory>> getSessionMessages(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @PathVariable String sessionId) {
        return Result.success(historyService.getSessionMessages(token.getUserId(), sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "清空会话", description = "逻辑删除某个会话的所有消息")
    public Result<Void> clearSession(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @PathVariable String sessionId) {
        historyService.clearSession(token.getUserId(), sessionId);
        return Result.success();
    }
}
