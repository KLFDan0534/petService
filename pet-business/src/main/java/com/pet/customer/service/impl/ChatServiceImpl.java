package com.pet.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pet.common.BusinessException;
import com.pet.customer.dto.ChatMessageDTO;
import com.pet.customer.entity.ChatMessage;
import com.pet.customer.mapper.ChatMessageMapper;
import com.pet.customer.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {
    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final Set<String> ALLOWED_TYPES = Set.of("text", "image", "video", "file");

    private final ChatMessageMapper chatMessageMapper;

    public ChatServiceImpl(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    @Override
    public List<ChatMessageDTO> getConversation(Long userId1, Long userId2, Long orderId, Long beforeId, Integer size) {
        if (userId1 == null || userId2 == null) {
            throw new BusinessException(400, "conversation users cannot be empty");
        }
        int pageSize = normalizeSize(size);
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                .and(w -> w.eq(ChatMessage::getFrom_user_id_wsh, userId1)
                        .eq(ChatMessage::getTo_user_id_wsh, userId2)
                        .or()
                        .eq(ChatMessage::getFrom_user_id_wsh, userId2)
                        .eq(ChatMessage::getTo_user_id_wsh, userId1));
        if (orderId != null) {
            wrapper.eq(ChatMessage::getOrder_id_wsh, orderId);
        } else {
            wrapper.isNull(ChatMessage::getOrder_id_wsh);
        }
        if (beforeId != null) {
            wrapper.lt(ChatMessage::getId_wsh, beforeId);
        }

        List<ChatMessage> descending = chatMessageMapper.selectList(
                wrapper.orderByDesc(ChatMessage::getId_wsh).last("LIMIT " + pageSize));
        return descending.stream()
                .sorted((left, right) -> Long.compare(nullSafeId(left), nullSafeId(right)))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessageDTO> getUnreadMessages(Long userId) {
        if (userId == null) {
            throw new BusinessException(400, "user cannot be empty");
        }
        return toDTOList(chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getTo_user_id_wsh, userId)
                        .eq(ChatMessage::getRead_wsh, 0)
                        .orderByDesc(ChatMessage::getCreated_at_wsh)
                        .last("LIMIT " + MAX_PAGE_SIZE)));
    }

    @Override
    public long countUnreadMessages(Long userId) {
        if (userId == null) {
            return 0;
        }
        Long count = chatMessageMapper.selectCount(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getTo_user_id_wsh, userId)
                        .eq(ChatMessage::getRead_wsh, 0));
        return count == null ? 0 : count;
    }

    @Transactional
    @Override
    public ChatMessageDTO sendMessage(ChatMessage message) {
        if (message == null) {
            throw new BusinessException(400, "message cannot be empty");
        }
        if (message.getFrom_user_id_wsh() == null) {
            throw new BusinessException(400, "sender cannot be empty");
        }
        if (message.getTo_user_id_wsh() == null) {
            throw new BusinessException(400, "receiver cannot be empty");
        }
        if (message.getFrom_user_id_wsh().equals(message.getTo_user_id_wsh())) {
            throw new BusinessException(400, "cannot send message to self");
        }
        String content = trimToNull(message.getContent_wsh());
        String fileUrl = trimToNull(message.getFile_url_wsh());
        if (content == null && fileUrl == null) {
            throw new BusinessException(400, "message content or attachment cannot be empty");
        }
        if (content != null && content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(400, "message content is too long");
        }

        message.setContent_wsh(content);
        message.setFile_url_wsh(fileUrl);
        message.setType_wsh(normalizeType(message.getType_wsh(), fileUrl));
        message.setRead_wsh(0);
        chatMessageMapper.insert(message);
        return toDTO(message);
    }

    @Transactional
    @Override
    public void markAsRead(Long messageId, Long userId) {
        if (messageId == null || userId == null) {
            return;
        }
        ChatMessage update = new ChatMessage();
        update.setRead_wsh(1);
        chatMessageMapper.update(
                update,
                new LambdaUpdateWrapper<ChatMessage>()
                        .eq(ChatMessage::getId_wsh, messageId)
                        .eq(ChatMessage::getTo_user_id_wsh, userId)
                        .eq(ChatMessage::getRead_wsh, 0));
    }

    @Transactional
    @Override
    public void markConversationAsRead(Long currentUserId, Long otherUserId, Long orderId) {
        if (currentUserId == null || otherUserId == null) {
            return;
        }
        LambdaUpdateWrapper<ChatMessage> wrapper = new LambdaUpdateWrapper<ChatMessage>()
                .eq(ChatMessage::getFrom_user_id_wsh, otherUserId)
                .eq(ChatMessage::getTo_user_id_wsh, currentUserId)
                .eq(ChatMessage::getRead_wsh, 0);
        if (orderId != null) {
            wrapper.eq(ChatMessage::getOrder_id_wsh, orderId);
        } else {
            wrapper.isNull(ChatMessage::getOrder_id_wsh);
        }
        ChatMessage update = new ChatMessage();
        update.setRead_wsh(1);
        chatMessageMapper.update(update, wrapper);
    }

    private ChatMessageDTO toDTO(ChatMessage msg) {
        if (msg == null) {
            return null;
        }
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId_wsh(msg.getId_wsh());
        dto.setFrom_user_id_wsh(msg.getFrom_user_id_wsh());
        dto.setTo_user_id_wsh(msg.getTo_user_id_wsh());
        dto.setOrder_id_wsh(msg.getOrder_id_wsh());
        dto.setContent_wsh(msg.getContent_wsh());
        dto.setType_wsh(msg.getType_wsh());
        dto.setFile_url_wsh(msg.getFile_url_wsh());
        dto.setRead_wsh(msg.getRead_wsh());
        dto.setCreated_at_wsh(msg.getCreated_at_wsh());
        return dto;
    }

    private List<ChatMessageDTO> toDTOList(List<ChatMessage> list) {
        if (list == null) {
            return List.of();
        }
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String normalizeType(String type, String fileUrl) {
        String normalized = trimToNull(type);
        if (normalized == null) {
            return fileUrl == null ? "text" : "image";
        }
        normalized = normalized.toLowerCase();
        if (!ALLOWED_TYPES.contains(normalized)) {
            throw new BusinessException(400, "unsupported message type");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private long nullSafeId(ChatMessage message) {
        return message.getId_wsh() == null ? 0L : message.getId_wsh();
    }
}
