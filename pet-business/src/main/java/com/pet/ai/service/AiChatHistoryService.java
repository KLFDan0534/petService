package com.pet.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pet.ai.entity.AiChatHistory;
import com.pet.ai.mapper.AiChatHistoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI 聊天记录持久化服务。
 * <p>支持：保存消息、查询会话列表、查询会话消息、清空会话。</p>
 */
@Service
@Slf4j
public class AiChatHistoryService {

    private final AiChatHistoryMapper mapper;

    /**
     * 【构造AI聊天记录服务】
     *
     * 业务作用：注入 AI 聊天记录的 Mapper。
     *
     * 调用场景：Spring 容器启动创建 Bean 时调用。
     *
     * 调用链：Spring 容器 → 本构造器 → 赋值 mapper 字段。
     *
     * 数据处理：无。
     *
     * 业务规则：依赖为 final 字段。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：本类提供保存、会话列表、会话消息、清空会话四类能力，会话 ID 由 newSessionId 生成。
     */
    public AiChatHistoryService(AiChatHistoryMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 保存一条聊天记录
     */
    public AiChatHistory save(Long userId, String sessionId, String role, String content, List<String> sources, Boolean needHuman) {
        AiChatHistory record = new AiChatHistory();
        record.setUser_id_wsh(userId);
        record.setSession_id_wsh(sessionId);
        record.setRole_wsh(role);
        record.setContent_wsh(content);
        record.setSources_wsh(sources != null && !sources.isEmpty() ? String.join(",", sources) : null);
        record.setNeed_human_wsh(needHuman);
        mapper.insert(record);
        return record;
    }

    /**
     * 保存一条聊天消息（简化版，无 sources/needHuman）
     */
    public AiChatHistory saveSimple(Long userId, String sessionId, String role, String content) {
        return save(userId, sessionId, role, content, null, null);
    }

    /**
     * 查询用户的会话列表（按最新消息时间倒序，每个 session 只取一条用于摘要）
     */
    public List<AiChatHistory> listSessions(Long userId) {
        LambdaQueryWrapper<AiChatHistory> wrapper = Wrappers.<AiChatHistory>lambdaQuery()
                .eq(AiChatHistory::getUser_id_wsh, userId)
                .orderByDesc(AiChatHistory::getCreated_at_wsh);
        return mapper.selectList(wrapper);
    }

    /**
     * 获取某个会话的所有消息（按时间正序）
     */
    public List<AiChatHistory> getSessionMessages(Long userId, String sessionId) {
        LambdaQueryWrapper<AiChatHistory> wrapper = Wrappers.<AiChatHistory>lambdaQuery()
                .eq(AiChatHistory::getUser_id_wsh, userId)
                .eq(AiChatHistory::getSession_id_wsh, sessionId)
                .orderByAsc(AiChatHistory::getCreated_at_wsh);
        return mapper.selectList(wrapper);
    }

    /**
     * 清空某个会话（逻辑删除）
     */
    public void clearSession(Long userId, String sessionId) {
        LambdaQueryWrapper<AiChatHistory> wrapper = Wrappers.<AiChatHistory>lambdaQuery()
                .eq(AiChatHistory::getUser_id_wsh, userId)
                .eq(AiChatHistory::getSession_id_wsh, sessionId);
        mapper.delete(wrapper);
    }

    /**
     * 生成一个新的会话 ID
     */
    public static String newSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
