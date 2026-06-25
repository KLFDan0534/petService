package com.pet.customer.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.customer.entity.ChatMessage;
import com.pet.customer.service.ChatService;
import com.pet.security.JwtAuthenticationToken;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "聊天管理", description = "用户聊天/消息管理")
@Slf4j
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 获取与另一用户在某个订单中的会话消息
     * @param token 当前用户认证信息
     * @param otherUserId 对方用户ID
     * @param orderId 订单ID
     * @return 会话消息列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/conversation")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取会话消息", description = "获取与另一用户在某个订单中的会话消息")
    public Result<List<ChatMessage>> getConversation(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @RequestParam Long otherUserId,
            @RequestParam Long orderId) {
        log.info("调用 getConversation()");
        return Result.success(chatService.getConversation(token.getUserId(), otherUserId, orderId));
    }

    /**
     * 获取当前用户的未读消息列表
     * @param token 当前用户认证信息
     * @return 未读消息列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/unread")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取未读消息", description = "获取当前用户的未读消息列表")
    public Result<List<ChatMessage>> getUnread(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 getUnread()");
        return Result.success(chatService.getUnreadMessages(token.getUserId()));
    }

    /**
     * 发送聊天消息
     * @param token 当前用户认证信息
     * @param message 消息信息
     * @return 发送的消息
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "发送消息", description = "向另一用户发送聊天消息")
    public Result<ChatMessage> send(@AuthenticationPrincipal JwtAuthenticationToken token,
            @Valid @RequestBody ChatMessage message) {
        log.info("调用 send()");
        message.setFrom_user_id_wsh(token.getUserId());
        return Result.success(chatService.sendMessage(message));
    }

    /**
     * 标记单条消息为已读
     * @param token 当前用户认证信息
     * @param messageId 消息ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/read/{messageId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "标记消息已读", description = "标记单条消息为已读")
    public Result<Void> markAsRead(@AuthenticationPrincipal JwtAuthenticationToken token,
            @PathVariable Long messageId) {
        log.info("调用 markAsRead()");
        chatService.markAsRead(messageId, token.getUserId());
        return Result.success();
    }

    /**
     * 标记整个会话为已读
     * @param token 当前用户认证信息
     * @param body 请求体，包含other_user_id_wsh和可选的order_id_wsh
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/read-conversation")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "标记会话已读", description = "标记整个会话的所有消息为已读")
    public Result<Void> markConversationAsRead(@AuthenticationPrincipal JwtAuthenticationToken token,
            @RequestBody Map<String, Object> body) {
        log.info("调用 markConversationAsRead()");
        Long otherUserId = Long.valueOf(body.get("other_user_id_wsh").toString());
        Long orderId = body.get("order_id_wsh") != null ? Long.valueOf(body.get("order_id_wsh").toString()) : null;
        chatService.markConversationAsRead(token.getUserId(), otherUserId, orderId);
        return Result.success();
    }
}
