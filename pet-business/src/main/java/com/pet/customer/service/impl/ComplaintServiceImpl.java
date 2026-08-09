package com.pet.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.common.BusinessException;
import com.pet.common.PageRequestDTO;
import com.pet.customer.dto.ChatMessageDTO;
import com.pet.customer.dto.ComplaintCreateRequestDTO;
import com.pet.customer.dto.ComplaintDTO;
import com.pet.customer.dto.ComplaintEvidenceDTO;
import com.pet.customer.dto.ComplaintEvidenceSummaryDTO;
import com.pet.customer.entity.ChatMessage;
import com.pet.customer.entity.Complaint;
import com.pet.customer.mapper.ChatMessageMapper;
import com.pet.customer.mapper.ComplaintMapper;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ComplaintServiceImpl implements ComplaintService {

    private static final int EVIDENCE_CHAT_LIMIT = 50;
    private static final int EVIDENCE_CARE_LIMIT = 20;

    private final ComplaintMapper complaintMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final OrderMapper orderMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final CareRecordMapper careRecordMapper;
    private final MerchantMapper merchantMapper;
    private final MerchantCustomerServiceService merchantCustomerServiceService;
    private final MessageSender messageSender;

    public ComplaintServiceImpl(ComplaintMapper complaintMapper,
                                UserMapper userMapper,
                                NotificationService notificationService,
                                OrderMapper orderMapper,
                                ChatMessageMapper chatMessageMapper,
                                CareRecordMapper careRecordMapper,
                                MerchantMapper merchantMapper,
                                MerchantCustomerServiceService merchantCustomerServiceService,
                                MessageSender messageSender) {
        this.complaintMapper = complaintMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
        this.orderMapper = orderMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.careRecordMapper = careRecordMapper;
        this.merchantMapper = merchantMapper;
        this.merchantCustomerServiceService = merchantCustomerServiceService;
        this.messageSender = messageSender;
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
        Page<Complaint> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        IPage<Complaint> pageResult = complaintMapper.selectPage(page,
                new LambdaQueryWrapper<Complaint>().orderByDesc(Complaint::getCreated_at_wsh));
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
    public IPage<ComplaintDTO> listPageForStaff(PageRequestDTO pageParam, Long staffUserId,
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
        IPage<Complaint> pageResult = complaintMapper.selectPage(page,
                new LambdaQueryWrapper<Complaint>()
                        .in(Complaint::getMerchant_id_wsh, merchantIds)
                        .orderByDesc(Complaint::getCreated_at_wsh));
        return pageResult.convert(this::toDTO);
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
    @Transactional
    @Override
    public ComplaintDTO create(ComplaintCreateRequestDTO request, Long ownerId) {
        log.info("调用 create()");
        PetOrder order = validateOrderComplaint(request, ownerId);
        Long merchantId = resolveMerchantId(request, order);
        validateMerchantIfProvided(merchantId);
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
        complaintMapper.insert(complaint);
        return toDTO(complaint);
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
