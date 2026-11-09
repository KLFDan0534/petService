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
import com.pet.customer.dto.TicketListRequestDTO;
import com.pet.customer.dto.TicketMessageDTO;
import com.pet.customer.entity.Ticket;
import com.pet.customer.entity.TicketMessage;
import com.pet.customer.mapper.TicketMapper;
import com.pet.customer.mapper.TicketMessageMapper;
import com.pet.customer.service.ChatEventBroadcaster;
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

import java.time.LocalDateTime;
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
    private final ChatEventBroadcaster chatEventBroadcaster;

    /**
     * 【构造方法】注入工单相关依赖
     *
     * 业务作用：由 Spring 注入工单、工单留言、用户、通知、订单、商家、客服授权与聊天广播等依赖。
     *
     * 调用场景：Spring 容器启动创建该 Service Bean 时调用。
     *
     * 调用链：Spring 容器 → 本构造方法 → 各 final 字段赋值。
     *
     * 数据处理：仅字段赋值。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：依赖缺失时由 Spring 抛出 Bean 创建异常。
     *
     * 注意事项：依赖为 final 字段，新增依赖需同步修改构造参数。
     */
    public TicketServiceImpl(TicketMapper ticketMapper,
                             TicketMessageMapper ticketMessageMapper,
                             UserMapper userMapper,
                             NotificationService notificationService,
                             OrderMapper orderMapper,
                             MerchantMapper merchantMapper,
                             MerchantCustomerServiceService merchantCustomerServiceService,
                             ChatEventBroadcaster chatEventBroadcaster) {
        this.ticketMapper = ticketMapper;
        this.ticketMessageMapper = ticketMessageMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
        this.orderMapper = orderMapper;
        this.merchantMapper = merchantMapper;
        this.merchantCustomerServiceService = merchantCustomerServiceService;
        this.chatEventBroadcaster = chatEventBroadcaster;
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
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<Ticket>()
                .orderByDesc(Ticket::getCreated_at_wsh);
        if (pageParam instanceof TicketListRequestDTO ticketParam) {
            applyTicketFilters(wrapper, ticketParam);
        }
        Page<Ticket> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        IPage<Ticket> result = ticketMapper.selectPage(page, wrapper);
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
    public IPage<TicketDTO> listPageForStaff(TicketListRequestDTO pageParam, Long staffUserId,
                                             boolean admin, boolean merchant, boolean customerService) {
        if (admin) {
            return listPage(pageParam);
        }
        Set<Long> merchantIds = staffMerchantIds(staffUserId, merchant, customerService);
        Page<TicketDTO> emptyPage = new Page<>(pageParam.getPage(), pageParam.getSize(), 0);
        // 无可见商家直接返回空页，避免拼出空 in 条件
        if (merchantIds.isEmpty()) {
            emptyPage.setRecords(List.of());
            return emptyPage;
        }
        Page<Ticket> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        IPage<Ticket> result = ticketMapper.selectPage(page, buildFilterWrapper(pageParam, merchantIds));
        Page<TicketDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(toDTOList(result.getRecords()));
        return dtoPage;
    }

    /**
     * 【业务名称】构建工单筛选查询条件（实现）
     * 业务作用：根据筛选参数与可见商家范围构建工单查询条件。
     * 调用场景：客服/商家/管理员筛选工单列表。
     * 数据处理：支持状态、分类、优先级、处理人、商家、关键字筛选。
     * 业务规则：可见商家范围为空时不构建查询（调用方提前返回空页）。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：关键字对标题/内容做 LIKE 模糊匹配。
     */
    private LambdaQueryWrapper<Ticket> buildFilterWrapper(TicketListRequestDTO pageParam, Set<Long> merchantIds) {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<Ticket>()
                .in(Ticket::getMerchant_id_wsh, merchantIds)
                .orderByDesc(Ticket::getCreated_at_wsh);
        applyTicketFilters(wrapper, pageParam);
        return wrapper;
    }

    /**
     * 【追加工单筛选条件】把请求DTO中的筛选字段拼到查询条件上
     *
     * 业务作用：为工单列表查询统一追加状态、分类、优先级、处理人、商家、关键字等条件。
     *
     * 调用场景：listPage 与 buildFilterWrapper 构建查询条件时调用。
     *
     * 调用链：listPage / buildFilterWrapper → 本方法 → LambdaQueryWrapper。
     *
     * 数据处理：读取 status_wsh、category_wsh、priority_wsh、assignee_id_wsh、merchant_id_wsh、keyword，分别追加 eq 或 and(like or like)。
     *
     * 业务规则：字符串字段先 trim 再判空，空串不参与过滤；keyword 对 title_wsh 与 content_wsh 做 OR 模糊匹配并用 and 包裹避免污染其它条件。
     *
     * 状态影响：无，仅修改查询条件对象。
     *
     * 异常情况：pageParam 为 null 时直接返回，不抛异常。
     *
     * 注意事项：本方法只追加条件不做权限过滤，商家范围由调用方先通过 in 限定。
     */
    private void applyTicketFilters(LambdaQueryWrapper<Ticket> wrapper, TicketListRequestDTO pageParam) {
        if (pageParam == null) {
            return;
        }
        if (pageParam.getStatus_wsh() != null && !pageParam.getStatus_wsh().isBlank()) {
            wrapper.eq(Ticket::getStatus_wsh, pageParam.getStatus_wsh().trim());
        }
        if (pageParam.getCategory_wsh() != null && !pageParam.getCategory_wsh().isBlank()) {
            wrapper.eq(Ticket::getCategory_wsh, pageParam.getCategory_wsh().trim());
        }
        if (pageParam.getPriority_wsh() != null && !pageParam.getPriority_wsh().isBlank()) {
            wrapper.eq(Ticket::getPriority_wsh, pageParam.getPriority_wsh().trim());
        }
        if (pageParam.getAssignee_id_wsh() != null) {
            wrapper.eq(Ticket::getAssignee_id_wsh, pageParam.getAssignee_id_wsh());
        }
        if (pageParam.getMerchant_id_wsh() != null) {
            wrapper.eq(Ticket::getMerchant_id_wsh, pageParam.getMerchant_id_wsh());
        }
        // 关键字对标题与内容做 OR 模糊匹配
        if (pageParam.getKeyword() != null && !pageParam.getKeyword().isBlank()) {
            String kw = pageParam.getKeyword().trim();
            wrapper.and(w -> w.like(Ticket::getTitle_wsh, kw)
                    .or()
                    .like(Ticket::getContent_wsh, kw));
        }
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
        // 先校验订单归属，商家以订单为准
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
        // 工单落库后提醒该商家的授权客服
        notifyCsStaff(merchantId, ticket.getId_wsh(), ticket.getTitle_wsh());
        return toDTO(ticket);
    }

    /**
     * 【业务名称】新工单提醒客服（实现）
     * 业务作用：工单创建后，向服务该商家的所有已授权客服发送站内通知。
     * 调用场景：用户提交工单后，客服收到待处理提醒。
     * 数据处理：查询商家的 approved 客服并逐一创建通知。
     * 业务规则：商家为空或无需提醒时不发送。
     * 状态影响：新增通知记录。
     * 异常情况：通知失败仅记录日志，不影响主流程。
     * 注意事项：与投诉提醒逻辑保持一致。
     */
    private void notifyCsStaff(Long merchantId, Long ticketId, String title) {
        try {
            // 取该商家已授权的客服逐个发站内通知
            Set<Long> csUserIds = merchantCustomerServiceService.getApprovedCsUserIds(merchantId);
            for (Long csUserId : csUserIds) {
                if (csUserId == null) {
                    continue;
                }
                Notification notification = new Notification();
                notification.setUser_id_wsh(csUserId);
                notification.setTitle_wsh("新工单待处理");
                notification.setContent_wsh("收到新工单：「" + title + "」，请及时处理。");
                notification.setType_wsh("ticket");
                notification.setRelated_id_wsh(ticketId);
                notification.setIs_read_wsh(0);
                notificationService.create(notification);
            }
        } catch (Exception e) {
            log.warn("发送新工单客服提醒失败: ticketId={}", ticketId, e);
        }
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
        // 解决时自动补一条处理结果留言
        addMessage(id,
                ticket.getAssignee_id_wsh() != null ? ticket.getAssignee_id_wsh() : ticket.getUser_id_wsh(),
                result != null ? result : "Ticket has been resolved",
                null);
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
    public TicketMessageDTO addMessage(Long ticketId, Long userId, String content, String fileUrl) {
        String text = content == null ? null : content.trim();
        String file = fileUrl == null ? null : fileUrl.trim();
        // 文字与附件至少填一项
        if ((text == null || text.isEmpty()) && (file == null || file.isEmpty())) {
            throw new BusinessException(400, "消息内容不能为空");
        }
        TicketMessage msg = new TicketMessage();
        msg.setTicket_id_wsh(ticketId);
        msg.setUser_id_wsh(userId);
        msg.setContent_wsh(text);
        msg.setFile_url_wsh(file);
        msg.setIs_read_wsh(0);
        ticketMessageMapper.insert(msg);
        broadcastTicketMessage(ticketId, userId, text, file, msg.getCreated_at_wsh());
        return toMessageDTO(msg);
    }

    /**
     * 【业务名称】广播工单消息给接收方（实现）
     * 业务作用：用户发言时实时推送给该商家客服；客服/内部发言时实时推送给工单主人。
     * 调用场景：工单留言后 SSE 实时红点提醒。
     * 数据处理：取工单及其商家，判断发送方身份后确定接收方。
     * 业务规则：通知失败仅记录日志，不影响主流程。
     */
    private void broadcastTicketMessage(Long ticketId, Long userId, String content, String fileUrl, LocalDateTime createdAt) {
        try {
            Ticket ticket = ticketMapper.selectById(ticketId);
            if (ticket == null) {
                return;
            }
            // 发起人发言推给客服，内部发言推给工单主人
            boolean isOwner = userId != null && userId.equals(ticket.getUser_id_wsh());
            if (isOwner) {
                Set<Long> csUserIds = merchantCustomerServiceService.getApprovedCsUserIds(ticket.getMerchant_id_wsh());
                if (csUserIds != null) {
                    for (Long csUserId : csUserIds) {
                        if (csUserId == null || csUserId.equals(userId)) {
                            continue;
                        }
                        chatEventBroadcaster.broadcastThreadMessage(
                                "ticket", ticketId, csUserId, userId, content, fileUrl, createdAt);
                    }
                }
            } else if (ticket.getUser_id_wsh() != null) {
                chatEventBroadcaster.broadcastThreadMessage(
                        "ticket", ticketId, ticket.getUser_id_wsh(), userId, content, fileUrl, createdAt);
            }
        } catch (Exception e) {
            log.warn("广播工单消息失败: {}", e.getMessage());
        }
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
                                              boolean merchant, boolean customerService, String content, String fileUrl) {
        Ticket ticket = getByIdRaw(ticketId);
        assertVisible(ticket, userId, admin, merchant, customerService);
        return addMessage(ticketId, userId, content, fileUrl);
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
        if (userId != null) {
            ticketMessageMapper.markReadByTicketId(ticketId, userId);
        }
        return listMessages(ticketId);
    }

    /**
     * 【工单可见性校验】判断用户是否有权查看该工单
     *
     * 业务作用：管理员、工单发起人、以及工单所属商家的商家/客服可查看，其余拒绝。
     *
     * 调用场景：getByIdForUser、addMessageForUser、listMessagesForUser 中调用。
     *
     * 调用链：getByIdForUser / addMessageForUser / listMessagesForUser → 本方法 → staffMerchantIds。
     *
     * 数据处理：输入 Ticket、userId 与三个角色标记 → 无输出，校验失败抛异常。
     *
     * 业务规则：admin 直接放行；user_id_wsh 等于当前用户放行；工单 merchant_id_wsh 在 staffMerchantIds 集合内放行。
     *
     * 状态影响：无。
     *
     * 异常情况：不满足任一条件抛 BusinessException(403)。
     *
     * 注意事项：工单未关联商家（merchant_id_wsh 为 null）时，只有管理员与发起人可访问。
     */
    private void assertVisible(Ticket ticket, Long userId, boolean admin, boolean merchant, boolean customerService) {
        if (admin) {
            return;
        }
        // 工单发起人本人可查看
        if (ticket.getUser_id_wsh() != null && ticket.getUser_id_wsh().equals(userId)) {
            return;
        }
        if (ticket.getMerchant_id_wsh() != null
                && staffMerchantIds(userId, merchant, customerService).contains(ticket.getMerchant_id_wsh())) {
            return;
        }
        throw new BusinessException(403, "no permission to access this ticket");
    }

    /**
     * 【工单管理权限校验】判断内部人员是否有权操作该工单
     *
     * 业务作用：分配、解决、关闭等写操作前的权限校验，仅管理员或该商家范围内的商家/客服可通过。
     *
     * 调用场景：assignForStaff、resolveForStaff、closeForStaff 中调用。
     *
     * 调用链：assignForStaff / resolveForStaff / closeForStaff → 本方法 → staffMerchantIds。
     *
     * 数据处理：输入 Ticket、userId 与三个角色标记 → 无输出。
     *
     * 业务规则：admin 放行；否则工单 merchant_id_wsh 必须属于该用户可见商家集合。
     *
     * 状态影响：无。
     *
     * 异常情况：不满足条件抛 BusinessException(403)。
     *
     * 注意事项：与 assertVisible 的区别是不允许工单发起人通过，写操作必须走内部人员。
     */
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

    /**
     * 【计算可见商家集合】返回内部人员可管理的商家ID集合
     *
     * 业务作用：商家角色返回其本人拥有的商家，客服角色返回其已授权的服务商家，二者合并去重。
     *
     * 调用场景：assertVisible、assertStaffCanManage、listPageForStaff 中调用。
     *
     * 调用链：assertVisible / assertStaffCanManage / listPageForStaff → 本方法 → merchantMapper.selectOne / merchantCustomerServiceService.getApprovedMerchantIds。
     *
     * 数据处理：按 merchant 标记查 merchant 表（user_id_wsh 匹配，LIMIT 1），按 customerService 标记取授权商家，合并进 LinkedHashSet。
     *
     * 业务规则：userId 为 null 返回空集合；同时具备商家与客服身份时取并集。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：无显式抛出。
     *
     * 注意事项：商家只取一条记录（LIMIT 1），同一用户拥有多个商家时仅第一个生效；每次校验都重新查库，无缓存。
     */
    private Set<Long> staffMerchantIds(Long userId, boolean merchant, boolean customerService) {
        if (userId == null) {
            return Set.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        // 商家身份只取其名下第一个商家
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

    /**
     * 【校验工单关联订单】确认订单存在且归当前用户所有
     *
     * 业务作用：创建工单时若指定了订单，则校验订单存在性与归属，并返回订单用于解析商家。
     *
     * 调用场景：create 在创建工单前调用。
     *
     * 调用链：create → 本方法 → orderMapper.selectById。
     *
     * 数据处理：输入 TicketCreateRequestDTO 与 userId → 输出 PetOrder（未指定订单时为 null）。
     *
     * 业务规则：order_id_wsh 为 null 时直接返回 null 不做校验；订单必须存在；订单 owner_id_wsh 必须等于当前用户。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：订单不存在抛 BusinessException(404)；非订单主人抛 BusinessException(403)。
     *
     * 注意事项：订单存在时商家ID以订单上的 merchant_id_wsh 为准，请求中的 merchant_id_wsh 会被忽略。
     */
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

    /**
     * 【解析工单商家】确定工单归属的商家ID
     *
     * 业务作用：关联订单时取订单的商家，未关联订单时取请求中显式指定的商家。
     *
     * 调用场景：create 在校验订单后调用。
     *
     * 调用链：create → 本方法。
     *
     * 数据处理：输入 TicketCreateRequestDTO 与 PetOrder → 输出 merchantId。
     *
     * 业务规则：order 非 null 优先取 order.merchant_id_wsh，即使其为 null 也不会回退到请求参数。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：订单未指派商家时返回 null，工单将不归属任何商家，后续仅管理员与发起人可见。
     */
    private Long resolveMerchantId(TicketCreateRequestDTO request, PetOrder order) {
        if (order != null) {
            return order.getMerchant_id_wsh();
        }
        return request.getMerchant_id_wsh();
    }

    /**
     * 【校验商家存在】商家ID非空时确认商家存在
     *
     * 业务作用：避免创建出指向不存在商家的工单。
     *
     * 调用场景：create 解析出商家ID后调用。
     *
     * 调用链：create → 本方法 → merchantMapper.selectById。
     *
     * 数据处理：输入 merchantId → 无输出。
     *
     * 业务规则：merchantId 为 null 时直接返回。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：商家不存在抛 BusinessException(404)。
     *
     * 注意事项：只校验存在性，不校验商家状态（如是否已停用）。
     */
    private void validateMerchantIfProvided(Long merchantId) {
        if (merchantId == null) {
            return;
        }
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(404, "merchant not found");
        }
    }

    /**
     * 【按ID取工单实体】查询工单并在不存在时抛错
     *
     * 业务作用：本类内部统一的工单加载入口，保证后续逻辑拿到的实体一定存在。
     *
     * 调用场景：getById、getByIdForUser、assign、resolve、close 及其 ForStaff 变体、addMessageForUser、listMessagesForUser 中调用。
     *
     * 调用链：上述方法 → 本方法 → ticketMapper.selectById。
     *
     * 数据处理：输入 id → 输出 Ticket 实体。
     *
     * 业务规则：工单必须存在。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：工单不存在抛 BusinessException(404)。
     *
     * 注意事项：id 为 null 时 selectById 返回 null，同样抛 404 而非 400。
     */
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

        // 回填用户昵称与商家名称，缺失时留空
        User user = ticket.getUser_id_wsh() == null ? null : userMapper.selectById(ticket.getUser_id_wsh());
        if (user != null) {
            dto.setUser_name_wsh(user.getNickname_wsh() != null ? user.getNickname_wsh() : user.getUsername_wsh());
        }
        Merchant merchant = ticket.getMerchant_id_wsh() == null
                ? null
                : merchantMapper.selectById(ticket.getMerchant_id_wsh());
        if (merchant != null) {
            dto.setMerchant_name_wsh(merchant.getName_wsh());
        }
        return dto;
    }

    /**
     * 【工单列表转DTO】批量把 Ticket 实体转为 TicketDTO
     *
     * 业务作用：列表接口统一做实体到 DTO 的转换。
     *
     * 调用场景：listByUser、listAll、listPage、listPageForStaff 中调用。
     *
     * 调用链：上述方法 → 本方法 → toDTO。
     *
     * 数据处理：stream 映射逐条转换。
     *
     * 业务规则：入参为 null 时返回空列表。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：toDTO 内部会按工单逐条查用户与商家，列表较大时存在 N+1 查询。
     */
    private List<TicketDTO> toDTOList(List<Ticket> list) {
        if (list == null) {
            return List.of();
        }
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 【工单留言转DTO】把 TicketMessage 实体字段拷贝为 DTO
     *
     * 业务作用：留言输出时屏蔽实体字段。
     *
     * 调用场景：addMessage 返回结果时调用，以及 toMessageDTOList 内部调用。
     *
     * 调用链：addMessage / toMessageDTOList → 本方法。
     *
     * 数据处理：拷贝 id_wsh、ticket_id_wsh、user_id_wsh、content_wsh、file_url_wsh、created_at_wsh。
     *
     * 业务规则：入参为 null 时返回 null。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：不返回 is_read_wsh 与用户昵称，前端需另取用户信息。
     */
    private TicketMessageDTO toMessageDTO(TicketMessage msg) {
        if (msg == null) {
            return null;
        }
        TicketMessageDTO dto = new TicketMessageDTO();
        dto.setId_wsh(msg.getId_wsh());
        dto.setTicket_id_wsh(msg.getTicket_id_wsh());
        dto.setUser_id_wsh(msg.getUser_id_wsh());
        dto.setContent_wsh(msg.getContent_wsh());
        dto.setFile_url_wsh(msg.getFile_url_wsh());
        dto.setCreated_at_wsh(msg.getCreated_at_wsh());
        return dto;
    }

    /**
     * 【留言列表转DTO】批量把 TicketMessage 转为 DTO
     *
     * 业务作用：留言列表接口统一转换。
     *
     * 调用场景：listMessages 中调用。
     *
     * 调用链：listMessages → 本方法 → toMessageDTO。
     *
     * 数据处理：stream 映射逐条转换。
     *
     * 业务规则：入参为 null 时返回空列表。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：无。
     */
    private List<TicketMessageDTO> toMessageDTOList(List<TicketMessage> list) {
        if (list == null) {
            return List.of();
        }
        return list.stream().map(this::toMessageDTO).collect(Collectors.toList());
    }

    /**
     * 【发送工单通知】向指定用户写入一条工单类站内通知
     *
     * 业务作用：工单分配、解决、关闭等状态变更后通知工单发起人。
     *
     * 调用场景：assign、resolve、close 在状态更新后调用。
     *
     * 调用链：assign / resolve / close → 本方法 → notificationService.create。
     *
     * 数据处理：写入 notification（user_id_wsh、title_wsh、content_wsh、type_wsh=ticket、related_id_wsh=工单ID、is_read_wsh=0）。
     *
     * 业务规则：无。
     *
     * 状态影响：写 notification 表。
     *
     * 异常情况：内部 catch Exception 仅 log.warn，通知失败不影响工单状态变更。
     *
     * 注意事项：userId 为 null 时未做判空直接写库，可能产生 user_id 为空的通知记录；与 OrderFulfillmentServiceImpl.notifyUser 的失败策略不一致。
     */
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
            log.warn("发送工单通知失败", e);
        }
    }
}
