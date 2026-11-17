package com.pet.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.ai.entity.AgentSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 智能下单会话状态 Mapper。
 * <p>
 * 除基础 CRUD 外提供三条**带状态条件**的写入语句：
 * <ul>
 *   <li>{@code markConfirmed}：AWAIT_CONFIRM → CONFIRMED，影响行数 1 才允许建单（幂等 + 一会话一单）；</li>
 *   <li>{@code updateStateIfNotConfirmed}：续轮写状态，但绝不动已下单的会话；</li>
 *   <li>{@code updateStateIfConfirmed}：下单成功后写状态，与续轮写入互斥。</li>
 * </ul>
 * 刻意不暴露「整行 updateById」的用法，否则并发下会把 CONFIRMED 覆盖回 AWAIT_CONFIRM。
 */
@Mapper
public interface AgentSessionMapper extends BaseMapper<AgentSession> {

    /**
     * 原子占用会话用于下单：只有处于 AWAIT_CONFIRM 的会话能被改成 CONFIRMED。
     *
     * @return 影响行数，1 = 占用成功；0 = 已下单 / 状态不对 / 会话不存在
     */
    @Update("UPDATE agent_session_wsh SET status_wsh = 'CONFIRMED', updated_at_wsh = CURRENT_TIMESTAMP "
            + "WHERE session_id_wsh = #{sessionId} AND user_id_wsh = #{userId} AND status_wsh = 'AWAIT_CONFIRM'")
    int markConfirmed(@Param("sessionId") String sessionId, @Param("userId") Long userId);

    /**
     * 续轮写状态：仅当会话尚未下单时生效，防止把已确认的会话改回可确认。
     *
     * @return 影响行数，0 表示会话已被并发下单或不存在
     */
    @Update("UPDATE agent_session_wsh SET state_json_wsh = #{stateJson}, status_wsh = #{status}, "
            + "updated_at_wsh = CURRENT_TIMESTAMP "
            + "WHERE session_id_wsh = #{sessionId} AND user_id_wsh = #{userId} AND status_wsh <> 'CONFIRMED'")
    int updateStateIfNotConfirmed(@Param("sessionId") String sessionId,
                                  @Param("userId") Long userId,
                                  @Param("stateJson") String stateJson,
                                  @Param("status") String status);

    /**
     * 下单成功后写状态（只写 JSON，不改 status）。
     *
     * @return 影响行数，0 表示会话不处于 CONFIRMED（异常情况）
     */
    @Update("UPDATE agent_session_wsh SET state_json_wsh = #{stateJson}, updated_at_wsh = CURRENT_TIMESTAMP "
            + "WHERE session_id_wsh = #{sessionId} AND user_id_wsh = #{userId} AND status_wsh = 'CONFIRMED'")
    int updateStateIfConfirmed(@Param("sessionId") String sessionId,
                               @Param("userId") Long userId,
                               @Param("stateJson") String stateJson);
}
