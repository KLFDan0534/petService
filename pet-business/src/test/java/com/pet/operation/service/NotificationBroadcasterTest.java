package com.pet.operation.service;

import com.pet.operation.entity.Notification;
import com.pet.sse.SseHub;
import com.pet.sse.SseTestSupport;
import com.pet.sse.SseTestSupport.CapturingSseEmitter;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link NotificationBroadcaster} 行为测试。
 *
 * <p>该类是三者中最简单的实现：无心跳、无事务钩子、事件只有两种
 * （握手 {@code connected} 与推送 {@code notification}）。</p>
 */
class NotificationBroadcasterTest {

    private final SseHub hub = new SseHub();
    private final NotificationBroadcaster broadcaster = new NotificationBroadcaster(hub);

    // ---------- 1. 单用户连接 ----------

    @Test
    void connect_registersOneConnectionForUser() {
        SseEmitter emitter = broadcaster.connect(10L);

        assertNotNull(emitter, "connect 必须返回 emitter");
        assertEquals(1, SseTestSupport.connectionCount(hub, 10L));
        assertEquals(1, SseTestSupport.userCount(hub));
    }

    @Test
    void connect_sendsConnectedHandshakeOnce() {
        SseEmitter emitter = broadcaster.connect(10L);

        assertEquals(1, SseTestSupport.bufferedEventCount(emitter),
                "connect 必须且只发送一次 connected 握手事件");
    }

    // ---------- 2. 同一用户多连接 ----------

    @Test
    void connect_sameUserTwice_keepsBothConnections() {
        broadcaster.connect(10L);
        broadcaster.connect(10L);

        assertEquals(2, SseTestSupport.connectionCount(hub, 10L),
                "同一用户允许多端在线");
        assertEquals(1, SseTestSupport.userCount(hub));
    }

    // ---------- 3. 不同用户隔离 ----------

    @Test
    void broadcast_onlyReachesTargetUser() {
        CapturingSseEmitter user1 = new CapturingSseEmitter();
        CapturingSseEmitter user2 = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, user1);
        SseTestSupport.inject(hub, 2L, user2);

        Notification n = notification(99L);
        broadcaster.broadcast(1L, n);

        assertEquals(1, user1.sendCount(), "目标用户应收到 1 条");
        assertSame(n, user1.payloads().get(0), "载荷必须是原始 Notification 实例");
        assertEquals(0, user2.sendCount(), "其他用户不得收到");
    }

    @Test
    void broadcast_reachesAllConnectionsOfSameUser() {
        CapturingSseEmitter a = new CapturingSseEmitter();
        CapturingSseEmitter b = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, a);
        SseTestSupport.inject(hub, 1L, b);

        broadcaster.broadcast(1L, notification(1L));

        assertEquals(1, a.sendCount());
        assertEquals(1, b.sendCount());
    }

    // ---------- 4. 事件正常发送 ----------

    @Test
    void broadcast_sendsNotificationPayload() {
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 5L, emitter);

        Notification n = notification(7L);
        broadcaster.broadcast(5L, n);

        assertEquals(1, emitter.sendCount());
        assertSame(n, emitter.payloads().get(0));
        assertEquals(java.util.List.of("notification"), emitter.eventNames(),
                "事件名固定为 notification，前端据此分发");
    }

    // ---------- 入参防御 ----------

    @Test
    void broadcast_nullUserId_isNoOp() {
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, emitter);

        broadcaster.broadcast(null, notification(1L));

        assertEquals(0, emitter.sendCount());
    }

    @Test
    void broadcast_nullNotification_isNoOp() {
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, emitter);

        broadcaster.broadcast(1L, null);

        assertEquals(0, emitter.sendCount());
    }

    @Test
    void broadcast_userWithoutConnection_isNoOp() {
        broadcaster.broadcast(123L, notification(1L));

        assertEquals(0, SseTestSupport.userCount(hub));
    }

    // ---------- 5/7. 发送异常与失效连接清理 ----------

    @Test
    void broadcast_sendFails_removesBrokenConnection() {
        CapturingSseEmitter broken = new CapturingSseEmitter();
        broken.failNextSendWithIOException();
        SseTestSupport.inject(hub, 1L, broken);

        broadcaster.broadcast(1L, notification(1L));

        assertEquals(0, SseTestSupport.connectionCount(hub, 1L),
                "发送 IOException 的连接必须被移除");
        assertEquals(0, SseTestSupport.userCount(hub),
                "连接清空后用户条目也要清理，避免 Map 膨胀");
    }

    @Test
    void broadcast_oneBrokenConnectionDoesNotAffectOthers() {
        CapturingSseEmitter broken = new CapturingSseEmitter();
        broken.failNextSendWithIOException();
        CapturingSseEmitter healthy = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, broken);
        SseTestSupport.inject(hub, 1L, healthy);

        broadcaster.broadcast(1L, notification(1L));

        assertEquals(1, SseTestSupport.connectionCount(hub, 1L));
        assertEquals(1, healthy.sendCount(), "健康连接仍应收到消息");
    }

    // ---------- 6. 生命周期回调 ----------

    @Test
    void completionCallback_removesConnection() {
        SseEmitter emitter = broadcaster.connect(10L);

        SseTestSupport.fireCompletion(emitter);

        assertEquals(0, SseTestSupport.connectionCount(hub, 10L));
        assertEquals(0, SseTestSupport.userCount(hub));
    }

    @Test
    void timeoutCallback_removesConnection() {
        SseEmitter emitter = broadcaster.connect(10L);

        SseTestSupport.fireTimeout(emitter);

        assertEquals(0, SseTestSupport.connectionCount(hub, 10L));
    }

    @Test
    void errorCallback_removesConnection() {
        SseEmitter emitter = broadcaster.connect(10L);

        SseTestSupport.fireError(emitter, new RuntimeException("client aborted"));

        assertEquals(0, SseTestSupport.connectionCount(hub, 10L));
    }

    @Test
    void removingOneOfTwoConnections_keepsUserEntry() {
        SseEmitter first = broadcaster.connect(10L);
        broadcaster.connect(10L);

        SseTestSupport.fireCompletion(first);

        assertEquals(1, SseTestSupport.connectionCount(hub, 10L));
        assertEquals(1, SseTestSupport.userCount(hub));
    }

    // ---------- 9. 心跳（当前实现不支持，作为契约固化） ----------

    @Test
    void heartbeat_sendsCommentFrameWithoutDisturbingBusinessEvents() {
        // 批次 04 第二阶段正式批准加入心跳：心跳走 SSE 注释帧（':' 开头），EventSource 会忽略，
        // 因此不改变业务事件名，也不产生业务载荷。
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 10L, emitter);

        broadcaster.broadcast(10L, notification(10L));
        hub.heartbeat();

        assertEquals(java.util.List.of("notification"), emitter.eventNames(),
                "心跳不得改变业务事件名");
        assertEquals(1, emitter.payloads().size(), "心跳不得携带业务载荷");
        assertEquals(1, emitter.comments().size(), "心跳以 SSE 注释帧下发");
        assertTrue(emitter.comments().get(0).contains("keep-alive"));
    }

    // ---------- 10. 并发注册 / 发送 / 清理 ----------

    @Test
    void concurrentConnectBroadcastAndCleanup_keepsRegistryConsistent() throws Exception {
        final int threads = 8;
        final int perThread = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger failures = new AtomicInteger();

        List<Long> userIds = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            userIds.add((long) (i + 1));
        }

        for (int t = 0; t < threads; t++) {
            final Long userId = userIds.get(t);
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        SseEmitter e = broadcaster.connect(userId);
                        broadcaster.broadcast(userId, notification(userId));
                        // 一半连接立即触发完成回调，模拟客户端断开
                        if (i % 2 == 0) {
                            SseTestSupport.fireCompletion(e);
                        }
                    }
                } catch (Throwable ex) {
                    failures.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "并发任务必须在 30 秒内完成");
        pool.shutdownNow();

        assertEquals(0, failures.get(), "并发过程不应抛出异常");
        // 每个用户 50 条连接，其中偶数索引（25 条）被移除，应剩 25 条
        for (Long userId : userIds) {
            assertEquals(perThread / 2, SseTestSupport.connectionCount(hub, userId),
                    "用户 " + userId + " 的连接数应与清理次数一致");
        }
        assertFalse(SseTestSupport.registry(hub).isEmpty());
    }

    private static Notification notification(Long userId) {
        Notification n = new Notification();
        n.setUser_id_wsh(userId);
        n.setTitle_wsh("测试通知");
        return n;
    }
}
