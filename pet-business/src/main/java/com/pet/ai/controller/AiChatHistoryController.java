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

    /**
     * 【构造函数：注入聊天记录服务】
     *
     * 业务作用：注入 AiChatHistoryService，供会话列表与消息查询使用。
     *
     * 调用场景：容器启动创建 AiChatHistoryController Bean 时自动调用。
     *
     * 调用链：Spring IoC → AiChatHistoryController(构造) → historyService 字段赋值。
     *
     * 数据处理：仅做字段赋值。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：本控制器类级标注 isAuthenticated，所有接口都要求登录。
     */
    public AiChatHistoryController(AiChatHistoryService historyService) {
        this.historyService = historyService;
    }

    /**
     * 【获取 AI 会话列表】
     *
     * 业务作用：把用户的历史消息按 session_id 聚合为会话摘要，按最新消息时间倒序返回。
     *
     * 调用场景：智能客服页左侧会话列表加载。
     *
     * 调用链：GET /api/ai/history/sessions → AiChatHistoryController.listSessions → AiChatHistoryService.listSessions → 按 session_id 分组聚合。
     *
     * 数据处理：分组后每个会话取第一条消息内容作为摘要、记录消息条数、取首条创建时间与末条创建时间，最后按末条时间倒序排序。
     *
     * 业务规则：只能看到自己的会话，用户 ID 取自令牌。
     *
     * 状态影响：只读。
     *
     * 异常情况：未登录返回 401；会话消息为空时不会进入分组。
     *
     * 注意事项：分组依赖服务层返回顺序，取「第一条/最后一条」隐含了服务层按时间升序返回的前提；返回结构是 Map 而非 DTO。
     */
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

    /**
     * 【获取某个会话的全部消息】
     *
     * 业务作用：返回指定会话下属于当前用户的全部聊天消息。
     *
     * 调用场景：用户点击会话后加载历史对话。
     *
     * 调用链：GET /api/ai/history/sessions/{sessionId} → AiChatHistoryController.getSessionMessages → AiChatHistoryService.getSessionMessages。
     *
     * 数据处理：把会话 ID 与当前用户 ID 一起下传，由服务层做归属过滤。
     *
     * 业务规则：只能查询自己的会话。
     *
     * 状态影响：只读。
     *
     * 异常情况：会话不存在或不属于当前用户时返回空列表（由服务层实现决定）。
     *
     * 注意事项：sessionId 为字符串路径参数，未做格式校验。
     */
    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "获取会话消息", description = "返回某个会话的所有聊天消息")
    public Result<List<AiChatHistory>> getSessionMessages(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @PathVariable String sessionId) {
        return Result.success(historyService.getSessionMessages(token.getUserId(), sessionId));
    }

    /**
     * 【清空会话】
     *
     * 业务作用：逻辑删除当前用户某个会话下的全部消息。
     *
     * 调用场景：用户在会话列表点击「删除会话」。
     *
     * 调用链：DELETE /api/ai/history/sessions/{sessionId} → AiChatHistoryController.clearSession → AiChatHistoryService.clearSession。
     *
     * 数据处理：传会话 ID 与用户 ID，由服务层执行逻辑删除。
     *
     * 业务规则：只能清空自己的会话；为逻辑删除而非物理删除。
     *
     * 状态影响：更新聊天记录表的删除标记。
     *
     * 异常情况：无显式异常，即使会话不存在也返回成功。
     *
     * 注意事项：接口无返回值，调用方无法从响应判断是否真的有数据被删除。
     */
    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "清空会话", description = "逻辑删除某个会话的所有消息")
    public Result<Void> clearSession(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @PathVariable String sessionId) {
        historyService.clearSession(token.getUserId(), sessionId);
        return Result.success();
    }
}
