package com.pet.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pet.ai.dto.AgentSessionState;
import com.pet.ai.entity.AgentSession;
import com.pet.ai.mapper.AgentSessionMapper;
import com.pet.ai.service.AgentSessionStore;
import com.pet.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话状态存储实现：MyBatis-Plus + Jackson。
 * <p>
 * 只做「存 / 取 / CAS」，不含任何业务判断；业务编排全部留在 AgentServiceImpl。
 */
@Service
@Slf4j
public class AgentSessionStoreImpl implements AgentSessionStore {

    private final AgentSessionMapper mapper;
    /** 独立副本：注册 JavaTimeModule，保证 LocalDate / LocalDateTime 能进 JSON 列 */
    private final ObjectMapper json;

    public AgentSessionStoreImpl(AgentSessionMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.json = objectMapper.copy().registerModule(new JavaTimeModule());
    }

    @Override
    public AgentSession find(Long userId, String sessionId) {
        if (userId == null || !StringUtils.hasText(sessionId)) {
            return null;
        }
        return mapper.selectOne(new LambdaQueryWrapper<AgentSession>()
                .eq(AgentSession::getSession_id_wsh, sessionId)
                .eq(AgentSession::getUser_id_wsh, userId));
    }

    @Override
    public List<AgentSession> listByUser(Long userId) {
        return mapper.selectList(new LambdaQueryWrapper<AgentSession>()
                .eq(AgentSession::getUser_id_wsh, userId)
                .orderByDesc(AgentSession::getUpdated_at_wsh));
    }

    @Override
    public void insert(AgentSession session) {
        LocalDateTime now = LocalDateTime.now();
        if (session.getCreated_at_wsh() == null) {
            session.setCreated_at_wsh(now);
        }
        session.setUpdated_at_wsh(now);
        mapper.insert(session);
    }

    @Override
    public int updateState(String sessionId, Long userId, String stateJson, String status) {
        return mapper.updateStateIfNotConfirmed(sessionId, userId, stateJson, status);
    }

    @Override
    public int updateConfirmedState(String sessionId, Long userId, String stateJson) {
        return mapper.updateStateIfConfirmed(sessionId, userId, stateJson);
    }

    @Override
    public int markConfirmed(String sessionId, Long userId) {
        return mapper.markConfirmed(sessionId, userId);
    }

    @Override
    public void delete(Long userId, String sessionId) {
        mapper.delete(new LambdaQueryWrapper<AgentSession>()
                .eq(AgentSession::getSession_id_wsh, sessionId)
                .eq(AgentSession::getUser_id_wsh, userId));
    }

    @Override
    public AgentSessionState readState(AgentSession session) {
        if (session == null || !StringUtils.hasText(session.getState_json_wsh())) {
            AgentSessionState empty = new AgentSessionState();
            if (session != null && StringUtils.hasText(session.getStatus_wsh())) {
                empty.setStatus(session.getStatus_wsh());
            }
            return empty;
        }
        try {
            AgentSessionState state = json.readValue(session.getState_json_wsh(), AgentSessionState.class);
            if (state == null) {
                return new AgentSessionState();
            }
            if (!StringUtils.hasText(state.getStatus())) {
                state.setStatus(session.getStatus_wsh());
            }
            return state;
        } catch (Exception e) {
            // 状态列损坏不应让整个会话不可用：退回空状态，由下一轮重新收集
            log.warn("Agent 会话状态反序列化失败 session={}: {}", session.getSession_id_wsh(), e.getMessage());
            AgentSessionState empty = new AgentSessionState();
            empty.setStatus(session.getStatus_wsh());
            return empty;
        }
    }

    @Override
    public String writeState(AgentSessionState state) {
        try {
            return json.writeValueAsString(state);
        } catch (Exception e) {
            throw new BusinessException(500, "会话状态保存失败，请稍后重试。");
        }
    }
}
