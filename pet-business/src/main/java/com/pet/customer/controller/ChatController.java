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
     * 【构造聊天控制器】
     *
     * 业务作用：注入聊天服务、订单履约服务和聊天事件广播器。
     *
     * 调用场景：Spring 容器启动创建 Bean 时调用。
     *
     * 调用链：Spring 容器 → 本构造器 → 赋值 chatService / fulfillmentService / chatEventBroadcaster 字段。
     *
     * 数据处理：无。
     *
     * 业务规则：依赖均为 final 字段。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
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
     * 【获取会话消息】
     *
     * 业务作用：拉取当前用户与指定用户的聊天记录，支持按订单维度和游标分页。
     *
     * 调用场景：用户打开聊天窗口或上拉加载更多历史消息。
     *
     * 调用链：GET /api/chat/conversation → getConversation() → fulfillmentService.listConversation()（带 orderId）或 chatService.getConversation()（不带 orderId）。
     *
     * 数据处理：orderId 非空走订单会话（额外带管理员标记），为空走通用会话并显式传 null 订单维度。
     *
     * 业务规则：需登录；otherUserId 必填，beforeId 与 size 可选。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：参数缺失由 Spring 返回 400；未登录返回 401。
     *
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
                    token.getUserId(), isAdmin(token), orderId, otherUserId, beforeId, size));
        }
        return Result.success(chatService.getConversation(token.getUserId(), otherUserId, null, beforeId, size));
    }

    /**
     * 【随机分配人工客服】
     *
     * 业务作用：为当前用户随机分配一名在线客服，返回其用户信息用于直接发起聊天。
     *
     * 调用场景：智能客服页面点击「转人工」。
     *
     * 调用链：POST /api/chat/assign-agent → assignAgent() → chatService.assignCustomerServiceAgent()。
     *
     * 数据处理：以当前用户 ID 下传，服务层据此排除自身后随机挑选客服。
     *
     * 业务规则：需登录。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：暂无可用客服时由服务层抛 BusinessException(400)。
     *
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
     * 【获取未读消息列表】
     *
     * 业务作用：返回当前用户所有未读聊天消息。
     *
     * 调用场景：用户查看未读消息列表页。
     *
     * 调用链：GET /api/chat/unread → getUnread() → chatService.getUnreadMessages()。
     *
     * 数据处理：以当前用户 ID 作为接收方查询未读消息。
     *
     * 业务规则：需登录。
     *
     * 状态影响：无，只读，不改变已读状态。
     *
     * 异常情况：未登录返回 401。
     *
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
     * 【获取未读消息数量】
     *
     * 业务作用：返回当前用户的未读消息总数，用于角标展示。
     *
     * 调用场景：前端轮询或进入页面时刷新未读角标。
     *
     * 调用链：GET /api/chat/unread-count → getUnreadCount() → chatService.countUnreadMessages()。
     *
     * 数据处理：以当前用户 ID 作为接收方统计未读条数。
     *
     * 业务规则：需登录。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：未登录返回 401。
     *
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
     * 【发送聊天消息】
     *
     * 业务作用：发送一条聊天消息，订单维度与通用维度走不同通道。
     *
     * 调用场景：用户在聊天窗口发送文本、图片或文件。
     *
     * 调用链：POST /api/chat/send → send() → fulfillmentService.sendMessage()（带 orderId）或 chatService.sendMessage() + chatEventBroadcaster.broadcastMessage()。
     *
     * 数据处理：带 orderId 时把请求字段转成 SendOrderMessageRequestDTO 下传；不带 orderId 时手工组装 ChatMessage 实体后落库，并广播消息事件。
     *
     * 业务规则：需登录；请求体经 @Valid 校验；发送人固定取 token 中的用户 ID。
     *
     * 状态影响：新增一条聊天消息记录；通用维度还会通过 SSE 广播给接收方。
     *
     * 异常情况：内容与附件同时为空、内容超长、类型不支持等由服务层抛 BusinessException(400)。
     *
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

    /**
     * 【标记单条消息已读】
     *
     * 业务作用：把指定消息标记为已读。
     *
     * 调用场景：用户点开某条消息。
     *
     * 调用链：POST /api/chat/read/{messageId} → markAsRead() → chatService.markAsRead()。
     *
     * 数据处理：以消息 ID 和当前用户 ID 作为条件更新已读标记。
     *
     * 业务规则：需登录；只有接收方本人的未读消息会被更新。
     *
     * 状态影响：聊天消息的 read_wsh 置为 1。
     *
     * 异常情况：未登录返回 401；消息不存在或非本人接收时不报错，更新影响 0 行。
     *
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
     * 【标记会话已读】
     *
     * 业务作用：把当前用户与指定用户之间的全部未读消息标记为已读。
     *
     * 调用场景：用户进入某个聊天会话。
     *
     * 调用链：POST /api/chat/read-conversation → markConversationAsRead() → fulfillmentService.markConversationAsRead()（带 orderId）或 chatService.markConversationAsRead()（不带 orderId）。
     *
     * 数据处理：按请求体中的 order_id_wsh 判断走订单会话还是通用会话，other_user_id_wsh 为对方用户。
     *
     * 业务规则：需登录；请求体经 @Valid 校验。
     *
     * 状态影响：该会话下所有未读消息的 read_wsh 置为 1。
     *
     * 异常情况：参数缺失返回 400；未登录返回 401。
     *
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
                    token.getUserId(), isAdmin(token), request.getOrder_id_wsh(), request.getOther_user_id_wsh());
        } else {
            chatService.markConversationAsRead(
                    token.getUserId(), request.getOther_user_id_wsh(), null);
        }
        return Result.success();
    }

    /**
     * 【判断是否管理员】
     *
     * 业务作用：检查 token 权限中是否包含 ROLE_ADMIN。
     *
     * 调用场景：getConversation / send / markConversationAsRead 决定是否按管理员视角处理。
     *
     * 调用链：getConversation() / send() / markConversationAsRead() → isAdmin() → token.getAuthorities() 遍历比对。
     *
     * 数据处理：流式遍历权限字符串，与 ROLE_ADMIN 精确比较。
     *
     * 业务规则：只有 ADMIN 角色返回 true，客服与商家角色均返回 false。
     *
     * 状态影响：无。
     *
     * 异常情况：token 为 null 时会空指针，实际调用前均已由 @AuthenticationPrincipal 注入。
     *
     * 注意事项：与 ComplaintController.hasRole 不同，这里固定写死 ROLE_ADMIN。
     */
    private boolean isAdmin(JwtAuthenticationToken token) {
        return token.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
