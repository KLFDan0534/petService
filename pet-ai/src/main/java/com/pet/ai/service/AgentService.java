package com.pet.ai.service;

import com.pet.ai.dto.AgentExecuteResult;
import com.pet.ai.dto.AgentMessageRequestDTO;
import com.pet.ai.dto.AgentSessionDetail;
import com.pet.ai.dto.AgentSessionSummaryVO;
import com.pet.ai.dto.AgentTurnResult;

import java.util.List;

/**
 * 智能下单服务：把「一句话 → 一张方案卡片 → 确认」升级为「一个能记住的会话」。
 * <p>
 * 三层职责：
 * 1. 对话层：每轮 user / assistant 消息落 {@code ai_chat_history_wsh}（复用现有聊天记录服务）；
 * 2. 状态层：结构化状态（意图 / 方案 / 状态 / 日志）落 {@code agent_session_wsh}，多轮 = 状态被 patch；
 * 3. 业务层：匹配、校验、报价、落单仍在本服务的私有方法里，LLM 只做语义增量。
 * <p>
 * 红线：金额只来自后端报价（orderService.previewOrder）、只支持按夜寄养、
 * 副作用（建单 / 支付）只能由 confirm 这个显式事件触发。
 */
public interface AgentService {

    /**
     * 处理一轮对话。
     * <p>
     * {@code session_id_wsh} 为空 = 首轮：先在内存里跑完整轮，成功后才建会话入库，
     * 因此「点开页面没发消息」不会产生任何记录。
     * <p>
     * 返回 status：{@code await_confirm} / {@code collecting} / {@code failed}。
     */
    AgentTurnResult message(Long userId, AgentMessageRequestDTO request);

    /** 历史会话列表（按最后活动时间倒序，只含有消息的会话）。 */
    List<AgentSessionSummaryVO> listSessions(Long userId);

    /** 会话详情：消息 + 当前方案卡片，用于刷新页面或切换历史会话。 */
    AgentSessionDetail getSession(Long userId, String sessionId);

    /** 删除会话：状态行 + 消息一起清。 */
    void deleteSession(Long userId, String sessionId);

    /**
     * 确认下单：凭会话ID创建订单与支付记录，autoPay=true 时校验支付密码并直接支付。
     * <p>
     * 幂等由 status 的 CAS 保证：只有 AWAIT_CONFIRM 能迁移到 CONFIRMED，
     * 影响行数为 1 才允许建单；事务失败整体回滚，状态自动退回，天然可重试。
     */
    AgentExecuteResult confirm(Long userId, String sessionId, Boolean autoPay, String paymentPassword);
}
