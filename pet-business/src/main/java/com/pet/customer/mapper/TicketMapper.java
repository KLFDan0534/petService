package com.pet.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.customer.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单数据访问接口，提供 Ticket 实体的基础 CRUD 操作。
 * 映射表 ticket_wsh。
 */
@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {
}
