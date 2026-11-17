package com.pet.order.service;

import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.order.dto.OrderStatusEventDTO;
import com.pet.order.entity.PetOrder;
import com.pet.sse.SseHub;
import com.pet.sse.SseTestSupport;
import com.pet.sse.SseTestSupport.CapturingSseEmitter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * {@link OrderStatusBroadcaster} 行为测试。
 *
 * <p>该类比另外两套多出两个真实差异点，单独覆盖：</p>
 * <ul>
 *   <li><b>接收人解析</b>：订单主人 + 看护者账号 + 商家账号，用 LinkedHashSet 去重；</li>
 *   <li><b>事务后置</b>：{@code broadcastPoolChange} 在事务中注册 afterCommit，避免前端读到未提交数据。</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class OrderStatusBroadcasterTest {

    @Mock
    private KeeperMapper keeperMapper;
    @Mock
    private MerchantMapper merchantMapper;

    private SseHub hub;
    private OrderStatusBroadcaster broadcaster;

    @BeforeEach
    void setUp() {
        hub = new SseHub();
        broadcaster = new OrderStatusBroadcaster(hub, keeperMapper, merchantMapper);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // ---------- 1. 单用户连接 ----------

    @Test
    void connect_registersConnectionAndSendsHandshake() {
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

    // ---------- 4. 事件正常发送 ----------

    @Test
    void broadcast_sendsOrderStatusToOwner() {
        CapturingSseEmitter owner = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, owner);

        broadcaster.broadcast(order(1L, null, null));

        assertEquals(1, owner.sendCount());
        OrderStatusEventDTO payload = (OrderStatusEventDTO) owner.payloads().get(0);
        assertEquals(100L, payload.getOrder_id_wsh());
        assertEquals("paid", payload.getStatus_wsh());
        assertEquals(List.of("order-status"), owner.eventNames(),
                "订单状态事件名固定为 order-status");
    }

    // ---------- 接收人解析 ----------

    @Test
    void broadcast_resolvesKeeperAndMerchantAccounts() {
        when(keeperMapper.selectById(5L)).thenReturn(keeper(5L, 2L));
        when(merchantMapper.selectById(9L)).thenReturn(merchant(9L, 3L));

        CapturingSseEmitter owner = new CapturingSseEmitter();
        CapturingSseEmitter keeperUser = new CapturingSseEmitter();
        CapturingSseEmitter merchantUser = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, owner);
        SseTestSupport.inject(hub, 2L, keeperUser);
        SseTestSupport.inject(hub, 3L, merchantUser);

        broadcaster.broadcast(order(1L, 5L, 9L));

        assertEquals(1, owner.sendCount(), "主人收到");
        assertEquals(1, keeperUser.sendCount(), "看护者账号收到");
        assertEquals(1, merchantUser.sendCount(), "商家账号收到");
    }

    @Test
    void broadcast_sameAccountForAllRoles_pushesOnlyOnce() {
        // 主人 / 看护者账号 / 商家账号都是同一个用户
        when(keeperMapper.selectById(5L)).thenReturn(keeper(5L, 1L));
        when(merchantMapper.selectById(9L)).thenReturn(merchant(9L, 1L));

        CapturingSseEmitter emitter = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, emitter);

        broadcaster.broadcast(order(1L, 5L, 9L));

        assertEquals(1, emitter.sendCount(), "LinkedHashSet 去重后只推一次");
    }

    @Test
    void broadcast_missingKeeperOrMerchant_skipsThem() {
        when(keeperMapper.selectById(5L)).thenReturn(null);
        when(merchantMapper.selectById(9L)).thenReturn(null);

        CapturingSseEmitter owner = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, owner);

        broadcaster.broadcast(order(1L, 5L, 9L));

        assertEquals(1, owner.sendCount(), "查不到参与方时不应影响主人推送");
    }

    @Test
    void broadcast_nullOrder_isNoOp() {
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, emitter);

        broadcaster.broadcast(null);

        assertEquals(0, emitter.sendCount());
    }

    // ---------- 3. 不同用户隔离 ----------

    @Test
    void broadcast_doesNotReachUnrelatedUser() {
        CapturingSseEmitter bystander = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 99L, bystander);

        broadcaster.broadcast(order(1L, null, null));

        assertEquals(0, bystander.sendCount());
    }

    // ---------- 待分配池：事务后置 ----------

    @Test
    void broadcastPoolChange_withoutTransaction_sendsImmediately() {
        when(keeperMapper.selectList(any())).thenReturn(List.of(keeper(5L, 2L)));

        CapturingSseEmitter keeperUser = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 2L, keeperUser);

        broadcaster.broadcastPoolChange(order(1L, null, 9L), "pool-new");

        assertEquals(1, keeperUser.sendCount(), "无事务时应立即推送");
        assertEquals(List.of("pool-new"), keeperUser.eventNames(),
                "待分配池事件名由调用方指定（pool-new / pool-removed）");
    }

    @Test
    void broadcastPoolChange_insideTransaction_defersUntilAfterCommit() {
        when(keeperMapper.selectList(any())).thenReturn(List.of(keeper(5L, 2L)));

        CapturingSseEmitter keeperUser = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 2L, keeperUser);

        TransactionSynchronizationManager.initSynchronization();
        broadcaster.broadcastPoolChange(order(1L, null, 9L), "pool-new");

        assertEquals(0, keeperUser.sendCount(), "事务未提交前不得推送");

        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, syncs.size(), "应注册一个 afterCommit 同步器");
        syncs.forEach(TransactionSynchronization::afterCommit);

        assertEquals(1, keeperUser.sendCount(), "提交后才推送");
    }

    @Test
    void broadcastPoolChange_nullMerchant_isNoOp() {
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 2L, emitter);

        broadcaster.broadcastPoolChange(order(1L, null, null), "pool-new");

        assertEquals(0, emitter.sendCount());
    }

    @Test
    void broadcastPoolChange_noActiveKeepers_isNoOp() {
        when(keeperMapper.selectList(any())).thenReturn(List.of());

        CapturingSseEmitter emitter = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 2L, emitter);

        broadcaster.broadcastPoolChange(order(1L, null, 9L), "pool-new");

        assertEquals(0, emitter.sendCount());
    }

    // ---------- 5/7. 异常与清理 ----------

    @Test
    void broadcast_sendFailure_removesBrokenConnection() {
        CapturingSseEmitter broken = new CapturingSseEmitter();
        broken.failNextSendWithIOException();
        SseTestSupport.inject(hub, 1L, broken);

        broadcaster.broadcast(order(1L, null, null));

        assertEquals(0, SseTestSupport.connectionCount(hub, 1L));
        assertEquals(0, SseTestSupport.userCount(hub));
    }

    @Test
    void completionCallback_removesConnection() {
        SseEmitter emitter = broadcaster.connect(10L);

        SseTestSupport.fireCompletion(emitter);

        assertEquals(0, SseTestSupport.connectionCount(hub, 10L));
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
    void heartbeat_sendsCommentFrameWithoutDisturbingOrderEvents() {
        // 批次 04 第二阶段正式批准加入心跳：注释帧不改变 order-status 事件名与载荷。
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        SseTestSupport.inject(hub, 1L, emitter);

        broadcaster.broadcast(order(1L, null, null));
        hub.heartbeat();

        assertEquals(List.of("order-status"), emitter.eventNames());
        assertEquals(1, emitter.payloads().size());
        assertEquals(1, emitter.comments().size(), "心跳以 SSE 注释帧下发");
    }

    // ---------- 10. 并发 ----------

    @Test
    void concurrentConnectBroadcastAndCleanup_keepsRegistryConsistent() throws Exception {
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
                        SseEmitter e = broadcaster.connect(userId);
                        broadcaster.broadcast(order(userId, null, null));
                        if (i % 2 == 0) {
                            SseTestSupport.fireTimeout(e);
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

        assertEquals(0, failures.get());
        for (int t = 0; t < threads; t++) {
            assertEquals(perThread / 2,
                    SseTestSupport.connectionCount(hub, (t + 1) * 100L));
        }
        assertNotNull(SseTestSupport.registry(hub));
    }

    private static PetOrder order(Long ownerId, Long keeperId, Long merchantId) {
        PetOrder o = new PetOrder();
        o.setId_wsh(100L);
        o.setOrder_no_wsh("ORD-100");
        o.setOwner_id_wsh(ownerId);
        o.setKeeper_id_wsh(keeperId);
        o.setMerchant_id_wsh(merchantId);
        o.setStatus_wsh("paid");
        o.setUpdated_at_wsh(LocalDateTime.now());
        return o;
    }

    private static Keeper keeper(Long id, Long userId) {
        Keeper k = new Keeper();
        k.setId_wsh(id);
        k.setUser_id_wsh(userId);
        return k;
    }

    private static Merchant merchant(Long id, Long userId) {
        Merchant m = new Merchant();
        m.setId_wsh(id);
        m.setUser_id_wsh(userId);
        return m;
    }
}
