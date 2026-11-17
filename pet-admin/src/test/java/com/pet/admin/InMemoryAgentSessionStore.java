package com.pet.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pet.ai.dto.AgentSessionState;
import com.pet.ai.entity.AgentSession;
import com.pet.ai.service.AgentSessionStore;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * AgentSessionStore 的内存实现，供单元测试使用。
 * <p>
 * 刻意模仿数据库语义：
 * - find 返回副本（避免测试里「改内存对象就等于改了库」的假象）；
 * - markConfirmed 只允许 AWAIT_CONFIRM → CONFIRMED，并返回影响行数（对应 SQL 的 CAS）；
 * - 状态通过真实 JSON 序列化往返，能暴露 LocalDate 等类型无法序列化的问题。
 */
class InMemoryAgentSessionStore implements AgentSessionStore {

    private final Map<String, AgentSession> rows = new LinkedHashMap<>();
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    final AtomicInteger insertCount = new AtomicInteger();
    final AtomicInteger confirmCasCount = new AtomicInteger();

    /** 模拟「本轮处理期间，另一个请求先完成了下单」，用于验证状态写入不会覆盖 CONFIRMED。 */
    boolean simulateConcurrentConfirm;

    @Override
    public AgentSession find(Long userId, String sessionId) {
        if (userId == null || !StringUtils.hasText(sessionId)) {
            return null;
        }
        AgentSession row = rows.get(sessionId);
        if (row == null || !userId.equals(row.getUser_id_wsh())) {
            return null;
        }
        return copy(row);
    }

    @Override
    public List<AgentSession> listByUser(Long userId) {
        return rows.values().stream()
                .filter(r -> userId.equals(r.getUser_id_wsh()))
                .sorted(Comparator.comparing(AgentSession::getUpdated_at_wsh,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::copy)
                .collect(Collectors.toList());
    }

    @Override
    public void insert(AgentSession session) {
        // 模拟数据库主键：session_id_wsh 是唯一键，重复插入必须报错，
        // 而不是悄悄覆盖（否则「两个用户撞了同一个会话ID」在单测里永远发现不了）
        if (rows.containsKey(session.getSession_id_wsh())) {
            throw new DuplicateKeyException("Duplicate entry for session_id_wsh: " + session.getSession_id_wsh());
        }
        LocalDateTime now = LocalDateTime.now();
        session.setCreated_at_wsh(now);
        session.setUpdated_at_wsh(now);
        rows.put(session.getSession_id_wsh(), copy(session));
        insertCount.incrementAndGet();
    }

    @Override
    public int updateState(String sessionId, Long userId, String stateJson, String status) {
        AgentSession row = rows.get(sessionId);
        if (row == null || !userId.equals(row.getUser_id_wsh())) {
            return 0;
        }
        if (simulateConcurrentConfirm) {
            // 模拟「本轮处理期间，另一个请求先完成了下单」
            row.setStatus_wsh("CONFIRMED");
            return 0;
        }
        // 模拟 SQL 的 status <> 'CONFIRMED' 条件
        if ("CONFIRMED".equals(row.getStatus_wsh())) {
            return 0;
        }
        row.setState_json_wsh(stateJson);
        row.setStatus_wsh(status);
        row.setUpdated_at_wsh(LocalDateTime.now());
        return 1;
    }

    @Override
    public int updateConfirmedState(String sessionId, Long userId, String stateJson) {
        AgentSession row = rows.get(sessionId);
        if (row == null || !userId.equals(row.getUser_id_wsh()) || !"CONFIRMED".equals(row.getStatus_wsh())) {
            return 0;
        }
        row.setState_json_wsh(stateJson);
        row.setUpdated_at_wsh(LocalDateTime.now());
        return 1;
    }

    @Override
    public int markConfirmed(String sessionId, Long userId) {
        confirmCasCount.incrementAndGet();
        AgentSession row = rows.get(sessionId);
        if (row == null || !userId.equals(row.getUser_id_wsh())) {
            return 0;
        }
        if (!"AWAIT_CONFIRM".equals(row.getStatus_wsh())) {
            return 0;
        }
        row.setStatus_wsh("CONFIRMED");
        row.setUpdated_at_wsh(LocalDateTime.now());
        return 1;
    }

    @Override
    public void delete(Long userId, String sessionId) {
        AgentSession row = rows.get(sessionId);
        if (row != null && userId.equals(row.getUser_id_wsh())) {
            rows.remove(sessionId);
        }
    }

    @Override
    public AgentSessionState readState(AgentSession session) {
        if (session == null || !StringUtils.hasText(session.getState_json_wsh())) {
            return new AgentSessionState();
        }
        try {
            AgentSessionState state = json.readValue(session.getState_json_wsh(), AgentSessionState.class);
            return state == null ? new AgentSessionState() : state;
        } catch (Exception e) {
            throw new IllegalStateException("状态 JSON 往返失败", e);
        }
    }

    @Override
    public String writeState(AgentSessionState state) {
        try {
            return json.writeValueAsString(state);
        } catch (Exception e) {
            throw new IllegalStateException("状态 JSON 序列化失败", e);
        }
    }

    /** 模拟「另一台实例已把会话改成 CONFIRMED」，用于并发用例。 */
    void forceStatus(String sessionId, String status) {
        AgentSession row = rows.get(sessionId);
        if (row != null) {
            row.setStatus_wsh(status);
        }
    }

    List<String> sessionIds() {
        return new ArrayList<>(rows.keySet());
    }

    private AgentSession copy(AgentSession src) {
        AgentSession dst = new AgentSession();
        dst.setSession_id_wsh(src.getSession_id_wsh());
        dst.setUser_id_wsh(src.getUser_id_wsh());
        dst.setTitle_wsh(src.getTitle_wsh());
        dst.setState_json_wsh(src.getState_json_wsh());
        dst.setStatus_wsh(src.getStatus_wsh());
        dst.setCreated_at_wsh(src.getCreated_at_wsh());
        dst.setUpdated_at_wsh(src.getUpdated_at_wsh());
        return dst;
    }
}
