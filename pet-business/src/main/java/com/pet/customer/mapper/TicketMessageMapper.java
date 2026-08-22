package com.pet.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.customer.entity.TicketMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 工单消息数据访问接口，提供 TicketMessage 实体的基础 CRUD 操作。
 * 映射表 ticket_message_wsh。
 */
@Mapper
public interface TicketMessageMapper extends BaseMapper<TicketMessage> {

    /**
     * 批量查询多张工单各自的最新一条消息（用于线程列表展示）
     *
     * @param ticketIds 工单ID集合
     * @return 每张工单的最新消息（无消息的工单不返回）
     */
    @Select("<script>" +
            "SELECT tm.* FROM ticket_message_wsh tm " +
            "WHERE tm.deleted_wsh = 0 AND tm.id_wsh IN ( " +
            "  SELECT MAX(m.id_wsh) FROM ticket_message_wsh m " +
            "  WHERE m.deleted_wsh = 0 AND m.ticket_id_wsh IN " +
            "  <foreach collection='ticketIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "  GROUP BY m.ticket_id_wsh) " +
            "</script>")
    List<TicketMessage> selectLatestByTicketIds(@Param("ticketIds") Collection<Long> ticketIds);

    /**
     * 批量统计每张工单中发给指定用户且未读的消息数（排除本人发出的消息）
     *
     * @param ticketIds 工单ID集合
     * @param userId    当前用户ID
     * @return 每行 {ticketId, cnt}
     */
    @Select("<script>" +
            "SELECT ticket_id_wsh AS ticketId, COUNT(*) AS cnt FROM ticket_message_wsh " +
            "WHERE deleted_wsh = 0 AND is_read_wsh = 0 " +
            "AND user_id_wsh &lt;&gt; #{userId} " +
            "AND ticket_id_wsh IN " +
            "<foreach collection='ticketIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY ticket_id_wsh" +
            "</script>")
    List<Map<String, Object>> selectUnreadCountByTicketIds(@Param("ticketIds") Collection<Long> ticketIds,
                                                           @Param("userId") Long userId);

    /**
     * 将指定工单中发给指定用户的消息全部标记为已读（排除本人发出的消息）
     *
     * @param ticketId 工单ID
     * @param userId   当前用户ID
     * @return 更新的行数
     */
    @Update("UPDATE ticket_message_wsh SET is_read_wsh = 1 " +
            "WHERE deleted_wsh = 0 AND ticket_id_wsh = #{ticketId} " +
            "AND user_id_wsh != #{userId}")
    int markReadByTicketId(@Param("ticketId") Long ticketId,
                           @Param("userId") Long userId);
}
