package com.pet.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.customer.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天消息数据访问接口，提供 ChatMessage 实体的基础 CRUD 操作。
 * 映射表 chat_message_wsh。
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
