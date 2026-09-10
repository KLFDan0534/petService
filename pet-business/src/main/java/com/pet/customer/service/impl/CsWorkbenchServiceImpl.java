package com.pet.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.customer.dto.CsConversationDTO;
import com.pet.customer.dto.CsMerchantDTO;
import com.pet.customer.dto.CsThreadDTO;
import com.pet.customer.dto.CsWorkbenchStatsDTO;
import com.pet.customer.entity.ChatMessage;
import com.pet.customer.entity.Complaint;
import com.pet.customer.entity.ComplaintMessage;
import com.pet.customer.entity.Ticket;
import com.pet.customer.entity.TicketMessage;
import com.pet.customer.mapper.ChatMessageMapper;
import com.pet.customer.mapper.ComplaintMapper;
import com.pet.customer.mapper.ComplaintMessageMapper;
import com.pet.customer.mapper.TicketMapper;
import com.pet.customer.mapper.TicketMessageMapper;
import com.pet.customer.service.CsWorkbenchService;
import com.pet.customer.service.MerchantCustomerServiceService;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 【业务模块】客服工作台（实现）
 * 业务作用：为客服提供工作台统计、服务商家列表与会话列表。
 * 权限规则：商家维度隔离——客服仅统计授权服务商家，管理员统计全局。
 */
@Service
@Slf4j
public class CsWorkbenchServiceImpl implements CsWorkbenchService {

    private final TicketMapper ticketMapper;
    private final ComplaintMapper complaintMapper;
    private final MerchantMapper merchantMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final UserMapper userMapper;
    private final MerchantCustomerServiceService merchantCustomerServiceService;
    private final ComplaintMessageMapper complaintMessageMapper;
    private final TicketMessageMapper ticketMessageMapper;
    private final OrderMapper orderMapper;

    public CsWorkbenchServiceImpl(TicketMapper ticketMapper,
                                  ComplaintMapper complaintMapper,
                                  MerchantMapper merchantMapper,
                                  ChatMessageMapper chatMessageMapper,
                                  UserMapper userMapper,
                                  MerchantCustomerServiceService merchantCustomerServiceService,
                                  ComplaintMessageMapper complaintMessageMapper,
                                  TicketMessageMapper ticketMessageMapper,
                                  OrderMapper orderMapper) {
        this.ticketMapper = ticketMapper;
        this.complaintMapper = complaintMapper;
        this.merchantMapper = merchantMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.userMapper = userMapper;
        this.merchantCustomerServiceService = merchantCustomerServiceService;
        this.complaintMessageMapper = complaintMessageMapper;
        this.ticketMessageMapper = ticketMessageMapper;
        this.orderMapper = orderMapper;
    }

    @Override
    public CsWorkbenchStatsDTO stats(Long staffUserId, boolean admin, boolean merchant, boolean customerService) {
        CsWorkbenchStatsDTO dto = new CsWorkbenchStatsDTO();
        Set<Long> merchantIds = admin ? null : staffMerchantIds(staffUserId, merchant, customerService);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        dto.setPending_tickets_wsh(countTickets(merchantIds, "pending", null, null, null));
        dto.setMy_processing_tickets_wsh(countTickets(null, "processing", staffUserId, null, null));
        dto.setPending_complaints_wsh(countComplaints(merchantIds, "pending", null));
        dto.setResolved_tickets_today_wsh(countTickets(merchantIds, "resolved", null, null, todayStart));
        dto.setResolved_complaints_today_wsh(countComplaints(merchantIds, "resolved", todayStart));
        if (admin) {
            Long merchantTotal = merchantMapper.selectCount(null);
            dto.setMerchant_count_wsh(merchantTotal == null ? 0 : merchantTotal);
        } else {
            dto.setMerchant_count_wsh(merchantIds == null ? 0 : merchantIds.size());
        }
        return dto;
    }

    @Override
    public List<CsMerchantDTO> merchants(Long staffUserId) {
        Set<Long> merchantIds = new LinkedHashSet<>();
        // 客服：授权服务商家；非客服：本人拥有的商家
        Merchant owned = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUser_id_wsh, staffUserId)
                .last("LIMIT 1"));
        if (owned != null && owned.getId_wsh() != null) {
            merchantIds.add(owned.getId_wsh());
        }
        merchantIds.addAll(merchantCustomerServiceService.getApprovedMerchantIds(staffUserId));

        List<CsMerchantDTO> result = new ArrayList<>();
        for (Long merchantId : merchantIds) {
            if (merchantId == null) {
                continue;
            }
            Merchant merchant = merchantMapper.selectById(merchantId);
            if (merchant == null) {
                continue;
            }
            CsMerchantDTO dto = new CsMerchantDTO();
            dto.setMerchant_id_wsh(merchantId);
            dto.setMerchant_name_wsh(merchant.getName_wsh());
            dto.setMerchant_status_wsh(merchant.getStatus_wsh());
            dto.setPending_ticket_count_wsh(countTickets(Set.of(merchantId), "pending", null, null, null));
            dto.setPending_complaint_count_wsh(countComplaints(Set.of(merchantId), "pending", null));
            result.add(dto);
        }
        result.sort((a, b) -> Long.compare(
                b.getPending_ticket_count_wsh() + b.getPending_complaint_count_wsh(),
                a.getPending_ticket_count_wsh() + a.getPending_complaint_count_wsh()));
        return result;
    }

    @Override
    public List<CsConversationDTO> conversations(Long userId) {
        List<ChatMessage> latestMessages = chatMessageMapper.selectConversations(userId);
        Map<Long, Long> unreadBySender = new HashMap<>();
        for (Map<String, Object> row : chatMessageMapper.selectUnreadGroupBySender(userId)) {
            Object sender = row.get("from_user_id_wsh");
            Object count = row.get("unread_count");
            if (sender != null) {
                try {
                    unreadBySender.put(Long.valueOf(String.valueOf(sender)),
                            count == null ? 0L : Long.parseLong(String.valueOf(count)));
                } catch (NumberFormatException ignored) {
                    // 忽略脏数据
                }
            }
        }

        List<CsConversationDTO> result = new ArrayList<>();
        for (ChatMessage message : latestMessages) {
            Long otherUserId;
            if (message.getFrom_user_id_wsh() != null && message.getFrom_user_id_wsh().equals(userId)) {
                otherUserId = message.getTo_user_id_wsh();
            } else {
                otherUserId = message.getFrom_user_id_wsh();
            }
            if (otherUserId == null) {
                continue;
            }
            CsConversationDTO dto = new CsConversationDTO();
            dto.setOther_user_id_wsh(otherUserId);
            dto.setOther_user_name_wsh(displayName(otherUserId));
            dto.setLast_message_wsh(message.getContent_wsh());
            dto.setLast_time_wsh(message.getCreated_at_wsh());
            dto.setUnread_count_wsh(unreadBySender.getOrDefault(otherUserId, 0L));
            result.add(dto);
        }
        return result;
    }

    @Override
    public List<CsThreadDTO> threads(Long userId, boolean admin, boolean merchant, boolean customerService) {
        Set<Long> merchantIds = admin ? null : staffMerchantIds(userId, merchant, customerService);
        List<CsThreadDTO> result = new ArrayList<>();

        // 投诉线程：pending/processing 或已有留言的投诉各自成一条线程
        LambdaQueryWrapper<Complaint> complaintWrapper = new LambdaQueryWrapper<>();
        if (merchantIds != null) {
            if (merchantIds.isEmpty()) {
                return List.of();
            }
            complaintWrapper.in(Complaint::getMerchant_id_wsh, merchantIds);
        }
        List<Complaint> complaints = complaintMapper.selectList(complaintWrapper);
        if (!complaints.isEmpty()) {
            List<Long> complaintIds = complaints.stream()
                    .map(Complaint::getId_wsh).filter(Objects::nonNull).collect(Collectors.toList());
            Map<Long, ComplaintMessage> latestByComplaint = complaintMessageMapper
                    .selectLatestByComplaintIds(complaintIds).stream()
                    .collect(Collectors.toMap(ComplaintMessage::getComplaint_id_wsh, Function.identity(), (a, b) -> a));
            Map<Long, Long> unreadByComplaint = toLongMap(
                    complaintMessageMapper.selectUnreadCountByComplaintIds(complaintIds, userId));
            Map<Long, PetOrder> orderMap = loadOrders(complaints.stream()
                    .map(Complaint::getOrder_id_wsh).filter(Objects::nonNull).collect(Collectors.toSet()));
            for (Complaint c : complaints) {
                ComplaintMessage latest = latestByComplaint.get(c.getId_wsh());
                boolean actionable = "pending".equals(c.getStatus_wsh()) || "processing".equals(c.getStatus_wsh());
                if (latest == null && !actionable) {
                    continue;
                }
                CsThreadDTO dto = new CsThreadDTO();
                dto.setType_wsh("complaint");
                dto.setBiz_id_wsh(c.getId_wsh());
                dto.setTitle_wsh(c.getTitle_wsh());
                dto.setOther_user_id_wsh(c.getOwner_id_wsh());
                dto.setOther_user_name_wsh(displayName(c.getOwner_id_wsh()));
                dto.setStatus_wsh(c.getStatus_wsh());
                dto.setUnread_count_wsh(unreadByComplaint.getOrDefault(c.getId_wsh(), 0L));
                PetOrder order = c.getOrder_id_wsh() == null ? null : orderMap.get(c.getOrder_id_wsh());
                dto.setOrder_no_wsh(order == null ? null : order.getOrder_no_wsh());
                if (latest != null) {
                    dto.setLast_message_wsh(latest.getContent_wsh());
                    dto.setLast_time_wsh(latest.getCreated_at_wsh());
                }
                result.add(dto);
            }
        }

        // 工单线程：pending/processing 或已有留言的工单各自成一条线程
        LambdaQueryWrapper<Ticket> ticketWrapper = new LambdaQueryWrapper<>();
        if (merchantIds != null) {
            ticketWrapper.in(Ticket::getMerchant_id_wsh, merchantIds);
        }
        List<Ticket> tickets = ticketMapper.selectList(ticketWrapper);
        if (!tickets.isEmpty()) {
            List<Long> ticketIds = tickets.stream()
                    .map(Ticket::getId_wsh).filter(Objects::nonNull).collect(Collectors.toList());
            Map<Long, TicketMessage> latestByTicket = ticketMessageMapper
                    .selectLatestByTicketIds(ticketIds).stream()
                    .collect(Collectors.toMap(TicketMessage::getTicket_id_wsh, Function.identity(), (a, b) -> a));
            Map<Long, Long> unreadByTicket = toLongMap(
                    ticketMessageMapper.selectUnreadCountByTicketIds(ticketIds, userId));
            Map<Long, PetOrder> orderMap = loadOrders(tickets.stream()
                    .map(Ticket::getOrder_id_wsh).filter(Objects::nonNull).collect(Collectors.toSet()));
            for (Ticket t : tickets) {
                TicketMessage latest = latestByTicket.get(t.getId_wsh());
                boolean actionable = "pending".equals(t.getStatus_wsh()) || "processing".equals(t.getStatus_wsh());
                if (latest == null && !actionable) {
                    continue;
                }
                CsThreadDTO dto = new CsThreadDTO();
                dto.setType_wsh("ticket");
                dto.setBiz_id_wsh(t.getId_wsh());
                dto.setTitle_wsh(t.getTitle_wsh());
                dto.setOther_user_id_wsh(t.getUser_id_wsh());
                dto.setOther_user_name_wsh(displayName(t.getUser_id_wsh()));
                dto.setStatus_wsh(t.getStatus_wsh());
                dto.setUnread_count_wsh(unreadByTicket.getOrDefault(t.getId_wsh(), 0L));
                PetOrder order = t.getOrder_id_wsh() == null ? null : orderMap.get(t.getOrder_id_wsh());
                dto.setOrder_no_wsh(order == null ? null : order.getOrder_no_wsh());
                if (latest != null) {
                    dto.setLast_message_wsh(latest.getContent_wsh());
                    dto.setLast_time_wsh(latest.getCreated_at_wsh());
                }
                result.add(dto);
            }
        }

        result.sort((a, b) -> {
            LocalDateTime ta = a.getLast_time_wsh();
            LocalDateTime tb = b.getLast_time_wsh();
            if (ta == null && tb == null) {
                return 0;
            }
            if (ta == null) {
                return 1;
            }
            if (tb == null) {
                return -1;
            }
            return tb.compareTo(ta);
        });
        return result;
    }

    @Override
    public void markThreadRead(Long userId, String type, Long bizId) {
        if (userId == null || bizId == null) {
            return;
        }
        if ("complaint".equals(type)) {
            complaintMessageMapper.markReadByComplaintId(bizId, userId);
        } else if ("ticket".equals(type)) {
            ticketMessageMapper.markReadByTicketId(bizId, userId);
        }
    }

    private Map<Long, Long> toLongMap(List<Map<String, Object>> rows) {
        Map<Long, Long> result = new HashMap<>();
        if (rows == null) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            Object id = row.get("complaintId");
            Object cnt = row.get("cnt");
            if (id == null && row.get("ticketId") != null) {
                id = row.get("ticketId");
            }
            if (id != null) {
                try {
                    result.put(Long.valueOf(String.valueOf(id)),
                            cnt == null ? 0L : Long.parseLong(String.valueOf(cnt)));
                } catch (NumberFormatException ignored) {
                    // 忽略脏数据
                }
            }
        }
        return result;
    }

    private Map<Long, PetOrder> loadOrders(Set<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        return orderMapper.selectList(new LambdaQueryWrapper<PetOrder>()
                        .in(PetOrder::getId_wsh, orderIds))
                .stream()
                .collect(Collectors.toMap(PetOrder::getId_wsh, Function.identity()));
    }

    private long countTickets(Set<Long> merchantIds, String status, Long assigneeId, Long merchantId, LocalDateTime updatedSince) {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        if (merchantIds != null) {
            if (merchantIds.isEmpty()) {
                return 0;
            }
            wrapper.in(Ticket::getMerchant_id_wsh, merchantIds);
        }
        if (merchantId != null) {
            wrapper.eq(Ticket::getMerchant_id_wsh, merchantId);
        }
        if (status != null) {
            wrapper.eq(Ticket::getStatus_wsh, status);
        }
        if (assigneeId != null) {
            wrapper.eq(Ticket::getAssignee_id_wsh, assigneeId);
        }
        if (updatedSince != null) {
            wrapper.ge(Ticket::getUpdated_at_wsh, updatedSince);
        }
        Long count = ticketMapper.selectCount(wrapper);
        return count == null ? 0 : count;
    }

    private long countComplaints(Set<Long> merchantIds, String status, LocalDateTime updatedSince) {
        LambdaQueryWrapper<Complaint> wrapper = new LambdaQueryWrapper<>();
        if (merchantIds != null) {
            if (merchantIds.isEmpty()) {
                return 0;
            }
            wrapper.in(Complaint::getMerchant_id_wsh, merchantIds);
        }
        if (status != null) {
            wrapper.eq(Complaint::getStatus_wsh, status);
        }
        if (updatedSince != null) {
            wrapper.ge(Complaint::getUpdated_at_wsh, updatedSince);
        }
        Long count = complaintMapper.selectCount(wrapper);
        return count == null ? 0 : count;
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

    private String displayName(Long userId) {
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            return String.valueOf(userId);
        }
        return user.getNickname_wsh() != null ? user.getNickname_wsh() : user.getUsername_wsh();
    }
}
