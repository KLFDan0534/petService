package com.pet.operation.service;

import com.pet.operation.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 基于 Server-Sent Events (SSE) 的通知实时推送广播器。
 * <p>
 * 为每个用户维护一组 {@link SseEmitter} 连接，当有新的通知创建时，
 * 通过已有的 SSE 连接将通知内容实时推送给目标用户。
 * 连接断开、超时或出错时自动清理已失效的 emitter。
 */
@Service
@Slf4j
public class NotificationBroadcaster {
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    /**
     * 为用户建立一个 SSE 长连接。连接建立后向客户端发送 "connected" 事件以确认握手。
     * <p>
     * 超时时间设为 0（无限超时），连接生命周期由客户端关闭或网络异常控制。
     *
     * @param userId 用户ID
     * @return SSE 发射器实例，可用于后续推送
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
     * 向指定用户的所有活跃 SSE 连接推送一条通知
     * <p>
     * 事件名称为 "notification"，数据为序列化后的 {@link Notification} 实体。
     * 推送失败的 emitter 会自动移除。
     *
     * @param userId       目标用户ID
     * @param notification 待推送的通知实体
     */
    public void broadcast(Long userId, Notification notification) {
        if (userId == null || notification == null) {
            return;
        }
        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notification));
            } catch (IOException e) {
                remove(userId, emitter);
            }
        }
    }

    /**
     * 从用户连接列表中移除指定的 SSE emitter，列表为空时清理用户条目
     */
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
}
