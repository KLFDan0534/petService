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

    /**
     * 订阅聊天事件流
     *
     * <p>API: GET /api/chat-events/stream (SSE, text/event-stream)</p>
     * <p>请求来源：前端聊天页面，通过EventSource建立SSE连接，实时接收新消息推送</p>
     * <p>权限要求：需携带有效JWT Token进行身份验证</p>
     * <p>输入参数：@query token - JWT认证令牌</p>
     * <p>返回数据：SseEmitter - 持续推送聊天消息事件，客户端可监听"chat-message"事件</p>
     * <p>异常情况：
     * <ul>
     *   <li>401 - Token无效或已过期</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
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
