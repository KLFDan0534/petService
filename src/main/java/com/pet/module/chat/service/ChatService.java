package com.pet.module.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.module.chat.entity.ChatMessage;
import com.pet.module.chat.mapper.ChatMessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatService {

    private final ChatMessageMapper chatMessageMapper;

    public ChatService(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    public List<ChatMessage> getConversation(Long userId1, Long userId2, Long orderId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                .and(w -> w.eq(ChatMessage::getFromUserId, userId1)
                        .eq(ChatMessage::getToUserId, userId2)
                        .or()
                        .eq(ChatMessage::getFromUserId, userId2)
                        .eq(ChatMessage::getToUserId, userId1));
        if (orderId != null) {
            wrapper.eq(ChatMessage::getOrderId, orderId);
        }
        return chatMessageMapper.selectList(wrapper.orderByAsc(ChatMessage::getCreatedAt));
    }

    public List<ChatMessage> getUnreadMessages(Long userId) {
        return chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getToUserId, userId)
                        .eq(ChatMessage::getRead, 0));
    }

    @Transactional
    public ChatMessage sendMessage(ChatMessage message) {
        message.setRead(0);
        chatMessageMapper.insert(message);
        return message;
    }

    @Transactional
    public void markAsRead(Long messageId, Long userId) {
        ChatMessage msg = chatMessageMapper.selectById(messageId);
        if (msg != null && msg.getToUserId().equals(userId)) {
            msg.setRead(1);
            chatMessageMapper.updateById(msg);
        }
    }

    @Transactional
    public void markConversationAsRead(Long fromUserId, Long toUserId, Long orderId) {
        List<ChatMessage> unread = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getFromUserId, fromUserId)
                        .eq(ChatMessage::getToUserId, toUserId)
                        .eq(ChatMessage::getRead, 0));
        for (ChatMessage msg : unread) {
            msg.setRead(1);
            chatMessageMapper.updateById(msg);
        }
    }
}
