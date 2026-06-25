package com.pet.customer.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageParam;
import com.pet.customer.entity.Ticket;
import com.pet.customer.entity.TicketMessage;

import java.util.List;

public interface TicketService {
    /**
     * 根据用户ID获取工单列表
     * @param userId 用户ID
     * @return 工单列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Ticket> listByUser(Long userId);
    /**
     * 获取所有工单列表
     * @return 工单列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Ticket> listAll();
    /**
     * 分页查询工单列表
     * @param pageParam 分页参数
     * @return 分页工单数据
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    IPage<Ticket> listPage(PageParam pageParam);
    /**
     * 根据ID获取工单详情
     * @param id 工单ID
     * @return 工单实体
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Ticket getById(Long id);
    /**
     * 创建工单
     * @param userId 用户ID
     * @param ticket 工单实体
     * @return 创建后的工单
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Ticket create(Long userId, Ticket ticket);
    /**
     * 指派工单给处理人
     * @param id 工单ID
     * @param assigneeId 处理人ID
     * @return 更新后的工单
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Ticket assign(Long id, Long assigneeId);
    /**
     * 解决工单
     * @param id 工单ID
     * @param result 处理结果
     * @return 更新后的工单
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Ticket resolve(Long id, String result);
    /**
     * 关闭工单
     * @param id 工单ID
     * @return 关闭后的工单
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Ticket close(Long id);
    /**
     * 添加工单回复消息
     * @param ticketId 工单ID
     * @param userId 用户ID
     * @param content 消息内容
     * @return 工单消息实体
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    TicketMessage addMessage(Long ticketId, Long userId, String content);
    /**
     * 获取工单消息列表
     * @param ticketId 工单ID
     * @return 工单消息列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<TicketMessage> listMessages(Long ticketId);
}

