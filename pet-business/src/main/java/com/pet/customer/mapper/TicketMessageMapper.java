package com.pet.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.customer.entity.TicketMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单消息数据访问接口，提供 TicketMessage 实体的基础 CRUD 操作。
 * 映射表 ticket_message_wsh。
 */
@Mapper
public interface TicketMessageMapper extends BaseMapper<TicketMessage> {
}
