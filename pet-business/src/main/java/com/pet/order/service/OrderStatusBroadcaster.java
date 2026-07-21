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
