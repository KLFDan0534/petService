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

    @Override
    public List<ComplaintDTO> listByOwner(Long ownerId) {
        return toDTOList(complaintMapper.selectList(
                new LambdaQueryWrapper<Complaint>()
                        .eq(Complaint::getOwner_id_wsh, ownerId)
                        .orderByDesc(Complaint::getCreated_at_wsh)));
    }

    @Override
    public List<ComplaintDTO> listAll() {
        return toDTOList(complaintMapper.selectList(
                new LambdaQueryWrapper<Complaint>().orderByDesc(Complaint::getCreated_at_wsh)));
    }

    @Override
    public IPage<ComplaintDTO> listPage(PageRequestDTO pageParam) {
        Page<Complaint> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        IPage<Complaint> result = complaintMapper.selectPage(page,
                new LambdaQueryWrapper<Complaint>().orderByDesc(Complaint::getCreated_at_wsh));
        Page<ComplaintDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(toDTOList(result.getRecords()));
        return dtoPage;
    }

    @Override
    public IPage<ComplaintDTO> listPageForStaff(PageRequestDTO pageParam, Long staffUserId,
                                                boolean admin, boolean merchant, boolean customerService) {
        if (admin) {
            return listPage(pageParam);
        }
        Set<Long> merchantIds = staffMerchantIds(staffUserId, merchant, customerService);
        Page<ComplaintDTO> emptyPage = new Page<>(pageParam.getPage(), pageParam.getSize(), 0);
        if (merchantIds.isEmpty()) {
            emptyPage.setRecords(List.of());
            return emptyPage;
        }
        Page<Complaint> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        IPage<Complaint> result = complaintMapper.selectPage(page,
                new LambdaQueryWrapper<Complaint>()
                        .in(Complaint::getMerchant_id_wsh, merchantIds)
                        .orderByDesc(Complaint::getCreated_at_wsh));
        Page<ComplaintDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(toDTOList(result.getRecords()));
        return dtoPage;
    }

    @Override
    public ComplaintEvidenceDTO getEvidence(Long id) {
        return buildEvidence(requireComplaint(id));
    }

    @Override
    public ComplaintEvidenceDTO getEvidenceForStaff(Long id, Long staffUserId,
                                                    boolean admin, boolean merchant, boolean customerService) {
        Complaint complaint = requireComplaint(id);
        assertStaffCanManage(complaint, staffUserId, admin, merchant, customerService);
        return buildEvidence(complaint);
    }

    @Transactional
    @Override
    public ComplaintDTO create(ComplaintCreateRequestDTO request, Long ownerId) {
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

    @Transactional
    @Override
    public ComplaintDTO process(Long id, String result, String status) {
        return processLoaded(requireComplaint(id), result, status);
    }

    @Transactional
    @Override
    public ComplaintDTO processForStaff(Long id, String result, String status, Long staffUserId,
                                        boolean admin, boolean merchant, boolean customerService) {
        Complaint complaint = requireComplaint(id);
        assertStaffCanManage(complaint, staffUserId, admin, merchant, customerService);
        return processLoaded(complaint, result, status);
    }

    private ComplaintDTO processLoaded(Complaint complaint, String result, String status) {
        complaint.setStatus_wsh(status);
        complaint.setResult_wsh(result);
        complaintMapper.updateById(complaint);
        sendComplaintNotification(complaint.getOwner_id_wsh(), complaint.getId_wsh(),
                "Complaint processed",
                "Complaint " + complaint.getTitle_wsh() + " has been "
                        + ("resolved".equals(status) ? "resolved" : "rejected")
                        + (result != null ? ": " + result : ""));
        Long complaintId = complaint.getId_wsh();
        Runnable task = () -> {
            try {
                messageSender.sendComplaintProcess(complaintId);
            } catch (Exception e) {
                log.warn("Failed to send complaint process message, complaintId: {}", complaintId, e);
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

    private Complaint requireComplaint(Long id) {
        Complaint complaint = complaintMapper.selectById(id);
        if (complaint == null) {
            throw new BusinessException(404, "complaint not found");
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
        throw new BusinessException(403, "no permission for this merchant complaint");
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
            throw new BusinessException(400, "complaint cannot be empty");
        }
        Long orderId = request.getOrder_id_wsh();
        if (orderId == null) {
            return null;
        }
        PetOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "order not found");
        }
        if (ownerId == null || !ownerId.equals(order.getOwner_id_wsh())) {
            throw new BusinessException(403, "only order owner can complain about this order");
        }
        return order;
    }

    private Long resolveMerchantId(ComplaintCreateRequestDTO request, PetOrder order) {
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

    private Long resolveTargetId(ComplaintCreateRequestDTO request, PetOrder order) {
        if (request.getTarget_id_wsh() != null || order == null) {
            return request.getTarget_id_wsh();
        }
        if (order.getKeeper_id_wsh() != null) {
            return order.getKeeper_id_wsh();
        }
        return order.getMerchant_id_wsh();
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

    private ComplaintDTO toDTO(Complaint c) {
        if (c == null) {
            return null;
        }
        ComplaintDTO dto = new ComplaintDTO();
        dto.setId_wsh(c.getId_wsh());
        dto.setOrder_id_wsh(c.getOrder_id_wsh());
        dto.setMerchant_id_wsh(c.getMerchant_id_wsh());
        dto.setOwner_id_wsh(c.getOwner_id_wsh());
        if (c.getOwner_id_wsh() != null) {
            User owner = userMapper.selectById(c.getOwner_id_wsh());
            dto.setOwner_name_wsh(owner != null
                    ? (owner.getNickname_wsh() != null ? owner.getNickname_wsh() : owner.getUsername_wsh())
                    : null);
        }
        dto.setTarget_id_wsh(c.getTarget_id_wsh());
        dto.setTarget_type_wsh(c.getTarget_type_wsh());
        dto.setTitle_wsh(c.getTitle_wsh());
        dto.setContent_wsh(c.getContent_wsh());
        dto.setImages_wsh(c.getImages_wsh());
        dto.setStatus_wsh(c.getStatus_wsh());
        dto.setResult_wsh(c.getResult_wsh());
        dto.setEvidence_summary_wsh(buildEvidenceSummary(c.getOrder_id_wsh()));
        dto.setCreated_at_wsh(c.getCreated_at_wsh());
        return dto;
    }

    private List<ComplaintDTO> toDTOList(List<Complaint> list) {
        if (list == null) {
            return List.of();
        }
        return list.stream().map(this::toDTO).collect(Collectors.toList());
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
            log.warn("Failed to send complaint notification", e);
        }
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
