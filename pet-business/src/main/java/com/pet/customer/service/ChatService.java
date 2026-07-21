package com.pet.customer.service;

import com.pet.customer.dto.ChatMessageDTO;
import com.pet.customer.entity.ChatMessage;

import java.util.List;

public interface ChatService {
    int DEFAULT_PAGE_SIZE = 100;
    int MAX_PAGE_SIZE = 200;

    default List<ChatMessageDTO> getConversation(Long userId1, Long userId2, Long orderId) {
        return getConversation(userId1, userId2, orderId, null, DEFAULT_PAGE_SIZE);
    }

    List<ChatMessageDTO> getConversation(Long userId1, Long userId2, Long orderId, Long beforeId, Integer size);

    List<ChatMessageDTO> getUnreadMessages(Long userId);

    long countUnreadMessages(Long userId);

    ChatMessageDTO sendMessage(ChatMessage message);

    void markAsRead(Long messageId, Long userId);

    void markConversationAsRead(Long currentUserId, Long otherUserId, Long orderId);
}
