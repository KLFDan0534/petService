package com.pet.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.common.BusinessException;
import com.pet.common.PageRequestDTO;
import com.pet.customer.dto.ChatMessageDTO;
import com.pet.customer.dto.ComplaintCreateRequestDTO;
import com.pet.customer.dto.ComplaintDTO;
import com.pet.customer.dto.ComplaintEvidenceDTO;
import com.pet.customer.dto.ComplaintEvidenceSummaryDTO;
import com.pet.customer.dto.ComplaintListRequestDTO;
import com.pet.customer.dto.ComplaintMessageDTO;
import com.pet.customer.dto.ComplaintTargetsDTO;
import com.pet.customer.entity.ChatMessage;
import com.pet.customer.entity.Complaint;
import com.pet.customer.entity.ComplaintMessage;
import com.pet.customer.mapper.ChatMessageMapper;
import com.pet.customer.mapper.ComplaintMapper;
import com.pet.customer.mapper.ComplaintMessageMapper;
import com.pet.customer.service.ChatEventBroadcaster;
import com.pet.customer.service.ComplaintService;
import com.pet.customer.service.IssueAccess;
import com.pet.customer.service.IssueNotifier;
import com.pet.customer.service.MerchantCustomerServiceService;
import com.pet.mq.MessageSender;
import com.pet.operation.service.NotificationService;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.pet.dto.CareRecordDTO;
import com.pet.pet.entity.CareRecord;
import com.pet.pet.mapper.CareRecordMapper;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ComplaintServiceImpl implements ComplaintService {

    private static final int EVIDENCE_CHAT_LIMIT = 50;
    private static final int EVIDENCE_CARE_LIMIT = 20;

    /** 投诉状态：待处理 */
    private static final String STATUS_PENDING = "pending";
    /** 投诉状态：处理中 */
    private static final String STATUS_PROCESSING = "processing";
    /** 投诉状态：已处理（终态） */
    private static final String STATUS_RESOLVED = "resolved";
    /** 投诉状态：已驳回（终态） */
    private static final String STATUS_REJECTED = "rejected";

    private final ComplaintMapper complaintMapper;
    private final ComplaintMessageMapper complaintMessageMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final OrderMapper orderMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final CareRecordMapper careRecordMapper;
    private final MerchantMapper merchantMapper;
    private final KeeperMapper keeperMapper;
    private final MerchantCustomerServiceService merchantCustomerServiceService;
    private final MessageSender messageSender;
    private final ChatEventBroadcaster chatEventBroadcaster;
    /** 与工单共用的商家范围/可见性判定，避免三处各写一份 */
    private final IssueAccess issueAccess;
    /** 与工单共用的站内通知投递 */
    private final IssueNotifier issueNotifier;

    /**
     * 业务作用：由 Spring 注入投诉、订单、聊天、护理记录、商家、寄养师、通知与消息发送等依赖。
     * 注意事项：全部字段为 final，必须经构造器注入，无法使用字段注入。
     */
    public ComplaintServiceImpl(ComplaintMapper complaintMapper,
                                ComplaintMessageMapper complaintMessageMapper,
                                UserMapper userMapper,
                                NotificationService notificationService,
                                OrderMapper orderMapper,
                                ChatMessageMapper chatMessageMapper,
                                CareRecordMapper careRecordMapper,
                                MerchantMapper merchantMapper,
                                KeeperMapper keeperMapper,
                                MerchantCustomerServiceService merchantCustomerServiceService,
                                MessageSender messageSender,
                                ChatEventBroadcaster chatEventBroadcaster,
                                IssueAccess issueAccess,
                                IssueNotifier issueNotifier) {
        this.complaintMapper = complaintMapper;
        this.complaintMessageMapper = complaintMessageMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
        this.orderMapper = orderMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.careRecordMapper = careRecordMapper;
        this.merchantMapper = merchantMapper;
        this.keeperMapper = keeperMapper;
        this.merchantCustomerServiceService = merchantCustomerServiceService;
        this.messageSender = messageSender;
        this.chatEventBroadcaster = chatEventBroadcaster;
        this.issueAccess = issueAccess;
        this.issueNotifier = issueNotifier;
    }

    /**
     * 按投诉人查询投诉列表（实现）
     *
     * 业务作用：根据投诉人 ID 获取其发起的投诉列表，按创建时间倒序。
     */
    @Override
    public List<ComplaintDTO> listByOwner(Long ownerId) {
        log.info("调用 listByOwner()");
        List<Complaint> list = complaintMapper.selectList(
                new LambdaQueryWrapper<Complaint>()
                        .eq(Complaint::getOwner_id_wsh, ownerId)
                        .orderByDesc(Complaint::getCreated_at_wsh));
        return toDTOList(list);
    }

    /**
     * 查询全部投诉（实现）
     *
     * 业务作用：获取所有投诉记录，按创建时间倒序。
     */
    /**
     * 分页查询投诉（实现）
     *
     * 业务作用：分页查询全部投诉。
     */
    @Override
    public IPage<ComplaintDTO> listPage(PageRequestDTO pageParam) {
        log.info("调用 listPage()");
        LambdaQueryWrapper<Complaint> wrapper = new LambdaQueryWrapper<Complaint>()
                .orderByDesc(Complaint::getCreated_at_wsh);
        if (pageParam instanceof ComplaintListRequestDTO complaintParam) {
            applyComplaintFilters(wrapper, complaintParam);
        }
        Page<Complaint> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        IPage<Complaint> pageResult = complaintMapper.selectPage(page, wrapper);
        return pageResult.convert(this::toDTO);
    }

    /**
     * 按角色分页查询投诉（实现）
     *
     * 业务作用：按角色权限分页查询投诉列表。
     * 业务规则：角色权限隔离。
     */
    @Override
    public IPage<ComplaintDTO> listPageForStaff(ComplaintListRequestDTO pageParam, Long staffUserId,
                                                boolean admin, boolean merchant, boolean customerService) {
        log.info("调用 listPageForStaff()");
        if (admin) {
            return listPage(pageParam);
        }
        Set<Long> merchantIds = issueAccess.staffMerchantIds(staffUserId, merchant, customerService);
        if (merchantIds.isEmpty()) {
            Page<ComplaintDTO> emptyPage = new Page<>(pageParam.getPage(), pageParam.getSize(), 0);
            emptyPage.setRecords(List.of());
            return emptyPage;
        }
        Page<Complaint> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        LambdaQueryWrapper<Complaint> wrapper = new LambdaQueryWrapper<Complaint>()
                .in(Complaint::getMerchant_id_wsh, merchantIds)
                .orderByDesc(Complaint::getCreated_at_wsh);
        applyComplaintFilters(wrapper, pageParam);
        IPage<Complaint> pageResult = complaintMapper.selectPage(page, wrapper);
        return pageResult.convert(this::toDTO);
    }

    /**
     * 构建投诉筛选条件（实现）
     *
     * 业务作用：根据筛选参数构建投诉查询条件。
     * 注意事项：关键字对标题/内容做 LIKE 模糊匹配。
     */
    private void applyComplaintFilters(LambdaQueryWrapper<Complaint> wrapper, ComplaintListRequestDTO pageParam) {
        if (pageParam == null) {
            return;
        }
        if (pageParam.getStatus_wsh() != null && !pageParam.getStatus_wsh().isBlank()) {
            wrapper.eq(Complaint::getStatus_wsh, pageParam.getStatus_wsh().trim());
        }
        if (pageParam.getMerchant_id_wsh() != null) {
            wrapper.eq(Complaint::getMerchant_id_wsh, pageParam.getMerchant_id_wsh());
        }
        if (pageParam.getKeyword() != null && !pageParam.getKeyword().isBlank()) {
            String kw = pageParam.getKeyword().trim();
            wrapper.and(w -> w.like(Complaint::getTitle_wsh, kw)
                    .or()
                    .like(Complaint::getContent_wsh, kw));
        }
    }

    /**
     * 获取投诉证据详情（实现）
     *
     * 业务作用：获取投诉证据详情。
     * 业务规则：投诉不存在抛异常。
     * 异常情况：投诉不存在抛 BusinessException(404)。
     */
    /**
     * 按角色获取投诉证据详情（实现）
     *
     * 业务作用：按角色权限获取投诉证据详情。
     * 业务规则：权限校验。
     * 异常情况：无权限抛 BusinessException(403)。
     */
    @Override
    public ComplaintEvidenceDTO getEvidenceForStaff(Long id, Long staffUserId,
                                                    boolean admin, boolean merchant, boolean customerService) {
        log.info("调用 getEvidenceForStaff()");
        Complaint complaint = requireComplaint(id);
        assertCanManage(complaint, staffUserId, admin, merchant, customerService);
        return buildEvidence(complaint);
    }

    /**
     * 创建投诉（实现）
     *
     * 业务作用：用户对指定订单发起投诉。
     * 业务规则：需校验订单归属和商家有效性。
     * 异常情况：订单不存在/不归属/商家不存在抛异常。
     * 注意事项：@Transactional 保证事务一致性。
     */
    /**
     * 获取可投诉目标列表（实现）
     *
     * 业务作用：返回当前用户可投诉/举报的对象（订单、消费过的商家、服务过的寄养师）。
     * 业务规则：只返回与当前用户相关的对象。
     */
    @Override
    public ComplaintTargetsDTO getTargets(Long ownerId) {
        log.info("调用 getTargets(), ownerId={}", ownerId);
        ComplaintTargetsDTO result = new ComplaintTargetsDTO();
        List<PetOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getOwner_id_wsh, ownerId)
                        .orderByDesc(PetOrder::getCreated_at_wsh)
                        .last("LIMIT 50"));
        if (orders.isEmpty()) {
            result.setOrders_wsh(List.of());
            result.setMerchants_wsh(List.of());
            result.setKeepers_wsh(List.of());
            return result;
        }

        // 商家名称缓存
        Set<Long> merchantIds = orders.stream()
                .map(PetOrder::getMerchant_id_wsh)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Merchant> merchantMap = merchantIds.isEmpty() ? Map.of()
                : merchantMapper.selectBatchIds(merchantIds).stream()
                .collect(Collectors.toMap(Merchant::getId_wsh, m -> m, (a, b) -> a));

        // 1) 订单目标
        List<ComplaintTargetsDTO.OrderTarget> orderTargets = new ArrayList<>();
        for (PetOrder o : orders) {
            ComplaintTargetsDTO.OrderTarget t = new ComplaintTargetsDTO.OrderTarget();
            t.setId_wsh(o.getId_wsh());
            t.setOrder_no_wsh(o.getOrder_no_wsh());
            t.setStatus_wsh(o.getStatus_wsh());
            t.setMerchant_id_wsh(o.getMerchant_id_wsh());
            Merchant m = o.getMerchant_id_wsh() != null ? merchantMap.get(o.getMerchant_id_wsh()) : null;
            t.setMerchant_name_wsh(m != null ? m.getName_wsh() : null);
            t.setKeeper_id_wsh(o.getKeeper_id_wsh());
            t.setCreated_at_wsh(o.getCreated_at_wsh());
            orderTargets.add(t);
        }
        result.setOrders_wsh(orderTargets);

        // 2) 商家目标（我消费过的商家）
        List<ComplaintTargetsDTO.MerchantTarget> merchantTargets = new ArrayList<>();
        for (Long mid : merchantIds) {
            Merchant m = merchantMap.get(mid);
            if (m == null || m.getName_wsh() == null) {
                continue;
            }
            ComplaintTargetsDTO.MerchantTarget mt = new ComplaintTargetsDTO.MerchantTarget();
            mt.setId_wsh(mid);
            mt.setName_wsh(m.getName_wsh());
            long count = orders.stream().filter(o -> mid.equals(o.getMerchant_id_wsh())).count();
            mt.setOrder_count_wsh(count);
            orders.stream()
                    .filter(o -> mid.equals(o.getMerchant_id_wsh()))
                    .max(Comparator.comparing(PetOrder::getCreated_at_wsh, Comparator.nullsFirst(Comparator.naturalOrder())))
                    .ifPresent(o -> mt.setLast_order_at_wsh(o.getCreated_at_wsh()));
            merchantTargets.add(mt);
        }
        result.setMerchants_wsh(merchantTargets);

        // 3) 寄养师目标（服务过我的寄养师）
        Set<Long> keeperIds = orders.stream()
                .map(PetOrder::getKeeper_id_wsh)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Keeper> keeperMap = keeperIds.isEmpty() ? Map.of()
                : keeperMapper.selectBatchIds(keeperIds).stream()
                .collect(Collectors.toMap(Keeper::getId_wsh, k -> k, (a, b) -> a));
        List<ComplaintTargetsDTO.KeeperTarget> keeperTargets = new ArrayList<>();
        for (Long kid : keeperIds) {
            Keeper k = keeperMap.get(kid);
            if (k == null || k.getName_wsh() == null) {
                continue;
            }
            ComplaintTargetsDTO.KeeperTarget kt = new ComplaintTargetsDTO.KeeperTarget();
            kt.setId_wsh(kid);
            kt.setName_wsh(k.getName_wsh());
            kt.setMerchant_id_wsh(k.getMerchant_id_wsh());
            Merchant m = k.getMerchant_id_wsh() != null ? merchantMap.get(k.getMerchant_id_wsh()) : null;
            kt.setMerchant_name_wsh(m != null ? m.getName_wsh() : null);
            keeperTargets.add(kt);
        }
        result.setKeepers_wsh(keeperTargets);
        return result;
    }

    /**
     * 创建投诉（实现）
     *
     * 业务作用：用户对指定订单或商家发起投诉，含防滥用校验。
     * 业务规则：订单归属校验；商家须为消费过的商家；重复投诉拦截；限流；长度限制。
     * 异常情况：见各校验的 BusinessException。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public ComplaintDTO create(ComplaintCreateRequestDTO request, Long ownerId) {
        log.info("调用 create()");
        PetOrder order = validateOrderComplaint(request, ownerId);
        Long merchantId = resolveMerchantId(request, order);
        validateMerchantIfProvided(merchantId);
        assertCanComplain(order, merchantId, request, ownerId);
        Complaint complaint = new Complaint();
        complaint.setTitle_wsh(request.getTitle_wsh());
        complaint.setContent_wsh(request.getContent_wsh());
        complaint.setOrder_id_wsh(request.getOrder_id_wsh());
        complaint.setMerchant_id_wsh(merchantId);
        complaint.setTarget_id_wsh(resolveTargetId(request, order));
        complaint.setTarget_type_wsh(resolveTargetType(request, order));
        complaint.setImages_wsh(request.getImages_wsh());
        complaint.setOwner_id_wsh(ownerId);
        complaint.setStatus_wsh("pending");
        validateTargetBelongsToMerchant(complaint);
        complaintMapper.insert(complaint);
        issueNotifier.notifyCustomerService("complaint", merchantId, complaint.getId_wsh(),
                "新投诉待处理", "收到新投诉：「" + complaint.getTitle_wsh() + "」，请及时受理处理。");
        return toDTO(complaint);
    }

    /**
     * 防滥用校验（实现）
     *
     * 业务作用：投诉创建前的长度限制、频率限制、重复投诉拦截、商家归属校验。
     * 业务规则：标题≤100、内容≤2000；10分钟内最多3条；同商家处理中投诉只能1条； 无订单时商家必须是我消费过的商家。
     * 异常情况：违反任一规则抛 BusinessException。
     */
    private void assertCanComplain(PetOrder order, Long merchantId,
                                   ComplaintCreateRequestDTO request, Long ownerId) {
        String title = request.getTitle_wsh() == null ? "" : request.getTitle_wsh().trim();
        String content = request.getContent_wsh() == null ? "" : request.getContent_wsh().trim();
        if (title.length() > 100) {
            throw new BusinessException(400, "投诉标题不能超过100字");
        }
        if (content.length() > 2000) {
            throw new BusinessException(400, "投诉内容不能超过2000字");
        }
        Long recentCount = complaintMapper.selectCount(new LambdaQueryWrapper<Complaint>()
                .eq(Complaint::getOwner_id_wsh, ownerId)
                .gt(Complaint::getCreated_at_wsh, LocalDateTime.now().minusMinutes(10)));
        if (recentCount != null && recentCount >= 3) {
            throw new BusinessException(429, "投诉提交过于频繁，请稍后再试");
        }
        if (merchantId != null) {
            Long dup = complaintMapper.selectCount(new LambdaQueryWrapper<Complaint>()
                    .eq(Complaint::getOwner_id_wsh, ownerId)
                    .eq(Complaint::getMerchant_id_wsh, merchantId)
                    .in(Complaint::getStatus_wsh, List.of("pending", "processing")));
            if (dup != null && dup > 0) {
                throw new BusinessException(400, "您已对该商家提交过投诉，正在处理中，请勿重复提交");
            }
        }
        if (order == null && merchantId != null) {
            Long cnt = orderMapper.selectCount(new LambdaQueryWrapper<PetOrder>()
                    .eq(PetOrder::getOwner_id_wsh, ownerId)
                    .eq(PetOrder::getMerchant_id_wsh, merchantId));
            if (cnt == null || cnt == 0) {
                throw new BusinessException(403, "只能投诉您消费过的商家");
            }
        }
    }

    /**
     * 投诉目标归属校验（实现）
     *
     * 业务作用：当投诉指定寄养师时，校验该寄养师属于所选商家，防止指向他人。
     * 业务规则：寄养师不存在抛404；不属于所选商家抛403。
     * 异常情况：见业务规则。
     */
    private void validateTargetBelongsToMerchant(Complaint complaint) {
        if (!"keeper".equals(complaint.getTarget_type_wsh()) || complaint.getTarget_id_wsh() == null) {
            return;
        }
        Keeper keeper = keeperMapper.selectById(complaint.getTarget_id_wsh());
        if (keeper == null) {
            throw new BusinessException(404, "寄养师不存在");
        }
        if (complaint.getMerchant_id_wsh() != null
                && keeper.getMerchant_id_wsh() != null
                && !complaint.getMerchant_id_wsh().equals(keeper.getMerchant_id_wsh())) {
            throw new BusinessException(403, "寄养师不属于所选商家");
        }
    }

    /**
     * 按角色处理投诉（实现）
     *
     * 业务作用：按角色权限处理投诉。
     * 业务规则：权限校验。
     * 异常情况：无权限抛 BusinessException(403)。
     */
    @Transactional
    @Override
    public ComplaintDTO processForStaff(Long id, String result, String status, Long staffUserId,
                                        boolean admin, boolean merchant, boolean customerService) {
        log.info("调用 processForStaff()");
        Complaint complaint = requireComplaint(id);
        assertCanManage(complaint, staffUserId, admin, merchant, customerService);
        return processLoaded(complaint, result, status);
    }

    /**
     * 业务作用：把待处理的投诉置为处理中，并给投诉人发送受理通知。
     * 业务规则：仅 status_wsh 为 pending 的投诉可受理，否则返回 400；非管理员需对投诉所属商家有权限。
     * 异常情况：投诉不存在抛 BusinessException(404)；无权限抛 403；状态非 pending 抛 400。
     * 注意事项：方法带 @Transactional，但通知在事务内发送，通知失败仅记录 warn 日志不会回滚。
     */
    @Transactional
    @Override
    public ComplaintDTO accept(Long id, Long staffUserId, boolean admin, boolean merchant, boolean customerService) {
        log.info("调用 accept(), id={}", id);
        Complaint complaint = requireComplaint(id);
        assertCanManage(complaint, staffUserId, admin, merchant, customerService);
        // 条件更新（CAS）：WHERE id=? AND status='pending'，两个客服同时点受理只有一个能成功
        int affected = complaintMapper.casUpdateStatus(
                complaint.getId_wsh(), STATUS_PENDING, STATUS_PROCESSING, "投诉已受理，正在处理中");
        if (affected == 0) {
            throw new BusinessException(400, "仅待处理的投诉可受理");
        }
        complaint.setStatus_wsh(STATUS_PROCESSING);
        complaint.setResult_wsh("投诉已受理，正在处理中");
        issueNotifier.notifyUser("complaint", complaint.getOwner_id_wsh(), complaint.getId_wsh(),
                "投诉受理通知",
                "您的投诉「" + complaint.getTitle_wsh() + "」已被受理，正在处理中。");
        return toDTO(complaint);
    }

    /**
     * 查询投诉沟通消息（实现）
     *
     * 业务作用：查询某投诉的往来消息。
     * 业务规则：按创建时间正序返回。
     * 异常情况：无权限抛 BusinessException(403)。
     */
    @Override
    public List<ComplaintMessageDTO> listMessages(Long id, Long userId,
                                                  boolean admin, boolean merchant, boolean customerService) {
        log.info("调用 listMessages(), complaintId={}", id);
        Complaint complaint = requireComplaint(id);
        if (userId == null || !userId.equals(complaint.getOwner_id_wsh())) {
            assertCanManage(complaint, userId, admin, merchant, customerService);
        }
        if (userId != null) {
            complaintMessageMapper.markReadByComplaintId(id, userId);
        }
        return complaintMessageMapper.selectList(
                        new LambdaQueryWrapper<ComplaintMessage>()
                                .eq(ComplaintMessage::getComplaint_id_wsh, id)
                                .orderByAsc(ComplaintMessage::getCreated_at_wsh))
                .stream()
                .map(this::toMessageDTO)
                .collect(Collectors.toList());
    }

    /**
     * 发送投诉沟通消息（实现）
     *
     * 业务作用：投诉人或内部人员发送消息，并站内通知接收方。
     * 业务规则：内容必填且≤1000字；仅投诉人与可管理该投诉的内部人员可发言。
     * 异常情况：无权限抛 403；内容为空/超长抛 400。
     */
    @Transactional
    @Override
    public ComplaintMessageDTO sendMessage(Long id, Long senderId, String content, String fileUrl,
                                           boolean admin, boolean merchant, boolean customerService) {
        log.info("调用 sendMessage(), complaintId={}", id);
        Complaint complaint = requireComplaint(id);
        boolean isOwner = senderId != null && senderId.equals(complaint.getOwner_id_wsh());
        if (!isOwner) {
            assertCanManage(complaint, senderId, admin, merchant, customerService);
        }
        String text = content == null ? null : content.trim();
        String file = fileUrl == null ? null : fileUrl.trim();
        if ((text == null || text.isEmpty()) && (file == null || file.isEmpty())) {
            throw new BusinessException(400, "消息内容不能为空");
        }
        if (text != null && text.length() > 1000) {
            throw new BusinessException(400, "消息不能超过1000字");
        }
        ComplaintMessage message = new ComplaintMessage();
        message.setComplaint_id_wsh(id);
        message.setFrom_user_id_wsh(senderId);
        message.setContent_wsh(text);
        message.setFile_url_wsh(file);
        message.setIs_read_wsh(0);
        if (isOwner) {
            message.setTo_user_id_wsh(null);
            complaintMessageMapper.insert(message);
            Set<Long> csUserIds = merchantCustomerServiceService.getApprovedCsUserIds(complaint.getMerchant_id_wsh());
            for (Long csUserId : csUserIds) {
                if (csUserId == null) {
                    continue;
                }
                chatEventBroadcaster.broadcastThreadMessage("complaint", id, csUserId, senderId, text, file, message.getCreated_at_wsh());
            }
            issueNotifier.notifyCustomerService("complaint", complaint.getMerchant_id_wsh(), id,
                    "投诉新消息", "投诉「" + complaint.getTitle_wsh() + "」有新的用户消息，请及时回复。");
        } else {
            message.setTo_user_id_wsh(complaint.getOwner_id_wsh());
            complaintMessageMapper.insert(message);
            chatEventBroadcaster.broadcastThreadMessage(
                    "complaint", id, complaint.getOwner_id_wsh(), senderId, text, file, message.getCreated_at_wsh());
            issueNotifier.notifyUser("complaint", complaint.getOwner_id_wsh(), id,
                    "投诉新消息",
                    "您的投诉「" + complaint.getTitle_wsh() + "」有新的客服回复，请查看。");
        }
        return toMessageDTO(message);
    }

    /**
     * 业务作用：把 ComplaintMessage 实体转换为 ComplaintMessageDTO，并补全发送人显示名。
     * 业务规则：发送人不存在时 from_user_name_wsh 置空。
     * 注意事项：每条消息都会触发一次 userMapper 查询，列表场景存在 N+1 查询问题。
     */
    private ComplaintMessageDTO toMessageDTO(ComplaintMessage message) {
        ComplaintMessageDTO dto = new ComplaintMessageDTO();
        dto.setId_wsh(message.getId_wsh());
        dto.setComplaint_id_wsh(message.getComplaint_id_wsh());
        dto.setFrom_user_id_wsh(message.getFrom_user_id_wsh());
        if (message.getFrom_user_id_wsh() != null) {
            User sender = userMapper.selectById(message.getFrom_user_id_wsh());
            dto.setFrom_user_name_wsh(sender != null
                    ? (sender.getNickname_wsh() != null ? sender.getNickname_wsh() : sender.getUsername_wsh())
                    : null);
        }
        dto.setTo_user_id_wsh(message.getTo_user_id_wsh());
        dto.setContent_wsh(message.getContent_wsh());
        dto.setFile_url_wsh(message.getFile_url_wsh());
        dto.setIs_read_wsh(message.getIs_read_wsh());
        dto.setCreated_at_wsh(message.getCreated_at_wsh());
        return dto;
    }

    /**
     * 业务作用：按主键加载投诉实体，不存在则抛异常，供各业务方法复用。
     * 业务规则：id 查不到记录即视为资源不存在。
     * 异常情况：记录为空抛 BusinessException(404, "投诉不存在")。
     * 注意事项：未做 id 为空的前置判断，id 为 null 时会直接走数据库查询。
     */
    private Complaint requireComplaint(Long id) {
        Complaint complaint = complaintMapper.selectById(id);
        if (complaint == null) {
            throw new BusinessException(404, "投诉不存在");
        }
        return complaint;
    }

    /**
     * 业务作用：判断当前内部人员是否有权操作该投诉，无权则抛 403。
     * 业务规则：判定本身在 IssueAccess 中（与工单共用），这里只保留投诉自己的 403 文案。
     * 异常情况：无权限抛 BusinessException(403, "无权限操作此商家的投诉")。
     */
    private void assertCanManage(Complaint complaint, Long userId,
                                 boolean admin, boolean merchant, boolean customerService) {
        if (!issueAccess.canManageMerchant(
                complaint.getMerchant_id_wsh(), userId, admin, merchant, customerService)) {
            throw new BusinessException(403, "无权限操作此商家的投诉");
        }
    }

    /**
     * 业务作用：校验请求中的订单存在且属于当前投诉人，并返回订单实体。
     * 数据处理：request 为空抛 400；order_id_wsh 为空时返回 null 表示不关联订单；否则按 id 查订单并比对 owner_id_wsh。
     * 业务规则：只能对本人订单发起投诉。
     * 异常情况：请求为空抛 400；订单不存在抛 404；非本人订单抛 403。
     * 注意事项：返回 null 是合法分支，表示按商家维度投诉而非订单维度。
     */
    private PetOrder validateOrderComplaint(ComplaintCreateRequestDTO request, Long ownerId) {
        if (request == null) {
            throw new BusinessException(400, "投诉内容不能为空");
        }
        Long orderId = request.getOrder_id_wsh();
        if (orderId == null) {
            return null;
        }
        PetOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (ownerId == null || !ownerId.equals(order.getOwner_id_wsh())) {
            throw new BusinessException(403, "无权对非本人订单发起投诉");
        }
        return order;
    }

    /**
     * 业务作用：确定投诉应归属的商家 ID。
     * 业务规则：订单场景下忽略请求传入的商家，以订单为准，防止伪造归属。
     * 注意事项：两者都可能为 null，此时投诉无商家归属，后续客服通知也不会发送。
     */
    private Long resolveMerchantId(ComplaintCreateRequestDTO request, PetOrder order) {
        return order != null ? order.getMerchant_id_wsh() : request.getMerchant_id_wsh();
    }

    /**
     * 业务作用：商家 ID 非空时确认该商家在库中存在。
     * 业务规则：商家 ID 为 null 时跳过校验。
     * 异常情况：商家不存在抛 BusinessException(404, "商家不存在")。
     */
    private void validateMerchantIfProvided(Long merchantId) {
        if (merchantId != null && merchantMapper.selectById(merchantId) == null) {
            throw new BusinessException(404, "商家不存在");
        }
    }

    /**
     * 业务作用：确定投诉/举报指向的具体对象主键（寄养师或商家）。
     * 业务规则：显式传入优先于订单推导。
     * 注意事项：返回值可能为 null，需与 resolveTargetType() 的结果配合解读。
     */
    private Long resolveTargetId(ComplaintCreateRequestDTO request, PetOrder order) {
        if (request.getTarget_id_wsh() != null || order == null) {
            return request.getTarget_id_wsh();
        }
        return order.getKeeper_id_wsh() != null ? order.getKeeper_id_wsh() : order.getMerchant_id_wsh();
    }

    /**
     * 业务作用：确定投诉对象是寄养师（keeper）还是商家（merchant）。
     * 业务规则：显式类型优先；订单中有寄养师即判定为 keeper。
     * 注意事项：与 resolveTargetId() 的判断逻辑必须保持一致，否则会出现类型与 ID 不匹配。
     */
    private String resolveTargetType(ComplaintCreateRequestDTO request, PetOrder order) {
        if (request.getTarget_type_wsh() != null && !request.getTarget_type_wsh().isBlank()) {
            return request.getTarget_type_wsh().trim();
        }
        if (order == null) {
            return null;
        }
        return order.getKeeper_id_wsh() != null ? "keeper" : "merchant";
    }

    /**
     * 业务作用：写入处理结果与状态，通知投诉人，并在事务提交后发送 MQ 消息。
     * 业务规则：状态与结果直接采用调用方入参，不校验状态机流转；MQ 消息仅在事务提交后发送。
     * 异常情况：MQ 发送失败仅记录 warn 日志，不影响主流程。
     * 注意事项：通过 TransactionSynchronizationManager 判断是否存在事务，无事务时立即同步执行消息发送。
     */
    private ComplaintDTO processLoaded(Complaint complaint, String result, String status) {
        String current = complaint.getStatus_wsh();
        String targetResult = result == null ? null : result.trim();

        // 1) 幂等：已经是目标状态且结论一致 → 不写库、不发通知、直接返回当前数据
        if (status.equals(current) && Objects.equals(targetResult, trimResult(complaint.getResult_wsh()))) {
            log.info("投诉 {} 已是 {} 且结论一致，按幂等直接返回", complaint.getId_wsh(), status);
            return toDTO(complaint);
        }
        // 2) 终态不允许再变：resolved ↔ rejected 互转、终态覆盖结论一律拒绝
        if (STATUS_RESOLVED.equals(current) || STATUS_REJECTED.equals(current)) {
            throw new BusinessException(400, "该投诉已处理完成，不能再次处理");
        }
        // 3) 只有非终态（pending / processing）才允许进入终态
        if (!STATUS_PENDING.equals(current) && !STATUS_PROCESSING.equals(current)) {
            throw new BusinessException(400, "当前状态不允许该操作");
        }
        // 4) 条件更新（CAS）：WHERE id=? AND status=<读到的前态>，并发下只有一个能改成功
        int affected = complaintMapper.casUpdateStatus(
                complaint.getId_wsh(), current, status, targetResult);
        if (affected == 0) {
            // 别人先改了：如果改成了同样的结果，按幂等返回；否则要求刷新
            Complaint latest = requireComplaint(complaint.getId_wsh());
            if (status.equals(latest.getStatus_wsh())
                    && Objects.equals(targetResult, trimResult(latest.getResult_wsh()))) {
                log.info("投诉 {} 并发处理结果一致，按幂等返回", complaint.getId_wsh());
                return toDTO(latest);
            }
            throw new BusinessException(400, "该投诉状态已变化，请刷新后重试");
        }
        complaint.setStatus_wsh(status);
        complaint.setResult_wsh(targetResult);

        // 5) 用户通知的唯一投递路径是 MQ（ComplaintProcessHandlerImpl），此处不再同步发通知，
        //    否则一次处理会产生两条内容重复、语言不一致的站内通知。
        Long complaintId = complaint.getId_wsh();
        Runnable task = () -> {
            try {
                messageSender.sendComplaintProcess(complaintId);
            } catch (Exception e) {
                log.warn("发送投诉处理消息失败: complaintId={}", complaintId, e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
        return toDTO(complaint);
    }

    /** 结论比较前统一去空白：null 与 "" 视为相同，避免「只是多了个空格」被判成结果变更。 */
    private String trimResult(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * 业务作用：把投诉主体、证据摘要、聊天记录与护理记录打包成证据 DTO。
     * 业务规则：投诉无关联订单时聊天与护理记录返回空列表。
     * 注意事项：证据摘要由 toDTO() 内部生成，此处只是复用，不会重复查询摘要结构。
     */
    private ComplaintEvidenceDTO buildEvidence(Complaint complaint) {
        ComplaintDTO complaintDTO = toDTO(complaint);
        ComplaintEvidenceDTO evidence = new ComplaintEvidenceDTO();
        evidence.setComplaint_wsh(complaintDTO);
        evidence.setSummary_wsh(complaintDTO.getEvidence_summary_wsh());
        evidence.setChat_messages_wsh(listEvidenceChatMessages(complaint.getOrder_id_wsh()));
        evidence.setCare_records_wsh(listEvidenceCareRecords(complaint.getOrder_id_wsh()));
        return evidence;
    }

    /**
     * 业务作用：批量把 Complaint 实体转换为 ComplaintDTO。
     * 异常情况：入参为 null 时会在 stream() 处抛 NullPointerException。
     * 注意事项：每条记录都会触发用户查询与证据摘要查询，大列表时开销较高。
     */
    private List<ComplaintDTO> toDTOList(List<Complaint> list) {
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 业务作用：把 Complaint 实体转换为 ComplaintDTO，补全投诉人名称与证据摘要。
     * 业务规则：实体为 null 时返回 null。
     * 注意事项：列表转换时每条记录都会额外查询用户与订单，存在 N+1 查询。
     */
    private ComplaintDTO toDTO(Complaint entity) {
        if (entity == null) {
            return null;
        }
        ComplaintDTO dto = new ComplaintDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setOrder_id_wsh(entity.getOrder_id_wsh());
        dto.setMerchant_id_wsh(entity.getMerchant_id_wsh());
        dto.setOwner_id_wsh(entity.getOwner_id_wsh());
        if (entity.getOwner_id_wsh() != null) {
            User owner = userMapper.selectById(entity.getOwner_id_wsh());
            dto.setOwner_name_wsh(owner != null
                    ? (owner.getNickname_wsh() != null ? owner.getNickname_wsh() : owner.getUsername_wsh())
                    : null);
        }
        dto.setTarget_id_wsh(entity.getTarget_id_wsh());
        dto.setTarget_type_wsh(entity.getTarget_type_wsh());
        dto.setTitle_wsh(entity.getTitle_wsh());
        dto.setContent_wsh(entity.getContent_wsh());
        dto.setImages_wsh(entity.getImages_wsh());
        dto.setStatus_wsh(entity.getStatus_wsh());
        dto.setResult_wsh(entity.getResult_wsh());
        dto.setEvidence_summary_wsh(buildEvidenceSummary(entity.getOrder_id_wsh()));
        dto.setCreated_at_wsh(entity.getCreated_at_wsh());
        return dto;
    }

    /**
     * 业务作用：统计投诉关联订单的聊天条数、护理记录条数与最近时间，形成证据摘要。
     * 业务规则：orderId 为空或订单不存在时返回 null。
     * 注意事项：一次调用会触发 4 次统计查询，列表场景需注意性能。
     */
    private ComplaintEvidenceSummaryDTO buildEvidenceSummary(Long orderId) {
        if (orderId == null) {
            return null;
        }
        PetOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            return null;
        }
        ComplaintEvidenceSummaryDTO summary = new ComplaintEvidenceSummaryDTO();
        summary.setOrder_id_wsh(orderId);
        summary.setOrder_no_wsh(order.getOrder_no_wsh());
        summary.setOrder_status_wsh(order.getStatus_wsh());
        summary.setChat_message_count_wsh(countChatMessages(orderId));
        summary.setCare_record_count_wsh(countCareRecords(orderId));
        summary.setLatest_chat_time_wsh(latestChatTime(orderId));
        summary.setLatest_care_record_time_wsh(latestCareRecordTime(orderId));
        return summary;
    }

    /**
     * 业务作用：统计某订单下的聊天消息总条数。
     * 业务规则：统计结果为 null 时按 0 处理。
     */
    private long countChatMessages(Long orderId) {
        Long count = chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getOrder_id_wsh, orderId));
        return count == null ? 0L : count;
    }

    /**
     * 业务作用：统计某订单下的护理记录总条数。
     * 业务规则：统计结果为 null 时按 0 处理。
     */
    private long countCareRecords(Long orderId) {
        Long count = careRecordMapper.selectCount(new LambdaQueryWrapper<CareRecord>()
                .eq(CareRecord::getOrder_id_wsh, orderId));
        return count == null ? 0L : count;
    }

    /**
     * 业务作用：取某订单最新一条聊天消息的创建时间。
     * 业务规则：无消息时返回 null。
     * 注意事项：使用 last("LIMIT 1") 拼接 SQL，依赖数据库方言。
     */
    private java.time.LocalDateTime latestChatTime(Long orderId) {
        ChatMessage message = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getOrder_id_wsh, orderId)
                .orderByDesc(ChatMessage::getCreated_at_wsh)
                .last("LIMIT 1"));
        return message == null ? null : message.getCreated_at_wsh();
    }

    /**
     * 业务作用：取某订单最新一条护理记录的时间。
     * 业务规则：优先返回 record_time_wsh，为空时回退到 created_at_wsh；无记录返回 null。
     * 注意事项：排序用了两个字段，与 countCareRecords 的口径无冲突但需注意与列表查询排序保持一致。
     */
    private java.time.LocalDateTime latestCareRecordTime(Long orderId) {
        CareRecord record = careRecordMapper.selectOne(new LambdaQueryWrapper<CareRecord>()
                .eq(CareRecord::getOrder_id_wsh, orderId)
                .orderByDesc(CareRecord::getRecord_time_wsh)
                .orderByDesc(CareRecord::getCreated_at_wsh)
                .last("LIMIT 1"));
        if (record == null) {
            return null;
        }
        return record.getRecord_time_wsh() != null ? record.getRecord_time_wsh() : record.getCreated_at_wsh();
    }

    /**
     * 业务作用：取订单最近 50 条聊天消息作为投诉证据。
     * 业务规则：orderId 为空返回空列表。
     * 注意事项：返回顺序为倒序（最新在前），与前端展示需对齐。
     */
    private List<ChatMessageDTO> listEvidenceChatMessages(Long orderId) {
        if (orderId == null) {
            return List.of();
        }
        return chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getOrder_id_wsh, orderId)
                        .orderByDesc(ChatMessage::getCreated_at_wsh)
                        .last("LIMIT " + EVIDENCE_CHAT_LIMIT))
                .stream()
                .map(this::toChatDTO)
                .collect(Collectors.toList());
    }

    /**
     * 业务作用：取订单最近 20 条护理记录作为投诉证据。
     * 业务规则：orderId 为空返回空列表。
     * 注意事项：返回顺序为倒序（最新在前）。
     */
    private List<CareRecordDTO> listEvidenceCareRecords(Long orderId) {
        if (orderId == null) {
            return List.of();
        }
        return careRecordMapper.selectList(new LambdaQueryWrapper<CareRecord>()
                        .eq(CareRecord::getOrder_id_wsh, orderId)
                        .orderByDesc(CareRecord::getRecord_time_wsh)
                        .orderByDesc(CareRecord::getCreated_at_wsh)
                        .last("LIMIT " + EVIDENCE_CARE_LIMIT))
                .stream()
                .map(this::toCareDTO)
                .collect(Collectors.toList());
    }

    /**
     * 业务作用：把 ChatMessage 实体转换为 ChatMessageDTO 供证据展示。
     * 注意事项：不补全发送人名称，与 toMessageDTO() 的处理不一致。
     */
    private ChatMessageDTO toChatDTO(ChatMessage message) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId_wsh(message.getId_wsh());
        dto.setFrom_user_id_wsh(message.getFrom_user_id_wsh());
        dto.setTo_user_id_wsh(message.getTo_user_id_wsh());
        dto.setOrder_id_wsh(message.getOrder_id_wsh());
        dto.setContent_wsh(message.getContent_wsh());
        dto.setType_wsh(message.getType_wsh());
        dto.setFile_url_wsh(message.getFile_url_wsh());
        dto.setRead_wsh(message.getRead_wsh());
        dto.setCreated_at_wsh(message.getCreated_at_wsh());
        return dto;
    }

    /**
     * 业务作用：把 CareRecord 实体转换为 CareRecordDTO 供证据展示。
     */
    private CareRecordDTO toCareDTO(CareRecord record) {
        CareRecordDTO dto = new CareRecordDTO();
        dto.setId_wsh(record.getId_wsh());
        dto.setOrder_id_wsh(record.getOrder_id_wsh());
        dto.setPet_id_wsh(record.getPet_id_wsh());
        dto.setKeeper_id_wsh(record.getKeeper_id_wsh());
        dto.setType_wsh(record.getType_wsh());
        dto.setContent_wsh(record.getContent_wsh());
        dto.setImages_wsh(record.getImages_wsh());
        dto.setRecord_time_wsh(record.getRecord_time_wsh());
        dto.setCreated_at_wsh(record.getCreated_at_wsh());
        return dto;
    }
}
