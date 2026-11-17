package com.pet.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.KeeperMapper;
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
import com.pet.customer.service.IssueAccess;
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
import java.util.Collection;
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
    private final KeeperMapper keeperMapper;
    /** 与投诉/工单共用的商家范围判定，避免三处各写一份 */
    private final IssueAccess issueAccess;

    public CsWorkbenchServiceImpl(TicketMapper ticketMapper,
                                  ComplaintMapper complaintMapper,
                                  MerchantMapper merchantMapper,
                                  ChatMessageMapper chatMessageMapper,
                                  UserMapper userMapper,
                                  MerchantCustomerServiceService merchantCustomerServiceService,
                                  ComplaintMessageMapper complaintMessageMapper,
                                  TicketMessageMapper ticketMessageMapper,
                                  OrderMapper orderMapper,
                                  KeeperMapper keeperMapper,
                                  IssueAccess issueAccess) {
        this.ticketMapper = ticketMapper;
        this.complaintMapper = complaintMapper;
        this.merchantMapper = merchantMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.userMapper = userMapper;
        this.merchantCustomerServiceService = merchantCustomerServiceService;
        this.complaintMessageMapper = complaintMessageMapper;
        this.ticketMessageMapper = ticketMessageMapper;
        this.orderMapper = orderMapper;
        this.keeperMapper = keeperMapper;
        this.issueAccess = issueAccess;
    }

    /**
     * 【工作台统计】汇总待处理工单、处理中工单、待处理投诉与今日解决量
     *
     * 业务作用：为客服工作台顶部指标卡提供数据，管理员统计全局，商家/客服按授权商家范围统计。
     * 业务规则：admin 的商家数取全表计数，非 admin 取可见商家集合大小；merchantIds 为空集合时各计数方法直接返回 0。
     * 异常情况：无显式抛出，数据库异常向上传播。
     * 注意事项：my_processing_tickets_wsh 传 merchantIds=null 且 assigneeId=staffUserId，统计的是该用户在所有商家下的处理中工单，不受商家范围限制。
     */
    @Override
    public CsWorkbenchStatsDTO stats(Long staffUserId, boolean admin, boolean merchant, boolean customerService) {
        CsWorkbenchStatsDTO dto = new CsWorkbenchStatsDTO();
        Set<Long> merchantIds = admin ? null : issueAccess.staffMerchantIds(staffUserId, merchant, customerService);
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

    /**
     * 【服务商家列表】返回当前人员可见商家及其待处理数量
     *
     * 业务作用：客服工作台左侧商家列表，展示每个商家的待处理工单与投诉数。
     * 业务规则：商家ID去重（复用 IssueAccess.staffMerchantIds）；商家已不存在时跳过；排序按待处理工单+投诉总数倒序。
     * 异常情况：无显式抛出。
     * 注意事项：每个商家单独查详情与两次计数，商家较多时存在 N+1 查询；参数只有 staffUserId，管理员也走同一逻辑（仅返回其自身关联商家）。
     */
    @Override
    public List<CsMerchantDTO> merchants(Long staffUserId) {
        // 商家列表不区分角色：本人拥有的商家 + 已授权服务的商家，合并去重
        Set<Long> merchantIds = issueAccess.staffMerchantIds(staffUserId, true, true);

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

    /**
     * 【会话列表】返回当前用户的一对一聊天会话及未读数
     *
     * 业务作用：客服工作台展示私聊会话列表，含对方名称、最后一条消息、时间与未读数。
     * 业务规则：对方ID取消息中的另一端；对方ID为 null 的会话跳过；未读数解析失败时忽略脏数据。
     * 异常情况：无显式抛出。
     * 注意事项：未读数只按发送者汇总，未区分订单维度；用户名已改为一次 IN 批量查询，不再逐条 selectById。
     */
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

        // 先把所有会话的「对方」收集起来，一次 IN 查完用户名（原实现逐个 selectById）
        Set<Long> otherUserIds = new LinkedHashSet<>();
        for (ChatMessage message : latestMessages) {
            Long otherUserId = message.getFrom_user_id_wsh() != null
                    && message.getFrom_user_id_wsh().equals(userId)
                    ? message.getTo_user_id_wsh()
                    : message.getFrom_user_id_wsh();
            if (otherUserId != null) {
                otherUserIds.add(otherUserId);
            }
        }
        Map<Long, String> nameById = loadDisplayNames(otherUserIds);

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
            dto.setOther_user_name_wsh(nameOf(nameById, otherUserId));
            dto.setLast_message_wsh(message.getContent_wsh());
            dto.setLast_time_wsh(message.getCreated_at_wsh());
            dto.setUnread_count_wsh(unreadBySender.getOrDefault(otherUserId, 0L));
            result.add(dto);
        }
        return result;
    }

    /**
     * 【工单/投诉线程列表】合并投诉与工单为统一会话线程
     *
     * 业务作用：客服工作台的消息列表，把投诉与工单两类业务对象统一成 CsThreadDTO，供客服按最后消息时间倒序处理。
     * 业务规则：非管理员且可见商家集合为空时直接返回空列表；只有存在最新留言或状态为 pending/processing 的对象才成线程；未读数缺失按 0 处理。
     * 异常情况：无显式抛出。
     * 注意事项：非管理员无可见商家时直接 return List.of()，后续工单分支也不会执行；last_time_wsh 为 null 的线程（无留言且处于待处理状态）会排在列表末尾。
     */
    @Override
    public List<CsThreadDTO> threads(Long userId, boolean admin, boolean merchant, boolean customerService) {
        Set<Long> merchantIds = admin ? null : issueAccess.staffMerchantIds(userId, merchant, customerService);
        if (merchantIds != null && merchantIds.isEmpty()) {
            return List.of();
        }

        // ① 先把两类业务对象整体取出来，再统一批量解析「对方」用户名。
        //    旧实现每成一条线程就 selectById 查一次用户（N+1），线程一多，一个请求里全是单行查询。
        List<Complaint> complaints = loadComplaintsInScope(merchantIds);
        List<Ticket> tickets = loadTicketsInScope(merchantIds);

        // ② 投诉的「对方」按查看人算：投诉人看到被投诉方，其他人看到投诉人
        Map<Long, Long> complaintTargetUser = resolveComplaintTargetUserIds(complaints);
        Map<Long, Long> counterpartByComplaint = new HashMap<>();
        for (Complaint c : complaints) {
            counterpartByComplaint.put(c.getId_wsh(),
                    userId != null && userId.equals(c.getOwner_id_wsh())
                            ? complaintTargetUser.get(c.getId_wsh())
                            : c.getOwner_id_wsh());
        }

        // ③ 工单同理：发起人看到受理客服，其他人看到发起人
        Map<Long, Long> counterpartByTicket = new HashMap<>();
        for (Ticket t : tickets) {
            counterpartByTicket.put(t.getId_wsh(),
                    userId != null && userId.equals(t.getUser_id_wsh())
                            ? t.getAssignee_id_wsh()
                            : t.getUser_id_wsh());
        }

        // ④ 所有「对方」的用户名一次 IN 查完，替代原来逐条 selectById
        Set<Long> counterpartIds = new LinkedHashSet<>();
        for (Long id : counterpartByComplaint.values()) {
            if (id != null) {
                counterpartIds.add(id);
            }
        }
        for (Long id : counterpartByTicket.values()) {
            if (id != null) {
                counterpartIds.add(id);
            }
        }
        Map<Long, String> nameById = loadDisplayNames(counterpartIds);

        List<CsThreadDTO> result = new ArrayList<>();

        // 投诉线程：pending/processing 或已有留言的投诉各自成一条线程
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
                // 「对方」按查看人算：投诉人看到的是被投诉方，其他人看到的是投诉人。
                // 固定取 owner 会让投诉人自己打开时「对方」显示成自己。
                Long complaintCounterpart = counterpartByComplaint.get(c.getId_wsh());
                dto.setOther_user_id_wsh(complaintCounterpart);
                dto.setOther_user_name_wsh(nameOf(nameById, complaintCounterpart));
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
                // 「对方」按查看人算：工单发起人看到的是受理客服，其他人看到的是发起人。
                // 固定取发起人会让发起人自己打开时「对方」显示成自己。
                Long ticketCounterpart = counterpartByTicket.get(t.getId_wsh());
                dto.setOther_user_id_wsh(ticketCounterpart);
                dto.setOther_user_name_wsh(nameOf(nameById, ticketCounterpart));
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

    /**
     * 【标记线程已读】按业务类型把留言标记为已读
     *
     * 业务作用：客服打开某个投诉或工单线程时清空该用户的未读留言。
     * 业务规则：userId 或 bizId 为 null 时直接返回；type 为 complaint 走投诉留言表，为 ticket 走工单留言表，其它值静默忽略。
     * 异常情况：无显式抛出。
     * 注意事项：type 为其它值时不报错也不处理，调用方需保证传入合法类型。
     */
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

    /**
     * 【未读计数转Map】把 MyBatis 返回的行列表转成 id→计数的 Map
     *
     * 业务作用：统一处理投诉与工单两种未读统计 SQL 的返回结构。
     * 业务规则：rows 为 null 返回空 Map；id 为 null 跳过；数字解析失败（NumberFormatException）忽略该行；cnt 为 null 按 0。
     * 异常情况：无显式抛出，解析异常被吞掉。
     * 注意事项：列名 complaintId/ticketId/cnt 依赖 Mapper XML 中的别名，改动 SQL 别名会导致本方法静默取不到数据。
     */
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

    /**
     * 【批量加载订单】按ID集合查询订单并转成 Map
     *
     * 业务作用：为线程列表补充订单号，避免逐条查询订单。
     * 业务规则：集合为空时直接返回 Map.of()，不查库。
     * 异常情况：无显式抛出。
     * 注意事项：使用 Collectors.toMap 未提供合并函数，若 id_wsh 重复会抛 IllegalStateException（主键唯一时可忽略）。
     */
    private Map<Long, PetOrder> loadOrders(Set<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        return orderMapper.selectList(new LambdaQueryWrapper<PetOrder>()
                        .in(PetOrder::getId_wsh, orderIds))
                .stream()
                .collect(Collectors.toMap(PetOrder::getId_wsh, Function.identity()));
    }

    /**
     * 【工单计数】按商家范围、状态、处理人、更新时间统计工单数
     *
     * 业务作用：为工作台统计与商家列表提供工单数量。
     * 业务规则：merchantIds 为 null 表示不限商家；merchantIds 为空集合直接返回 0（表示无可见商家）；计数结果为 null 时按 0。
     * 异常情况：无显式抛出。
     * 注意事项：merchantIds 与 merchantId 两个参数可同时生效（AND 关系），调用时通常只用其一。
     */
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

    /**
     * 【投诉计数】按商家范围、状态、更新时间统计投诉数
     *
     * 业务作用：为工作台统计与商家列表提供投诉数量。
     * 业务规则：merchantIds 为 null 表示不限商家；为空集合直接返回 0；计数结果为 null 时按 0。
     * 异常情况：无显式抛出。
     * 注意事项：与 countTickets 相比少了处理人与单商家维度参数。
     */
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

    /**
     * 【用户显示名】返回昵称，缺失时回退用户名或用户ID
     *
     * 业务作用：为会话与线程列表填充对方显示名。
     * 业务规则：用户不存在时返回 userId 的字符串形式。
     * 异常情况：无显式抛出。
     * 注意事项：用户不存在时返回的是ID字符串，前端会直接展示数字ID；每个调用点单独查库，存在 N+1 查询。
     */
    /**
     * 业务作用：把 threads() 里的范围条件集中到一处，便于单独验证权限范围。
     * 业务规则：merchantIds 为 null 表示管理员（不限范围）；为空集合时调用方已提前返回。
     */
    private List<Complaint> loadComplaintsInScope(Set<Long> merchantIds) {
        LambdaQueryWrapper<Complaint> wrapper = new LambdaQueryWrapper<>();
        if (merchantIds != null) {
            wrapper.in(Complaint::getMerchant_id_wsh, merchantIds);
        }
        return complaintMapper.selectList(wrapper);
    }

    /**
     * 业务作用：同 loadComplaintsInScope，用于工单。
     * 业务规则：merchantIds 为 null 表示管理员（不限范围）。
     */
    private List<Ticket> loadTicketsInScope(Set<Long> merchantIds) {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        if (merchantIds != null) {
            wrapper.in(Ticket::getMerchant_id_wsh, merchantIds);
        }
        return ticketMapper.selectList(wrapper);
    }

    /**
     * 业务作用：投诉线程要显示「对方」，投诉人视角看到的是被投诉方，需要把 target_id（商家 ID 或看护者 ID）换成对应的登录账号。
     * 业务规则：target_id 为空、或对应商家/看护者已不存在时，映射结果为 null（保持原语义）。
     * 注意事项：旧实现每条投诉查一次商家/看护者（N+1），这里改成两次批量查询。
     */
    private Map<Long, Long> resolveComplaintTargetUserIds(List<Complaint> complaints) {
        Map<Long, Long> result = new HashMap<>();
        if (complaints.isEmpty()) {
            return result;
        }
        Set<Long> keeperIds = new LinkedHashSet<>();
        Set<Long> merchantIds = new LinkedHashSet<>();
        for (Complaint c : complaints) {
            if (c.getTarget_id_wsh() == null) {
                continue;
            }
            if ("keeper".equals(c.getTarget_type_wsh())) {
                keeperIds.add(c.getTarget_id_wsh());
            } else {
                merchantIds.add(c.getTarget_id_wsh());
            }
        }
        Map<Long, Long> keeperUserById = new HashMap<>();
        if (!keeperIds.isEmpty()) {
            for (Keeper keeper : keeperMapper.selectBatchIds(keeperIds)) {
                keeperUserById.put(keeper.getId_wsh(), keeper.getUser_id_wsh());
            }
        }
        Map<Long, Long> merchantUserById = new HashMap<>();
        if (!merchantIds.isEmpty()) {
            for (Merchant merchant : merchantMapper.selectBatchIds(merchantIds)) {
                merchantUserById.put(merchant.getId_wsh(), merchant.getUser_id_wsh());
            }
        }
        for (Complaint c : complaints) {
            if (c.getTarget_id_wsh() == null) {
                continue;
            }
            result.put(c.getId_wsh(), "keeper".equals(c.getTarget_type_wsh())
                    ? keeperUserById.get(c.getTarget_id_wsh())
                    : merchantUserById.get(c.getTarget_id_wsh()));
        }
        return result;
    }

    /**
     * 业务作用：工作台列表要显示「对方」的昵称，一次 IN 查询取回全部用户名， 替代逐条 selectById（N+1）。
     * 业务规则：入参为空集合时不查库。
     */
    private Map<Long, String> loadDisplayNames(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> nameById = new HashMap<>();
        for (User user : userMapper.selectBatchIds(userIds)) {
            nameById.put(user.getId_wsh(),
                    user.getNickname_wsh() != null ? user.getNickname_wsh() : user.getUsername_wsh());
        }
        return nameById;
    }

    /**
     * 业务作用：从批量结果里取用户名，保持旧 displayName 的兜底语义。
     * 业务规则：userId 为 null 返回 null；用户查不到时返回 ID 字符串； 用户存在但昵称与用户名都为空时返回 null（与旧实现一致）。
     */
    private String nameOf(Map<Long, String> nameById, Long userId) {
        if (userId == null) {
            return null;
        }
        if (!nameById.containsKey(userId)) {
            return String.valueOf(userId);
        }
        return nameById.get(userId);
    }
}
