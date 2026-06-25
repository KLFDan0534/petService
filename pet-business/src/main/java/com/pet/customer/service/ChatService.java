package com.pet.customer.service;

import com.pet.customer.entity.ChatMessage;

import java.util.List;

public interface ChatService {
    /**
     * 获取聊天对话记录
     * @param userId1 用户ID1
     * @param userId2 用户ID2
     * @param orderId 订单ID
     * @return 聊天消息列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<ChatMessage> getConversation(Long userId1, Long userId2, Long orderId);
    /**
     * 获取用户的未读消息列表
     * @param userId 用户ID
     * @return 未读消息列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<ChatMessage> getUnreadMessages(Long userId);
    /**
     * 发送聊天消息
     * @param message 聊天消息实体
     * @return 发送后的消息
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    ChatMessage sendMessage(ChatMessage message);
    /**
     * 标记消息为已读
     * @param messageId 消息ID
     * @param userId 用户ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void markAsRead(Long messageId, Long userId);
    /**
     * 标记整个对话为已读
     * @param fromUserId 发送方用户ID
     * @param toUserId 接收方用户ID
     * @param orderId 订单ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void markConversationAsRead(Long fromUserId, Long toUserId, Long orderId);
}

