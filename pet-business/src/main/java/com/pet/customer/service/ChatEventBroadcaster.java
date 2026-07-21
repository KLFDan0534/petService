package com.pet.customer.service;

import com.pet.customer.dto.ChatMessageDTO;
import com.pet.customer.dto.ChatMessageEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class ChatEventBroadcaster {
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public SseEmitter connect(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        emittersByUser.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(ex -> remove(userId, emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok", MediaType.TEXT_PLAIN));
        } catch (IOException e) {
            remove(userId, emitter);
        }
        return emitter;
    }

    public void broadcastMessage(ChatMessageDTO message) {
        if (message == null) {
            return;
        }
        ChatMessageEventDTO event = toEvent(message);
        sendToUser(message.getTo_user_id_wsh(), event);
        sendToUser(message.getFrom_user_id_wsh(), event);
    }

    private void sendToUser(Long userId, ChatMessageEventDTO event) {
        if (userId == null) {
            return;
        }
        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("chat-message").data(event));
            } catch (IOException e) {
                remove(userId, emitter);
            }
        }
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

    private ChatMessageEventDTO toEvent(ChatMessageDTO message) {
        ChatMessageEventDTO event = new ChatMessageEventDTO();
        event.setMessage_id_wsh(message.getId_wsh());
        event.setOrder_id_wsh(message.getOrder_id_wsh());
        event.setFrom_user_id_wsh(message.getFrom_user_id_wsh());
        event.setTo_user_id_wsh(message.getTo_user_id_wsh());
        event.setType_wsh(message.getType_wsh());
        event.setCreated_at_wsh(message.getCreated_at_wsh() != null ? message.getCreated_at_wsh() : LocalDateTime.now());
        return event;
    }
}
