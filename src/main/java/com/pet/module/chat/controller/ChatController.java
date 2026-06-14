package com.pet.module.chat.controller;

import com.pet.common.Result;
import com.pet.module.chat.entity.ChatMessage;
import com.pet.module.chat.service.ChatService;
import com.pet.security.JwtAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "聊天管理", description = "用户之间的聊天消息管理")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/conversation")
    @Operation(summary = "获取聊天记录", description = "获取与指定用户的聊天对话记录")
    public Result<List<ChatMessage>> getConversation(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @RequestParam Long otherUserId,
            @RequestParam(required = false) Long orderId) {
        return Result.success(chatService.getConversation(token.getUserId(), otherUserId, orderId));
    }

    @GetMapping("/unread")
    @Operation(summary = "获取未读消息", description = "获取当前用户的所有未读消息")
    public Result<List<ChatMessage>> getUnread(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(chatService.getUnreadMessages(token.getUserId()));
    }

    @PostMapping("/send")
    @Operation(summary = "发送消息", description = "向指定用户发送一条聊天消息")
    public Result<ChatMessage> send(@AuthenticationPrincipal JwtAuthenticationToken token,
                                     @RequestBody ChatMessage message) {
        message.setFromUserId(token.getUserId());
        return Result.success(chatService.sendMessage(message));
    }

    @PostMapping("/read/{messageId}")
    @Operation(summary = "标记消息已读", description = "将指定消息标记为已读")
    public Result<Void> markAsRead(@AuthenticationPrincipal JwtAuthenticationToken token,
                                    @PathVariable Long messageId) {
        chatService.markAsRead(messageId, token.getUserId());
        return Result.success();
    }

    @PostMapping("/read-conversation")
    @Operation(summary = "标记对话已读", description = "将整个对话的所有消息标记为已读")
    public Result<Void> markConversationAsRead(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                @RequestBody Map<String, Object> body) {
        Long otherUserId = Long.valueOf(body.get("otherUserId").toString());
        Long orderId = body.get("orderId") != null ? Long.valueOf(body.get("orderId").toString()) : null;
        chatService.markConversationAsRead(otherUserId, token.getUserId(), orderId);
        return Result.success();
    }
}
