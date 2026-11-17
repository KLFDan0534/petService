package com.pet.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.customer.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 工单数据访问接口，提供 Ticket 实体的基础 CRUD 操作。
 * 映射表 ticket_wsh。
 *
 * <p>状态迁移一律走下面的 CAS 方法，不用 updateById：</p>
 * <p>updateById 会把整行按主键覆盖，两个并发请求都先读到 pending、再各自 update，
 * 后写的会静默覆盖先写的（双指派、双解决都拦不住）。CAS 把「前态」写进 WHERE，
 * 只有一个能 affected=1，其余 affected=0 由业务层判定为冲突或幂等。</p>
 */
@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {

    /**
     * 指派工单：pending → processing，同时写入处理人。
     *
     * <p>处理人与状态在同一条 UPDATE 里落库，不会出现「状态变了但处理人没变」的中间态。</p>
     *
     * @param id             工单 ID
     * @param assigneeId     处理人用户 ID
     * @param expectedStatus 期望前态（业务层传 pending）
     * @param status         目标状态（业务层传 processing）
     * @return 受影响行数，1 表示指派成功，0 表示已被他人抢先指派或状态已变
     */
    @Update("UPDATE ticket_wsh SET assignee_id_wsh = #{assigneeId}, status_wsh = #{status}, "
            + "updated_at_wsh = NOW() "
            + "WHERE id_wsh = #{id} AND status_wsh = #{expectedStatus} AND deleted_wsh = 0")
    int casAssign(@Param("id") Long id,
                  @Param("assigneeId") Long assigneeId,
                  @Param("expectedStatus") String expectedStatus,
                  @Param("status") String status);

    /**
     * 解决工单：pending / processing → resolved，同时写入处理结论。
     *
     * <p>允许两个前态，所以用 IN。两条前态由业务层传入，SQL 里不硬编码，
     * 避免状态字面量散落在 XML/注解里。</p>
     *
     * @param id           工单 ID
     * @param result       处理结论，可为 null
     * @param status       目标状态（业务层传 resolved）
     * @param expectedA    允许的前态之一（pending）
     * @param expectedB    允许的前态之二（processing）
     * @return 受影响行数，1 表示解决成功，0 表示已被并发处理或已关闭
     */
    @Update("UPDATE ticket_wsh SET status_wsh = #{status}, result_wsh = #{result}, "
            + "updated_at_wsh = NOW() "
            + "WHERE id_wsh = #{id} AND status_wsh IN (#{expectedA}, #{expectedB}) AND deleted_wsh = 0")
    int casResolve(@Param("id") Long id,
                   @Param("result") String result,
                   @Param("status") String status,
                   @Param("expectedA") String expectedA,
                   @Param("expectedB") String expectedB);

    /**
     * 关闭工单：resolved → closed。
     *
     * @param id             工单 ID
     * @param expectedStatus 期望前态（业务层传 resolved）
     * @param status         目标状态（业务层传 closed）
     * @return 受影响行数，1 表示关闭成功，0 表示已被并发关闭或未解决
     */
    @Update("UPDATE ticket_wsh SET status_wsh = #{status}, updated_at_wsh = NOW() "
            + "WHERE id_wsh = #{id} AND status_wsh = #{expectedStatus} AND deleted_wsh = 0")
    int casClose(@Param("id") Long id,
                 @Param("expectedStatus") String expectedStatus,
                 @Param("status") String status);
}
