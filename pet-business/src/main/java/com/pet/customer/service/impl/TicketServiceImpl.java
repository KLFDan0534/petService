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
import com.pet.customer.service.IssueAccess;
import com.pet.customer.service.IssueNotifier;
import com.pet.customer.service.MerchantCustomerServiceService;
import com.pet.customer.service.TicketService;
import com.pet.operation.service.NotificationService;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
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

    /** 工单状态：待处理 */
    private static final String STATUS_PENDING = "pending";
    /** 工单状态：处理中 */
    private static final String STATUS_PROCESSING = "processing";
    /** 工单状态：已解决 */
    private static final String STATUS_RESOLVED = "resolved";
    /** 工单状态：已关闭 */
    private static final String STATUS_CLOSED = "closed";

    private final TicketMapper ticketMapper;
    private final TicketMessageMapper ticketMessageMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final OrderMapper orderMapper;
    private final MerchantMapper merchantMapper;
    private final MerchantCustomerServiceService merchantCustomerServiceService;
    private final ChatEventBroadcaster chatEventBroadcaster;
    /** 与投诉共用的商家范围/可见性判定，避免三处各写一份 */
    private final IssueAccess issueAccess;
    /** 与投诉共用的站内通知投递 */
    private final IssueNotifier issueNotifier;

    public TicketServiceImpl(TicketMapper ticketMapper,
                             TicketMessageMapper ticketMessageMapper,
                             UserMapper userMapper,
                             NotificationService notificationService,
                             OrderMapper orderMapper,
                             MerchantMapper merchantMapper,
                             MerchantCustomerServiceService merchantCustomerServiceService,
                             ChatEventBroadcaster chatEventBroadcaster,
                             IssueAccess issueAccess,
                             IssueNotifier issueNotifier) {
        this.ticketMapper = ticketMapper;
        this.ticketMessageMapper = ticketMessageMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
        this.orderMapper = orderMapper;
        this.merchantMapper = merchantMapper;
        this.merchantCustomerServiceService = merchantCustomerServiceService;
        this.chatEventBroadcaster = chatEventBroadcaster;
        this.issueAccess = issueAccess;
        this.issueNotifier = issueNotifier;
    }

    /**
     * 查询用户发起的工单列表（实现）
     *
     * 业务作用：查询指定用户发起的工单列表。
     */
    @Override
    public List<TicketDTO> listByUser(Long userId) {
        return toDTOList(ticketMapper.selectList(
                new LambdaQueryWrapper<Ticket>()
                        .eq(Ticket::getUser_id_wsh, userId)
                        .orderByDesc(Ticket::getCreated_at_wsh)));
    }

    /**
     * 查询全部工单列表（实现）
     *
     * 业务作用：获取全部工单列表，按创建时间倒序。
     */
    /**
     * 分页查询全部工单（实现）
     *
     * 业务作用：分页查询全部工单列表。
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
     * 按角色分页查询工单（实现）
     *
     * 业务作用：按角色权限分页查询工单列表。
     * 业务规则：角色权限隔离。
     */
    @Override
    public IPage<TicketDTO> listPageForStaff(TicketListRequestDTO pageParam, Long staffUserId,
                                             boolean admin, boolean merchant, boolean customerService) {
        if (admin) {
            return listPage(pageParam);
        }
        Set<Long> merchantIds = issueAccess.staffMerchantIds(staffUserId, merchant, customerService);
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
     * 构建工单筛选查询条件（实现）
     *
     * 业务作用：根据筛选参数与可见商家范围构建工单查询条件。
     * 业务规则：可见商家范围为空时不构建查询（调用方提前返回空页）。
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
     * 业务规则：字符串字段先 trim 再判空，空串不参与过滤；keyword 对 title_wsh 与 content_wsh 做 OR 模糊匹配并用 and 包裹避免污染其它条件。
     * 异常情况：pageParam 为 null 时直接返回，不抛异常。
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
     * 查询工单详情（实现）
     *
     * 业务作用：根据ID查询工单详情。
     * 业务规则：工单不存在抛异常。
     * 异常情况：工单不存在抛 BusinessException(404)。
     */
    /**
     * 按角色查询工单详情（实现）
     *
     * 业务作用：根据ID和角色权限查询工单详情。
     * 业务规则：权限校验。
     * 异常情况：无权限抛 BusinessException(403)。
     */
    @Override
    public TicketDTO getByIdForUser(Long id, Long userId, boolean admin, boolean merchant, boolean customerService) {
        Ticket ticket = getByIdRaw(id);
        assertCanView(ticket, userId, admin, merchant, customerService);
        return toDTO(ticket);
    }

    /**
     * 创建工单（实现）
     *
     * 业务作用：用户提交售后/客服工单，可关联订单或直接指定商家。
     * 业务规则：订单需存在且归用户所属；商家需存在。
     * 异常情况：订单不存在抛 404；非订单主人抛 403；商家不存在抛 404。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public TicketDTO create(Long userId, TicketCreateRequestDTO request) {
        if (request == null) {
            throw new BusinessException(400, "工单内容不能为空");
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
        ticket.setStatus_wsh(STATUS_PENDING);
        ticketMapper.insert(ticket);
        // 工单落库后提醒该商家的授权客服
        issueNotifier.notifyCustomerService("ticket", merchantId, ticket.getId_wsh(),
                "新工单待处理", "收到新工单：「" + ticket.getTitle_wsh() + "」，请及时处理。");
        return toDTO(ticket);
    }

    /**
     * 分配工单（实现）
     *
     * 业务作用：分配工单给处理人，状态变为 processing，通过站内通知告知用户。
     * 调用链：assign() → getByIdRaw() → 幂等判定 → 状态守卫 → 处理人校验 → casAssign() → 通知。
     * 业务规则：仅 pending 状态可分配；同一处理人重复指派视为幂等（返回当前数据，不重复通知）。
     * 异常情况：状态不允许抛 400；处理人不存在抛 404；并发被他人抢先抛 400。
     * 注意事项：并发保护靠 casAssign 的条件更新，不能只依赖读后 if 判断。
     */
    @Transactional
    @Override
    public TicketDTO assign(Long id, Long assigneeId) {
        Ticket ticket = getByIdRaw(id);

        // ① 幂等：已经指派给同一个人，直接返回当前数据（不重复写库、不重复通知）
        if (STATUS_PROCESSING.equals(ticket.getStatus_wsh())
                && Objects.equals(assigneeId, ticket.getAssignee_id_wsh())) {
            return toDTO(ticket);
        }
        // ② 业务状态守卫：只有 pending 可以进入 processing
        if (!STATUS_PENDING.equals(ticket.getStatus_wsh())) {
            throw new BusinessException(400, "当前状态不允许指派");
        }
        User assignee = userMapper.selectById(assigneeId);
        if (assignee == null) {
            throw new BusinessException(404, "处理人不存在");
        }

        // ③ 并发保护：条件更新，只有前态仍是 pending 的那一次能成功
        int affected = ticketMapper.casAssign(id, assigneeId, STATUS_PENDING, STATUS_PROCESSING);
        if (affected == 0) {
            Ticket latest = ticketMapper.selectById(id);
            if (latest != null && STATUS_PROCESSING.equals(latest.getStatus_wsh())) {
                if (Objects.equals(assigneeId, latest.getAssignee_id_wsh())) {
                    // 并发下同一处理人重复提交，语义等价，按幂等返回
                    return toDTO(latest);
                }
                throw new BusinessException(400, "工单已被指派给其他人，请刷新后重试");
            }
            throw new BusinessException(400, "工单状态已变化，请刷新后重试");
        }

        ticket.setAssignee_id_wsh(assigneeId);
        ticket.setStatus_wsh(STATUS_PROCESSING);
        issueNotifier.notifyUser("ticket", ticket.getUser_id_wsh(), ticket.getId_wsh(),
                "工单已受理",
                "您的工单「" + ticket.getTitle_wsh() + "」已受理，正在处理中");
        return toDTO(ticket);
    }

    /**
     * 按角色分配工单（实现）
     *
     * 业务作用：按角色权限分配工单给处理人。
     * 业务规则：权限隔离。
     * 异常情况：无权限抛 BusinessException(403)。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public TicketDTO assignForStaff(Long id, Long assigneeId, Long staffUserId,
                                    boolean admin, boolean merchant, boolean customerService) {
        Ticket ticket = getByIdRaw(id);
        assertCanManage(ticket, staffUserId, admin, merchant, customerService);
        return assign(id, assigneeId);
    }

    /**
     * 解决工单（实现）
     *
     * 业务作用：解决 pending 或 processing 状态的工单，状态变为 resolved，自动添加解决消息。
     * 调用链：resolve() → getByIdRaw() → 幂等/终态判定 → 状态守卫 → casResolve() → addMessage() → 通知。
     * 业务规则：仅 pending 或 processing 可解决；已解决且结论相同视为幂等；结论不同拒绝覆盖。
     * 异常情况：状态不允许抛 400；并发冲突抛 400。
     * 注意事项：留言与通知只在 CAS 成功后执行，重复调用不会产生第二条留言或第二条通知。
     */
    @Transactional
    @Override
    public TicketDTO resolve(Long id, String result) {
        Ticket ticket = getByIdRaw(id);

        // ① 幂等：已解决且结论一致，直接返回，不写库、不补留言、不发通知
        if (STATUS_RESOLVED.equals(ticket.getStatus_wsh())) {
            if (Objects.equals(ticket.getResult_wsh(), result)) {
                return toDTO(ticket);
            }
            throw new BusinessException(400, "工单已处理完成，不能修改处理结果");
        }
        // ② 终态守卫：已关闭的工单不能再被解决
        if (STATUS_CLOSED.equals(ticket.getStatus_wsh())) {
            throw new BusinessException(400, "工单已关闭，不能再次处理");
        }
        // ③ 业务状态守卫：只允许 pending / processing 进入 resolved
        if (!STATUS_PENDING.equals(ticket.getStatus_wsh())
                && !STATUS_PROCESSING.equals(ticket.getStatus_wsh())) {
            throw new BusinessException(400, "当前状态不允许解决");
        }

        // ④ 并发保护
        int affected = ticketMapper.casResolve(id, result, STATUS_RESOLVED, STATUS_PENDING, STATUS_PROCESSING);
        if (affected == 0) {
            Ticket latest = ticketMapper.selectById(id);
            if (latest != null && STATUS_RESOLVED.equals(latest.getStatus_wsh())
                    && Objects.equals(latest.getResult_wsh(), result)) {
                // 并发下同一结论重复提交，语义等价，按幂等返回
                return toDTO(latest);
            }
            throw new BusinessException(400, "工单状态已变化，请刷新后重试");
        }

        ticket.setStatus_wsh(STATUS_RESOLVED);
        ticket.setResult_wsh(result);
        // 解决时自动补一条处理结果留言（仅在真正迁移成功时执行）
        addMessage(id,
                ticket.getAssignee_id_wsh() != null ? ticket.getAssignee_id_wsh() : ticket.getUser_id_wsh(),
                result != null ? result : "工单已处理完成",
                null);
        issueNotifier.notifyUser("ticket", ticket.getUser_id_wsh(), ticket.getId_wsh(),
                "工单处理完成",
                "您的工单「" + ticket.getTitle_wsh() + "」已处理完成");
        return toDTO(ticket);
    }

    /**
     * 按角色解决工单（实现）
     *
     * 业务作用：按角色权限解决工单。
     * 业务规则：权限隔离。
     * 异常情况：无权限抛 BusinessException(403)。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public TicketDTO resolveForStaff(Long id, String result, Long staffUserId,
                                     boolean admin, boolean merchant, boolean customerService) {
        Ticket ticket = getByIdRaw(id);
        assertCanManage(ticket, staffUserId, admin, merchant, customerService);
        return resolve(id, result);
    }

    /**
     * 关闭工单（实现）
     *
     * 业务作用：关闭已解决的工单，状态变为 closed。
     * 调用链：close() → getByIdRaw() → 幂等判定 → 状态守卫 → casClose() → 通知。
     * 业务规则：仅 resolved 状态可关闭；已关闭重复调用视为幂等。
     * 异常情况：非 resolved 状态抛 400；并发冲突抛 400。
     * 注意事项：关闭后工单不可再被解决（resolve 有终态守卫）。
     */
    @Transactional
    @Override
    public TicketDTO close(Long id) {
        Ticket ticket = getByIdRaw(id);

        // ① 幂等：已关闭直接返回，不重复通知
        if (STATUS_CLOSED.equals(ticket.getStatus_wsh())) {
            return toDTO(ticket);
        }
        // ② 业务状态守卫：只有 resolved 可以关闭
        if (!STATUS_RESOLVED.equals(ticket.getStatus_wsh())) {
            throw new BusinessException(400, "仅已解决的工单可关闭");
        }

        // ③ 并发保护
        int affected = ticketMapper.casClose(id, STATUS_RESOLVED, STATUS_CLOSED);
        if (affected == 0) {
            Ticket latest = ticketMapper.selectById(id);
            if (latest != null && STATUS_CLOSED.equals(latest.getStatus_wsh())) {
                return toDTO(latest);
            }
            throw new BusinessException(400, "工单状态已变化，请刷新后重试");
        }

        ticket.setStatus_wsh(STATUS_CLOSED);
        issueNotifier.notifyUser("ticket", ticket.getUser_id_wsh(), ticket.getId_wsh(),
                "工单已关闭",
                "您的工单「" + ticket.getTitle_wsh() + "」已关闭");
        return toDTO(ticket);
    }

    /**
     * 按角色关闭工单（实现）
     *
     * 业务作用：按角色权限关闭工单。
     * 业务规则：权限隔离。
     * 异常情况：无权限抛 BusinessException(403)。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public TicketDTO closeForStaff(Long id, Long staffUserId, boolean admin, boolean merchant, boolean customerService) {
        Ticket ticket = getByIdRaw(id);
        assertCanManage(ticket, staffUserId, admin, merchant, customerService);
        return close(id);
    }

    /**
     * 添加工单消息（实现）
     *
     * 业务作用：向工单中添加一条留言消息。
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
     * 广播工单消息给接收方（实现）
     *
     * 业务作用：用户发言时实时推送给该商家客服；客服/内部发言时实时推送给工单主人。
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
     * 按角色添加工单消息（实现）
     *
     * 业务作用：按角色权限向工单中添加留言消息。
     * 业务规则：权限校验。
     * 异常情况：无权限抛 BusinessException(403)。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public TicketMessageDTO addMessageForUser(Long ticketId, Long userId, boolean admin,
                                              boolean merchant, boolean customerService, String content, String fileUrl) {
        Ticket ticket = getByIdRaw(ticketId);
        assertCanView(ticket, userId, admin, merchant, customerService);
        return addMessage(ticketId, userId, content, fileUrl);
    }

    /**
     * 获取工单留言列表（实现）
     *
     * 业务作用：获取工单的所有留言，按创建时间正序排列。
     */
    @Override
    public List<TicketMessageDTO> listMessages(Long ticketId) {
        return toMessageDTOList(ticketMessageMapper.selectList(
                new LambdaQueryWrapper<TicketMessage>()
                        .eq(TicketMessage::getTicket_id_wsh, ticketId)
                        .orderByAsc(TicketMessage::getCreated_at_wsh)));
    }

    /**
     * 按角色获取工单留言列表（实现）
     *
     * 业务作用：按角色权限获取工单留言列表。
     * 业务规则：权限校验。
     * 异常情况：无权限抛 BusinessException(403)。
     */
    @Override
    public List<TicketMessageDTO> listMessagesForUser(Long ticketId, Long userId,
                                                      boolean admin, boolean merchant, boolean customerService) {
        Ticket ticket = getByIdRaw(ticketId);
        assertCanView(ticket, userId, admin, merchant, customerService);
        if (userId != null) {
            ticketMessageMapper.markReadByTicketId(ticketId, userId);
        }
        return listMessages(ticketId);
    }

    /**
     * 【工单可见性校验】判断用户是否有权查看该工单
     *
     * 业务作用：管理员、工单发起人、以及工单所属商家的商家/客服可查看，其余拒绝。
     * 业务规则：判定逻辑在 IssueAccess 中（与投诉共用），这里只保留工单自己的 403 文案。
     * 异常情况：不满足条件抛 BusinessException(403, "无权查看该工单")。
     * 注意事项：工单未关联商家时，只有管理员与发起人可访问（沿用既有行为）。
     */
    private void assertCanView(Ticket ticket, Long userId, boolean admin, boolean merchant, boolean customerService) {
        if (!issueAccess.canView(ticket.getUser_id_wsh(), ticket.getMerchant_id_wsh(),
                userId, admin, merchant, customerService)) {
            throw new BusinessException(403, "无权查看该工单");
        }
    }

    /**
     * 【工单管理权限校验】判断内部人员是否有权操作该工单
     *
     * 业务作用：分配、解决、关闭等写操作前的权限校验。
     * 业务规则：与 assertCanView 的区别是不允许工单发起人通过，写操作必须走内部人员。
     * 异常情况：不满足条件抛 BusinessException(403, "无权操作此商家的工单")。
     */
    private void assertCanManage(Ticket ticket, Long userId, boolean admin, boolean merchant, boolean customerService) {
        if (!issueAccess.canManageMerchant(ticket.getMerchant_id_wsh(), userId, admin, merchant, customerService)) {
            throw new BusinessException(403, "无权操作此商家的工单");
        }
    }

    /**
     * 【校验工单关联订单】确认订单存在且归当前用户所有
     *
     * 业务作用：创建工单时若指定了订单，则校验订单存在性与归属，并返回订单用于解析商家。
     * 业务规则：order_id_wsh 为 null 时直接返回 null 不做校验；订单必须存在；订单 owner_id_wsh 必须等于当前用户。
     * 异常情况：订单不存在抛 BusinessException(404)；非订单主人抛 BusinessException(403)。
     * 注意事项：订单存在时商家ID以订单上的 merchant_id_wsh 为准，请求中的 merchant_id_wsh 会被忽略。
     */
    private PetOrder validateOrderTicket(TicketCreateRequestDTO request, Long userId) {
        if (request.getOrder_id_wsh() == null) {
            return null;
        }
        PetOrder order = orderMapper.selectById(request.getOrder_id_wsh());
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (userId == null || !userId.equals(order.getOwner_id_wsh())) {
            throw new BusinessException(403, "只能为自己名下的订单提交工单");
        }
        return order;
    }

    /**
     * 【解析工单商家】确定工单归属的商家ID
     *
     * 业务作用：关联订单时取订单的商家，未关联订单时取请求中显式指定的商家。
     * 业务规则：order 非 null 优先取 order.merchant_id_wsh，即使其为 null 也不会回退到请求参数。
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
     * 业务规则：merchantId 为 null 时直接返回。
     * 异常情况：商家不存在抛 BusinessException(404)。
     * 注意事项：只校验存在性，不校验商家状态（如是否已停用）。
     */
    private void validateMerchantIfProvided(Long merchantId) {
        if (merchantId == null) {
            return;
        }
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(404, "商家不存在");
        }
    }

    /**
     * 【按ID取工单实体】查询工单并在不存在时抛错
     *
     * 业务作用：本类内部统一的工单加载入口，保证后续逻辑拿到的实体一定存在。
     * 业务规则：工单必须存在。
     * 异常情况：工单不存在抛 BusinessException(404)。
     * 注意事项：id 为 null 时 selectById 返回 null，同样抛 404 而非 400。
     */
    private Ticket getByIdRaw(Long id) {
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new BusinessException(404, "工单不存在");
        }
        return ticket;
    }

    /**
     * 工单实体转DTO
     *
     * 业务作用：将工单实体转换为DTO。
     * 业务规则：入参为null时返回null。
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
     * 业务规则：入参为 null 时返回空列表。
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
     * 业务规则：入参为 null 时返回 null。
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
     * 业务规则：入参为 null 时返回空列表。
     */
    private List<TicketMessageDTO> toMessageDTOList(List<TicketMessage> list) {
        if (list == null) {
            return List.of();
        }
        return list.stream().map(this::toMessageDTO).collect(Collectors.toList());
    }

}
