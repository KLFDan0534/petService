package com.pet.operation.controller;

import com.pet.operation.service.NotificationBroadcaster;
import com.pet.security.SseTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notification-events")
@Tag(name = "【用户端】通知事件", description = "SSE实时通知事件推送")
public class NotificationEventController {
    private final NotificationBroadcaster broadcaster;
    private final SseTokenService sseTokenService;

    public NotificationEventController(NotificationBroadcaster broadcaster, SseTokenService sseTokenService) {
        this.broadcaster = broadcaster;
        this.sseTokenService = sseTokenService;
    }

    /**
     * 订阅通知事件流
     *
     * <p>API: GET /api/notification-events/stream (SSE, text/event-stream)</p>
     * <p>请求来源：前端页面（全局），通过EventSource建立SSE连接，实时接收系统通知推送</p>
     * <p>权限要求：需携带有效JWT Token进行身份验证</p>
     * <p>输入参数：@query token - JWT认证令牌</p>
     * <p>返回数据：SseEmitter - 持续推送通知事件，客户端可监听"notification"事件</p>
     * <p>异常情况：
     * <ul>
     *   <li>401 - Token无效或已过期</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "订阅通知事件流", description = "通过SSE实时接收新通知推送")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功建立SSE连接，持续推送事件"),
            @ApiResponse(responseCode = "401", description = "Token无效或未授权"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public SseEmitter stream(@Parameter(description = "JWT认证令牌") @RequestParam("token") String token) {
        return broadcaster.connect(sseTokenService.requireUserId(token));
    }
}
