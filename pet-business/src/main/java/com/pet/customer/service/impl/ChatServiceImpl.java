package com.pet.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pet.common.BusinessException;
import com.pet.customer.dto.ChatAgentAssignDTO;
import com.pet.customer.dto.ChatMessageDTO;
import com.pet.customer.entity.ChatMessage;
import com.pet.customer.mapper.ChatMessageMapper;
import com.pet.customer.service.ChatService;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import com.pet.system.mapper.UserRoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
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
    /** 客服角色编码 */
    private static final String CUSTOMER_SERVICE_ROLE_CODE = "CUSTOMER_SERVICE";

    private final ChatMessageMapper chatMessageMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserMapper userMapper;

    /**
     * 【构造聊天服务】
     *
     * 业务作用：注入聊天消息、用户角色、用户三个 Mapper。
     *
     * 调用场景：Spring 容器启动创建 Bean 时调用。
     *
     * 调用链：Spring 容器 → 本构造器 → 赋值 chatMessageMapper / userRoleMapper / userMapper 字段。
     *
     * 数据处理：无。
     *
     * 业务规则：依赖均为 final 字段。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：无。
     */
    public ChatServiceImpl(ChatMessageMapper chatMessageMapper,
                           UserRoleMapper userRoleMapper,
                           UserMapper userMapper) {
        this.chatMessageMapper = chatMessageMapper;
        this.userRoleMapper = userRoleMapper;
        this.userMapper = userMapper;
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
        // 双方互为收发方，任一方向都算同一会话
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                .and(w -> w.eq(ChatMessage::getFrom_user_id_wsh, userId1)
                        .eq(ChatMessage::getTo_user_id_wsh, userId2)
                        .or()
                        .eq(ChatMessage::getFrom_user_id_wsh, userId2)
                        .eq(ChatMessage::getTo_user_id_wsh, userId1));
        // 按订单维度隔离会话，无订单时取通用会话
        if (orderId != null) {
            wrapper.eq(ChatMessage::getOrder_id_wsh, orderId);
        } else {
            wrapper.isNull(ChatMessage::getOrder_id_wsh);
        }
        // 游标分页，只取比游标更早的消息
        if (beforeId != null) {
            wrapper.lt(ChatMessage::getId_wsh, beforeId);
        }

        // 先倒序取最新一页，再翻回正序返回
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
        // 文字内容与附件不能同时为空
        if (content == null && fileUrl == null) {
            throw new BusinessException(400, "message content or attachment cannot be empty");
        }
        // 内容超长直接拒绝，避免撑爆存储
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
     * 【业务名称】随机分配人工客服（实现）
     * 业务作用：从拥有客服角色的有效用户中随机挑选一名（排除当前用户），返回其用户ID、名称与头像。
     * 调用场景：智能客服页面点击“转人工客服”按钮。
     * 调用链：assignCustomerServiceAgent() → userRoleMapper.selectActiveUserIdsByRoleCode() → userMapper.selectById()。
     * 数据处理：角色编码查询客服ID列表 → 排除当前用户 → ThreadLocalRandom随机取一名 → 组装昵称/头像。
     * 业务规则：候选为空时抛 BusinessException(400) 提示暂无在线客服。
     * 状态影响：无。
     * 异常情况：无可用客服时抛 BusinessException(400)。
     * 注意事项：无。
     */
    @Override
    public ChatAgentAssignDTO assignCustomerServiceAgent(Long userId) {
        List<Long> agentIds = userRoleMapper.selectActiveUserIdsByRoleCode(CUSTOMER_SERVICE_ROLE_CODE);
        // 排除当前用户，避免把会话转给自己
        List<Long> candidates = agentIds == null ? List.of() : agentIds.stream()
                .filter(id -> id != null && (userId == null || !id.equals(userId)))
                .collect(Collectors.toList());
        if (candidates.isEmpty()) {
            throw new BusinessException(400, "暂无可用的在线客服，请稍后再试");
        }
        // 在候选客服中随机挑一个，均摊会话压力
        Long agentId = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        User agent = agentId == null ? null : userMapper.selectById(agentId);
        if (agent == null) {
            throw new BusinessException(400, "暂无可用的在线客服，请稍后再试");
        }
        ChatAgentAssignDTO dto = new ChatAgentAssignDTO();
        dto.setUser_id_wsh(agentId);
        dto.setName_wsh(agent.getNickname_wsh() != null ? agent.getNickname_wsh() : agent.getUsername_wsh());
        dto.setAvatar_wsh(agent.getAvatar_wsh());
        return dto;
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

    /**
     * 【消息列表转DTO列表】
     *
     * 业务作用：批量把聊天消息实体转换为 DTO。
     *
     * 调用场景：getUnreadMessages 查询结果转换。
     *
     * 调用链：getUnreadMessages() → toDTOList() → toDTO()。
     *
     * 数据处理：入参为 null 时返回空列表，否则流式逐条转换。
     *
     * 业务规则：保证不返回 null。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：与 toDTO 不同，这里对 null 列表做了兜底。
     */
    private List<ChatMessageDTO> toDTOList(List<ChatMessage> list) {
        if (list == null) {
            return List.of();
        }
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 【分页大小归一】
     *
     * 业务作用：把请求的分页条数收敛到合法区间。
     *
     * 调用场景：getConversation 拼装 LIMIT 前。
     *
     * 调用链：getConversation() → normalizeSize() → 与 DEFAULT_PAGE_SIZE / MAX_PAGE_SIZE 比较。
     *
     * 数据处理：null 或非正数取 DEFAULT_PAGE_SIZE，超过上限取 MAX_PAGE_SIZE。
     *
     * 业务规则：分页条数上限由 MAX_PAGE_SIZE 决定。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：返回值为 int，直接拼进 SQL 的 LIMIT 子句，因此必须保证是受控整数。
     */
    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    /**
     * 【消息类型归一】
     *
     * 业务作用：把消息类型规范化为受支持的小写取值。
     *
     * 调用场景：sendMessage 落库前。
     *
     * 调用链：sendMessage() → normalizeType() → ALLOWED_TYPES 白名单校验。
     *
     * 数据处理：类型为空时，有附件按 image、无附件按 text；非空时 trim 后转小写。
     *
     * 业务规则：仅允许 text / image / video / file。
     *
     * 状态影响：无。
     *
     * 异常情况：类型不在白名单内抛 BusinessException(400)「unsupported message type」。
     *
     * 注意事项：类型为空但有附件时默认按 image 处理，不会按附件扩展名推断。
     */
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

    /**
     * 【去空白转null】
     *
     * 业务作用：把字符串 trim 后，空串统一转成 null。
     *
     * 调用场景：sendMessage 清洗消息内容与附件地址。
     *
     * 调用链：sendMessage() → trimToNull()。
     *
     * 数据处理：null 返回 null，trim 后为空串也返回 null。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：该方法只判空串不判 isBlank，全空白字符会被 trim 成空串后转 null。
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 【消息ID空值兜底】
     *
     * 业务作用：取出消息 ID，null 时返回 0 用于排序比较。
     *
     * 调用场景：getConversation 对查询结果按 ID 正序排序。
     *
     * 调用链：getConversation() → nullSafeId()。
     *
     * 数据处理：id 为 null 返回 0L，否则返回原值。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：数据库自增主键不会为 null，兜底仅防御性处理。
     */
    private long nullSafeId(ChatMessage message) {
        return message.getId_wsh() == null ? 0L : message.getId_wsh();
    }
}
