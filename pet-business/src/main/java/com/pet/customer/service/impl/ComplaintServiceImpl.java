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
import com.pet.customer.service.MerchantCustomerServiceService;
import com.pet.mq.MessageSender;
import com.pet.operation.entity.Notification;
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
                                ChatEventBroadcaster chatEventBroadcaster) {
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
    }

    /**
     * 【业务名称】按投诉人查询投诉列表（实现）
     * 业务作用：根据投诉人 ID 获取其发起的投诉列表，按创建时间倒序。
     * 调用场景：用户查看自己发起的投诉。
     * 调用链：listByOwner() → selectList() → toDTOList()。
     * 数据处理：按 owner_id 匹配，按创建时间倒序。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
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
     * 【业务名称】查询全部投诉（实现）
     * 业务作用：获取所有投诉记录，按创建时间倒序。
     * 调用场景：后台管理。
     * 调用链：listAll() → selectList() → toDTOList()。
     * 数据处理：无条件全量查询。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
    @Override
    public List<ComplaintDTO> listAll() {
        log.info("调用 listAll()");
        List<Complaint> list = complaintMapper.selectList(
                new LambdaQueryWrapper<Complaint>().orderByDesc(Complaint::getCreated_at_wsh));
        return toDTOList(list);
    }

    /**
     * 【业务名称】分页查询投诉（实现）
     * 业务作用：分页查询全部投诉。
     * 调用场景：后台分页管理。
     * 调用链：listPage() → selectPage() → toDTOPage()。
     * 数据处理：分页查询，按创建时间倒序。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
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
     * 【业务名称】按角色分页查询投诉（实现）
     * 业务作用：按角色权限分页查询投诉列表。
     * 调用场景：不同角色查看投诉。
     * 调用链：listPageForStaff() → 确定商家ID范围 → selectPage() → toDTOPage()。
     * 数据处理：管理员查全部；商家只能查自己商家的投诉；客服可查其服务商家的投诉。
     * 业务规则：角色权限隔离。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
    @Override
    public IPage<ComplaintDTO> listPageForStaff(ComplaintListRequestDTO pageParam, Long staffUserId,
                                                boolean admin, boolean merchant, boolean customerService) {
        log.info("调用 listPageForStaff()");
        if (admin) {
            return listPage(pageParam);
        }
        Set<Long> merchantIds = staffMerchantIds(staffUserId, merchant, customerService);
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
     * 【业务名称】构建投诉筛选条件（实现）
     * 业务作用：根据筛选参数构建投诉查询条件。
     * 调用场景：投诉列表筛选。
     * 数据处理：支持状态、商家、关键字筛选。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
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
     * 【业务名称】获取投诉证据详情（实现）
     * 业务作用：获取投诉证据详情。
     * 调用场景：用户查看投诉处理详情。
     * 调用链：getEvidence() → selectById() → buildEvidence()。
     * 数据处理：查询投诉信息，关联订单摘要、聊天记录和护理记录。
     * 业务规则：投诉不存在抛异常。
     * 状态影响：无。
     * 异常情况：投诉不存在抛 BusinessException(404)。
     * 注意事项：无。
     */
    @Override
    public ComplaintEvidenceDTO getEvidence(Long id) {
        log.info("调用 getEvidence()");
        return buildEvidence(requireComplaint(id));
    }

    /**
     * 【业务名称】按角色获取投诉证据详情（实现）
     * 业务作用：按角色权限获取投诉证据详情。
     * 调用场景：内部人员审核投诉。
     * 调用链：getEvidenceForStaff() → assertStaffCanManage() → buildEvidence()。
     * 数据处理：同 getEvidence()，增加权限校验。
     * 业务规则：权限校验。
     * 状态影响：无。
     * 异常情况：无权限抛 BusinessException(403)。
     * 注意事项：无。
     */
    @Override
    public ComplaintEvidenceDTO getEvidenceForStaff(Long id, Long staffUserId,
                                                    boolean admin, boolean merchant, boolean customerService) {
        log.info("调用 getEvidenceForStaff()");
        Complaint complaint = requireComplaint(id);
        assertStaffCanManage(complaint, staffUserId, admin, merchant, customerService);
        return buildEvidence(complaint);
    }

    /**
     * 【业务名称】创建投诉（实现）
     * 业务作用：用户对指定订单发起投诉。
     * 调用场景：用户提交投诉。
     * 调用链：create() → validateOrderComplaint() → resolveMerchantId() → insert() → toDTO()。
     * 数据处理：校验订单和商家 → 创建 pending 投诉。
     * 业务规则：需校验订单归属和商家有效性。
     * 状态影响：新增一条 pending 投诉记录。
     * 异常情况：订单不存在/不归属/商家不存在抛异常。
     * 注意事项：@Transactional 保证事务一致性。
     */
    /**
     * 【业务名称】获取可投诉目标列表（实现）
     * 业务作用：返回当前用户可投诉/举报的对象（订单、消费过的商家、服务过的寄养师）。
     * 调用场景：投诉/举报表单第一步"选择投诉对象"。
     * 调用链：getTargets() → orderMapper.selectList() → merchantMapper.selectBatchIds() → keeperMapper.selectBatchIds()。
     * 数据处理：按 owner 查询最近订单，聚合出商家与寄养师。
     * 业务规则：只返回与当前用户相关的对象。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
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
     * 【业务名称】创建投诉（实现）
     * 业务作用：用户对指定订单或商家发起投诉，含防滥用校验。
     * 调用场景：用户提交投诉。
     * 调用链：create() → validateOrderComplaint() → resolveMerchantId() → 防滥用校验 → insert() → toDTO()。
     * 数据处理：校验→解析目标→防滥用校验→插入 pending 投诉。
     * 业务规则：订单归属校验；商家须为消费过的商家；重复投诉拦截；限流；长度限制。
     * 状态影响：新增 pending 投诉；通知客服。
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
        notifyCsStaff(merchantId, complaint.getId_wsh(), complaint.getTitle_wsh());
        return toDTO(complaint);
    }

    /**
     * 【业务名称】防滥用校验（实现）
     * 业务作用：投诉创建前的长度限制、频率限制、重复投诉拦截、商家归属校验。
     * 调用场景：create() 内调用。
     * 数据处理：按 ownerId/merchantId 计数。
     * 业务规则：标题≤100、内容≤2000；10分钟内最多3条；同商家处理中投诉只能1条；
     * 无订单时商家必须是我消费过的商家。
     * 状态影响：无。
     * 异常情况：违反任一规则抛 BusinessException。
     * 注意事项：无。
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
     * 【业务名称】投诉目标归属校验（实现）
     * 业务作用：当投诉指定寄养师时，校验该寄养师属于所选商家，防止指向他人。
     * 调用场景：create() 内调用。
     * 数据处理：按 target_id 查询寄养师并比对商家。
     * 业务规则：寄养师不存在抛404；不属于所选商家抛403。
     * 状态影响：无。
     * 异常情况：见业务规则。
     * 注意事项：无。
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
     * 【业务名称】新投诉提醒客服（实现）
     * 业务作用：投诉创建后，向服务该商家的所有已授权客服发送站内通知。
     * 调用场景：用户提交投诉后，客服收到待处理提醒。
     * 数据处理：查询商家的 approved 客服并逐一创建通知。
     * 业务规则：商家为空或无需提醒时不发送。
     * 状态影响：新增通知记录。
     * 异常情况：通知失败仅记录日志，不影响主流程。
     * 注意事项：与工单提醒逻辑保持一致。
     */
    private void notifyCsStaff(Long merchantId, Long complaintId, String title) {
        try {
            Set<Long> csUserIds = merchantCustomerServiceService.getApprovedCsUserIds(merchantId);
            for (Long csUserId : csUserIds) {
                if (csUserId == null) {
                    continue;
                }
                Notification notification = new Notification();
                notification.setUser_id_wsh(csUserId);
                notification.setTitle_wsh("新投诉待处理");
                notification.setContent_wsh("收到新投诉：「" + title + "」，请及时受理处理。");
                notification.setType_wsh("complaint");
                notification.setRelated_id_wsh(complaintId);
                notification.setIs_read_wsh(0);
                notificationService.create(notification);
            }
        } catch (Exception e) {
            log.warn("发送新投诉客服提醒失败: complaintId={}", complaintId, e);
        }
    }

    /**
     * 【业务名称】处理投诉（实现）
     * 业务作用：处理投诉，更新状态和结果，发送通知和异步消息。
     * 调用场景：后台管理处理投诉。
     * 调用链：process() → selectById() → processLoaded()。
     * 数据处理：更新状态和结果 → 通知 → 异步消息。
     * 业务规则：仅管理员可调用。
     * 状态影响：投诉状态变更；发送站内通知。
     * 异常情况：投诉不存在抛异常。
     * 注意事项：@Transactional + 事务提交后异步消息。
     */
    @Transactional
    @Override
    public ComplaintDTO process(Long id, String result, String status) {
        log.info("调用 process()");
        return processLoaded(requireComplaint(id), result, status);
    }

    /**
     * 【业务名称】按角色处理投诉（实现）
     * 业务作用：按角色权限处理投诉。
     * 调用场景：内部人员处理投诉。
     * 调用链：processForStaff() → assertStaffCanManage() → processLoaded()。
     * 数据处理：同 process()，增加权限校验。
     * 业务规则：权限校验。
     * 状态影响：同 process()。
     * 异常情况：无权限抛 BusinessException(403)。
     * 注意事项：无。
     */
    @Transactional
    @Override
    public ComplaintDTO processForStaff(Long id, String result, String status, Long staffUserId,
                                        boolean admin, boolean merchant, boolean customerService) {
        log.info("调用 processForStaff()");
        Complaint complaint = requireComplaint(id);
        assertStaffCanManage(complaint, staffUserId, admin, merchant, customerService);
        return processLoaded(complaint, result, status);
    }

    @Transactional
    @Override
    public ComplaintDTO accept(Long id, Long staffUserId, boolean admin, boolean merchant, boolean customerService) {
        log.info("调用 accept(), id={}", id);
        Complaint complaint = requireComplaint(id);
        assertStaffCanManage(complaint, staffUserId, admin, merchant, customerService);
        if (!"pending".equals(complaint.getStatus_wsh())) {
            throw new BusinessException(400, "仅待处理的投诉可受理");
        }
        complaint.setStatus_wsh("processing");
        complaint.setResult_wsh("投诉已受理，正在处理中");
        complaintMapper.updateById(complaint);
        sendComplaintNotification(
                complaint.getOwner_id_wsh(),
                complaint.getId_wsh(),
                "投诉受理通知",
                "您的投诉「" + complaint.getTitle_wsh() + "」已被受理，正在处理中。");
        return toDTO(complaint);
    }

    /**
     * 【业务名称】查询投诉沟通消息（实现）
     * 业务作用：查询某投诉的往来消息。
     * 调用场景：投诉详情展示沟通记录。
     * 调用链：listMessages() → requireComplaint() → 权限校验 → selectList() → toMessageDTO()。
     * 数据处理：投诉人本人或可管理该投诉的内部人员可查看。
     * 业务规则：按创建时间正序返回。
     * 状态影响：无。
     * 异常情况：无权限抛 BusinessException(403)。
     * 注意事项：无。
     */
    @Override
    public List<ComplaintMessageDTO> listMessages(Long id, Long userId,
                                                  boolean admin, boolean merchant, boolean customerService) {
        log.info("调用 listMessages(), complaintId={}", id);
        Complaint complaint = requireComplaint(id);
        if (userId == null || !userId.equals(complaint.getOwner_id_wsh())) {
            assertStaffCanManage(complaint, userId, admin, merchant, customerService);
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
     * 【业务名称】发送投诉沟通消息（实现）
     * 业务作用：投诉人或内部人员发送消息，并站内通知接收方。
     * 调用场景：用户追问 / 客服回复。
     * 调用链：sendMessage() → requireComplaint() → 权限校验 → insert() → 通知。
     * 数据处理：用户发言通知客服组；客服发言通知投诉人。
     * 业务规则：内容必填且≤1000字；仅投诉人与可管理该投诉的内部人员可发言。
     * 状态影响：新增消息；发送站内通知。
     * 异常情况：无权限抛 403；内容为空/超长抛 400。
     * 注意事项：无。
     */
    @Transactional
    @Override
    public ComplaintMessageDTO sendMessage(Long id, Long senderId, String content, String fileUrl,
                                           boolean admin, boolean merchant, boolean customerService) {
        log.info("调用 sendMessage(), complaintId={}", id);
        Complaint complaint = requireComplaint(id);
        boolean isOwner = senderId != null && senderId.equals(complaint.getOwner_id_wsh());
        if (!isOwner) {
            assertStaffCanManage(complaint, senderId, admin, merchant, customerService);
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
            notifyCsStaffNewMessage(complaint.getMerchant_id_wsh(), id, complaint.getTitle_wsh());
        } else {
            message.setTo_user_id_wsh(complaint.getOwner_id_wsh());
            complaintMessageMapper.insert(message);
            chatEventBroadcaster.broadcastThreadMessage(
                    "complaint", id, complaint.getOwner_id_wsh(), senderId, text, file, message.getCreated_at_wsh());
            sendComplaintNotification(
                    complaint.getOwner_id_wsh(),
                    id,
                    "投诉新消息",
                    "您的投诉「" + complaint.getTitle_wsh() + "」有新的客服回复，请查看。");
        }
        return toMessageDTO(message);
    }

    /**
     * 【业务名称】投诉新消息提醒客服（实现）
     * 业务作用：投诉人发言后，通知服务该商家的客服查看。
     * 调用场景：用户追问投诉。
     * 数据处理：查询商家的 approved 客服并逐一创建通知。
     * 业务规则：商家为空或通知失败仅记录日志。
     * 状态影响：新增通知。
     * 异常情况：无。
     * 注意事项：与 notifyCsStaff 逻辑一致。
     */
    private void notifyCsStaffNewMessage(Long merchantId, Long complaintId, String title) {
        try {
            Set<Long> csUserIds = merchantCustomerServiceService.getApprovedCsUserIds(merchantId);
            for (Long csUserId : csUserIds) {
                if (csUserId == null) {
                    continue;
                }
                Notification notification = new Notification();
                notification.setUser_id_wsh(csUserId);
                notification.setTitle_wsh("投诉新消息");
                notification.setContent_wsh("投诉「" + title + "」有新的用户消息，请及时回复。");
                notification.setType_wsh("complaint");
                notification.setRelated_id_wsh(complaintId);
                notification.setIs_read_wsh(0);
                notificationService.create(notification);
            }
        } catch (Exception e) {
            log.warn("发送投诉新消息客服提醒失败: complaintId={}", complaintId, e);
        }
    }

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

    private Complaint requireComplaint(Long id) {
        Complaint complaint = complaintMapper.selectById(id);
        if (complaint == null) {
            throw new BusinessException(404, "投诉不存在");
        }
        return complaint;
    }

    private void assertStaffCanManage(Complaint complaint, Long userId,
                                      boolean admin, boolean merchant, boolean customerService) {
        if (admin) {
            return;
        }
        if (complaint.getMerchant_id_wsh() != null
                && staffMerchantIds(userId, merchant, customerService).contains(complaint.getMerchant_id_wsh())) {
            return;
        }
        throw new BusinessException(403, "无权限操作此商家的投诉");
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

    private Long resolveMerchantId(ComplaintCreateRequestDTO request, PetOrder order) {
        return order != null ? order.getMerchant_id_wsh() : request.getMerchant_id_wsh();
    }

    private void validateMerchantIfProvided(Long merchantId) {
        if (merchantId != null && merchantMapper.selectById(merchantId) == null) {
            throw new BusinessException(404, "商家不存在");
        }
    }

    private Long resolveTargetId(ComplaintCreateRequestDTO request, PetOrder order) {
        if (request.getTarget_id_wsh() != null || order == null) {
            return request.getTarget_id_wsh();
        }
        return order.getKeeper_id_wsh() != null ? order.getKeeper_id_wsh() : order.getMerchant_id_wsh();
    }

    private String resolveTargetType(ComplaintCreateRequestDTO request, PetOrder order) {
        if (request.getTarget_type_wsh() != null && !request.getTarget_type_wsh().isBlank()) {
            return request.getTarget_type_wsh().trim();
        }
        if (order == null) {
            return null;
        }
        return order.getKeeper_id_wsh() != null ? "keeper" : "merchant";
    }

    private ComplaintDTO processLoaded(Complaint complaint, String result, String status) {
        complaint.setStatus_wsh(status);
        complaint.setResult_wsh(result);
        complaintMapper.updateById(complaint);
        sendComplaintNotification(
                complaint.getOwner_id_wsh(),
                complaint.getId_wsh(),
                "投诉处理通知",
                "您的投诉「" + complaint.getTitle_wsh() + "」已处理，结果：" + result);
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

    private ComplaintEvidenceDTO buildEvidence(Complaint complaint) {
        ComplaintDTO complaintDTO = toDTO(complaint);
        ComplaintEvidenceDTO evidence = new ComplaintEvidenceDTO();
        evidence.setComplaint_wsh(complaintDTO);
        evidence.setSummary_wsh(complaintDTO.getEvidence_summary_wsh());
        evidence.setChat_messages_wsh(listEvidenceChatMessages(complaint.getOrder_id_wsh()));
        evidence.setCare_records_wsh(listEvidenceCareRecords(complaint.getOrder_id_wsh()));
        return evidence;
    }

    private void sendComplaintNotification(Long userId, Long complaintId, String title, String content) {
        try {
            Notification notification = new Notification();
            notification.setUser_id_wsh(userId);
            notification.setTitle_wsh(title);
            notification.setContent_wsh(content);
            notification.setType_wsh("complaint");
            notification.setRelated_id_wsh(complaintId);
            notification.setIs_read_wsh(0);
            notificationService.create(notification);
        } catch (Exception e) {
            log.warn("发送投诉通知失败: complaintId={}", complaintId, e);
        }
    }

    private List<ComplaintDTO> toDTOList(List<Complaint> list) {
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

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

    private long countChatMessages(Long orderId) {
        Long count = chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getOrder_id_wsh, orderId));
        return count == null ? 0L : count;
    }

    private long countCareRecords(Long orderId) {
        Long count = careRecordMapper.selectCount(new LambdaQueryWrapper<CareRecord>()
                .eq(CareRecord::getOrder_id_wsh, orderId));
        return count == null ? 0L : count;
    }

    private java.time.LocalDateTime latestChatTime(Long orderId) {
        ChatMessage message = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getOrder_id_wsh, orderId)
                .orderByDesc(ChatMessage::getCreated_at_wsh)
                .last("LIMIT 1"));
        return message == null ? null : message.getCreated_at_wsh();
    }

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
