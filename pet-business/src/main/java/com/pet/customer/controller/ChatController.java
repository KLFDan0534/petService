package com.pet.customer.controller;

import com.pet.common.Result;
import com.pet.customer.dto.ChatAgentAssignDTO;
import com.pet.customer.dto.ChatMarkConversationReadRequestDTO;
import com.pet.customer.dto.ChatMessageDTO;
import com.pet.customer.dto.ChatSendRequestDTO;
import com.pet.customer.entity.ChatMessage;
import com.pet.customer.service.ChatEventBroadcaster;
import com.pet.customer.service.ChatService;
import com.pet.fulfillment.dto.SendOrderMessageRequestDTO;
import com.pet.fulfillment.service.OrderFulfillmentService;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "【用户端】聊天管理", description = "用户聊天和订单消息管理（用户/商家/看护者使用）")
@Slf4j
public class ChatController {
    private final ChatService chatService;
    private final OrderFulfillmentService fulfillmentService;
    private final ChatEventBroadcaster chatEventBroadcaster;

    public ChatController(ChatService chatService,
                          OrderFulfillmentService fulfillmentService,
                          ChatEventBroadcaster chatEventBroadcaster) {
        this.chatService = chatService;
        this.fulfillmentService = fulfillmentService;
        this.chatEventBroadcaster = chatEventBroadcaster;
    }

    @GetMapping("/conversation")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取会话消息", description = "获取用户之间的聊天会话消息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<ChatMessageDTO>> getConversation(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @RequestParam Long otherUserId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(required = false) Integer size) {
        if (orderId != null) {
            return Result.success(fulfillmentService.listConversation(
                    token.getUserId(), isAdmin(token), orderId, otherUserId, beforeId, size));
        }
        return Result.success(chatService.getConversation(token.getUserId(), otherUserId, null, beforeId, size));
    }

    @PostMapping("/assign-agent")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "随机分配人工客服", description = "智能客服转人工时，从在线客服中随机分配一名，返回其用户信息用于直接发起聊天")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "暂无可用的在线客服"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ChatAgentAssignDTO> assignAgent(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(chatService.assignCustomerServiceAgent(token.getUserId()));
    }

    @GetMapping("/unread")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取未读消息", description = "获取用户未读的聊天消息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<ChatMessageDTO>> getUnread(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(chatService.getUnreadMessages(token.getUserId()));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取未读消息数量", description = "获取用户未读的聊天消息数量")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Long> getUnreadCount(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(chatService.countUnreadMessages(token.getUserId()));
    }

    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "发送聊天消息", description = "发送一条聊天消息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ChatMessageDTO> send(@AuthenticationPrincipal JwtAuthenticationToken token,
                                       @Valid @RequestBody ChatSendRequestDTO request) {
        if (request.getOrder_id_wsh() != null) {
            SendOrderMessageRequestDTO orderRequest = new SendOrderMessageRequestDTO();
            orderRequest.setTo_user_id_wsh(request.getTo_user_id_wsh());
            orderRequest.setContent_wsh(request.getContent_wsh());
            orderRequest.setType_wsh(request.getType_wsh());
            orderRequest.setFile_url_wsh(request.getFile_url_wsh());
            return Result.success(fulfillmentService.sendMessage(
                    token.getUserId(), isAdmin(token), request.getOrder_id_wsh(), orderRequest));
        }

        ChatMessage message = new ChatMessage();
        message.setFrom_user_id_wsh(token.getUserId());
        message.setTo_user_id_wsh(request.getTo_user_id_wsh());
        message.setContent_wsh(request.getContent_wsh());
        message.setType_wsh(request.getType_wsh());
        message.setFile_url_wsh(request.getFile_url_wsh());
        ChatMessageDTO saved = chatService.sendMessage(message);
        chatEventBroadcaster.broadcastMessage(saved);
        return Result.success(saved);
    }

    @PostMapping("/read/{messageId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "标记消息已读", description = "标记单条聊天消息为已读")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "404", description = "消息不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> markAsRead(@AuthenticationPrincipal JwtAuthenticationToken token,
                                   @Parameter(description = "消息ID") @PathVariable Long messageId) {
        chatService.markAsRead(messageId, token.getUserId());
        return Result.success();
    }

    @PostMapping("/read-conversation")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "标记会话已读", description = "标记整个会话为已读")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> markConversationAsRead(@AuthenticationPrincipal JwtAuthenticationToken token,
                                               @Valid @RequestBody ChatMarkConversationReadRequestDTO request) {
        if (request.getOrder_id_wsh() != null) {
            fulfillmentService.markConversationAsRead(
                    token.getUserId(), isAdmin(token), request.getOrder_id_wsh(), request.getOther_user_id_wsh());
        } else {
            chatService.markConversationAsRead(
                    token.getUserId(), request.getOther_user_id_wsh(), null);
        }
        return Result.success();
    }

    private boolean isAdmin(JwtAuthenticationToken token) {
        return token.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
