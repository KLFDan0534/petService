package com.pet.customer.service;

import com.pet.customer.dto.ChatMessageDTO;
import com.pet.sse.SseHub;
import com.pet.sse.SseTestSupport;
import com.pet.sse.SseTestSupport.CapturingSseEmitter;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * {@link ChatEventBroadcaster} 行为测试。
 *
 * <p>与 {@code NotificationBroadcaster} 的差异点：一次聊天事件会推给<b>收发双方</b>，
 * 且载荷需要经 {@code UserMapper} 补全发送人昵称。这两点分别有独立用例覆盖。</p>
 */
@ExtendWith(MockitoExtension.class)
class ChatEventBroadcasterTest {

    @Mock
    private UserMapper userMapper;

    private SseHub hub;
    private ChatEventBroadcaster broadcaster;

    @BeforeEach
    void setUp() {
        hub = new SseHub();
        broadcaster = new ChatEventBroadcaster(hub, userMapper);
    }

    // ---------- 1. 单用户连接 ----------

    @Test
    void connect_registersOneConnectionAndSendsHandshake() {
        SseEmitter emitter = broadcaster.connect(10L);

        assertEquals(1, SseTestSupport.connectionCount(hub, 10L));
        assertEquals(1, SseTestSupport.bufferedEventCount(emitter),
                "connect 必须发送一次 connected 握手");
    }

    // ---------- 2. 同一用户多连接 ----------

    @Test
    void connect_sameUserTwice_keepsBoth() {
        broadcaster.connect(10L);
        broadcaster.connect(10L);

        assertEquals(2, SseTestSupport.connectionCount(hub, 10L));
    }

    // ---------- 4. 事件正常发送：推给收发双方 ----------

    @Test
    void broadcastMessage_reachesBothSenderAndReceiver() {
        CapturingSseEmitter sender = new CapturingSseEmitter();
        CapturingSseEmitter receiver = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, sender);
        SseTestSupport.inject(hub, 2L, receiver);

        broadcaster.broadcastMessage(message(1L, 2L));

        assertEquals(1, sender.sendCount(), "发送方也要收到（多端同步）");
        assertEquals(1, receiver.sendCount(), "接收方必须收到");
        assertEquals(java.util.List.of("chat-message"), receiver.eventNames(),
                "聊天事件名固定为 chat-message");
    }

    @Test
    void broadcastMessage_payloadIsEventDtoNotRawMessage() {
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 2L, emitter);

        broadcaster.broadcastMessage(message(1L, 2L));

        Object payload = emitter.payloads().get(0);
        assertTrue(payload instanceof com.pet.customer.dto.ChatMessageEventDTO,
                "推送载荷应是精简后的 ChatMessageEventDTO，而非原始 ChatMessageDTO");
    }

    @Test
    void broadcastMessage_senderNameComesFromNicknameThenUsername() {
        User withNickname = new User();
        withNickname.setNickname_wsh("小明");
        withNickname.setUsername_wsh("ming");
        when(userMapper.selectById(1L)).thenReturn(withNickname);

        CapturingSseEmitter emitter = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 2L, emitter);

        broadcaster.broadcastMessage(message(1L, 2L));

        com.pet.customer.dto.ChatMessageEventDTO event =
                (com.pet.customer.dto.ChatMessageEventDTO) emitter.payloads().get(0);
        assertEquals("小明", event.getFrom_user_name_wsh(), "优先取昵称");
    }

    @Test
    void broadcastMessage_missingUser_leavesSenderNameNull() {
        when(userMapper.selectById(any())).thenReturn(null);

        CapturingSseEmitter emitter = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 2L, emitter);

        broadcaster.broadcastMessage(message(1L, 2L));

        com.pet.customer.dto.ChatMessageEventDTO event =
                (com.pet.customer.dto.ChatMessageEventDTO) emitter.payloads().get(0);
        assertEquals(null, event.getFrom_user_name_wsh());
    }

    /**
     * 现状记录：收发双方是同一账号时，{@code broadcastMessage} 会对同一条连接发两次。
     * 前端按 message_id 去重可以接受，但这是当前实现的真实行为，固化为契约。
     */
    @Test
    void broadcastMessage_selfConversation_pushesTwiceToSameConnection() {
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, emitter);

        broadcaster.broadcastMessage(message(1L, 1L));

        assertEquals(2, emitter.sendCount(),
                "当前实现对同一用户会推送两次（to + from 各一次）");
    }

    // ---------- 3. 不同用户隔离 ----------

    @Test
    void broadcastMessage_doesNotReachUnrelatedUser() {
        CapturingSseEmitter bystander = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 99L, bystander);

        broadcaster.broadcastMessage(message(1L, 2L));

        assertEquals(0, bystander.sendCount());
    }

    // ---------- 线程事件 ----------

    @Test
    void broadcastThreadMessage_reachesBothParties() {
        CapturingSseEmitter a = new CapturingSseEmitter();
        CapturingSseEmitter b = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, a);
        SseTestSupport.inject(hub, 2L, b);

        broadcaster.broadcastThreadMessage("complaint", 7L, 2L, 1L, "内容", null, LocalDateTime.now());

        assertEquals(1, a.sendCount());
        assertEquals(1, b.sendCount());
        assertTrue(a.payloads().get(0) instanceof java.util.Map,
                "线程事件载荷是 Map（含 type/biz_id 等字段）");
        assertEquals(java.util.List.of("thread-message"), a.eventNames(),
                "线程事件名固定为 thread-message，与 chat-message 区分");
    }

    // ---------- 入参防御 ----------

    @Test
    void broadcastMessage_null_isNoOp() {
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, emitter);

        broadcaster.broadcastMessage(null);

        assertEquals(0, emitter.sendCount());
    }

    // ---------- 5/7. 异常与清理 ----------

    @Test
    void broadcastMessage_sendFailure_removesBrokenConnection() {
        CapturingSseEmitter broken = new CapturingSseEmitter();
        broken.failNextSendWithIOException();
        SseTestSupport.inject(hub, 2L, broken);

        broadcaster.broadcastMessage(message(1L, 2L));

        assertEquals(0, SseTestSupport.connectionCount(hub, 2L));
    }

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

        SseTestSupport.fireError(emitter, new RuntimeException("boom"));

        assertEquals(0, SseTestSupport.connectionCount(hub, 10L));
    }

    // ---------- 9. 心跳 ----------

    @Test
    void heartbeat_sendsCommentFrameWithoutDisturbingChatEvents() {
        // 批次 04 第二阶段正式批准加入心跳：注释帧不改变 chat-message 事件名与载荷。
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 2L, emitter);

        broadcaster.broadcastMessage(message(1L, 2L));
        hub.heartbeat();

        assertEquals(java.util.List.of("chat-message"), emitter.eventNames());
        assertEquals(1, emitter.payloads().size());
        assertEquals(1, emitter.comments().size(), "心跳以 SSE 注释帧下发");
    }

    // ---------- 10. 并发 ----------

    @Test
    void concurrentConnectAndBroadcast_keepsRegistryConsistent() throws Exception {
        final int threads = 6;
        final int perThread = 40;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger failures = new AtomicInteger();

        for (int t = 0; t < threads; t++) {
            final long base = (t + 1) * 100L;
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        SseEmitter e = broadcaster.connect(base);
                        broadcaster.broadcastMessage(message(base, base + 1));
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
            assertEquals(perThread / 2,
                    SseTestSupport.connectionCount(hub, (t + 1) * 100L),
                    "并发后连接数应等于未清理的数量");
        }
    }

    private static ChatMessageDTO message(Long from, Long to) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId_wsh(1L);
        dto.setFrom_user_id_wsh(from);
        dto.setTo_user_id_wsh(to);
        dto.setContent_wsh("你好");
        dto.setType_wsh("text");
        dto.setCreated_at_wsh(LocalDateTime.now());
        return dto;
    }
}
