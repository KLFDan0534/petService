package com.pet.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pet.ai.entity.AiChatHistory;
import com.pet.ai.mapper.AiChatHistoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * AI 聊天记录持久化服务。
 * <p>支持：保存消息、查询会话列表、查询会话消息、清空会话。</p>
 */
@Service
@Slf4j
public class AiChatHistoryService {

    private final AiChatHistoryMapper mapper;

    /**
     * 业务规则：依赖为 final 字段。
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
     * 获取某个会话的所有消息（按时间正序，同一时刻按自增主键兜底）
     * <p>
     * 用户消息与助手回复在同一事务内连续插入，{@code created_at_wsh} 精度只到秒，
     * 只按时间排序时两者的先后顺序不确定（气泡会错位）。加自增主键做第二排序键后，
     * 「先插的在前」始终成立。
     */
    public List<AiChatHistory> getSessionMessages(Long userId, String sessionId) {
        LambdaQueryWrapper<AiChatHistory> wrapper = Wrappers.<AiChatHistory>lambdaQuery()
                .eq(AiChatHistory::getUser_id_wsh, userId)
                .eq(AiChatHistory::getSession_id_wsh, sessionId)
                .orderByAsc(AiChatHistory::getCreated_at_wsh)
                .orderByAsc(AiChatHistory::getId_wsh);
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
