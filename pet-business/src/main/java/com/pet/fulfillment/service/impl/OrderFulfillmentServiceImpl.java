package com.pet.fulfillment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.service.KeeperAttendanceService;
import com.pet.common.BusinessException;
import com.pet.customer.dto.ChatMessageDTO;
import com.pet.customer.entity.ChatMessage;
import com.pet.customer.service.ChatEventBroadcaster;
import com.pet.customer.service.ChatService;
import com.pet.fulfillment.dto.CreateCareRecordRequestDTO;
import com.pet.fulfillment.dto.DailyStatusDTO;
import com.pet.fulfillment.dto.SendOrderMessageRequestDTO;
import com.pet.fulfillment.service.OrderFulfillmentService;
import com.pet.fulfillment.vo.OrderFulfillmentOverviewVO;
import com.pet.operation.entity.FileRecord;
import com.pet.operation.entity.Notification;
import com.pet.operation.service.FileRecordService;
import com.pet.operation.service.NotificationService;
import com.pet.operation.service.impl.MinIoService;
import com.pet.order.dto.OrderDTO;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.pet.dto.CareRecordDTO;
import com.pet.pet.entity.CareRecord;
import com.pet.pet.mapper.CareRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderFulfillmentServiceImpl implements OrderFulfillmentService {

    private static final DateTimeFormatter DATETIME_WITH_SPACE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OrderMapper orderMapper;
    private final KeeperMapper keeperMapper;
    private final MerchantMapper merchantMapper;
    private final CareRecordMapper careRecordMapper;
    private final ChatService chatService;
    private final ChatEventBroadcaster chatEventBroadcaster;
    private final MinIoService minIoService;
    private final FileRecordService fileRecordService;
    private final NotificationService notificationService;
    private final KeeperAttendanceService keeperAttendanceService;

    public OrderFulfillmentServiceImpl(OrderMapper orderMapper,
                                       KeeperMapper keeperMapper,
                                       MerchantMapper merchantMapper,
                                       CareRecordMapper careRecordMapper,
                                       ChatService chatService,
                                       ChatEventBroadcaster chatEventBroadcaster,
                                       MinIoService minIoService,
                                       FileRecordService fileRecordService,
                                       NotificationService notificationService,
                                       KeeperAttendanceService keeperAttendanceService) {
        this.orderMapper = orderMapper;
        this.keeperMapper = keeperMapper;
        this.merchantMapper = merchantMapper;
        this.careRecordMapper = careRecordMapper;
        this.chatService = chatService;
        this.chatEventBroadcaster = chatEventBroadcaster;
        this.minIoService = minIoService;
        this.fileRecordService = fileRecordService;
        this.notificationService = notificationService;
        this.keeperAttendanceService = keeperAttendanceService;
    }

    @Override
    public OrderFulfillmentOverviewVO getOverview(Long userId, boolean admin, Long orderId) {
        OrderAccess access = requireAccess(userId, admin, orderId);
        PetOrder entity = access.order();

        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId_wsh(entity.getId_wsh());
        orderDTO.setOrder_no_wsh(entity.getOrder_no_wsh());
        orderDTO.setOwner_id_wsh(entity.getOwner_id_wsh());
        orderDTO.setPet_id_wsh(entity.getPet_id_wsh());
        orderDTO.setKeeper_id_wsh(entity.getKeeper_id_wsh());
        orderDTO.setMerchant_id_wsh(entity.getMerchant_id_wsh());
        orderDTO.setService_id_wsh(entity.getService_id_wsh());
        orderDTO.setStart_date_wsh(entity.getStart_date_wsh());
        orderDTO.setEnd_date_wsh(entity.getEnd_date_wsh());
        orderDTO.setDays_wsh(entity.getDays_wsh());
        orderDTO.setPrice_per_day_wsh(entity.getPrice_per_day_wsh());
        orderDTO.setTotal_amount_wsh(entity.getTotal_amount_wsh());
        orderDTO.setDiscount_wsh(entity.getDiscount_wsh());
        orderDTO.setFinal_amount_wsh(entity.getFinal_amount_wsh());
        orderDTO.setStatus_wsh(entity.getStatus_wsh());
        orderDTO.setRemark_wsh(entity.getRemark_wsh());
        orderDTO.setCreated_at_wsh(entity.getCreated_at_wsh());

        OrderFulfillmentOverviewVO vo = new OrderFulfillmentOverviewVO();
        vo.setOrder_wsh(orderDTO);
        vo.setRole_wsh(access.roleName());
        vo.setOwner_user_id_wsh(entity.getOwner_id_wsh());
        vo.setKeeper_user_id_wsh(access.keeperUserId());
        vo.setMerchant_user_id_wsh(access.merchantUserId());
        vo.setTimeline_wsh(listTimeline(userId, admin, orderId).stream()
                .map(this::toCareRecordDTO)
                .collect(Collectors.toList()));
        vo.setDaily_status_wsh(getDailyUploadStatus(userId, admin, orderId));
        return vo;
    }

    @Override
    public List<CareRecord> listTimeline(Long userId, boolean admin, Long orderId) {
        requireAccess(userId, admin, orderId);
        return careRecordMapper.selectList(
                new LambdaQueryWrapper<CareRecord>()
                        .eq(CareRecord::getOrder_id_wsh, orderId)
                        .orderByDesc(CareRecord::getRecord_time_wsh)
                        .orderByDesc(CareRecord::getCreated_at_wsh));
    }

    @Override
    public DailyStatusDTO getDailyUploadStatus(Long userId, boolean admin, Long orderId) {
        OrderAccess access = requireAccess(userId, admin, orderId);
        List<CareRecord> records = careRecordMapper.selectList(
                new LambdaQueryWrapper<CareRecord>()
                        .eq(CareRecord::getOrder_id_wsh, orderId)
                        .orderByAsc(CareRecord::getRecord_time_wsh));

        Set<LocalDate> uploadedDays = new LinkedHashSet<>();
        for (CareRecord record : records) {
            if (record.getRecord_time_wsh() != null) {
                uploadedDays.add(record.getRecord_time_wsh().toLocalDate());
            }
        }

        List<String> requiredDays = new ArrayList<>();
        List<String> missingDays = new ArrayList<>();
        LocalDate cursor = access.order().getStart_date_wsh();
        LocalDate end = access.order().getEnd_date_wsh();
        while (cursor != null && end != null && cursor.isBefore(end)) {
            requiredDays.add(cursor.toString());
            if (!uploadedDays.contains(cursor)) {
                missingDays.add(cursor.toString());
            }
            cursor = cursor.plusDays(1);
        }

        DailyStatusDTO status = new DailyStatusDTO();
        status.setRequired_days_wsh(requiredDays);
        status.setUploaded_days_wsh(uploadedDays.stream().map(LocalDate::toString).toList());
        status.setMissing_days_wsh(missingDays);
        status.setComplete_wsh(missingDays.isEmpty());
        status.setRecord_count_wsh(records.size());
        return status;
    }

    @Transactional
    @Override
    public CareRecord createTimelineRecord(Long userId, boolean admin, Long orderId, CreateCareRecordRequestDTO request) {
        OrderAccess access = requireWritableCareAccess(userId, admin, orderId);
        if (request == null) {
            throw new BusinessException(400, "care record cannot be empty");
        }
        if (isBlank(request.getImages_wsh())) {
            throw new BusinessException(400, "care photos cannot be empty");
        }
        CareRecord record = buildCareRecord(access, request.getType_wsh(), request.getContent_wsh(),
                request.getImages_wsh(), request.getRecord_time_wsh());
        careRecordMapper.insert(record);
        notifyOwner(access, "Care update", buildCareNotificationContent(record));
        return record;
    }

    @Transactional
    @Override
    public CareRecord createTimelineRecordWithFiles(Long userId, boolean admin, Long orderId,
                                                    String type, String content, String recordTime,
                                                    List<MultipartFile> files) {
        OrderAccess access = requireWritableCareAccess(userId, admin, orderId);
        List<String> urls = uploadFiles(userId, orderId, "care-records", files);
        if (urls.isEmpty()) {
            throw new BusinessException(400, "care photos cannot be empty");
        }
        CareRecord record = buildCareRecord(access, type, content, String.join(",", urls), parseDateTime(recordTime));
        careRecordMapper.insert(record);
        notifyOwner(access, "Care photos updated", buildCareNotificationContent(record));
        return record;
    }

    @Override
    public List<ChatMessageDTO> listConversation(Long userId, boolean admin, Long orderId, Long otherUserId,
                                                 Long beforeId, Integer size) {
        OrderAccess access = requireAccess(userId, admin, orderId);
        Long resolvedOtherUserId = resolveOtherUserId(access, userId, otherUserId);
        return chatService.getConversation(resolveChatActorUserId(access, userId), resolvedOtherUserId, orderId, beforeId, size);
    }

    @Transactional
    @Override
    public ChatMessageDTO sendMessage(Long userId, boolean admin, Long orderId, SendOrderMessageRequestDTO request) {
        OrderAccess access = requireAccess(userId, admin, orderId);
        if (request == null) {
            throw new BusinessException(400, "message cannot be empty");
        }
        Long fromUserId = resolveChatActorUserId(access, userId);
        Long toUserId = resolveOtherUserId(access, fromUserId, request.getTo_user_id_wsh());
        ChatMessage message = buildMessage(fromUserId, toUserId, orderId, request.getContent_wsh(),
                request.getType_wsh(), request.getFile_url_wsh());
        ChatMessageDTO saved = chatService.sendMessage(message);
        notifyUser(toUserId, "New order message", defaultText(request.getContent_wsh(), "Attachment received"), orderId);
        chatEventBroadcaster.broadcastMessage(saved);
        return saved;
    }

    @Transactional
    @Override
    public ChatMessageDTO sendMessageWithFile(Long userId, boolean admin, Long orderId,
                                              Long toUserId, String content, String type, MultipartFile file) {
        OrderAccess access = requireAccess(userId, admin, orderId);
        Long fromUserId = resolveChatActorUserId(access, userId);
        Long resolvedToUserId = resolveOtherUserId(access, fromUserId, toUserId);
        List<String> urls = uploadFiles(fromUserId, orderId, "order-chat", file == null ? List.of() : List.of(file));
        String fileUrl = urls.isEmpty() ? null : urls.get(0);
        ChatMessage message = buildMessage(fromUserId, resolvedToUserId, orderId, content,
                defaultText(type, "image"), fileUrl);
        ChatMessageDTO saved = chatService.sendMessage(message);
        notifyUser(resolvedToUserId, "New order message", defaultText(content, "Photo received"), orderId);
        chatEventBroadcaster.broadcastMessage(saved);
        return saved;
    }

    @Transactional
    @Override
    public void markConversationAsRead(Long userId, boolean admin, Long orderId, Long otherUserId) {
        OrderAccess access = requireAccess(userId, admin, orderId);
        Long currentUserId = resolveChatActorUserId(access, userId);
        Long resolvedOtherUserId = resolveOtherUserId(access, currentUserId, otherUserId);
        chatService.markConversationAsRead(currentUserId, resolvedOtherUserId, orderId);
    }

    @Override
    public PetOrder requireReadableOrder(Long userId, boolean admin, Long orderId) {
        return requireAccess(userId, admin, orderId).order();
    }

    private CareRecord buildCareRecord(OrderAccess access, String type, String content,
                                       String images, LocalDateTime recordTime) {
        if (isBlank(content) && isBlank(images)) {
            throw new BusinessException(400, "care content or photo cannot be empty");
        }
        LocalDateTime happenedAt = recordTime != null ? recordTime : LocalDateTime.now();
        LocalDate start = access.order().getStart_date_wsh();
        LocalDate end = access.order().getEnd_date_wsh();
        if (start != null && end != null) {
            LocalDate happenedDate = happenedAt.toLocalDate();
            if (happenedDate.isBefore(start) || !happenedDate.isBefore(end)) {
                throw new BusinessException(400, "care record date must be within the service period");
            }
        }

        CareRecord record = new CareRecord();
        record.setOrder_id_wsh(access.order().getId_wsh());
        record.setPet_id_wsh(access.order().getPet_id_wsh());
        record.setKeeper_id_wsh(access.order().getKeeper_id_wsh());
        record.setType_wsh(defaultText(type, "note"));
        record.setContent_wsh(content);
        record.setImages_wsh(images);
        record.setRecord_time_wsh(happenedAt);
        return record;
    }

    private ChatMessage buildMessage(Long fromUserId, Long toUserId, Long orderId,
                                     String content, String type, String fileUrl) {
        if (fromUserId == null) {
            throw new BusinessException(400, "sender cannot be empty");
        }
        if (toUserId == null) {
            throw new BusinessException(400, "receiver cannot be empty");
        }
        if (fromUserId.equals(toUserId)) {
            throw new BusinessException(400, "cannot send message to self");
        }
        if (isBlank(content) && isBlank(fileUrl)) {
            throw new BusinessException(400, "message content or attachment cannot be empty");
        }
        ChatMessage message = new ChatMessage();
        message.setFrom_user_id_wsh(fromUserId);
        message.setTo_user_id_wsh(toUserId);
        message.setOrder_id_wsh(orderId);
        message.setContent_wsh(content);
        message.setType_wsh(defaultText(type, fileUrl == null ? "text" : "image"));
        message.setFile_url_wsh(fileUrl);
        return message;
    }

    private List<String> uploadFiles(Long userId, Long orderId, String directory, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        List<String> urls = new ArrayList<>();
        String objectDirectory = directory + "/" + orderId;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String contentType = file.getContentType();
            if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                throw new BusinessException(400, "uploaded file must be an image");
            }
            String objectName = minIoService.uploadFile(file, objectDirectory);
            FileRecord record = new FileRecord();
            record.setOriginal_name_wsh(file.getOriginalFilename());
            record.setObject_name_wsh(objectName);
            record.setSize_wsh(file.getSize());
            record.setContent_type_wsh(file.getContentType());
            record.setUser_id_wsh(userId);
            fileRecordService.create(record);
            urls.add(minIoService.getFileUrl(objectName));
        }
        return urls;
    }

    private OrderAccess requireWritableCareAccess(Long userId, boolean admin, Long orderId) {
        OrderAccess access = requireAccess(userId, admin, orderId);
        if (!access.admin() && !access.keeperUser() && !access.merchantUser()) {
            throw new BusinessException(403, "only keeper, merchant or admin can upload care records");
        }
        if (!access.admin()) {
            keeperAttendanceService.requireKeeperOnDuty(
                    access.order().getKeeper_id_wsh(),
                    access.order().getMerchant_id_wsh());
        }
        return access;
    }

    private OrderAccess requireAccess(Long userId, boolean admin, Long orderId) {
        if (orderId == null) {
            throw new BusinessException(400, "order id cannot be empty");
        }
        PetOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "order not found");
        }
        Keeper keeper = order.getKeeper_id_wsh() == null ? null : keeperMapper.selectById(order.getKeeper_id_wsh());
        Merchant merchant = order.getMerchant_id_wsh() == null ? null : merchantMapper.selectById(order.getMerchant_id_wsh());
        boolean owner = order.getOwner_id_wsh() != null && order.getOwner_id_wsh().equals(userId);
        boolean keeperUser = keeper != null && keeper.getUser_id_wsh() != null && keeper.getUser_id_wsh().equals(userId);
        boolean merchantUser = merchant != null && merchant.getUser_id_wsh() != null && merchant.getUser_id_wsh().equals(userId);
        OrderAccess access = new OrderAccess(order, keeper, merchant, admin, owner, keeperUser, merchantUser);
        if (!access.canRead()) {
            throw new BusinessException(403, "no permission to access this order");
        }
        return access;
    }

    private Long resolveChatActorUserId(OrderAccess access, Long currentUserId) {
        if (!access.admin()) {
            return currentUserId;
        }
        Long ownerId = access.order().getOwner_id_wsh();
        if (currentUserId != null && access.isParticipantUser(currentUserId)) {
            return currentUserId;
        }
        if (ownerId != null) {
            return ownerId;
        }
        throw new BusinessException(400, "cannot determine order chat sender");
    }

    private Long resolveOtherUserId(OrderAccess access, Long currentUserId, Long requestedUserId) {
        if (requestedUserId != null) {
            if (!access.isParticipantUser(requestedUserId)) {
                throw new BusinessException(403, "receiver is not an order participant");
            }
            if (currentUserId != null && currentUserId.equals(requestedUserId)) {
                throw new BusinessException(400, "cannot send message to self");
            }
            return requestedUserId;
        }
        if (access.owner()) {
            if (access.keeperUserId() != null) {
                return access.keeperUserId();
            }
            if (access.merchantUserId() != null) {
                return access.merchantUserId();
            }
        }
        if (access.keeperUser() || access.merchantUser() || access.admin()) {
            return access.order().getOwner_id_wsh();
        }
        if (currentUserId != null && !currentUserId.equals(access.order().getOwner_id_wsh())) {
            return access.order().getOwner_id_wsh();
        }
        throw new BusinessException(400, "cannot determine order chat receiver");
    }

    private void notifyOwner(OrderAccess access, String title, String content) {
        Long ownerId = access.order().getOwner_id_wsh();
        if (ownerId != null) {
            notifyUser(ownerId, title, content, access.order().getId_wsh());
        }
    }

    private void notifyUser(Long userId, String title, String content, Long orderId) {
        if (userId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUser_id_wsh(userId);
        notification.setTitle_wsh(title);
        notification.setContent_wsh(content);
        notification.setType_wsh("order_fulfillment");
        notification.setRelated_id_wsh(orderId);
        notificationService.create(notification);
    }

    private String buildCareNotificationContent(CareRecord record) {
        return "[" + defaultText(record.getType_wsh(), "note") + "] "
                + defaultText(record.getContent_wsh(), "New care photo uploaded");
    }

    private LocalDateTime parseDateTime(String value) {
        if (isBlank(value)) {
            return null;
        }
        String text = value.trim();
        return text.contains("T")
                ? LocalDateTime.parse(text)
                : LocalDateTime.parse(text, DATETIME_WITH_SPACE);
    }

    private String defaultText(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private CareRecordDTO toCareRecordDTO(CareRecord entity) {
        CareRecordDTO dto = new CareRecordDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setOrder_id_wsh(entity.getOrder_id_wsh());
        dto.setPet_id_wsh(entity.getPet_id_wsh());
        dto.setKeeper_id_wsh(entity.getKeeper_id_wsh());
        dto.setType_wsh(entity.getType_wsh());
        dto.setContent_wsh(entity.getContent_wsh());
        dto.setImages_wsh(entity.getImages_wsh());
        dto.setRecord_time_wsh(entity.getRecord_time_wsh());
        dto.setCreated_at_wsh(entity.getCreated_at_wsh());
        return dto;
    }

    private record OrderAccess(PetOrder order, Keeper keeper, Merchant merchant,
                               boolean admin, boolean owner, boolean keeperUser, boolean merchantUser) {
        boolean canRead() {
            return admin || owner || keeperUser || merchantUser;
        }

        Long keeperUserId() {
            return keeper != null ? keeper.getUser_id_wsh() : null;
        }

        Long merchantUserId() {
            return merchant != null ? merchant.getUser_id_wsh() : null;
        }

        boolean isParticipantUser(Long userId) {
            if (userId == null) {
                return false;
            }
            if (order.getOwner_id_wsh() != null && order.getOwner_id_wsh().equals(userId)) {
                return true;
            }
            if (keeperUserId() != null && keeperUserId().equals(userId)) {
                return true;
            }
            return merchantUserId() != null && merchantUserId().equals(userId);
        }

        String roleName() {
            if (admin) return "admin";
            if (owner) return "owner";
            if (keeperUser) return "keeper";
            if (merchantUser) return "merchant";
            return "unknown";
        }
    }
}
