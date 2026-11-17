package com.pet.sse;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SseHub} 自身行为测试。
 *
 * <p>覆盖连接注册、握手、按用户发送、多用户发送、两类连接失效异常、生命周期回调、心跳与并发。
 * 这里只测「连接生命周期」，不测任何业务事件语义（那是三套 Broadcaster 的测试职责）。</p>
 */
class SseHubTest {

    private final SseHub hub = new SseHub();

    // ---------- 连接注册与握手 ----------

    @Test
    void connect_registersAndSendsConnectedHandshake() {
        SseEmitter emitter = hub.connect(1L);

        assertEquals(1, SseTestSupport.connectionCount(hub, 1L));
        assertEquals(1, SseTestSupport.bufferedEventCount(emitter));
    }

    @Test
    void connect_sameUserTwice_keepsBothConnections() {
        hub.connect(1L);
        hub.connect(1L);

        assertEquals(2, SseTestSupport.connectionCount(hub, 1L));
        assertEquals(1, SseTestSupport.userCount(hub));
    }

    // ---------- 发送 ----------

    @Test
    void send_deliversToAllConnectionsOfUser() {
        CapturingSseEmitter a = new CapturingSseEmitter();
        CapturingSseEmitter b = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, a);
        SseTestSupport.inject(hub, 1L, b);

        Object payload = java.util.Map.of("order_id_wsh", 1L);
        hub.send(1L, "order-status", payload);

        assertEquals(1, a.sendCount());
        assertEquals(1, b.sendCount());
        assertEquals(List.of("order-status"), a.eventNames());
        assertEquals(List.of(payload), a.payloads());
    }

    @Test
    void send_isolatesUsers() {
        CapturingSseEmitter target = new CapturingSseEmitter();
        CapturingSseEmitter bystander = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, target);
        SseTestSupport.inject(hub, 2L, bystander);

        hub.send(1L, "x", "p");

        assertEquals(1, target.sendCount());
        assertEquals(0, bystander.sendCount());
    }

    @Test
    void send_nullUserIdOrNoConnection_isNoOp() {
        CapturingSseEmitter e = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, e);

        hub.send(null, "x", "p");
        hub.send(999L, "x", "p");

        assertEquals(0, e.sendCount());
    }

    @Test
    void sendAll_deliversToEachUserAndSkipsNull() {
        CapturingSseEmitter a = new CapturingSseEmitter();
        CapturingSseEmitter b = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, a);
        SseTestSupport.inject(hub, 2L, b);

        List<Long> recipients = new ArrayList<>();
        recipients.add(1L);
        recipients.add(2L);
        recipients.add(null);
        recipients.add(3L);   // 无连接
        hub.sendAll(recipients, "pool-new", "p");

        assertEquals(1, a.sendCount());
        assertEquals(1, b.sendCount());
    }

    // ---------- 两类连接失效异常 ----------

    @Test
    void send_ioException_removesOnlyBrokenConnection() {
        CapturingSseEmitter broken = new CapturingSseEmitter();
        broken.failNextSendWithIOException();
        CapturingSseEmitter healthy = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, broken);
        SseTestSupport.inject(hub, 1L, healthy);

        hub.send(1L, "x", "p");

        assertEquals(1, SseTestSupport.connectionCount(hub, 1L));
        assertEquals(1, healthy.sendCount(), "健康连接必须仍收到消息");
    }

    /**
     * 本次修复的核心缺陷：已 complete 的 emitter 写入抛 {@link IllegalStateException}，
     * 旧实现只捕获 IOException，异常会冒泡并中断同一用户后续连接的推送。
     */
    @Test
    void send_illegalStateException_doesNotAbortRemainingConnections() {
        CapturingSseEmitter completed = new CapturingSseEmitter();
        completed.failNextSendWithIllegalState();
        CapturingSseEmitter healthy = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, completed);
        SseTestSupport.inject(hub, 1L, healthy);

        hub.send(1L, "x", "p");

        assertEquals(1, SseTestSupport.connectionCount(hub, 1L), "失效连接被移除");
        assertEquals(1, healthy.sendCount(), "后续连接不得因前一条异常而漏推");
    }

    @Test
    void send_failureDoesNotAffectOtherUsers() {
        CapturingSseEmitter broken = new CapturingSseEmitter();
        broken.failNextSendWithIllegalState();
        CapturingSseEmitter other = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, broken);
        SseTestSupport.inject(hub, 2L, other);

        hub.send(1L, "x", "p");
        hub.send(2L, "x", "p");

        assertEquals(1, other.sendCount());
    }

    // ---------- 生命周期回调 ----------

    @Test
    void completionCallback_removesConnection() {
        SseEmitter emitter = hub.connect(1L);

        SseTestSupport.fireCompletion(emitter);

        assertEquals(0, SseTestSupport.connectionCount(hub, 1L));
        assertEquals(0, SseTestSupport.userCount(hub), "空用户条目一并清理");
    }

    @Test
    void timeoutCallback_removesConnection() {
        SseEmitter emitter = hub.connect(1L);

        SseTestSupport.fireTimeout(emitter);

        assertEquals(0, SseTestSupport.connectionCount(hub, 1L));
    }

    @Test
    void errorCallback_removesConnection() {
        SseEmitter emitter = hub.connect(1L);

        SseTestSupport.fireError(emitter, new RuntimeException("client aborted"));

        assertEquals(0, SseTestSupport.connectionCount(hub, 1L));
    }

    @Test
    void removingOneOfTwoConnections_keepsUserEntry() {
        SseEmitter first = hub.connect(1L);
        hub.connect(1L);

        SseTestSupport.fireCompletion(first);

        assertEquals(1, SseTestSupport.connectionCount(hub, 1L));
        assertEquals(1, SseTestSupport.userCount(hub));
    }

    // ---------- 心跳 ----------

    @Test
    void heartbeat_sendsCommentFrameNotBusinessEvent() {
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, emitter);

        hub.heartbeat();

        assertEquals(1, emitter.comments().size(), "心跳必须发出一个注释帧");
        assertTrue(emitter.comments().get(0).contains("keep-alive"));
        assertEquals(0, emitter.eventNames().size(), "心跳不得伪装成业务事件");
        assertEquals(0, emitter.payloads().size(), "心跳不得携带业务载荷");
    }

    @Test
    void heartbeat_reachesAllActiveConnections() {
        CapturingSseEmitter a = new CapturingSseEmitter();
        CapturingSseEmitter b = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, a);
        SseTestSupport.inject(hub, 2L, b);

        hub.heartbeat();

        assertEquals(1, a.comments().size());
        assertEquals(1, b.comments().size());
    }

    @Test
    void heartbeat_failureRemovesOnlyThatConnection() {
        CapturingSseEmitter broken = new CapturingSseEmitter();
        broken.failNextSendWithIllegalState();
        CapturingSseEmitter healthy = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, broken);
        SseTestSupport.inject(hub, 1L, healthy);

        hub.heartbeat();

        assertEquals(1, SseTestSupport.connectionCount(hub, 1L));
        assertEquals(1, healthy.comments().size(), "一条心跳失败不影响其它连接");
    }

    @Test
    void heartbeat_doesNotResendToRemovedConnection() {
        CapturingSseEmitter dead = new CapturingSseEmitter();
        dead.failNextSendWithIllegalState();
        SseTestSupport.inject(hub, 1L, dead);

        hub.heartbeat();   // 第一次：失败并被移除
        int afterFirst = dead.comments().size();
        hub.heartbeat();   // 第二次：该连接已不在容器中

        assertEquals(0, SseTestSupport.connectionCount(hub, 1L));
        assertEquals(afterFirst, dead.comments().size(), "已移除的连接不应再收到心跳");
    }

    @Test
    void heartbeat_withNoConnections_isNoOp() {
        hub.heartbeat();

        assertEquals(0, SseTestSupport.userCount(hub));
    }

    /**
     * 调度契约：心跳必须由 {@code @Scheduled} 驱动、间隔 30 秒，且整个 SseHub 只有一个调度方法
     * （避免重复创建调度器）。
     *
     * <p>运行时前提：项目已在 {@code PetApplication} 上开启 {@code @EnableScheduling}，
     * 启动日志中可观察到 {@code scheduling-1} 线程，说明调度基础设施已生效。</p>
     */
    @Test
    void heartbeat_isScheduledOnceEvery30Seconds() throws Exception {
        org.springframework.scheduling.annotation.Scheduled scheduled = SseHub.class
                .getMethod("heartbeat")
                .getAnnotation(org.springframework.scheduling.annotation.Scheduled.class);

        assertEquals(30_000L, scheduled.fixedDelay(), "心跳间隔必须是 30 秒");

        long scheduledMethods = 0;
        for (java.lang.reflect.Method m : SseHub.class.getDeclaredMethods()) {
            if (m.isAnnotationPresent(org.springframework.scheduling.annotation.Scheduled.class)) {
                scheduledMethods++;
            }
        }
        assertEquals(1, scheduledMethods, "SseHub 只允许一个调度方法，避免重复调度器");
    }

    // ---------- 并发 ----------

    @Test
    void concurrentConnectSendHeartbeatAndCleanup_keepsContainerConsistent() throws Exception {
        final int threads = 6;
        final int perThread = 40;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger failures = new AtomicInteger();

        for (int t = 0; t < threads; t++) {
            final long userId = (t + 1) * 100L;
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        SseEmitter e = hub.connect(userId);
                        hub.send(userId, "x", "p");
                        hub.heartbeat();
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
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertEquals(0, failures.get(), "并发过程不应抛异常");
        for (int t = 0; t < threads; t++) {
            assertEquals(perThread / 2, SseTestSupport.connectionCount(hub, (t + 1) * 100L),
                    "并发后连接数应等于未清理的数量");
        }
    }
}
