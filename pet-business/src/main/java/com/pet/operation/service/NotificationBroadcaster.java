package com.pet.operation.service;

import com.pet.operation.entity.Notification;
import com.pet.sse.SseHub;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 站内通知的 SSE 推送适配层。
 *
 * <p>只保留本业务独有的东西：事件名 {@code notification}、载荷为原始 {@link Notification} 实体、
 * 入参防御。连接的注册、握手、按用户投递、失败清理、心跳全部委托给 {@link SseHub}。</p>
 *
 * <p>事务语义：本类不做事务后置处理，保持原有推送时序（调用方
 * {@code NotificationServiceImpl.create} 为 {@code @Transactional}，推送在方法内即事务提交前）。
 * 该时序为迁移前既有行为，本次未改变。</p>
 */
@Service
public class NotificationBroadcaster {

    /** 事件名固定，前端按此订阅；不得改写。 */
    private static final String EVENT_NOTIFICATION = "notification";

    private final SseHub sseHub;

    public NotificationBroadcaster(SseHub sseHub) {
        this.sseHub = sseHub;
    }

    /**
     * 为用户建立 SSE 连接（委托 SseHub，握手行为不变：event connected / data ok / TEXT_PLAIN）。
     */
    public SseEmitter connect(Long userId) {
        return sseHub.connect(userId);
    }

    /**
     * 向指定用户推送一条通知。
     *
     * @param userId       目标用户；为 null 时忽略
     * @param notification 通知实体；为 null 时忽略
     */
    public void broadcast(Long userId, Notification notification) {
        if (userId == null || notification == null) {
            return;
        }
        sseHub.send(userId, EVENT_NOTIFICATION, notification);
    }
}
