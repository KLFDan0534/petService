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
        SseEmitter emitter = new SseEmitter(0L);
        emittersByUser.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError((ex) -> remove(userId, emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok", MediaType.TEXT_PLAIN));
        } catch (IOException e) {
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
        payload.setUpdated_at_wsh(order.getUpdated_at_wsh() != null ? order.getUpdated_at_wsh() : LocalDateTime.now());
        for (Long userId : resolveRecipients(order)) {
            sendToUser(userId, payload);
        }
    }

    private void sendToUser(Long userId, OrderStatusEventDTO payload) {
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
                        .name("order-status")
                        .data(payload));
            } catch (IOException e) {
                remove(userId, emitter);
            }
        }
    }

    private Set<Long> resolveRecipients(PetOrder order) {
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
