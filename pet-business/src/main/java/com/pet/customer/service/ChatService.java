package com.pet.customer.service;

import com.pet.customer.dto.ChatMessageDTO;
import com.pet.customer.entity.ChatMessage;

import java.util.List;

/**
 * 【业务模块】即时通讯管理
 * 业务作用：提供用户之间、用户与商家/照看者之间的即时通讯能力。
 * 支持文本、图片、视频、文件等多种消息类型，以及消息的已读未读管理和历史会话拉取。
 */
public interface ChatService {
    /** 默认每页查询条数 */
    int DEFAULT_PAGE_SIZE = 100;
    /** 每页最大查询条数 */
    int MAX_PAGE_SIZE = 200;

    /**
     * 【业务名称】获取历史会话记录（默认分页）
     * 业务作用：获取两个用户之间的历史会话记录，默认取最近100条。
     * 调用场景：用户打开聊天窗口。
     * 调用链：getConversation() → getConversation()（带分页参数）。
     * 数据处理：按两个用户+订单查询，按时间正序。
     * 业务规则：orderId 为空时返回非订单维度的通用聊天。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：默认取最近100条。
     *
     * @param userId1 会话参与方1的用户ID
     * @param userId2 会话参与方2的用户ID
     * @param orderId 关联订单ID，为空则只返回非订单维度的通用聊天
     * @return 按时间正序排列的聊天消息列表
     */
    default List<ChatMessageDTO> getConversation(Long userId1, Long userId2, Long orderId) {
        return getConversation(userId1, userId2, orderId, null, DEFAULT_PAGE_SIZE);
    }

    /**
     * 【业务名称】获取历史会话记录（游标分页）
     * 业务作用：获取两个用户之间的历史会话记录，支持基于游标的分页拉取。
     * 调用场景：聊天窗口上拉加载更多。
     * 调用链：getConversation() → ChatMessageMapper.selectByUsers()。
     * 数据处理：按两个用户+订单匹配，按消息ID游标分页，按时间正序。
     * 业务规则：beforeId 为空表示从最新开始。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：限制最多200条。
     *
     * @param userId1 会话参与方1的用户ID
     * @param userId2 会话参与方2的用户ID
     * @param orderId 关联订单ID，为空则只返回非订单维度的通用聊天
     * @param beforeId 游标ID，返回比此ID更早的消息，为空则从最新开始
     * @param size 返回条数上限
     * @return 按时间正序排列的聊天消息列表
     */
    List<ChatMessageDTO> getConversation(Long userId1, Long userId2, Long orderId, Long beforeId, Integer size);

    /**
     * 【业务名称】查询未读消息
     * 业务作用：查询指定用户所有未读消息。
     * 调用场景：用户查看未读消息列表。
     * 调用链：getUnreadMessages() → ChatMessageMapper.selectUnreadByUser()。
     * 数据处理：按接收方查询 is_read = false 的消息，按时间倒序。
     * 业务规则：最多返回200条。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：不改变已读状态。
     *
     * @param userId 用户ID
     * @return 未读消息列表（按时间倒序，最多200条）
     */
    List<ChatMessageDTO> getUnreadMessages(Long userId);

    /**
     * 【业务名称】统计未读消息总数
     * 业务作用：统计指定用户的未读消息总数。
     * 调用场景：用户首页显示未读消息角标。
     * 调用链：countUnreadMessages() → ChatMessageMapper.countUnreadByUser()。
     * 数据处理：统计接收方未读消息数。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：性能敏感，频繁调用。
     *
     * @param userId 用户ID
     * @return 未读消息数量
     */
    long countUnreadMessages(Long userId);

    /**
     * 【业务名称】发送消息
     * 业务作用：发送一条聊天消息，包含文本、图片、视频、文件等多种类型。
     * 调用场景：用户发送消息。
     * 调用链：sendMessage() → ChatMessageMapper.insert()。
     * 数据处理：插入消息记录。
     * 业务规则：消息已读状态默认为 false。
     * 状态影响：新增消息记录；接收方角度新增一条未读消息。
     * 异常情况：无。
     * 注意事项：发送方和接收方不可相同。
     *
     * @param message 待发送的消息实体，包含发送方、接收方、内容、类型、文件等信息
     * @return 发送成功后携带完整信息的消息DTO
     */
    ChatMessageDTO sendMessage(ChatMessage message);

    /**
     * 【业务名称】标记单条消息已读
     * 业务作用：将单条消息标记为已读。
     * 调用场景：用户已读取某条消息。
     * 调用链：markAsRead() → ChatMessageMapper.updateById()。
     * 数据处理：更新 is_read = true。
     * 业务规则：仅接收方可标记已读。
     * 状态影响：消息标记为已读。
     * 异常情况：无。
     * 注意事项：需校验 userId 是否是该消息的接收方。
     *
     * @param messageId 消息ID
     * @param userId 当前用户ID（接收方），用于校验归属
     */
    void markAsRead(Long messageId, Long userId);

    /**
     * 【业务名称】标记会话已读
     * 业务作用：将两个用户之间某订单维度（或非订单维度）的全部未读消息标记为已读。
     * 调用场景：用户进入聊天窗口时。
     * 调用链：markConversationAsRead() → ChatMessageMapper.markConversationAsRead()。
     * 数据处理：批量更新未读消息为已读。
     * 业务规则：orderId 为空则标记非订单维度的会话。
     * 状态影响：该会话的所有未读消息标记为已读。
     * 异常情况：无。
     * 注意事项：无。
     *
     * @param currentUserId 当前用户ID（接收方）
     * @param otherUserId 对方用户ID（发送方）
     * @param orderId 关联订单ID，为空则标记非订单维度的会话
     */
    void markConversationAsRead(Long currentUserId, Long otherUserId, Long orderId);
}
