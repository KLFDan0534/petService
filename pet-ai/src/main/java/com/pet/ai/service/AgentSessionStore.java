package com.pet.ai.service;

import com.pet.ai.dto.AgentSessionState;
import com.pet.ai.entity.AgentSession;

import java.util.List;

/**
 * 智能下单会话状态的读写门面。
 * <p>
 * 把「数据库行」与「结构化状态对象」之间的 JSON 转换、以及下单用的状态 CAS 收在一处，
 * 让编排层（AgentServiceImpl）只关心业务语义。
 * <p>
 * 注意：状态更新刻意**不提供**「整行 updateById」——那会把 CAS 之后的 CONFIRMED 覆盖回
 * AWAIT_CONFIRM，等于把防重复下单的保证抹掉。所有写入都带状态条件，并返回影响行数。
 */
public interface AgentSessionStore {

    /** 按会话ID + 用户ID取会话；不存在或不属于该用户返回 null（不区分，避免越权探测）。 */
    AgentSession find(Long userId, String sessionId);

    /** 用户的历史会话，按最后活动时间倒序。 */
    List<AgentSession> listByUser(Long userId);

    /** 首条消息处理成功后插入会话行。 */
    void insert(AgentSession session);

    /**
     * 续轮更新状态：只允许在**未下单**的会话上写入。
     *
     * @return 1 = 更新成功；0 = 会话不存在 / 不属于该用户 / 已被并发下单
     */
    int updateState(String sessionId, Long userId, String stateJson, String status);

    /**
     * 下单成功后写回状态：只允许在**已下单**的会话上写入，避免与续轮更新互相覆盖。
     *
     * @return 1 = 更新成功；0 = 状态已不是 CONFIRMED（异常情况）
     */
    int updateConfirmedState(String sessionId, Long userId, String stateJson);

    /**
     * 下单前的原子占用：仅 AWAIT_CONFIRM 可迁移到 CONFIRMED。
     *
     * @return 1 = 占用成功；0 = 已下单 / 状态不对 / 不存在
     */
    int markConfirmed(String sessionId, Long userId);

    /** 删除会话状态行（消息由调用方另行清理）。 */
    void delete(Long userId, String sessionId);

    /** 反序列化状态；空或损坏时返回带默认值的空状态，不抛错。 */
    AgentSessionState readState(AgentSession session);

    /** 序列化状态。 */
    String writeState(AgentSessionState state);
}
