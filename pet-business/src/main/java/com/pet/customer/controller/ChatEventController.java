package com.pet.customer.controller;

import com.pet.customer.service.ChatEventBroadcaster;
import com.pet.security.SseTokenService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/chat-events")
@Tag(name = "【用户端】聊天事件", description = "SSE实时聊天事件推送")
public class ChatEventController {
    private final ChatEventBroadcaster broadcaster;
    private final SseTokenService sseTokenService;

    public ChatEventController(ChatEventBroadcaster broadcaster, SseTokenService sseTokenService) {
        this.broadcaster = broadcaster;
        this.sseTokenService = sseTokenService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "订阅聊天事件流", description = "通过SSE实时接收聊天消息推送")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功建立SSE连接，持续推送事件"),
            @ApiResponse(responseCode = "401", description = "Token无效或未授权"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public SseEmitter stream(@Parameter(description = "JWT认证令牌") @RequestParam("token") String token) {
        return broadcaster.connect(sseTokenService.requireUserId(token));
    }
}
