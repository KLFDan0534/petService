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

    /**
     * 业务作用：注入聊天服务、订单履约服务和聊天事件广播器。
     * 业务规则：依赖均为 final 字段。
     * 注意事项：订单维度与通用维度的聊天分别由 fulfillmentService 和 chatService 处理。
     */
    public ChatController(ChatService chatService,
                          OrderFulfillmentService fulfillmentService,
                          ChatEventBroadcaster chatEventBroadcaster) {
        this.chatService = chatService;
        this.fulfillmentService = fulfillmentService;
        this.chatEventBroadcaster = chatEventBroadcaster;
    }

    /**
     * 业务作用：拉取当前用户与指定用户的聊天记录，支持按订单维度和游标分页。
     * 业务规则：需登录；otherUserId 必填，beforeId 与 size 可选。
     * 异常情况：参数缺失由 Spring 返回 400；未登录返回 401。
     * 注意事项：两条分支的分页实现不同，订单维度额外做了参与方权限校验。
     */
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
        // 带订单时走订单会话，额外做参与方校验
        if (orderId != null) {
            return Result.success(fulfillmentService.listConversation(
                    token.getUserId(), JwtAuthenticationToken.isAdmin(token), orderId, otherUserId, beforeId, size));
        }
        return Result.success(chatService.getConversation(token.getUserId(), otherUserId, null, beforeId, size));
    }

    /**
     * 业务作用：为当前用户随机分配一名在线客服，返回其用户信息用于直接发起聊天。
     * 业务规则：需登录。
     * 异常情况：暂无可用客服时由服务层抛 BusinessException(400)。
     * 注意事项：分配结果不落库，仅返回客服用户 ID、名称与头像。
     */
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

    /**
     * 业务作用：返回当前用户所有未读聊天消息。
     * 业务规则：需登录。
     * 异常情况：未登录返回 401。
     * 注意事项：服务层限制最多返回 200 条。
     */
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

    /**
     * 业务作用：返回当前用户的未读消息总数，用于角标展示。
     * 业务规则：需登录。
     * 异常情况：未登录返回 401。
     * 注意事项：用户 ID 为空时服务层返回 0。
     */
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

    /**
     * 业务作用：发送一条聊天消息，订单维度与通用维度走不同通道。
     * 业务规则：需登录；请求体经 @Valid 校验；发送人固定取 token 中的用户 ID。
     * 异常情况：内容与附件同时为空、内容超长、类型不支持等由服务层抛 BusinessException(400)。
     * 注意事项：订单维度是否广播由 fulfillmentService 内部负责，本方法不再重复广播。
     */
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
        // 订单消息交给履行服务统一广播
        if (request.getOrder_id_wsh() != null) {
            SendOrderMessageRequestDTO orderRequest = new SendOrderMessageRequestDTO();
            orderRequest.setTo_user_id_wsh(request.getTo_user_id_wsh());
            orderRequest.setContent_wsh(request.getContent_wsh());
            orderRequest.setType_wsh(request.getType_wsh());
            orderRequest.setFile_url_wsh(request.getFile_url_wsh());
            return Result.success(fulfillmentService.sendMessage(
                    token.getUserId(), JwtAuthenticationToken.isAdmin(token), request.getOrder_id_wsh(), orderRequest));
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

    /**
     * 业务作用：把指定消息标记为已读。
     * 业务规则：需登录；只有接收方本人的未读消息会被更新。
     * 异常情况：未登录返回 401；消息不存在或非本人接收时不报错，更新影响 0 行。
     * 注意事项：接口返回成功不代表真的更新成功，幂等且静默。
     */
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

    /**
     * 业务作用：把当前用户与指定用户之间的全部未读消息标记为已读。
     * 业务规则：需登录；请求体经 @Valid 校验。
     * 异常情况：参数缺失返回 400；未登录返回 401。
     * 注意事项：不带订单时服务层只处理 order_id 为空的通用消息。
     */
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
                    token.getUserId(), JwtAuthenticationToken.isAdmin(token), request.getOrder_id_wsh(), request.getOther_user_id_wsh());
        } else {
            chatService.markConversationAsRead(
                    token.getUserId(), request.getOther_user_id_wsh(), null);
        }
        return Result.success();
    }

}
