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

/**
 * 【业务模块】即时通讯管理（实现）
 * 业务作用：基于 MyBatis-Plus 提供即时通讯的持久化与查询能力。
 * 支持消息内容校验、类型规范化、已读标记以及基于游标的分页拉取。
 */
@Service
@Slf4j
public class ChatServiceImpl implements ChatService {
    /** 消息内容最大长度（字符数） */
    private static final int MAX_CONTENT_LENGTH = 2000;
    /** 允许的消息类型集合 */
    private static final Set<String> ALLOWED_TYPES = Set.of("text", "image", "video", "file");

    private final ChatMessageMapper chatMessageMapper;

    public ChatServiceImpl(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    /**
     * 【业务名称】获取历史会话记录（实现）
     * 业务作用：获取两个用户之间的历史会话记录，支持游标分页。
     * 调用场景：聊天窗口上拉加载更多。
     * 调用链：getConversation() → chatMessageMapper.selectList()。
     * 数据处理：双方互为收发方，按订单维度过滤，按ID游标分页倒序查询，结果按时间正序返回。
     * 业务规则：双方ID不可为空；orderId为空时查非订单维度的通用聊天；受 MAX_PAGE_SIZE 约束。
     * 状态影响：无。
     * 异常情况：双方用户ID为空抛 BusinessException(400)。
     * 注意事项：limit 受 MAX_PAGE_SIZE 约束。
     */
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

    /**
     * 【业务名称】查询未读消息（实现）
     * 业务作用：查询指定用户所有未读消息。
     * 调用场景：用户查看未读消息列表。
     * 调用链：getUnreadMessages() → chatMessageMapper.selectList()。
     * 数据处理：按接收方查询未读消息，按时间倒序，最多200条。
     * 业务规则：用户ID不可为空。
     * 状态影响：无。
     * 异常情况：用户ID为空抛 BusinessException(400)。
     * 注意事项：不改变已读状态。
     */
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

    /**
     * 【业务名称】统计未读消息总数（实现）
     * 业务作用：统计指定用户的未读消息总数。
     * 调用场景：用户首页显示未读消息角标。
     * 调用链：countUnreadMessages() → chatMessageMapper.selectCount()。
     * 数据处理：统计接收方未读消息数。
     * 业务规则：用户ID为空时返回0。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
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

    /**
     * 【业务名称】发送消息（实现）
     * 业务作用：发送一条聊天消息，校验合法性并进行规范化处理。
     * 调用场景：用户发送消息。
     * 调用链：sendMessage() → 参数校验 → 内容裁剪 → 类型规范化 → insert()。
     * 数据处理：校验发送方/接收方/内容 → 内容裁剪 → 类型规范化 → 插入未读消息。
     * 业务规则：发送方和接收方不可相同；内容或附件不可同时为空；内容长度 ≤ 2000字符；类型仅支持 text/image/video/file。
     * 状态影响：新增一条未读消息记录。
     * 异常情况：参数不合法时抛 BusinessException(400)。
     * 注意事项：@Transactional 保证事务一致性。
     */
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

    /**
     * 【业务名称】标记单条消息已读（实现）
     * 业务作用：将指定消息标记为已读。
     * 调用场景：用户已读取某条消息。
     * 调用链：markAsRead() → chatMessageMapper.update()。
     * 数据处理：更新 is_read = 1，校验接收方身份和当前未读状态。
     * 业务规则：仅接收方可标记已读，仅当前未读的消息会被更新。
     * 状态影响：消息 marked as read。
     * 异常情况：无。
     * 注意事项：入参为空时静默跳过。
     */
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

    /**
     * 【业务名称】标记会话已读（实现）
     * 业务作用：将两个用户之间指定订单维度的全部未读消息标记为已读。
     * 调用场景：用户进入聊天窗口时。
     * 调用链：markConversationAsRead() → chatMessageMapper.update()。
     * 数据处理：批量更新未读消息为已读。
     * 业务规则：orderId为空时标记非订单维度的会话。
     * 状态影响：该会话的所有未读消息标记为已读。
     * 异常情况：无。
     * 注意事项：@Transactional 保证事务一致性；入参为空时静默跳过。
     */
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

    /**
     * 【业务名称】消息实体转DTO
     * 业务作用：将消息实体转换为DTO。
     * 调用场景：对外暴露消息信息。
     * 调用链：toDTO()。
     * 数据处理：字段拷贝。
     * 业务规则：入参为null时返回null。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
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
