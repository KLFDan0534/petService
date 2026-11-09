package com.pet.order.service;

import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.order.dto.OrderStatusEventDTO;
import com.pet.order.entity.PetOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-Sent Events (SSE) broadcaster for real-time order status updates.
 * <p>
 * Maintains a concurrent map of user ID to active SSE emitters. When an order's
 * status changes, the broadcaster resolves all relevant recipients
 * (the order owner, the keeper's user account, and the merchant's user account)
 * and pushes an {@link OrderStatusEventDTO} to each recipient's connected emitters.
 * <p>
 * Emitters are automatically cleaned up on completion, timeout, or error.
 */
@Service
@Slf4j
public class OrderStatusBroadcaster {
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();
    private final KeeperMapper keeperMapper;
    private final MerchantMapper merchantMapper;

    /**
     * 【构造订单状态广播器】
     *
     * 业务作用：注入看护者与商家 Mapper，用于把订单关联账号解析成接收人。
     *
     * 调用场景：Spring 容器启动创建 Bean 时调用。
     *
     * 调用链：Spring 容器 → 本构造器 → 赋值 keeperMapper / merchantMapper 字段。
     *
     * 数据处理：无。
     *
     * 业务规则：依赖均为 final 字段。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：emittersByUser 为实例级 ConcurrentHashMap，多实例部署时各节点只推送本节点连接。
     */
    public OrderStatusBroadcaster(KeeperMapper keeperMapper, MerchantMapper merchantMapper) {
        this.keeperMapper = keeperMapper;
        this.merchantMapper = merchantMapper;
    }

    /**
     * Registers a new SSE connection for a user. Creates an emitter with no timeout
     * and registers lifecycle callbacks to clean up on completion/timeout/error.
     * Sends an initial "connected" event to confirm the connection is established.
     *
     * @param userId the user ID to associate with this emitter
     * @return the SseEmitter for pushing future events
     */
    /**
     * 【建立SSE连接】
     *
     * 业务作用：
     * 为指定用户创建SSE连接，用于实时推送订单状态变更事件。使用ConcurrentHashMap管理多用户连接，
     * 支持同一用户多设备连接。
     *
     * 调用场景：
     * 前端页面加载时建立SSE长连接，接收订单状态实时推送。
     *
     * 调用链：
     * 前端/SSE Controller
     * ↓
     * connect(userId)
     * ↓
     * 创建SseEmitter(超时时间0=永久) → 注册onCompletion/onTimeout/onError回调
     * → 发送"connected"事件确认 → 返回emitter
     *
     * 数据处理：
     * 使用CopyOnWriteArrayList存储每个用户的emitter集合，保证并发安全。
     *
     * @param userId 用户ID
     * @return SSE发射器
     */
    public SseEmitter connect(Long userId) {
        // 超时设为 0 表示连接不自动断开
        SseEmitter emitter = new SseEmitter(0L);
        emittersByUser.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        // 结束、超时、出错三种回调都清理注册表
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError((ex) -> remove(userId, emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok", MediaType.TEXT_PLAIN));
        } catch (IOException e) {
            // 发送失败说明连接已不可用，立即移除
            remove(userId, emitter);
        }
        return emitter;
    }

    /**
     * Broadcasts the current order status to all relevant recipients.
     * Resolves recipients via {@link #resolveRecipients} which includes the
     * order owner, the keeper's user account, and the merchant's user account.
     * Failures to send to individual emitters are handled gracefully by removing
     * the failed emitter.
     *
     * @param order the order whose status to broadcast (null-safe)
     */
    /**
     * Broadcasts the current order status to all relevant recipients.
     * Resolves recipients via {@link #resolveRecipients} which includes the
     * order owner, the keeper's user account, and the merchant's user account.
     * Failures to send to individual emitters are handled gracefully by removing
     * the failed emitter.
     *
     * @param order the order whose status to broadcast (null-safe)
     */
    /**
     * 【广播订单状态变更】
     *
     * 业务作用：
     * 将订单状态变更事件推送给所有相关用户（主人、看护者、商家），
     * 实现多端实时同步订单状态。
     *
     * 调用场景：
     * 订单状态变更后（create/cancel/accept/reject/deliver/receive/start/complete等）调用。
     *
     * 调用链：
     * OrderService内部方法 → broadcastOrderChange()
     * ↓
     * broadcast(order)
     * ↓
     * 构建OrderStatusEventDTO → resolveRecipients() → 逐个sendToUser()推送SSE事件
     *
     * 数据处理：
     * 推送给订单的主人、看护者关联用户、商家关联用户三端。
     *
     * 状态影响：
     * 仅推送通知，不修改业务数据。
     *
     * @param order 状态变更的订单
     */
    public void broadcast(PetOrder order) {
        if (order == null) {
            return;
        }
        OrderStatusEventDTO payload = new OrderStatusEventDTO();
        payload.setOrder_id_wsh(order.getId_wsh());
        payload.setOrder_no_wsh(order.getOrder_no_wsh());
        payload.setStatus_wsh(order.getStatus_wsh());
        // 更新时间缺失时用当前时间兜底
        payload.setUpdated_at_wsh(order.getUpdated_at_wsh() != null ? order.getUpdated_at_wsh() : LocalDateTime.now());
        for (Long userId : resolveRecipients(order)) {
            sendToUser(userId, payload);
        }
    }

    /**
     * 【待分配池变更广播】
     *
     * 业务作用：
     * 新订单支付后进入「待分配池」，或订单被抢/被指派后离开池子时，
     * 实时通知该商家名下**在职**看护者，让工作台的「待抢单」角标立即变化，
     * 而不是等用户手动刷新。
     *
     * 调用场景：
     * - 支付成功且订单无负责人 → eventName = "pool-new"
     * - 抢单 / 指派 / 取消成功 → eventName = "pool-removed"
     *
     * 数据处理：
     * 只推送事件，不修改业务数据；连接不存在时静默跳过。
     *
     * @param order     发生变化的订单
     * @param eventName 事件名（pool-new / pool-removed）
     */
    public void broadcastPoolChange(PetOrder order, String eventName) {
        if (order == null || order.getMerchant_id_wsh() == null) {
            return;
        }
        List<Keeper> keepers = keeperMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Keeper>()
                        .eq(Keeper::getMerchant_id_wsh, order.getMerchant_id_wsh())
                        .in(Keeper::getStatus_wsh, List.of(
                                com.pet.common.StatusCode.KEEPER_ACTIVE.getValue(),
                                com.pet.common.StatusCode.KEEPER_OFFLINE.getValue(),
                                com.pet.common.StatusCode.KEEPER_BUSY.getValue())));
        if (keepers.isEmpty()) {
            return;
        }
        OrderStatusEventDTO payload = new OrderStatusEventDTO();
        payload.setOrder_id_wsh(order.getId_wsh());
        payload.setOrder_no_wsh(order.getOrder_no_wsh());
        payload.setStatus_wsh(order.getStatus_wsh());
        payload.setUpdated_at_wsh(order.getUpdated_at_wsh() != null ? order.getUpdated_at_wsh() : LocalDateTime.now());
        for (Keeper keeper : keepers) {
            sendToUser(keeper.getUser_id_wsh(), payload, eventName);
        }
    }

    /**
     * 【向单个用户推送事件】
     *
     * 业务作用：把订单状态事件推送给指定用户当前所有在线的 SSE 连接。
     *
     * 调用场景：broadcast 遍历接收人时。
     *
     * 调用链：broadcast() → sendToUser() → emitter.send() / remove()。
     *
     * 数据处理：按用户 ID 取出 emitter 列表，逐个发送名为 order-status 的事件。
     *
     * 业务规则：用户 ID 为空或该用户没有连接时直接返回。
     *
     * 状态影响：无，仅推送通知。
     *
     * 异常情况：单个 emitter 发送抛 IOException 时移除该 emitter，不影响其他连接。
     *
     * 注意事项：同一用户多端登录会收到重复事件，由前端自行去重。
     */
    private void sendToUser(Long userId, OrderStatusEventDTO payload) {
        sendToUser(userId, payload, "order-status");
    }

    /** 按指定事件名推送（待分配池用 pool-new / pool-removed，与订单状态事件区分开）。 */
    private void sendToUser(Long userId, OrderStatusEventDTO payload, String eventName) {
        if (userId == null) {
            return;
        }
        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(payload));
            } catch (IOException e) {
                // 单个连接推送失败只影响该连接
                remove(userId, emitter);
            }
        }
    }

    /**
     * 【解析订单接收人】
     *
     * 业务作用：计算一条订单状态变更需要通知哪些用户账号。
     *
     * 调用场景：broadcast 推送前。
     *
     * 调用链：broadcast() → resolveRecipients() → keeperMapper.selectById() / merchantMapper.selectById()。
     *
     * 数据处理：先加入订单 owner_id_wsh；再按 keeper_id_wsh 查看护者取其 user_id_wsh；再按 merchant_id_wsh 查商家取其 user_id_wsh。
     *
     * 业务规则：使用 LinkedHashSet 去重，主人、看护者、商家若是同一账号只推一次。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：无，查不到看护者或商家时跳过对应接收人。
     *
     * 注意事项：每次广播会触发两次主键查询，高频广播时需关注数据库压力。
     */
    private Set<Long> resolveRecipients(PetOrder order) {
        // 用有序集合去重，同一账号只推一次
        Set<Long> recipients = new LinkedHashSet<>();
        if (order.getOwner_id_wsh() != null) {
            recipients.add(order.getOwner_id_wsh());
        }
        if (order.getKeeper_id_wsh() != null) {
            Keeper keeper = keeperMapper.selectById(order.getKeeper_id_wsh());
            if (keeper != null && keeper.getUser_id_wsh() != null) {
                recipients.add(keeper.getUser_id_wsh());
            }
        }
        if (order.getMerchant_id_wsh() != null) {
            Merchant merchant = merchantMapper.selectById(order.getMerchant_id_wsh());
            if (merchant != null && merchant.getUser_id_wsh() != null) {
                recipients.add(merchant.getUser_id_wsh());
            }
        }
        return recipients;
    }

    /**
     * 【移除失效连接】
     *
     * 业务作用：把失效的 SSE 连接从用户连接列表中删除。
     *
     * 调用场景：连接完成、超时、异常回调，以及推送失败时。
     *
     * 调用链：connect() 的回调 / sendToUser() 的 IOException 分支 → remove()。
     *
     * 数据处理：从 CopyOnWriteArrayList 中移除指定 emitter；列表为空时把该用户条目从 Map 中移除。
     *
     * 业务规则：用户不存在时直接返回。
     *
     * 状态影响：无，仅清理内存中的连接注册表。
     *
     * 异常情况：无。
     *
     * 注意事项：使用 Map.remove(key, value) 两参数形式，避免并发下误删其他线程刚创建的新列表。
     */
    private void remove(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        // 列表空时连用户条目一起清掉，避免泄漏
        if (emitters.isEmpty()) {
            emittersByUser.remove(userId, emitters);
        }
    }
}
