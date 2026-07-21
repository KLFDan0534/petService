package com.pet.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.common.BusinessException;
import com.pet.common.PageRequestDTO;
import com.pet.customer.dto.TicketCreateRequestDTO;
import com.pet.customer.dto.TicketDTO;
import com.pet.customer.dto.TicketMessageDTO;
import com.pet.customer.entity.Ticket;
import com.pet.customer.entity.TicketMessage;
import com.pet.customer.mapper.TicketMapper;
import com.pet.customer.mapper.TicketMessageMapper;
import com.pet.customer.service.MerchantCustomerServiceService;
import com.pet.customer.service.TicketService;
import com.pet.operation.entity.Notification;
import com.pet.operation.service.NotificationService;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TicketServiceImpl implements TicketService {

    private final TicketMapper ticketMapper;
    private final TicketMessageMapper ticketMessageMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final OrderMapper orderMapper;
    private final MerchantMapper merchantMapper;
    private final MerchantCustomerServiceService merchantCustomerServiceService;

    public TicketServiceImpl(TicketMapper ticketMapper,
                             TicketMessageMapper ticketMessageMapper,
                             UserMapper userMapper,
                             NotificationService notificationService,
                             OrderMapper orderMapper,
                             MerchantMapper merchantMapper,
                             MerchantCustomerServiceService merchantCustomerServiceService) {
        this.ticketMapper = ticketMapper;
        this.ticketMessageMapper = ticketMessageMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
        this.orderMapper = orderMapper;
        this.merchantMapper = merchantMapper;
        this.merchantCustomerServiceService = merchantCustomerServiceService;
    }

    @Override
    public List<TicketDTO> listByUser(Long userId) {
        return toDTOList(ticketMapper.selectList(
                new LambdaQueryWrapper<Ticket>()
                        .eq(Ticket::getUser_id_wsh, userId)
                        .orderByDesc(Ticket::getCreated_at_wsh)));
    }

    @Override
    public List<TicketDTO> listAll() {
        return toDTOList(ticketMapper.selectList(
                new LambdaQueryWrapper<Ticket>().orderByDesc(Ticket::getCreated_at_wsh)));
    }

    @Override
    public IPage<TicketDTO> listPage(PageRequestDTO pageParam) {
        Page<Ticket> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        IPage<Ticket> result = ticketMapper.selectPage(page,
                new LambdaQueryWrapper<Ticket>().orderByDesc(Ticket::getCreated_at_wsh));
        Page<TicketDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(toDTOList(result.getRecords()));
        return dtoPage;
    }

    @Override
    public IPage<TicketDTO> listPageForStaff(PageRequestDTO pageParam, Long staffUserId,
                                             boolean admin, boolean merchant, boolean customerService) {
        if (admin) {
            return listPage(pageParam);
        }
        Set<Long> merchantIds = staffMerchantIds(staffUserId, merchant, customerService);
        Page<TicketDTO> emptyPage = new Page<>(pageParam.getPage(), pageParam.getSize(), 0);
        if (merchantIds.isEmpty()) {
            emptyPage.setRecords(List.of());
            return emptyPage;
        }
        Page<Ticket> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        IPage<Ticket> result = ticketMapper.selectPage(page,
                new LambdaQueryWrapper<Ticket>()
                        .in(Ticket::getMerchant_id_wsh, merchantIds)
                        .orderByDesc(Ticket::getCreated_at_wsh));
        Page<TicketDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(toDTOList(result.getRecords()));
        return dtoPage;
    }

    @Override
    public TicketDTO getById(Long id) {
        return toDTO(getByIdRaw(id));
    }

    @Override
    public TicketDTO getByIdForUser(Long id, Long userId, boolean admin, boolean merchant, boolean customerService) {
        Ticket ticket = getByIdRaw(id);
        assertVisible(ticket, userId, admin, merchant, customerService);
        return toDTO(ticket);
    }

    @Transactional
    @Override
    public TicketDTO create(Long userId, TicketCreateRequestDTO request) {
        if (request == null) {
            throw new BusinessException(400, "ticket request cannot be empty");
        }
        PetOrder order = validateOrderTicket(request, userId);
        Long merchantId = resolveMerchantId(request, order);
        validateMerchantIfProvided(merchantId);
        Ticket ticket = new Ticket();
        ticket.setUser_id_wsh(userId);
        ticket.setOrder_id_wsh(request.getOrder_id_wsh());
        ticket.setMerchant_id_wsh(merchantId);
        ticket.setTitle_wsh(request.getTitle_wsh());
        ticket.setContent_wsh(request.getContent_wsh());
        ticket.setCategory_wsh(request.getCategory_wsh());
        ticket.setPriority_wsh(request.getPriority_wsh() == null ? "medium" : request.getPriority_wsh());
        ticket.setStatus_wsh("pending");
        ticketMapper.insert(ticket);
        return toDTO(ticket);
    }

    @Transactional
    @Override
    public TicketDTO assign(Long id, Long assigneeId) {
        Ticket ticket = getByIdRaw(id);
        if (!"pending".equals(ticket.getStatus_wsh())) {
            throw new BusinessException(400, "ticket cannot be assigned in current status");
        }
        User assignee = userMapper.selectById(assigneeId);
        if (assignee == null) {
            throw new BusinessException(404, "assignee not found");
        }
        ticket.setAssignee_id_wsh(assigneeId);
        ticket.setStatus_wsh("processing");
        ticketMapper.updateById(ticket);
        sendTicketNotification(ticket.getUser_id_wsh(), ticket.getId_wsh(),
                "Ticket accepted",
                "Ticket " + ticket.getTitle_wsh() + " is being processed");
        return toDTO(ticket);
    }

    @Transactional
    @Override
    public TicketDTO assignForStaff(Long id, Long assigneeId, Long staffUserId,
                                    boolean admin, boolean merchant, boolean customerService) {
        Ticket ticket = getByIdRaw(id);
        assertStaffCanManage(ticket, staffUserId, admin, merchant, customerService);
        return assign(id, assigneeId);
    }

    @Transactional
    @Override
    public TicketDTO resolve(Long id, String result) {
        Ticket ticket = getByIdRaw(id);
        if (!"pending".equals(ticket.getStatus_wsh()) && !"processing".equals(ticket.getStatus_wsh())) {
            throw new BusinessException(400, "ticket cannot be resolved in current status");
        }
        ticket.setStatus_wsh("resolved");
        ticket.setResult_wsh(result);
        ticketMapper.updateById(ticket);
        addMessage(id,
                ticket.getAssignee_id_wsh() != null ? ticket.getAssignee_id_wsh() : ticket.getUser_id_wsh(),
                result != null ? result : "Ticket has been resolved");
        sendTicketNotification(ticket.getUser_id_wsh(), ticket.getId_wsh(),
                "Ticket resolved",
                "Ticket " + ticket.getTitle_wsh() + " has been resolved");
        return toDTO(ticket);
    }

    @Transactional
    @Override
    public TicketDTO resolveForStaff(Long id, String result, Long staffUserId,
                                     boolean admin, boolean merchant, boolean customerService) {
        Ticket ticket = getByIdRaw(id);
        assertStaffCanManage(ticket, staffUserId, admin, merchant, customerService);
        return resolve(id, result);
    }

    @Transactional
    @Override
    public TicketDTO close(Long id) {
        Ticket ticket = getByIdRaw(id);
        if (!"resolved".equals(ticket.getStatus_wsh())) {
            throw new BusinessException(400, "ticket cannot be closed in current status");
        }
        ticket.setStatus_wsh("closed");
        ticketMapper.updateById(ticket);
        sendTicketNotification(ticket.getUser_id_wsh(), ticket.getId_wsh(),
                "Ticket closed",
                "Ticket " + ticket.getTitle_wsh() + " has been closed");
        return toDTO(ticket);
    }

    @Transactional
    @Override
    public TicketDTO closeForStaff(Long id, Long staffUserId, boolean admin, boolean merchant, boolean customerService) {
        Ticket ticket = getByIdRaw(id);
        assertStaffCanManage(ticket, staffUserId, admin, merchant, customerService);
        return close(id);
    }

    @Transactional
    @Override
    public TicketMessageDTO addMessage(Long ticketId, Long userId, String content) {
        TicketMessage msg = new TicketMessage();
        msg.setTicket_id_wsh(ticketId);
        msg.setUser_id_wsh(userId);
        msg.setContent_wsh(content);
        ticketMessageMapper.insert(msg);
        return toMessageDTO(msg);
    }

    @Transactional
    @Override
    public TicketMessageDTO addMessageForUser(Long ticketId, Long userId, boolean admin,
                                              boolean merchant, boolean customerService, String content) {
        Ticket ticket = getByIdRaw(ticketId);
        assertVisible(ticket, userId, admin, merchant, customerService);
        return addMessage(ticketId, userId, content);
    }

    @Override
    public List<TicketMessageDTO> listMessages(Long ticketId) {
        return toMessageDTOList(ticketMessageMapper.selectList(
                new LambdaQueryWrapper<TicketMessage>()
                        .eq(TicketMessage::getTicket_id_wsh, ticketId)
                        .orderByAsc(TicketMessage::getCreated_at_wsh)));
    }

    @Override
    public List<TicketMessageDTO> listMessagesForUser(Long ticketId, Long userId,
                                                      boolean admin, boolean merchant, boolean customerService) {
        Ticket ticket = getByIdRaw(ticketId);
        assertVisible(ticket, userId, admin, merchant, customerService);
        return listMessages(ticketId);
    }

    private void assertVisible(Ticket ticket, Long userId, boolean admin, boolean merchant, boolean customerService) {
        if (admin) {
            return;
        }
        if (ticket.getUser_id_wsh() != null && ticket.getUser_id_wsh().equals(userId)) {
            return;
        }
        if (ticket.getMerchant_id_wsh() != null
                && staffMerchantIds(userId, merchant, customerService).contains(ticket.getMerchant_id_wsh())) {
            return;
        }
        throw new BusinessException(403, "no permission to access this ticket");
    }

    private void assertStaffCanManage(Ticket ticket, Long userId, boolean admin, boolean merchant, boolean customerService) {
        if (admin) {
            return;
        }
        if (ticket.getMerchant_id_wsh() != null
                && staffMerchantIds(userId, merchant, customerService).contains(ticket.getMerchant_id_wsh())) {
            return;
        }
        throw new BusinessException(403, "no permission to manage this merchant ticket");
    }

    private Set<Long> staffMerchantIds(Long userId, boolean merchant, boolean customerService) {
        if (userId == null) {
            return Set.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        if (merchant) {
            Merchant owned = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                    .eq(Merchant::getUser_id_wsh, userId)
                    .last("LIMIT 1"));
            if (owned != null && owned.getId_wsh() != null) {
                ids.add(owned.getId_wsh());
            }
        }
        if (customerService) {
            ids.addAll(merchantCustomerServiceService.getApprovedMerchantIds(userId));
        }
        return ids;
    }

    private PetOrder validateOrderTicket(TicketCreateRequestDTO request, Long userId) {
        if (request.getOrder_id_wsh() == null) {
            return null;
        }
        PetOrder order = orderMapper.selectById(request.getOrder_id_wsh());
        if (order == null) {
            throw new BusinessException(404, "order not found");
        }
        if (userId == null || !userId.equals(order.getOwner_id_wsh())) {
            throw new BusinessException(403, "only order owner can create ticket for this order");
        }
        return order;
    }

    private Long resolveMerchantId(TicketCreateRequestDTO request, PetOrder order) {
        if (order != null) {
            return order.getMerchant_id_wsh();
        }
        return request.getMerchant_id_wsh();
    }

    private void validateMerchantIfProvided(Long merchantId) {
        if (merchantId == null) {
            return;
        }
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(404, "merchant not found");
        }
    }

    private Ticket getByIdRaw(Long id) {
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new BusinessException(404, "ticket not found");
        }
        return ticket;
    }

    private TicketDTO toDTO(Ticket ticket) {
        if (ticket == null) {
            return null;
        }
        TicketDTO dto = new TicketDTO();
        dto.setId_wsh(ticket.getId_wsh());
        dto.setUser_id_wsh(ticket.getUser_id_wsh());
        dto.setMerchant_id_wsh(ticket.getMerchant_id_wsh());
        dto.setOrder_id_wsh(ticket.getOrder_id_wsh());
        dto.setTitle_wsh(ticket.getTitle_wsh());
        dto.setContent_wsh(ticket.getContent_wsh());
        dto.setCategory_wsh(ticket.getCategory_wsh());
        dto.setPriority_wsh(ticket.getPriority_wsh());
        dto.setStatus_wsh(ticket.getStatus_wsh());
        dto.setResult_wsh(ticket.getResult_wsh());
        dto.setAssignee_id_wsh(ticket.getAssignee_id_wsh());
        dto.setCreated_at_wsh(ticket.getCreated_at_wsh());
        dto.setUpdated_at_wsh(ticket.getUpdated_at_wsh());
        return dto;
    }

    private List<TicketDTO> toDTOList(List<Ticket> list) {
        if (list == null) {
            return List.of();
        }
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private TicketMessageDTO toMessageDTO(TicketMessage msg) {
        if (msg == null) {
            return null;
        }
        TicketMessageDTO dto = new TicketMessageDTO();
        dto.setId_wsh(msg.getId_wsh());
        dto.setTicket_id_wsh(msg.getTicket_id_wsh());
        dto.setUser_id_wsh(msg.getUser_id_wsh());
        dto.setContent_wsh(msg.getContent_wsh());
        dto.setCreated_at_wsh(msg.getCreated_at_wsh());
        return dto;
    }

    private List<TicketMessageDTO> toMessageDTOList(List<TicketMessage> list) {
        if (list == null) {
            return List.of();
        }
        return list.stream().map(this::toMessageDTO).collect(Collectors.toList());
    }

    private void sendTicketNotification(Long userId, Long ticketId, String title, String content) {
        try {
            Notification notification = new Notification();
            notification.setUser_id_wsh(userId);
            notification.setTitle_wsh(title);
            notification.setContent_wsh(content);
            notification.setType_wsh("ticket");
            notification.setRelated_id_wsh(ticketId);
            notification.setIs_read_wsh(0);
            notificationService.create(notification);
        } catch (Exception e) {
            log.warn("Failed to send ticket notification", e);
        }
    }
}
