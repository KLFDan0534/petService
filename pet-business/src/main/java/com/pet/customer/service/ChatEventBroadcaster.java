package com.pet.customer.service;

import com.pet.customer.dto.ChatMessageDTO;
import com.pet.customer.dto.ChatMessageEventDTO;
import com.pet.sse.SseHub;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 实时聊天事件的 SSE 推送适配层。
 *
 * <p>本类只保留聊天域独有的东西：两种事件名（{@code chat-message} / {@code thread-message}）、
 * 载荷结构（{@link ChatMessageEventDTO} / Map）、发送人昵称补全、以及"收发双方都推"的规则。
 * 连接的注册、握手、按用户投递、失败清理、心跳全部委托给 {@link SseHub}。</p>
 *
 * <p><b>已固化的既有行为</b>：当收发双方是同一账号时，{@code broadcastMessage} 会对该连接推送两次
 * （to + from 各一次）。该行为由批次 04 第一阶段的测试固定，本次迁移保持不变，前端按 message_id 去重。</p>
 */
@Service
public class ChatEventBroadcaster {

    /** 聊天消息事件名固定，前端按此订阅；不得改写。 */
    private static final String EVENT_CHAT_MESSAGE = "chat-message";
    /** 投诉/工单线程事件名固定；与 chat-message 区分，供会话列表红点使用。 */
    private static final String EVENT_THREAD_MESSAGE = "thread-message";

    private final SseHub sseHub;
    private final UserMapper userMapper;

    public ChatEventBroadcaster(SseHub sseHub, UserMapper userMapper) {
        this.sseHub = sseHub;
        this.userMapper = userMapper;
    }

    /** 为用户建立 SSE 连接（委托 SseHub，握手行为不变）。 */
    public SseEmitter connect(Long userId) {
        return sseHub.connect(userId);
    }

    /**
     * 广播一条聊天消息事件给收发双方。
     *
     * <p>双方都推以保证多端消息同步；同一账号时即推送两次（既有行为，未改变）。</p>
     *
     * @param message 聊天消息；null 时忽略
     */
    public void broadcastMessage(ChatMessageDTO message) {
        if (message == null) {
            return;
        }
        ChatMessageEventDTO event = toEvent(message);
        sseHub.send(message.getTo_user_id_wsh(), EVENT_CHAT_MESSAGE, event);
        sseHub.send(message.getFrom_user_id_wsh(), EVENT_CHAT_MESSAGE, event);
    }

    /**
     * 广播一条投诉/工单线程消息事件给收发双方（客户端用于会话列表红点实时更新）。
     *
     * @param type       业务类型 complaint / ticket
     * @param bizId      业务ID
     * @param toUserId   接收方用户ID
     * @param fromUserId 发送方用户ID
     * @param content    消息内容
     * @param fileUrl    附件URL
     * @param createdAt  创建时间
     */
    public void broadcastThreadMessage(String type, Long bizId, Long toUserId, Long fromUserId,
                                       String content, String fileUrl, LocalDateTime createdAt) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        event.put("biz_id_wsh", bizId);
        event.put("from_user_id_wsh", fromUserId);
        event.put("from_user_name_wsh", userName(fromUserId));
        event.put("to_user_id_wsh", toUserId);
        event.put("content_wsh", content);
        event.put("file_url_wsh", fileUrl);
        event.put("created_at_wsh", createdAt != null ? createdAt : LocalDateTime.now());
        sseHub.send(toUserId, EVENT_THREAD_MESSAGE, event);
        sseHub.send(fromUserId, EVENT_THREAD_MESSAGE, event);
    }

    /**
     * 聊天消息 DTO 转推送事件：精简字段，发送人名称实时查询，创建时间缺失时取当前时间。
     *
     * <p>注意：每次推送都会触发一次用户查询（既有行为，未改变）。</p>
     */
    private ChatMessageEventDTO toEvent(ChatMessageDTO message) {
        ChatMessageEventDTO event = new ChatMessageEventDTO();
        event.setMessage_id_wsh(message.getId_wsh());
        event.setOrder_id_wsh(message.getOrder_id_wsh());
        event.setFrom_user_id_wsh(message.getFrom_user_id_wsh());
        event.setFrom_user_name_wsh(userName(message.getFrom_user_id_wsh()));
        event.setTo_user_id_wsh(message.getTo_user_id_wsh());
        event.setType_wsh(message.getType_wsh());
        event.setContent_wsh(message.getContent_wsh());
        event.setFile_url_wsh(message.getFile_url_wsh());
        event.setCreated_at_wsh(message.getCreated_at_wsh() != null
                ? message.getCreated_at_wsh() : LocalDateTime.now());
        return event;
    }

    /** 按用户 ID 取昵称，昵称为空时回退用户名；用户不存在或 ID 为空返回 null。 */
    private String userName(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return user.getNickname_wsh() != null ? user.getNickname_wsh() : user.getUsername_wsh();
    }
}
