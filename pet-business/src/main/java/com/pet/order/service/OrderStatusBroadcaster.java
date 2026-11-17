package com.pet.order.service;

import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.order.dto.OrderStatusEventDTO;
import com.pet.order.entity.PetOrder;
import com.pet.sse.SseHub;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 订单状态变更的 SSE 推送适配层。
 *
 * <p>本类只保留订单域独有的东西：接收人解析（主人 / 看护者账号 / 商家账号，LinkedHashSet 去重）、
 * 两种事件名（{@code order-status} 与待分配池的 {@code pool-new} / {@code pool-removed}）、
 * 载荷结构、以及待分配池的 afterCommit 时序。连接的注册、握手、按用户投递、失败清理、
 * 心跳全部委托给 {@link SseHub}。</p>
 */
@Service
public class OrderStatusBroadcaster {

    /** 订单状态事件名固定，前端按此订阅；不得改写。 */
    private static final String EVENT_ORDER_STATUS = "order-status";

    private final SseHub sseHub;
    private final KeeperMapper keeperMapper;
    private final MerchantMapper merchantMapper;

    public OrderStatusBroadcaster(SseHub sseHub, KeeperMapper keeperMapper, MerchantMapper merchantMapper) {
        this.sseHub = sseHub;
        this.keeperMapper = keeperMapper;
        this.merchantMapper = merchantMapper;
    }

    /** 为用户建立 SSE 连接（委托 SseHub，握手行为不变）。 */
    public SseEmitter connect(Long userId) {
        return sseHub.connect(userId);
    }

    /**
     * 广播订单状态变更给所有相关方：订单主人、看护者关联账号、商家关联账号（同一账号只推一次）。
     *
     * @param order 状态变更的订单；null 时忽略
     */
    public void broadcast(PetOrder order) {
        if (order == null) {
            return;
        }
        sseHub.sendAll(resolveRecipients(order), EVENT_ORDER_STATUS, buildPayload(order));
    }

    /**
     * 待分配池变更广播（新单入池 {@code pool-new} / 离开池子 {@code pool-removed}）。
     *
     * <p>事务语义保持不变：若当前处于事务中，注册 afterCommit 延后推送 —— 前端收到事件会立刻回查池子，
     * 事务未提交时读到的是旧数据，且没有补偿事件。</p>
     *
     * @param order     发生变化的订单
     * @param eventName 事件名（pool-new / pool-removed）
     */
    public void broadcastPoolChange(PetOrder order, String eventName) {
        if (order == null || order.getMerchant_id_wsh() == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doBroadcastPoolChange(order, eventName);
                }
            });
            return;
        }
        doBroadcastPoolChange(order, eventName);
    }

    private void doBroadcastPoolChange(PetOrder order, String eventName) {
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
        List<Long> keeperUserIds = new ArrayList<>(keepers.size());
        for (Keeper keeper : keepers) {
            keeperUserIds.add(keeper.getUser_id_wsh());
        }
        sseHub.sendAll(keeperUserIds, eventName, buildPayload(order));
    }

    /** 事件载荷：字段与迁移前逐字一致（updated_at 缺失时用当前时间兜底）。 */
    private OrderStatusEventDTO buildPayload(PetOrder order) {
        OrderStatusEventDTO payload = new OrderStatusEventDTO();
        payload.setOrder_id_wsh(order.getId_wsh());
        payload.setOrder_no_wsh(order.getOrder_no_wsh());
        payload.setStatus_wsh(order.getStatus_wsh());
        payload.setUpdated_at_wsh(order.getUpdated_at_wsh() != null
                ? order.getUpdated_at_wsh() : LocalDateTime.now());
        return payload;
    }

    /**
     * 解析订单接收人：主人 + 看护者账号 + 商家账号，用 LinkedHashSet 去重（同一账号只推一次）。
     *
     * <p>该逻辑属于订单域业务规则，<b>不</b>下沉到 SseHub。</p>
     */
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
}
