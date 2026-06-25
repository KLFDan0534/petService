package com.pet.fulfillment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.common.BusinessException;
import com.pet.customer.entity.ChatMessage;
import com.pet.customer.service.ChatService;
import com.pet.fulfillment.dto.CreateCareRecordRequest;
import com.pet.fulfillment.dto.SendOrderMessageRequest;
import com.pet.fulfillment.service.OrderFulfillmentService;
import com.pet.operation.entity.FileRecord;
import com.pet.operation.entity.Notification;
import com.pet.operation.service.FileRecordService;
import com.pet.operation.service.NotificationService;
import com.pet.operation.service.impl.MinIoService;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 订单履行服务实现
 * 处理订单护理记录、每日上传状态、文件上传及订单聊天功能
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Service
@Slf4j
public class OrderFulfillmentServiceImpl implements OrderFulfillmentService {

    private final OrderMapper orderMapper;
    private final KeeperMapper keeperMapper;
    private final MerchantMapper merchantMapper;
    private final CareRecordMapper careRecordMapper;
    private final ChatService chatService;
    private final MinIoService minIoService;
    private final FileRecordService fileRecordService;
    private final NotificationService notificationService;
    private static final DateTimeFormatter DATETIME_WITH_SPACE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public OrderFulfillmentServiceImpl(OrderMapper orderMapper,
                                       KeeperMapper keeperMapper,
                                       MerchantMapper merchantMapper,
                                       CareRecordMapper careRecordMapper,
                                       ChatService chatService,
                                       MinIoService minIoService,
                                       FileRecordService fileRecordService,
                                       NotificationService notificationService) {
        this.orderMapper = orderMapper;
        this.keeperMapper = keeperMapper;
        this.merchantMapper = merchantMapper;
        this.careRecordMapper = careRecordMapper;
        this.chatService = chatService;
        this.minIoService = minIoService;
        this.fileRecordService = fileRecordService;
        this.notificationService = notificationService;
    }

    /**
     * 获取订单履行概览（含订单、角色、时间线、每日状态）
     * @param userId 用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @return 概览数据Map
     */
    @Override
    public Map<String, Object> getOverview(Long userId, boolean admin, Long orderId) {
        OrderAccess access = requireAccess(userId, admin, orderId);
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("order_wsh", access.order());
        overview.put("role_wsh", access.roleName());
        overview.put("owner_user_id_wsh", access.order().getOwner_id_wsh());
        overview.put("keeper_user_id_wsh", access.keeper() != null ? access.keeper().getUser_id_wsh() : null);
        overview.put("merchant_user_id_wsh", access.merchant() != null ? access.merchant().getUser_id_wsh() : null);
        overview.put("timeline_wsh", listTimeline(userId, admin, orderId));
        overview.put("daily_status_wsh", getDailyUploadStatus(userId, admin, orderId));
        return overview;
    }

    /**
     * 获取订单护理时间线列表
     * @param userId 用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @return 护理记录列表
     */
    @Override
    public List<CareRecord> listTimeline(Long userId, boolean admin, Long orderId) {
        requireAccess(userId, admin, orderId);
        return careRecordMapper.selectList(
                new LambdaQueryWrapper<CareRecord>()
                        .eq(CareRecord::getOrder_id_wsh, orderId)
                        .orderByDesc(CareRecord::getRecord_time_wsh)
                        .orderByDesc(CareRecord::getCreated_at_wsh));
    }

    /**
     * 获取每日护理上传状态（已上传/缺失日期）
     * @param userId 用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @return 每日上传状态Map
     */
    @Override
    public Map<String, Object> getDailyUploadStatus(Long userId, boolean admin, Long orderId) {
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

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("required_days_wsh", requiredDays);
        status.put("uploaded_days_wsh", uploadedDays.stream().map(LocalDate::toString).toList());
        status.put("missing_days_wsh", missingDays);
        status.put("complete_wsh", missingDays.isEmpty());
        status.put("record_count_wsh", records.size());
        return status;
    }

    /**
     * 创建护理时间线记录
     * @param userId 用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @param request 创建护理记录请求
     * @return 创建的护理记录
     */
    @Transactional
    @Override
    public CareRecord createTimelineRecord(Long userId, boolean admin, Long orderId, CreateCareRecordRequest request) {
        OrderAccess access = requireWritableCareAccess(userId, admin, orderId);
        if (request == null) {
            throw new BusinessException(400, "护理记录不能为空");
        }
        if (isBlank(request.getImages_wsh())) {
            throw new BusinessException(400, "护理照片不能为空");
        }
        CareRecord record = buildCareRecord(access, request.getType_wsh(), request.getContent_wsh(),
                request.getImages_wsh(), request.getRecord_time_wsh());
        careRecordMapper.insert(record);
        notifyOwner(access, "收到护理更新", buildCareNotificationContent(record));
        return record;
    }

    /**
     * 创建护理时间线记录（含文件上传）
     * @param userId 用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @param type 记录类型
     * @param content 记录内容
     * @param recordTime 记录时间
     * @param files 上传文件列表
     * @return 创建的护理记录
     */
    @Transactional
    @Override
    public CareRecord createTimelineRecordWithFiles(Long userId, boolean admin, Long orderId,
                                                    String type, String content, String recordTime,
                                                    List<MultipartFile> files) {
        OrderAccess access = requireWritableCareAccess(userId, admin, orderId);
        List<String> urls = uploadFiles(userId, orderId, "care-records", files);
        if (urls.isEmpty()) {
            throw new BusinessException(400, "护理照片不能为空");
        }
        CareRecord record = buildCareRecord(access, type, content, String.join(",", urls), parseDateTime(recordTime));
        careRecordMapper.insert(record);
        notifyOwner(access, "护理照片已更新", buildCareNotificationContent(record));
        return record;
    }

    /**
     * 获取订单聊天对话列表
     * @param userId 用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @param otherUserId 对方用户ID（选填）
     * @return 聊天消息列表
     */
    @Override
    public List<ChatMessage> listConversation(Long userId, boolean admin, Long orderId, Long otherUserId) {
        OrderAccess access = requireAccess(userId, admin, orderId);
        Long resolvedOtherUserId = resolveOtherUserId(access, userId, otherUserId);
        return chatService.getConversation(userId, resolvedOtherUserId, orderId);
    }

    /**
     * 发送订单消息
     * @param userId 用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @param request 发送消息请求
     * @return 发送后的聊天消息
     */
    @Transactional
    @Override
    public ChatMessage sendMessage(Long userId, boolean admin, Long orderId, SendOrderMessageRequest request) {
        OrderAccess access = requireAccess(userId, admin, orderId);
        if (request == null) {
            throw new BusinessException(400, "消息不能为空");
        }
        Long toUserId = resolveOtherUserId(access, userId, request.getTo_user_id_wsh());
        ChatMessage message = buildMessage(userId, toUserId, orderId, request.getContent_wsh(),
                request.getType_wsh(), request.getFile_url_wsh());
        ChatMessage saved = chatService.sendMessage(message);
        notifyUser(toUserId, "新的订单消息", defaultText(request.getContent_wsh(), "对方发送了附件"), orderId);
        return saved;
    }

    /**
     * 发送订单消息（含文件附件）
     * @param userId 用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @param toUserId 接收方用户ID
     * @param content 消息内容
     * @param type 消息类型
     * @param file 上传文件
     * @return 发送后的聊天消息
     */
    @Transactional
    @Override
    public ChatMessage sendMessageWithFile(Long userId, boolean admin, Long orderId,
                                           Long toUserId, String content, String type, MultipartFile file) {
        OrderAccess access = requireAccess(userId, admin, orderId);
        Long resolvedToUserId = resolveOtherUserId(access, userId, toUserId);
        List<String> urls = uploadFiles(userId, orderId, "order-chat", file == null ? List.of() : List.of(file));
        String fileUrl = urls.isEmpty() ? null : urls.get(0);
        ChatMessage message = buildMessage(userId, resolvedToUserId, orderId, content,
                defaultText(type, "image"), fileUrl);
        ChatMessage saved = chatService.sendMessage(message);
        notifyUser(resolvedToUserId, "新的订单消息", defaultText(content, "对方发送了照片"), orderId);
        return saved;
    }

    /**
     * 校验并获取可读订单
     * @param userId 用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @return 订单实体
     */
    @Override
    public PetOrder requireReadableOrder(Long userId, boolean admin, Long orderId) {
        return requireAccess(userId, admin, orderId).order();
    }

    private CareRecord buildCareRecord(OrderAccess access, String type, String content,
                                       String images, LocalDateTime recordTime) {
        if (isBlank(content) && isBlank(images)) {
            throw new BusinessException(400, "护理内容或图片不能为空");
        }
        LocalDateTime happenedAt = recordTime != null ? recordTime : LocalDateTime.now();
        LocalDate start = access.order().getStart_date_wsh();
        LocalDate end = access.order().getEnd_date_wsh();
        if (start != null && end != null) {
            LocalDate happenedDate = happenedAt.toLocalDate();
            if (happenedDate.isBefore(start) || !happenedDate.isBefore(end)) {
                throw new BusinessException(400, "护理记录日期必须在订单服务日期范围内");
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
        if (toUserId == null) {
            throw new BusinessException(400, "接收人不能为空");
        }
        if (isBlank(content) && isBlank(fileUrl)) {
            throw new BusinessException(400, "消息内容或附件不能为空");
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
                throw new BusinessException(400, "护理照片必须是图片");
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
            throw new BusinessException(403, "只有看护者、商家或管理员可以上传护理记录");
        }
        return access;
    }

    private OrderAccess requireAccess(Long userId, boolean admin, Long orderId) {
        if (orderId == null) {
            throw new BusinessException(400, "订单ID不能为空");
        }
        PetOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        Keeper keeper = order.getKeeper_id_wsh() == null ? null : keeperMapper.selectById(order.getKeeper_id_wsh());
        Merchant merchant = order.getMerchant_id_wsh() == null ? null : merchantMapper.selectById(order.getMerchant_id_wsh());
        boolean owner = order.getOwner_id_wsh() != null && order.getOwner_id_wsh().equals(userId);
        boolean keeperUser = keeper != null && keeper.getUser_id_wsh() != null && keeper.getUser_id_wsh().equals(userId);
        boolean merchantUser = merchant != null && merchant.getUser_id_wsh() != null && merchant.getUser_id_wsh().equals(userId);
        OrderAccess access = new OrderAccess(order, keeper, merchant, admin, owner, keeperUser, merchantUser);
        if (!access.canRead()) {
            throw new BusinessException(403, "无权限访问此订单履行信息");
        }
        return access;
    }

    private Long resolveOtherUserId(OrderAccess access, Long currentUserId, Long requestedUserId) {
        if (requestedUserId != null) {
            if (!access.isParticipantUser(requestedUserId) && !access.admin()) {
                throw new BusinessException(403, "接收人不是订单参与者");
            }
            return requestedUserId;
        }
        if (access.owner()) {
            if (access.keeper() != null && access.keeper().getUser_id_wsh() != null) {
                return access.keeper().getUser_id_wsh();
            }
            if (access.merchant() != null) {
                return access.merchant().getUser_id_wsh();
            }
        }
        if (access.keeperUser() || access.merchantUser() || access.admin()) {
            return access.order().getOwner_id_wsh();
        }
        if (currentUserId != null && !currentUserId.equals(access.order().getOwner_id_wsh())) {
            return access.order().getOwner_id_wsh();
        }
        throw new BusinessException(400, "无法确定订单会话接收人");
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
        return "[" + defaultText(record.getType_wsh(), "备注") + "] "
                + defaultText(record.getContent_wsh(), "看护者上传了新照片");
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

    private record OrderAccess(PetOrder order, Keeper keeper, Merchant merchant,
                               boolean admin, boolean owner, boolean keeperUser, boolean merchantUser) {
        boolean canRead() {
            return admin || owner || keeperUser || merchantUser;
        }

        boolean isParticipantUser(Long userId) {
            if (userId == null) {
                return false;
            }
            if (order.getOwner_id_wsh() != null && order.getOwner_id_wsh().equals(userId)) {
                return true;
            }
            if (keeper != null && keeper.getUser_id_wsh() != null && keeper.getUser_id_wsh().equals(userId)) {
                return true;
            }
            return merchant != null && merchant.getUser_id_wsh() != null && merchant.getUser_id_wsh().equals(userId);
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
