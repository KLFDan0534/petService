package com.pet.customer.service;

import com.pet.customer.dto.ChatMessageDTO;
import com.pet.customer.dto.ChatMessageEventDTO;
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
        emittersByUser.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(ex -> remove(userId, emitter));
        try {
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
        sendToUser(message.getTo_user_id_wsh(), event);
        sendToUser(message.getFrom_user_id_wsh(), event);
    }

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

    private void remove(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByUser.remove(userId, emitters);
        }
    }

    private ChatMessageEventDTO toEvent(ChatMessageDTO message) {
        ChatMessageEventDTO event = new ChatMessageEventDTO();
        event.setMessage_id_wsh(message.getId_wsh());
        event.setOrder_id_wsh(message.getOrder_id_wsh());
        event.setFrom_user_id_wsh(message.getFrom_user_id_wsh());
        event.setTo_user_id_wsh(message.getTo_user_id_wsh());
        event.setType_wsh(message.getType_wsh());
        event.setCreated_at_wsh(message.getCreated_at_wsh() != null ? message.getCreated_at_wsh() : LocalDateTime.now());
        return event;
    }
}
