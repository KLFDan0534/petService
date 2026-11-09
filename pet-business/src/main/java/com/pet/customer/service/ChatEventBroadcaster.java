package com.pet.customer.service;

import com.pet.customer.dto.ChatMessageDTO;
import com.pet.customer.dto.ChatMessageEventDTO;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 基于 SSE (Server-Sent Events) 的实时聊天事件广播器。
 * 维护每个用户与服务器之间的 SSE 连接池，当有新消息时向收发双方推送事件通知，
 * 实现浏览器端的即时消息刷新。
 */
@Service
@Slf4j
public class ChatEventBroadcaster {
    /** 用户ID -> SSE Emitter 列表的映射，使用线程安全的并发容器 */
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();
    private final UserMapper userMapper;

    /**
     * 【聊天事件广播器的构造注入】
     *
     * 业务作用：注入用户 Mapper，用于推送事件时补全发送人名称。
     *
     * 调用场景：Spring 容器创建 ChatEventBroadcaster 时。
     *
     * 调用链：Spring 容器 → 本构造方法 → 赋值 userMapper。
     *
     * 数据处理：仅保存依赖引用。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：连接池 emittersByUser 为实例级 ConcurrentHashMap，多实例部署时各节点只持有本机连接。
     */
    public ChatEventBroadcaster(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 建立指定用户的 SSE 连接。
     * <p>创建一个永不超时的 SseEmitter，注册完成/超时/异常时的清理回调，
     * 并立即发送 "connected" 事件确认连接成功。</p>
     *
     * @param userId 用户ID
     * @return 新建的 SseEmitter 实例
     */
    public SseEmitter connect(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        // 同一用户可挂多条连接，支持多端在线
        emittersByUser.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(ex -> remove(userId, emitter));
        try {
            // 握手失败时直接清理该连接
            emitter.send(SseEmitter.event().name("connected").data("ok", MediaType.TEXT_PLAIN));
        } catch (IOException e) {
            remove(userId, emitter);
        }
        return emitter;
    }

    /**
     * 广播一条聊天消息事件给收发双方。
     * <p>将 ChatMessageDTO 转换为 ChatMessageEventDTO（精简字段），
     * 然后同时推送给发送方和接收方的所有活跃 SSE 连接。</p>
     *
     * @param message 聊天消息DTO
     */
    public void broadcastMessage(ChatMessageDTO message) {
        if (message == null) {
            return;
        }
        ChatMessageEventDTO event = toEvent(message);
        // 收发双方都推，保证多端消息同步
        sendToUser(message.getTo_user_id_wsh(), event);
        sendToUser(message.getFrom_user_id_wsh(), event);
    }

    /**
     * 广播一条投诉/工单线程消息事件给收发双方。
     * <p>客户端监听 "thread-message" 事件，用于会话列表红点实时更新。</p>
     *
     * @param type      业务类型 complaint / ticket
     * @param bizId     业务ID
     * @param toUserId  接收方用户ID
     * @param fromUserId 发送方用户ID
     * @param content   消息内容
     * @param createdAt 创建时间
     */
    public void broadcastThreadMessage(String type, Long bizId, Long toUserId, Long fromUserId,
                                       String content, String fileUrl, LocalDateTime createdAt) {
        Map<String, Object> event = new java.util.LinkedHashMap<>();
        event.put("type", type);
        event.put("biz_id_wsh", bizId);
        event.put("from_user_id_wsh", fromUserId);
        event.put("from_user_name_wsh", userName(fromUserId));
        event.put("to_user_id_wsh", toUserId);
        event.put("content_wsh", content);
        event.put("file_url_wsh", fileUrl);
        event.put("created_at_wsh", createdAt != null ? createdAt : LocalDateTime.now());
        sendThreadEvent(toUserId, event);
        sendThreadEvent(fromUserId, event);
    }

    /**
     * 【向指定用户推送线程事件】
     *
     * 业务作用：把投诉/工单线程事件推送给某个用户的所有活跃 SSE 连接。
     *
     * 调用场景：broadcastThreadMessage() 分别推给收发双方时。
     *
     * 调用链：broadcastThreadMessage() → sendThreadEvent() → emitter.send()。
     *
     * 数据处理：按用户 ID 取出连接列表，逐个以 thread-message 事件名发送。
     *
     * 业务规则：用户 ID 为空或该用户无连接时直接返回。
     *
     * 状态影响：无数据库写入；仅向浏览器推送 SSE 事件。
     *
     * 异常情况：发送 IOException 时移除该连接，不向上抛出。
     *
     * 注意事项：发送异常被吞掉，调用方无法感知推送失败。
     */
    private void sendThreadEvent(Long userId, Map<String, Object> event) {
        if (userId == null) {
            return;
        }
        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("thread-message").data(event));
            } catch (IOException e) {
                remove(userId, emitter);
            }
        }
    }

    /**
     * 【向指定用户推送聊天消息事件】
     *
     * 业务作用：把聊天消息事件推送给某个用户的所有活跃 SSE 连接。
     *
     * 调用场景：broadcastMessage() 分别推给收发双方时。
     *
     * 调用链：broadcastMessage() → sendToUser() → emitter.send()。
     *
     * 数据处理：按用户 ID 取出连接列表，逐个以 chat-message 事件名发送。
     *
     * 业务规则：用户 ID 为空或该用户无连接时直接返回。
     *
     * 状态影响：无数据库写入；仅推送 SSE 事件。
     *
     * 异常情况：发送 IOException 时移除该连接，不向上抛出。
     *
     * 注意事项：与 sendThreadEvent() 结构相同，仅事件名与载荷类型不同。
     */
    private void sendToUser(Long userId, ChatMessageEventDTO event) {
        if (userId == null) {
            return;
        }
        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("chat-message").data(event));
            } catch (IOException e) {
                remove(userId, emitter);
            }
        }
    }

    /**
     * 【移除失效的 SSE 连接】
     *
     * 业务作用：从连接池中移除指定连接，用户连接列表为空时连带清理该用户的 Map 条目。
     *
     * 调用场景：连接完成、超时、异常或发送失败时。
     *
     * 调用链：connect() 的回调 / sendThreadEvent() / sendToUser() → remove()。
     *
     * 数据处理：先取用户连接列表，移除该 emitter，列表为空时从 Map 中删除该键。
     *
     * 业务规则：用户不存在于 Map 时直接返回。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：CopyOnWriteArrayList 的 remove 为值比较，依赖 SseEmitter 未重写 equals 的对象相等语义。
     */
    private void remove(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        // 连接清空后移除用户条目，避免 Map 膨胀
        if (emitters.isEmpty()) {
            emittersByUser.remove(userId, emitters);
        }
    }

    /**
     * 【聊天消息 DTO 转推送事件】
     *
     * 业务作用：把完整聊天消息 DTO 精简为前端推送所需的事件对象。
     *
     * 调用场景：broadcastMessage() 组装事件时。
     *
     * 调用链：broadcastMessage() → toEvent() → userName()。
     *
     * 数据处理：拷贝消息 ID、订单 ID、收发双方、类型、内容、附件与创建时间；发送人名称实时查询；创建时间为空时取当前时间。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：每次推送都会触发一次用户查询。
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
        event.setCreated_at_wsh(message.getCreated_at_wsh() != null ? message.getCreated_at_wsh() : LocalDateTime.now());
        return event;
    }

    /**
     * 【查询用户显示名】
     *
     * 业务作用：按用户 ID 取昵称，昵称为空时回退为用户名。
     *
     * 调用场景：toEvent() 与 broadcastThreadMessage() 组装推送载荷。
     *
     * 调用链：toEvent()/broadcastThreadMessage() → userName() → userMapper.selectById()。
     *
     * 数据处理：查询 user 表并选择 nickname_wsh 或 username_wsh。
     *
     * 业务规则：userId 为空或用户不存在时返回 null。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：同步查询数据库，在高频推送场景下会成为瓶颈。
     */
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
