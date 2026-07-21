package com.pet.customer.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
import com.pet.customer.dto.TicketCreateRequestDTO;
import com.pet.customer.dto.TicketDTO;
import com.pet.customer.dto.TicketMessageDTO;

import java.util.List;

public interface TicketService {
    List<TicketDTO> listByUser(Long userId);

    List<TicketDTO> listAll();

    IPage<TicketDTO> listPage(PageRequestDTO pageParam);

    IPage<TicketDTO> listPageForStaff(PageRequestDTO pageParam, Long staffUserId, boolean admin, boolean merchant, boolean customerService);

    TicketDTO getById(Long id);

    TicketDTO getByIdForUser(Long id, Long userId, boolean admin, boolean merchant, boolean customerService);

    TicketDTO create(Long userId, TicketCreateRequestDTO request);

    TicketDTO assign(Long id, Long assigneeId);

    TicketDTO assignForStaff(Long id, Long assigneeId, Long staffUserId, boolean admin, boolean merchant, boolean customerService);

    TicketDTO resolve(Long id, String result);

    TicketDTO resolveForStaff(Long id, String result, Long staffUserId, boolean admin, boolean merchant, boolean customerService);

    TicketDTO close(Long id);

    TicketDTO closeForStaff(Long id, Long staffUserId, boolean admin, boolean merchant, boolean customerService);

    TicketMessageDTO addMessage(Long ticketId, Long userId, String content);

    TicketMessageDTO addMessageForUser(Long ticketId, Long userId, boolean admin, boolean merchant, boolean customerService, String content);

    List<TicketMessageDTO> listMessages(Long ticketId);

    List<TicketMessageDTO> listMessagesForUser(Long ticketId, Long userId, boolean admin, boolean merchant, boolean customerService);
}
