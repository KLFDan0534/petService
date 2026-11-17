package com.pet.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE 连接生命周期管理器。
 *
 * <p><b>只负责连接</b>：注册、握手、按用户发送、失败清理、心跳。不负责业务事件语义 ——
 * 事件名、载荷结构、接收人解析、事务时序全部留在各业务 Broadcaster 适配层。</p>
 *
 * <p><b>实例级容器</b>：{@code emittersByUser} 是 JVM 内 Map，多实例部署时各节点只持有本机连接，
 * 跨节点广播不在本类职责内（需 Redis Pub/Sub 等外部机制，本次未引入）。</p>
 *
 * <p><b>为什么继承式容错</b>：连接失效有两种表现 —— 写入时 {@link IOException}（管道断开），
 * 以及 emitter 已完成后再写抛 {@link IllegalStateException}（"ResponseBodyEmitter has already completed"）。
 * 旧实现只捕获 IOException，后者会冒泡并中断同一用户的后续推送，本类统一处理两种。
 * 只捕获这两类「已确认的连接失效异常」，不用 {@code catch (Throwable)} 吞掉程序错误。</p>
 */
@Service
@Slf4j
public class SseHub {

    /** 用户ID → 该用户当前所有 SSE 连接（同一用户允许多端在线）。 */
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    /** 心跳间隔：30 秒。与前端 EventSource 默认重连行为配合，避免被代理按空闲超时断开。 */
    private static final long HEARTBEAT_FIXED_DELAY_MS = 30_000L;

    /** 心跳注释帧内容。SSE 协议里以 ':' 开头的是注释，EventSource 会忽略，不产生业务事件。 */
    private static final String HEARTBEAT_COMMENT = "keep-alive";

    /**
     * 为用户建立一条 SSE 连接。
     *
     * <p>超时设为 0（不自动断开）；注册完成/超时/异常三个清理回调；
     * 建立后立即发送握手事件 {@code event: connected, data: ok}（与三套旧实现逐字一致）。</p>
     *
     * @param userId 用户ID
     * @return 新建的 emitter
     */
    public SseEmitter connect(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        emittersByUser.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(ex -> remove(userId, emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok", MediaType.TEXT_PLAIN));
        } catch (IOException | IllegalStateException e) {
            log.warn("SSE 握手失败，移除连接: userId={}, reason={}", userId, e.toString());
            remove(userId, emitter);
        }
        return emitter;
    }

    /**
     * 向指定用户的所有活跃连接发送一个业务事件。
     *
     * <p>逐连接发送：某一条失败只清理它自己，不影响同用户其它连接，也不影响其它用户。</p>
     *
     * @param userId    目标用户
     * @param eventName 事件名（由业务适配层决定，本类不改写）
     * @param payload   事件载荷（原样透传）
     */
    public void send(Long userId, String eventName, Object payload) {
        if (userId == null) {
            return;
        }
        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            sendToEmitter(userId, emitter, eventName, payload);
        }
    }

    /**
     * 向多个用户分别发送同一个事件。
     *
     * <p>接收人集合由业务适配层解析（例如订单状态需要解析主人/看护者/商家并去重），
     * 本类只负责按用户逐个投递。</p>
     */
    public void sendAll(Collection<Long> userIds, String eventName, Object payload) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        for (Long userId : userIds) {
            send(userId, eventName, payload);
        }
    }

    /**
     * 心跳：向所有已注册连接发送 SSE 注释帧。
     *
     * <p>用 {@code comment} 而不是新业务事件，前端 EventSource 会忽略它，因此不改变任何事件名。
     * 心跳失败同样视为连接失效，立即清理，避免死连接长期占用容器。</p>
     *
     * <p>由 Spring 的 {@code @Scheduled} 驱动（项目已在 {@code PetApplication} 上开启
     * {@code @EnableScheduling}），全应用只有一个调度入口，不会重复创建调度器。</p>
     */
    @Scheduled(fixedDelay = HEARTBEAT_FIXED_DELAY_MS)
    public void heartbeat() {
        emittersByUser.forEach((userId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment(HEARTBEAT_COMMENT));
                } catch (IOException | IllegalStateException e) {
                    log.warn("SSE 心跳失败，移除连接: userId={}, reason={}", userId, e.toString());
                    remove(userId, emitter);
                }
            }
        });
    }

    /** 单连接发送，失败即清理。 */
    private void sendToEmitter(Long userId, SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(payload));
        } catch (IOException | IllegalStateException e) {
            log.warn("SSE 推送失败，移除连接: userId={}, event={}, reason={}",
                    userId, eventName, e.toString());
            remove(userId, emitter);
        }
    }

    /**
     * 移除指定连接；该用户已无连接时连带清理 Map 条目。
     *
     * <p>使用 {@code Map.remove(key, value)} 双参数形式：只有当前值仍是这个 list 时才删除，
     * 避免并发下误删其它线程刚创建的新列表。</p>
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
