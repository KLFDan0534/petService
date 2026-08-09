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

/**
 * 【业务模块】工单售后管理（实现）
 * 业务作用：提供工单从创建、分配、处理到关闭的完整生命周期管理。
 * 以商家为维度进行权限隔离：管理员可管理所有工单，商家只能管理自己商家的工单，客服可管理其授权服务商家的工单。
 */
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

    /**
     * 【业务名称】查询用户发起的工单列表（实现）
     * 业务作用：查询指定用户发起的工单列表。
     * 调用场景：用户查看自己的工单。
     * 调用链：listByUser() → ticketMapper.selectList()。
     * 数据处理：按 user_id 匹配，按创建时间倒序。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
    @Override
    public List<TicketDTO> listByUser(Long userId) {
        return toDTOList(ticketMapper.selectList(
                new LambdaQueryWrapper<Ticket>()
                        .eq(Ticket::getUser_id_wsh, userId)
                        .orderByDesc(Ticket::getCreated_at_wsh)));
    }

    /**
     * 【业务名称】查询全部工单列表（实现）
     * 业务作用：获取全部工单列表，按创建时间倒序。
     * 调用场景：后台管理。
     * 调用链：listAll() → ticketMapper.selectList()。
     * 数据处理：无条件全量查询。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
    @Override
    public List<TicketDTO> listAll() {
        return toDTOList(ticketMapper.selectList(
                new LambdaQueryWrapper<Ticket>().orderByDesc(Ticket::getCreated_at_wsh)));
    }

    /**
     * 【业务名称】分页查询全部工单（实现）
     * 业务作用：分页查询全部工单列表。
     * 调用场景：后台分页管理。
     * 调用链：listPage() → ticketMapper.selectPage()。
     * 数据处理：分页查询，按创建时间倒序。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
    @Override
    public IPage<TicketDTO> listPage(PageRequestDTO pageParam) {
        Page<Ticket> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        IPage<Ticket> result = ticketMapper.selectPage(page,
                new LambdaQueryWrapper<Ticket>().orderByDesc(Ticket::getCreated_at_wsh));
        Page<TicketDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(toDTOList(result.getRecords()));
        return dtoPage;
    }

    /**
     * 【业务名称】按角色分页查询工单（实现）
     * 业务作用：按角色权限分页查询工单列表。
     * 调用场景：不同角色查看工单。
     * 调用链：listPageForStaff() → staffMerchantIds() → ticketMapper.selectPage()。
     * 数据处理：管理员查全部；商家和客服按商家ID范围过滤。
     * 业务规则：角色权限隔离。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
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

    /**
     * 【业务名称】查询工单详情（实现）
     * 业务作用：根据ID查询工单详情。
     * 调用场景：查看工单详细信息。
     * 调用链：getById() → getByIdRaw() → toDTO()。
     * 数据处理：按ID精确查询。
     * 业务规则：工单不存在抛异常。
     * 状态影响：无。
     * 异常情况：工单不存在抛 BusinessException(404)。
     * 注意事项：无。
     */
    @Override
    public TicketDTO getById(Long id) {
        return toDTO(getByIdRaw(id));
    }

    /**
     * 【业务名称】按角色查询工单详情（实现）
     * 业务作用：根据ID和角色权限查询工单详情。
     * 调用场景：内部人员查看工单详情。
     * 调用链：getByIdForUser() → getByIdRaw() → assertVisible() → toDTO()。
     * 数据处理：同 getById()，增加权限校验。
     * 业务规则：权限校验。
     * 状态影响：无。
     * 异常情况：无权限抛 BusinessException(403)。
     * 注意事项：无。
     */
    @Override
    public TicketDTO getByIdForUser(Long id, Long userId, boolean admin, boolean merchant, boolean customerService) {
        Ticket ticket = getByIdRaw(id);
        assertVisible(ticket, userId, admin, merchant, customerService);
        return toDTO(ticket);
    }

    /**
     * 【业务名称】创建工单（实现）
     * 业务作用：用户提交售后/客服工单，可关联订单或直接指定商家。
     * 调用场景：用户提交工单。
     * 调用链：create() → validateOrderTicket() → resolveMerchantId() → validateMerchantIfProvided() → insert()。
     * 数据处理：校验订单归属→解析商家ID→校验商家→创建 pending 工单。
     * 业务规则：订单需存在且归用户所属；商家需存在。
     * 状态影响：新增一条 pending 工单。
     * 异常情况：订单不存在抛 404；非订单主人抛 403；商家不存在抛 404。
     * 注意事项：@Transactional 保证事务一致性。
     */
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

    /**
     * 【业务名称】分配工单（实现）
     * 业务作用：分配工单给处理人，状态变为 processing，通过站内通知告知用户。
     * 调用场景：后台分配工单。
     * 调用链：assign() → getByIdRaw() → 校验状态 → updateById() → sendTicketNotification()。
     * 数据处理：更新 assignee_id 和状态为 processing。
     * 业务规则：仅 pending 状态可分配；处理人需存在。
     * 状态影响：工单状态 pending → processing。
     * 异常情况：状态不匹配抛 400；处理人不存在抛 404。
     * 注意事项：@Transactional 保证事务一致性。
     */
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

    /**
     * 【业务名称】按角色分配工单（实现）
     * 业务作用：按角色权限分配工单给处理人。
     * 调用场景：内部人员分配工单。
     * 调用链：assignForStaff() → assertStaffCanManage() → assign()。
     * 数据处理：同 assign()，增加权限校验。
     * 业务规则：权限隔离。
     * 状态影响：同 assign()。
     * 异常情况：无权限抛 BusinessException(403)。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public TicketDTO assignForStaff(Long id, Long assigneeId, Long staffUserId,
                                    boolean admin, boolean merchant, boolean customerService) {
        Ticket ticket = getByIdRaw(id);
        assertStaffCanManage(ticket, staffUserId, admin, merchant, customerService);
        return assign(id, assigneeId);
    }

    /**
     * 【业务名称】解决工单（实现）
     * 业务作用：解决 pending 或 processing 状态的工单，状态变为 resolved，自动添加解决消息。
     * 调用场景：客服完成工单处理。
     * 调用链：resolve() → getByIdRaw() → 校验状态 → updateById() → addMessage() → sendTicketNotification()。
     * 数据处理：更新 status=resolved，设置 result，自动添加一条系统解决消息。
     * 业务规则：仅 pending 或 processing 状态可解决。
     * 状态影响：工单状态 → resolved。
     * 异常情况：状态不匹配抛 400。
     * 注意事项：@Transactional 保证事务一致性。
     */
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

    /**
     * 【业务名称】按角色解决工单（实现）
     * 业务作用：按角色权限解决工单。
     * 调用场景：内部人员解决工单。
     * 调用链：resolveForStaff() → assertStaffCanManage() → resolve()。
     * 数据处理：同 resolve()，增加权限校验。
     * 业务规则：权限隔离。
     * 状态影响：同 resolve()。
     * 异常情况：无权限抛 BusinessException(403)。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public TicketDTO resolveForStaff(Long id, String result, Long staffUserId,
                                     boolean admin, boolean merchant, boolean customerService) {
        Ticket ticket = getByIdRaw(id);
        assertStaffCanManage(ticket, staffUserId, admin, merchant, customerService);
        return resolve(id, result);
    }

    /**
     * 【业务名称】关闭工单（实现）
     * 业务作用：关闭已解决的工单，状态变为 closed。
     * 调用场景：用户或管理员关闭工单。
     * 调用链：close() → getByIdRaw() → 校验状态 → updateById() → sendTicketNotification()。
     * 数据处理：更新 status=closed。
     * 业务规则：仅 resolved 状态可关闭。
     * 状态影响：工单状态 resolved → closed。
     * 异常情况：非 resolved 状态抛 400。
     * 注意事项：@Transactional 保证事务一致性。
     */
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

    /**
     * 【业务名称】按角色关闭工单（实现）
     * 业务作用：按角色权限关闭工单。
     * 调用场景：内部人员关闭工单。
     * 调用链：closeForStaff() → assertStaffCanManage() → close()。
     * 数据处理：同 close()，增加权限校验。
     * 业务规则：权限隔离。
     * 状态影响：同 close()。
     * 异常情况：无权限抛 BusinessException(403)。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public TicketDTO closeForStaff(Long id, Long staffUserId, boolean admin, boolean merchant, boolean customerService) {
        Ticket ticket = getByIdRaw(id);
        assertStaffCanManage(ticket, staffUserId, admin, merchant, customerService);
        return close(id);
    }

    /**
     * 【业务名称】添加工单消息（实现）
     * 业务作用：向工单中添加一条留言消息。
     * 调用场景：用户或客服在工单中留言。
     * 调用链：addMessage() → ticketMessageMapper.insert()。
     * 数据处理：插入消息记录。
     * 业务规则：无。
     * 状态影响：新增一条消息记录。
     * 异常情况：无。
     * 注意事项：@Transactional 保证事务一致性。
     */
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

    /**
     * 【业务名称】按角色添加工单消息（实现）
     * 业务作用：按角色权限向工单中添加留言消息。
     * 调用场景：内部人员在工单中留言。
     * 调用链：addMessageForUser() → assertVisible() → addMessage()。
     * 数据处理：同 addMessage()，增加权限校验。
     * 业务规则：权限校验。
     * 状态影响：同 addMessage()。
     * 异常情况：无权限抛 BusinessException(403)。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public TicketMessageDTO addMessageForUser(Long ticketId, Long userId, boolean admin,
                                              boolean merchant, boolean customerService, String content) {
        Ticket ticket = getByIdRaw(ticketId);
        assertVisible(ticket, userId, admin, merchant, customerService);
        return addMessage(ticketId, userId, content);
    }

    /**
     * 【业务名称】获取工单留言列表（实现）
     * 业务作用：获取工单的所有留言，按创建时间正序排列。
     * 调用场景：查看工单沟通记录。
     * 调用链：listMessages() → ticketMessageMapper.selectList()。
     * 数据处理：按 ticket_id 匹配，按创建时间正序。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
    @Override
    public List<TicketMessageDTO> listMessages(Long ticketId) {
        return toMessageDTOList(ticketMessageMapper.selectList(
                new LambdaQueryWrapper<TicketMessage>()
                        .eq(TicketMessage::getTicket_id_wsh, ticketId)
                        .orderByAsc(TicketMessage::getCreated_at_wsh)));
    }

    /**
     * 【业务名称】按角色获取工单留言列表（实现）
     * 业务作用：按角色权限获取工单留言列表。
     * 调用场景：内部人员查看工单沟通记录。
     * 调用链：listMessagesForUser() → assertVisible() → listMessages()。
     * 数据处理：同 listMessages()，增加权限校验。
     * 业务规则：权限校验。
     * 状态影响：无。
     * 异常情况：无权限抛 BusinessException(403)。
     * 注意事项：无。
     */
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

    /**
     * 【业务名称】工单实体转DTO
     * 业务作用：将工单实体转换为DTO。
     * 调用场景：对外暴露工单信息。
     * 调用链：toDTO()。
     * 数据处理：字段拷贝。
     * 业务规则：入参为null时返回null。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
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
