package com.pet.customer.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.customer.entity.ChatMessage;
import com.pet.customer.mapper.ChatMessageMapper;
import com.pet.customer.service.ChatService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatMessageMapper chatMessageMapper;

    public ChatServiceImpl(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    public List<ChatMessage> getConversation(Long userId1, Long userId2, Long orderId) {
        log.info("调用 getConversation()");
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                .and(w -> w.eq(ChatMessage::getFrom_user_id_wsh, userId1)
                        .eq(ChatMessage::getTo_user_id_wsh, userId2)
                        .or()
                        .eq(ChatMessage::getFrom_user_id_wsh, userId2)
                        .eq(ChatMessage::getTo_user_id_wsh, userId1));
        if (orderId != null) {
            wrapper.eq(ChatMessage::getOrder_id_wsh, orderId);
        }
        return chatMessageMapper.selectList(wrapper.orderByAsc(ChatMessage::getCreated_at_wsh));
    }

    public List<ChatMessage> getUnreadMessages(Long userId) {
        log.info("调用 getUnreadMessages()");
        return chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getTo_user_id_wsh, userId)
                        .eq(ChatMessage::getRead_wsh, 0));
    }

    @Transactional
    public ChatMessage sendMessage(ChatMessage message) {
        log.info("调用 sendMessage()");
        if (message.getFrom_user_id_wsh() == null) {
            throw new BusinessException(400, "发送人不能为空");
        }
        if (message.getTo_user_id_wsh() == null) {
            throw new BusinessException(400, "接收人不能为空");
        }
        message.setRead_wsh(0);
        if (message.getType_wsh() == null || message.getType_wsh().isBlank()) {
            message.setType_wsh("text");
        }
        chatMessageMapper.insert(message);
        return message;
    }

    @Transactional
    public void markAsRead(Long messageId, Long userId) {
        log.info("调用 markAsRead()");
        ChatMessage msg = chatMessageMapper.selectById(messageId);
        if (msg != null && msg.getTo_user_id_wsh().equals(userId)) {
            msg.setRead_wsh(1);
            chatMessageMapper.updateById(msg);
        }
    }

    @Transactional
    public void markConversationAsRead(Long fromUserId, Long toUserId, Long orderId) {
        log.info("调用 markConversationAsRead()");
        List<ChatMessage> unread = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getFrom_user_id_wsh, fromUserId)
                        .eq(ChatMessage::getTo_user_id_wsh, toUserId)
                        .eq(ChatMessage::getRead_wsh, 0));
        for (ChatMessage msg : unread) {
            msg.setRead_wsh(1);
            chatMessageMapper.updateById(msg);
        }
    }
}
