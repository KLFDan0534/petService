package com.pet.customer.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.BusinessException;
import com.pet.common.PageParam;
import com.pet.customer.entity.Ticket;
import com.pet.customer.entity.TicketMessage;
import com.pet.customer.mapper.TicketMapper;
import com.pet.customer.mapper.TicketMessageMapper;
import com.pet.customer.service.TicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class TicketServiceImpl implements TicketService {

    private final TicketMapper ticketMapper;
    private final TicketMessageMapper ticketMessageMapper;

    public TicketServiceImpl(TicketMapper ticketMapper, TicketMessageMapper ticketMessageMapper) {
        this.ticketMapper = ticketMapper;
        this.ticketMessageMapper = ticketMessageMapper;
    }

    public List<Ticket> listByUser(Long userId) {
        log.info("调用 listByUser()");
        return ticketMapper.selectList(
                new LambdaQueryWrapper<Ticket>()
                        .eq(Ticket::getUser_id_wsh, userId)
                        .orderByDesc(Ticket::getCreated_at_wsh));
    }

    public List<Ticket> listAll() {
        log.info("调用 listAll()");
        return ticketMapper.selectList(
                new LambdaQueryWrapper<Ticket>().orderByDesc(Ticket::getCreated_at_wsh));
    }

    public IPage<Ticket> listPage(PageParam pageParam) {
        log.info("调用 listPage()");
        Page<Ticket> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        return ticketMapper.selectPage(page,
                new LambdaQueryWrapper<Ticket>().orderByDesc(Ticket::getCreated_at_wsh));
    }

    public Ticket getById(Long id) {
        log.info("调用 getById()");
        Ticket t = ticketMapper.selectById(id);
        if (t == null) throw new BusinessException("工单不存在");
        return t;
    }

    @Transactional
    public Ticket create(Long userId, Ticket ticket) {
        log.info("调用 create()");
        ticket.setUser_id_wsh(userId);
        ticket.setStatus_wsh("pending");
        ticket.setPriority_wsh(ticket.getPriority_wsh() == null ? "medium" : ticket.getPriority_wsh());
        ticketMapper.insert(ticket);
        return ticket;
    }

    @Transactional
    public Ticket assign(Long id, Long assigneeId) {
        log.info("调用 assign()");
        Ticket t = getById(id);
        t.setAssignee_id_wsh(assigneeId);
        t.setStatus_wsh("processing");
        ticketMapper.updateById(t);
        return t;
    }

    @Transactional
    public Ticket resolve(Long id, String result) {
        log.info("调用 resolve()");
        Ticket t = getById(id);
        t.setStatus_wsh("resolved");
        ticketMapper.updateById(t);
        addMessage(id, t.getAssignee_id_wsh() != null ? t.getAssignee_id_wsh() : t.getUser_id_wsh(), result != null ? result : "工单已处理完成");
        return t;
    }

    @Transactional
    public Ticket close(Long id) {
        log.info("调用 close()");
        Ticket t = getById(id);
        t.setStatus_wsh("closed");
        ticketMapper.updateById(t);
        return t;
    }

    @Transactional
    public TicketMessage addMessage(Long ticketId, Long userId, String content) {
        log.info("调用 addMessage()");
        TicketMessage msg = new TicketMessage();
        msg.setTicket_id_wsh(ticketId);
        msg.setUser_id_wsh(userId);
        msg.setContent_wsh(content);
        ticketMessageMapper.insert(msg);
        return msg;
    }

    public List<TicketMessage> listMessages(Long ticketId) {
        log.info("调用 listMessages()");
        return ticketMessageMapper.selectList(
                new LambdaQueryWrapper<TicketMessage>()
                        .eq(TicketMessage::getTicket_id_wsh, ticketId)
                        .orderByAsc(TicketMessage::getCreated_at_wsh));
    }
}
