package com.pet.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.customer.entity.ComplaintMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mapper
public interface ComplaintMessageMapper extends BaseMapper<ComplaintMessage> {

    /**
     * 批量查询多张投诉各自的最新一条消息（用于线程列表展示）
     *
     * @param complaintIds 投诉ID集合
     * @return 每张投诉的最新消息（无消息的投诉不返回）
     */
    @Select("<script>" +
            "SELECT cm.* FROM complaint_message_wsh cm " +
            "WHERE cm.deleted_wsh = 0 AND cm.id_wsh IN ( " +
            "  SELECT MAX(m.id_wsh) FROM complaint_message_wsh m " +
            "  WHERE m.deleted_wsh = 0 AND m.complaint_id_wsh IN " +
            "  <foreach collection='complaintIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "  GROUP BY m.complaint_id_wsh) " +
            "</script>")
    List<ComplaintMessage> selectLatestByComplaintIds(@Param("complaintIds") Collection<Long> complaintIds);

    /**
     * 批量统计每张投诉中发给指定用户且未读的消息数（排除本人发出的消息）
     *
     * @param complaintIds 投诉ID集合
     * @param userId       当前用户ID
     * @return 每行 {complaintId, cnt}
     */
    @Select("<script>" +
            "SELECT complaint_id_wsh AS complaintId, COUNT(*) AS cnt FROM complaint_message_wsh " +
            "WHERE deleted_wsh = 0 AND is_read_wsh = 0 " +
            "AND from_user_id_wsh &lt;&gt; #{userId} " +
            "AND complaint_id_wsh IN " +
            "<foreach collection='complaintIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY complaint_id_wsh" +
            "</script>")
    List<Map<String, Object>> selectUnreadCountByComplaintIds(@Param("complaintIds") Collection<Long> complaintIds,
                                                              @Param("userId") Long userId);

    /**
     * 将指定投诉中发给指定用户的消息全部标记为已读（排除本人发出的消息）
     *
     * @param complaintId 投诉ID
     * @param userId      当前用户ID
     * @return 更新的行数
     */
    @Update("UPDATE complaint_message_wsh SET is_read_wsh = 1 " +
            "WHERE deleted_wsh = 0 AND complaint_id_wsh = #{complaintId} " +
            "AND from_user_id_wsh != #{userId}")
    int markReadByComplaintId(@Param("complaintId") Long complaintId,
                              @Param("userId") Long userId);
}
