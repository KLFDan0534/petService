package com.pet.ai.service;

import com.pet.ai.dto.AgentPlanSnapshot;
import com.pet.common.BusinessException;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能下单方案 token 内存缓存。
 * <p>
 * plan 阶段生成方案快照并放入缓存，confirm 阶段凭 token 原子认领后落单。
 * 单实例课程项目，不使用数据库表与分布式锁；缓存通过懒淘汰控制过期，不起定时任务。
 * <p>
 * 状态机：NEW → CLAIMED →（confirm 成功 afterCommit 时 CONSUMED/移除，失败 release 回 NEW）。
 */
@Component
public class PlanTokenStore {

    /** 方案总有效期 */
    private static final long TTL_MS = 10 * 60_000L;
    /** 被认领（处理中）超过此时长视为崩溃残留，可重新认领 */
    private static final long CLAIM_TTL_MS = 3 * 60_000L;

    private static final int STATE_NEW = 0;
    private static final int STATE_CLAIMED = 1;

    private final ConcurrentHashMap<String, Entry> map = new ConcurrentHashMap<>();

    /** 生成 token 并保存方案快照。 */
    public String put(Long userId, AgentPlanSnapshot snapshot) {
        String token = UUID.randomUUID().toString().replace("-", "");
        map.put(token, new Entry(userId, snapshot, System.currentTimeMillis()));
        return token;
    }

    /**
     * 原子认领方案（NEW → CLAIMED）。
     *
     * @throws BusinessException 400 方案不存在/已过期/处理中；403 方案不属于当前用户
     */
    public AgentPlanSnapshot claim(String token, Long userId) {
        AgentPlanSnapshot[] out = new AgentPlanSnapshot[1];
        map.compute(token, (k, entry) -> {
            if (entry == null || entry.expired()) {
                return null;
            }
            if (!entry.userId.equals(userId)) {
                throw new BusinessException(403, "该方案不属于当前用户");
            }
            if (entry.state == STATE_CLAIMED) {
                if (!entry.claimExpired()) {
                    throw new BusinessException(400, "该方案正在处理或已提交，请勿重复点击");
                }
                // 认领超时（进程崩溃残留），允许重新认领
                entry.claimedAt = System.currentTimeMillis();
                out[0] = entry.snapshot;
                return entry;
            }
            entry.state = STATE_CLAIMED;
            entry.claimedAt = System.currentTimeMillis();
            out[0] = entry.snapshot;
            return entry;
        });
        if (out[0] == null) {
            throw new BusinessException(400, "方案已过期或无效，请重新生成");
        }
        return out[0];
    }

    /** confirm 失败时释放认领（CLAIMED → NEW），允许用同一方案重试。 */
    public void release(String token, Long userId) {
        map.computeIfPresent(token, (k, entry) -> {
            if (entry.userId.equals(userId) && entry.state == STATE_CLAIMED) {
                entry.state = STATE_NEW;
                entry.claimedAt = 0L;
            }
            return entry;
        });
    }

    /** confirm 成功后移除方案（应在事务提交后调用）。 */
    public void consumeAndRemove(String token, Long userId) {
        map.computeIfPresent(token, (k, entry) -> {
            if (entry.userId.equals(userId)) {
                return null; // 移除
            }
            return entry;
        });
    }

    /** 可访问的用户隔离说明：claim/release/consume 均以 token + userId 双重校验。 */
    private static class Entry {
        private final Long userId;
        private final AgentPlanSnapshot snapshot;
        private final long createdAt;
        private int state = STATE_NEW;
        private long claimedAt;

        /**
         * 【方案缓存条目】
         *
         * 业务作用：保存方案快照、归属用户、创建时间与认领状态。
         *
         * 调用场景：put 方法写入缓存时构造。
         *
         * 调用链：put → new Entry → ConcurrentHashMap。
         *
         * 数据处理：初始状态为 NEW，认领时间为 0。
         *
         * 业务规则：条目不可跨用户访问，由 token 与 userId 双重校验。
         *
         * 状态影响：无。
         *
         * 异常情况：无。
         *
         * 注意事项：state 与 claimedAt 为可变字段，由 compute 保证原子更新。
         */
        private Entry(Long userId, AgentPlanSnapshot snapshot, long createdAt) {
            this.userId = userId;
            this.snapshot = snapshot;
            this.createdAt = createdAt;
        }

        /**
         * 【判断方案是否过期】
         *
         * 业务作用：判断条目自创建以来的时长是否超过方案总有效期。
         *
         * 调用场景：claim 认领前判断，过期条目直接移除。
         *
         * 调用链：claim → entry.expired。
         *
         * 数据处理：当前时间减创建时间与 10 分钟比较。
         *
         * 业务规则：有效期固定 10 分钟。
         *
         * 状态影响：无。
         *
         * 异常情况：无。
         *
         * 注意事项：采用懒淘汰，不依赖定时任务清理。
         */
        private boolean expired() {
            return System.currentTimeMillis() - createdAt > TTL_MS;
        }

        /**
         * 【判断认领是否超时】
         *
         * 业务作用：判断条目被认领后停留时长是否超过阈值，用于识别进程崩溃残留。
         *
         * 调用场景：claim 发现状态已是 CLAIMED 时判断是否可重新认领。
         *
         * 调用链：claim → entry.claimExpired。
         *
         * 数据处理：认领时间大于 0 且距今超过 3 分钟即视为超时。
         *
         * 业务规则：超时阈值固定 3 分钟。
         *
         * 状态影响：无。
         *
         * 异常情况：无。
         *
         * 注意事项：超时后允许重新认领，可能造成极短时间内的并发重复提交。
         */
        private boolean claimExpired() {
            return claimedAt > 0 && System.currentTimeMillis() - claimedAt > CLAIM_TTL_MS;
        }
    }
}