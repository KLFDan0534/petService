package com.pet.module.chat.controller;

import com.pet.common.Result;
import com.pet.module.chat.entity.ChatMessage;
import com.pet.module.chat.service.ChatService;
import com.pet.security.JwtAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/conversation")
    public Result<List<ChatMessage>> getConversation(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @RequestParam Long otherUserId,
            @RequestParam(required = false) Long orderId) {
        return Result.success(chatService.getConversation(token.getUserId(), otherUserId, orderId));
    }

    @GetMapping("/unread")
    public Result<List<ChatMessage>> getUnread(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(chatService.getUnreadMessages(token.getUserId()));
    }

    @PostMapping("/send")
    public Result<ChatMessage> send(@AuthenticationPrincipal JwtAuthenticationToken token,
                                     @RequestBody ChatMessage message) {
        message.setFromUserId(token.getUserId());
        return Result.success(chatService.sendMessage(message));
    }

    @PostMapping("/read/{messageId}")
    public Result<Void> markAsRead(@AuthenticationPrincipal JwtAuthenticationToken token,
                                    @PathVariable Long messageId) {
        chatService.markAsRead(messageId, token.getUserId());
        return Result.success();
    }

    @PostMapping("/read-conversation")
    public Result<Void> markConversationAsRead(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                @RequestBody Map<String, Object> body) {
        Long otherUserId = Long.valueOf(body.get("otherUserId").toString());
        Long orderId = body.get("orderId") != null ? Long.valueOf(body.get("orderId").toString()) : null;
        chatService.markConversationAsRead(otherUserId, token.getUserId(), orderId);
        return Result.success();
    }
}
