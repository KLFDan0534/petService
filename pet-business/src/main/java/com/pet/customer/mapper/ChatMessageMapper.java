package com.pet.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.customer.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 聊天消息数据访问接口，提供 ChatMessage 实体的基础 CRUD 操作。
 * 映射表 chat_message_wsh。
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 【业务名称】查询会话列表（每个对端最近一条消息）
     * 业务作用：为当前用户列出所有会话，返回每个对端的最近一条消息。
     * 调用场景：聊天会话列表页/客服工作台。
     * 数据处理：按对端用户分组取最新一条消息，按消息ID倒序。
     */
    @Select("SELECT cm.* FROM chat_message_wsh cm "
            + "WHERE cm.deleted_wsh = 0 AND cm.id_wsh IN ( "
            + "  SELECT MAX(m.id_wsh) FROM chat_message_wsh m "
            + "  WHERE m.deleted_wsh = 0 AND (m.from_user_id_wsh = #{userId} OR m.to_user_id_wsh = #{userId}) "
            + "  GROUP BY CASE WHEN m.from_user_id_wsh = #{userId} THEN m.to_user_id_wsh ELSE m.from_user_id_wsh END "
            + ") ORDER BY cm.id_wsh DESC")
    List<ChatMessage> selectConversations(@Param("userId") Long userId);

    /**
     * 【业务名称】按发送方统计未读数
     * 业务作用：统计当前用户从每个对端收到的未读消息数。
     * 调用场景：会话列表展示未读角标。
     * 返回：Map 包含 from_user_id_wsh 与 unread_count 两个键。
     */
    @Select("SELECT from_user_id_wsh, COUNT(*) AS unread_count FROM chat_message_wsh "
            + "WHERE deleted_wsh = 0 AND to_user_id_wsh = #{userId} AND read_wsh = 0 "
            + "GROUP BY from_user_id_wsh")
    List<Map<String, Object>> selectUnreadGroupBySender(@Param("userId") Long userId);
}
