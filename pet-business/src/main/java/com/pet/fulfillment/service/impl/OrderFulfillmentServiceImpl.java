package com.pet.fulfillment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.service.KeeperAttendanceService;
import com.pet.common.BookingUnit;
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

/**
 * 订单履行服务实现，提供宠物寄养/照看服务履行阶段的护理记录管理、
 * 订单参与者聊天通讯以及每日上传状态跟踪。
 * <p>权限控制：宠物主、照看者、商家和管理员均可查看订单履行信息；
 * 但仅照看者、商家和管理员可创建护理记录（照看者需在岗）。</p>
 */
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

    /**
     * 【构造方法】注入订单履行所需依赖
     *
     * 业务作用：由 Spring 注入订单、照看者、商家、护理记录、聊天、MinIO、文件记录、通知、考勤等 Mapper/Service，供后续履行流程使用。
     *
     * 调用场景：Spring 容器启动时创建该 Service Bean，自动装配调用。
     *
     * 调用链：Spring 容器 → 本构造方法 → 各依赖赋值给 final 字段。
     *
     * 数据处理：仅做字段赋值，不查询数据库、不做校验。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：依赖缺失时由 Spring 抛出 Bean 创建异常，本方法内部不抛异常。
     *
     * 注意事项：依赖为 final 字段，构造后不可替换；本类未使用 @RequiredArgsConstructor，新增依赖需同步改构造参数。
     */
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

    /**
     * 获取订单履行概览，聚合订单信息、用户角色、护理时间线和每日上传状态。
     *
     * @param userId 当前用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @return 订单履行概览视图对象
     */
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
        orderDTO.setBilling_unit_wsh(entity.getBilling_unit_wsh());
        orderDTO.setQuantity_wsh(entity.getQuantity_wsh());
        orderDTO.setUnit_price_wsh(entity.getUnit_price_wsh());
        orderDTO.setDuration_minutes_wsh(entity.getDuration_minutes_wsh());
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

    /**
     * 获取订单护理记录时间线。
     *
     * @param userId 当前用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @return 护理记录列表，按记录时间和创建时间倒序
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
     * 获取每日护理记录上传状态。
     * <p>遍历服务期间的每一天，检查是否有对应的护理记录上传，
     * 返回已上传天集合、缺失天集合和完整度标识。</p>
     *
     * @param userId 当前用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @return 每日上传状态DTO
     */
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
        LocalDate lastDay = lastServiceDay(access.order());
        // 按服务期逐日核对，含最后服务日（离店日当天仍需上传日报）
        while (cursor != null && lastDay != null && !cursor.isAfter(lastDay)) {
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

    /**
     * 服务期的最后一天（含）。
     * <p>
     * 只有 session/hour 的 end_date 是排他边界（= start+1），需回退一天；
     * night 的 end_date 是离店日（D+N），当天仍有交接，原样即最后服务日。
     * <p>
     * 提前接走已完成时，不要求上传未来日期的记录：最后一天收敛到接走确认日。
     */
    private LocalDate lastServiceDay(PetOrder order) {
        LocalDate end = order.getEnd_date_wsh();
        if (end == null) {
            return null;
        }
        String mode = BookingUnit.bookingMode(BookingUnit.normalize(order.getBilling_unit_wsh()));
        boolean slotUnit = BookingUnit.MODE_SLOT.equals(mode);
        LocalDate last = slotUnit ? end.minusDays(1) : end;
        LocalDateTime confirmedAt = order.getCheckout_confirmed_at_wsh();
        if (confirmedAt != null) {
            LocalDate confirmedDate = confirmedAt.toLocalDate();
            if (last.isAfter(confirmedDate)) {
                last = confirmedDate;
            }
        }
        return last;
    }

    /**
     * 创建护理时间线记录。
     * <p>校验写入权限（仅照看者/商家/管理员），检查记录日期在服务期间内，
     * 并校验照看者当前在岗状态。创建后发送站内通知给宠物主。</p>
     *
     * @param userId 当前用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @param request 创建护理记录请求
     * @return 创建的护理记录实体
     * @throws BusinessException 如果权限不足、日期越界或内容为空
     */
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

    /**
     * 创建护理时间线记录（含文件上传）。
     * <p>上传图片到 MinIO 存储，将返回的URL拼接后创建记录。
     * 仅支持图片类型文件上传。</p>
     *
     * @param userId 当前用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @param type 记录类型
     * @param content 记录文本内容
     * @param recordTime 记录时间字符串
     * @param files 上传的图片文件列表
     * @return 创建的护理记录实体
     * @throws BusinessException 如果图片为空或格式不正确
     */
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

    /**
     * 获取订单聊天会话列表（支持基于游标的分页）。
     * <p>自动解析消息收发双方的用户ID。</p>
     *
     * @param userId 当前用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @param otherUserId 对方用户ID（为空则自动推断）
     * @param beforeId 游标ID
     * @param size 返回条数上限
     * @return 聊天消息DTO列表
     */
    @Override
    public List<ChatMessageDTO> listConversation(Long userId, boolean admin, Long orderId, Long otherUserId,
                                                 Long beforeId, Integer size) {
        OrderAccess access = requireAccess(userId, admin, orderId);
        Long resolvedOtherUserId = resolveOtherUserId(access, userId, otherUserId);
        return chatService.getConversation(resolveChatActorUserId(access, userId), resolvedOtherUserId, orderId, beforeId, size);
    }

    /**
     * 【发送订单聊天消息】以文本或已有附件URL发送一条订单消息
     *
     * 业务作用：校验订单访问权限后组装 ChatMessage，落库并给接收方发站内通知，同时通过 SSE 广播消息。
     *
     * 调用场景：OrderFulfillmentController.sendMessage 接收前端 POST /{orderId}/conversation 请求时调用。
     *
     * 调用链：OrderFulfillmentController.sendMessage → 本方法 → requireAccess / resolveChatActorUserId / resolveOtherUserId / buildMessage / chatService.sendMessage / notifyUser / chatEventBroadcaster.broadcastMessage。
     *
     * 数据处理：入参 userId、orderId、SendOrderMessageRequestDTO（to_user_id_wsh、content_wsh、type_wsh、file_url_wsh）→ 组装 ChatMessage（from_user_id_wsh、to_user_id_wsh、order_id_wsh、content_wsh、type_wsh、file_url_wsh）→ 写入 chat_message 表，返回 ChatMessageDTO。
     *
     * 业务规则：request 不能为空；发送者由 resolveChatActorUserId 决定（管理员可代发）；接收者必须是订单参与方且不能是自己；内容与附件URL不能同时为空。
     *
     * 状态影响：写 chat_message 表新增一条消息；写 notification 表新增一条接收方通知（type=order_fulfillment）；向接收方广播 SSE 消息。
     *
     * 异常情况：request 为空抛 BusinessException(400)；无订单访问权限抛 403；订单不存在抛 404；收发双方非法抛 400。
     *
     * 注意事项：@Transactional 标注但通知与广播在同一事务内执行，广播失败会连带回滚；本方法只接受已有 file_url_wsh，不处理文件上传，上传走 sendMessageWithFile。
     */
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

    /**
     * 发送订单聊天消息（含文件上传）。
     *
     * @param userId 当前用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @param toUserId 接收方用户ID
     * @param content 消息内容
     * @param type 消息类型
     * @param file 上传的图片文件
     * @return 发送后的聊天消息DTO
     */
    /**
     * 发送订单聊天消息（含文件上传）。
     *
     * @param userId 当前用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @param toUserId 接收方用户ID
     * @param content 消息内容
     * @param type 消息类型
     * @param file 上传的图片文件
     * @return 发送后的聊天消息DTO
     */
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

    /**
     * 将订单聊天会话中对方发来的消息全部标记为已读。
     *
     * @param userId 当前用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @param otherUserId 对方用户ID
     */
    @Transactional
    @Override
    public void markConversationAsRead(Long userId, boolean admin, Long orderId, Long otherUserId) {
        OrderAccess access = requireAccess(userId, admin, orderId);
        Long currentUserId = resolveChatActorUserId(access, userId);
        Long resolvedOtherUserId = resolveOtherUserId(access, currentUserId, otherUserId);
        chatService.markConversationAsRead(currentUserId, resolvedOtherUserId, orderId);
    }

    /**
     * 校验并获取当前用户可读的订单。
     * <p>宠物主、照看者（通过Keeper关联）、商家（通过Merchant关联）和管理员均有权限。</p>
     *
     * @param userId 当前用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @return 订单实体
     * @throws BusinessException 如果订单不存在或用户无权限
     */
    @Override
    public PetOrder requireReadableOrder(Long userId, boolean admin, Long orderId) {
        return requireAccess(userId, admin, orderId).order();
    }

    /**
     * 【组装护理记录实体】校验并构造待落库的 CareRecord
     *
     * 业务作用：把请求参数与订单信息组装成 CareRecord 实体，同时完成内容非空与记录日期落在服务期内的校验。
     *
     * 调用场景：createTimelineRecord 与 createTimelineRecordWithFiles 在插入护理记录前调用。
     *
     * 调用链：createTimelineRecord / createTimelineRecordWithFiles → 本方法 → 返回 CareRecord（由调用方 careRecordMapper.insert 落库）。
     *
     * 数据处理：输入 type/content/images/recordTime 与 OrderAccess → 输出 CareRecord（order_id_wsh、pet_id_wsh、keeper_id_wsh 取自订单，type_wsh 缺省 note，record_time_wsh 缺省当前时间）。
     *
     * 业务规则：内容与图片不能同时为空；recordTime 为空时取当前时间；订单起止日期都存在时，记录日期必须落在 [开始日, 最后服务日]（含离店日）。
     *
     * 状态影响：无，本方法不写库，仅构造对象。
     *
     * 异常情况：内容与图片均为空抛 BusinessException(400)；记录日期早于开始日或晚于最后服务日抛 BusinessException(400)。
     *
     * 注意事项：最后服务日由 lastServiceDay 统一推导（提前接走会收敛到接走日），与 getDailyUploadStatus 口径一致。
     */
    private CareRecord buildCareRecord(OrderAccess access, String type, String content,
                                       String images, LocalDateTime recordTime) {
        if (isBlank(content) && isBlank(images)) {
            throw new BusinessException(400, "care content or photo cannot be empty");
        }
        LocalDateTime happenedAt = recordTime != null ? recordTime : LocalDateTime.now();
        LocalDate start = access.order().getStart_date_wsh();
        LocalDate lastDay = lastServiceDay(access.order());
        if (start != null && lastDay != null) {
            LocalDate happenedDate = happenedAt.toLocalDate();
            // 记录日期须在服务期内，含最后服务日
            if (happenedDate.isBefore(start) || happenedDate.isAfter(lastDay)) {
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

    /**
     * 【组装聊天消息实体】校验收发双方并构造 ChatMessage
     *
     * 业务作用：对发送者、接收者、消息内容做完整性校验，然后组装待落库的 ChatMessage。
     *
     * 调用场景：sendMessage 与 sendMessageWithFile 在调用 chatService.sendMessage 前调用。
     *
     * 调用链：sendMessage / sendMessageWithFile → 本方法 → 返回 ChatMessage（由 chatService.sendMessage 落库）。
     *
     * 数据处理：输入 fromUserId/toUserId/orderId/content/type/fileUrl → 输出 ChatMessage；type 缺省时按有无 fileUrl 取 image 或 text。
     *
     * 业务规则：发送者不能为空；接收者不能为空；不能发给自己；内容与附件URL不能同时为空。
     *
     * 状态影响：无，本方法不写库。
     *
     * 异常情况：上述任一校验失败均抛 BusinessException(400)。
     *
     * 注意事项：type 的兜底值是英文常量 text/image，前端展示时需按该枚举值映射。
     */
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

    /**
     * 【上传图片并登记文件记录】把 MultipartFile 存入 MinIO 并写 file_record
     *
     * 业务作用：逐个校验文件为图片类型，上传到 MinIO 指定目录，登记 file_record 表并返回可访问 URL 列表。
     *
     * 调用场景：createTimelineRecordWithFiles（directory=care-records）与 sendMessageWithFile（directory=order-chat）调用。
     *
     * 调用链：createTimelineRecordWithFiles / sendMessageWithFile → 本方法 → minIoService.uploadFile / fileRecordService.create / minIoService.getFileUrl。
     *
     * 数据处理：输入 userId、orderId、directory、files → 对象路径拼接为 directory/orderId；每个文件写入 file_record（original_name_wsh、object_name_wsh、size_wsh、content_type_wsh、user_id_wsh），输出 URL 列表。
     *
     * 业务规则：files 为空直接返回空列表；单个文件为 null 或空文件跳过；Content-Type 必须以 image/ 开头。
     *
     * 状态影响：MinIO 新增对象；file_record 表新增记录。
     *
     * 异常情况：非图片类型抛 BusinessException(400)；MinIO 上传或数据库写入异常向上抛出，由调用方事务回滚。
     *
     * 注意事项：Content-Type 由客户端提供，仅做前缀判断，未校验真实文件内容；空文件被静默跳过，调用方需自行判断返回列表是否为空。
     */
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
            // 只接受 image 前缀的图片文件
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

    /**
     * 【校验护理记录写权限】在可读权限基础上要求照看者/商家/管理员且在岗
     *
     * 业务作用：复用 requireAccess 的可读校验，再限制只有照看者、商家或管理员可写护理记录，非管理员还要求该订单照看者当前在岗。
     *
     * 调用场景：createTimelineRecord 与 createTimelineRecordWithFiles 的入口校验。
     *
     * 调用链：createTimelineRecord / createTimelineRecordWithFiles → 本方法 → requireAccess / keeperAttendanceService.requireKeeperOnDuty。
     *
     * 数据处理：输入 userId、admin、orderId → 输出 OrderAccess（含订单、照看者、商家与角色标记）。
     *
     * 业务规则：宠物主虽可读但不可写；管理员跳过在岗校验；非管理员必须通过订单对应照看者的在岗检查。
     *
     * 状态影响：无，仅查询订单、照看者、商家与考勤数据。
     *
     * 异常情况：无订单访问权限抛 403；无写权限抛 BusinessException(403)；订单不存在抛 404；照看者不在岗时由 requireKeeperOnDuty 抛业务异常。
     *
     * 注意事项：在岗校验用的是订单上的 keeper_id_wsh，若订单未指派照看者，需依赖 requireKeeperOnDuty 对 null 的处理逻辑。
     */
    private OrderAccess requireWritableCareAccess(Long userId, boolean admin, Long orderId) {
        OrderAccess access = requireAccess(userId, admin, orderId);
        if (!access.admin() && !access.keeperUser() && !access.merchantUser()) {
            throw new BusinessException(403, "only keeper, merchant or admin can upload care records");
        }
        // 非管理员还要求订单照看者当前在岗
        if (!access.admin()) {
            keeperAttendanceService.requireKeeperOnDuty(
                    access.order().getKeeper_id_wsh(),
                    access.order().getMerchant_id_wsh());
        }
        return access;
    }

    /**
     * 【加载订单并校验读权限】构造 OrderAccess 权限上下文
     *
     * 业务作用：按订单ID加载订单、关联照看者与商家，计算当前用户是宠物主/照看者/商家/管理员，无任何角色时拒绝访问。
     *
     * 调用场景：本类所有对外方法与写权限校验的公共入口。
     *
     * 调用链：getOverview / listTimeline / getDailyUploadStatus / listConversation / sendMessage / sendMessageWithFile / markConversationAsRead / requireReadableOrder / requireWritableCareAccess → 本方法 → orderMapper.selectById / keeperMapper.selectById / merchantMapper.selectById。
     *
     * 数据处理：输入 userId、admin、orderId → 查询 pet_order、keeper、merchant → 输出 OrderAccess（order、keeper、merchant、admin、owner、keeperUser、merchantUser）。
     *
     * 业务规则：orderId 不能为空；订单必须存在；owner 比对 owner_id_wsh，keeperUser/merchantUser 比对对应实体的 user_id_wsh；admin 恒可读。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：orderId 为空抛 BusinessException(400)；订单不存在抛 BusinessException(404)；无任何角色抛 BusinessException(403)。
     *
     * 注意事项：每次调用都重新查库，批量接口（如 getOverview 内再调 listTimeline）会重复查询同一订单。
     */
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

    /**
     * 【解析聊天发送者】确定本次消息实际以谁的身份发出
     *
     * 业务作用：普通用户直接返回自身ID；管理员若本身是订单参与方则用自身ID，否则代理为订单宠物主身份发言。
     *
     * 调用场景：listConversation、sendMessage、sendMessageWithFile、markConversationAsRead 在访问聊天服务前调用。
     *
     * 调用链：listConversation / sendMessage / sendMessageWithFile / markConversationAsRead → 本方法 → OrderAccess.isParticipantUser。
     *
     * 数据处理：输入 OrderAccess 与 currentUserId → 输出实际发送者 userId。
     *
     * 业务规则：非管理员原样返回 currentUserId；管理员优先用自身ID（需为参与方），否则退化为订单 owner_id_wsh。
     *
     * 状态影响：无。
     *
     * 异常情况：管理员且订单无 owner_id_wsh 时抛 BusinessException(400)。
     *
     * 注意事项：管理员代发消息在 chat_message 中记录的 from_user_id_wsh 是宠物主，前端需据此区分真实操作者。
     */
    private Long resolveChatActorUserId(OrderAccess access, Long currentUserId) {
        if (!access.admin()) {
            return currentUserId;
        }
        Long ownerId = access.order().getOwner_id_wsh();
        // 管理员本人是参与方时按本人身份发言
        if (currentUserId != null && access.isParticipantUser(currentUserId)) {
            return currentUserId;
        }
        if (ownerId != null) {
            return ownerId;
        }
        throw new BusinessException(400, "cannot determine order chat sender");
    }

    /**
     * 【解析聊天接收方】确定消息或会话的对方用户
     *
     * 业务作用：显式传入对方ID时校验其为订单参与方；未传时按当前用户角色推断默认对话对象。
     *
     * 调用场景：listConversation、sendMessage、sendMessageWithFile、markConversationAsRead 在调用聊天服务前调用。
     *
     * 调用链：listConversation / sendMessage / sendMessageWithFile / markConversationAsRead → 本方法 → OrderAccess.isParticipantUser。
     *
     * 数据处理：输入 OrderAccess、currentUserId、requestedUserId → 输出对方 userId。
     *
     * 业务规则：显式ID必须是订单参与方且不能等于自己；宠物主默认对向照看者，无照看者则对向商家；照看者/商家/管理员默认对向宠物主。
     *
     * 状态影响：无。
     *
     * 异常情况：对方非参与方抛 BusinessException(403)；对方等于自己抛 BusinessException(400)；无法推断接收方抛 BusinessException(400)。
     *
     * 注意事项：宠物主优先对向照看者而非商家，订单同时有照看者与商家时商家不在默认会话链路上。
     */
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
        // 宠物主优先找照看者，其次商家
        if (access.owner()) {
            if (access.keeperUserId() != null) {
                return access.keeperUserId();
            }
            if (access.merchantUserId() != null) {
                return access.merchantUserId();
            }
        }
        // 其余角色统一回复宠物主
        if (access.keeperUser() || access.merchantUser() || access.admin()) {
            return access.order().getOwner_id_wsh();
        }
        if (currentUserId != null && !currentUserId.equals(access.order().getOwner_id_wsh())) {
            return access.order().getOwner_id_wsh();
        }
        throw new BusinessException(400, "cannot determine order chat receiver");
    }

    /**
     * 【通知宠物主】向订单所属宠物主推送站内通知
     *
     * 业务作用：取订单 owner_id_wsh，非空时调用 notifyUser 生成一条订单履行类通知。
     *
     * 调用场景：createTimelineRecord 与 createTimelineRecordWithFiles 在护理记录落库后调用。
     *
     * 调用链：createTimelineRecord / createTimelineRecordWithFiles → 本方法 → notifyUser → notificationService.create。
     *
     * 数据处理：输入 OrderAccess、title、content → 输出无（写 notification 表）。
     *
     * 业务规则：订单无宠物主时静默跳过，不报错。
     *
     * 状态影响：写 notification 表一条记录（type=order_fulfillment，related_id=订单ID）。
     *
     * 异常情况：无显式抛出；通知写入失败会向上传播并回滚事务。
     *
     * 注意事项：与 sendMessage 中的 notifyUser 不同，本方法通知对象固定为宠物主。
     */
    private void notifyOwner(OrderAccess access, String title, String content) {
        Long ownerId = access.order().getOwner_id_wsh();
        if (ownerId != null) {
            notifyUser(ownerId, title, content, access.order().getId_wsh());
        }
    }

    /**
     * 【创建站内通知】按统一模板写入一条订单履行通知
     *
     * 业务作用：构造 Notification 并交给 notificationService 落库，通知类型固定为 order_fulfillment。
     *
     * 调用场景：notifyOwner 与 sendMessage / sendMessageWithFile 在消息发送后调用。
     *
     * 调用链：notifyOwner / sendMessage / sendMessageWithFile → 本方法 → notificationService.create。
     *
     * 数据处理：输入 userId、title、content、orderId → 写入 notification（user_id_wsh、title_wsh、content_wsh、type_wsh=order_fulfillment、related_id_wsh）。
     *
     * 业务规则：userId 为空时直接返回，不创建通知。
     *
     * 状态影响：写 notification 表。
     *
     * 异常情况：无显式抛出，通知写入异常向上传播。
     *
     * 注意事项：本方法未捕获异常，通知失败会导致外层业务（如发送消息）整体回滚；TicketServiceImpl 中的同类方法则是 catch 后仅记日志，两处策略不一致。
     */
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

    /**
     * 【拼接护理通知正文】生成形如 [类型] 内容的通知文案
     *
     * 业务作用：把护理记录类型与内容拼成通知正文，缺省时分别用 note 与 New care photo uploaded 兜底。
     *
     * 调用场景：createTimelineRecord 与 createTimelineRecordWithFiles 在 notifyOwner 前调用。
     *
     * 调用链：createTimelineRecord / createTimelineRecordWithFiles → 本方法 → defaultText。
     *
     * 数据处理：输入 CareRecord → 输出 "[type] content" 格式字符串。
     *
     * 业务规则：type_wsh、content_wsh 为空时使用英文兜底文案。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：仅上传图片未填文字时，正文固定为 New care photo uploaded，无法体现图片数量。
     */
    private String buildCareNotificationContent(CareRecord record) {
        return "[" + defaultText(record.getType_wsh(), "note") + "] "
                + defaultText(record.getContent_wsh(), "New care photo uploaded");
    }

    /**
     * 【解析时间字符串】兼容 ISO 与空格分隔两种格式
     *
     * 业务作用：把前端传入的时间字符串解析为 LocalDateTime。
     *
     * 调用场景：createTimelineRecordWithFiles 解析 recordTime 参数时调用。
     *
     * 调用链：createTimelineRecordWithFiles → 本方法 → LocalDateTime.parse。
     *
     * 数据处理：输入字符串 → 输出 LocalDateTime；空串返回 null。
     *
     * 业务规则：含 T 时按 ISO-8601 解析，否则按 yyyy-MM-dd HH:mm:ss 解析。
     *
     * 状态影响：无。
     *
     * 异常情况：格式不匹配时抛 DateTimeParseException（运行时异常，未被包装为 BusinessException）。
     *
     * 注意事项：不支持仅日期（yyyy-MM-dd）格式；解析异常不会被转换为 400 业务错误。
     */
    private LocalDateTime parseDateTime(String value) {
        if (isBlank(value)) {
            return null;
        }
        String text = value.trim();
        return text.contains("T")
                ? LocalDateTime.parse(text)
                : LocalDateTime.parse(text, DATETIME_WITH_SPACE);
    }

    /**
     * 【空值兜底】为空时返回默认文案，否则返回去空格后的值
     *
     * 业务作用：统一处理可选字符串字段的缺省值。
     *
     * 调用场景：buildCareRecord、buildMessage、buildCareNotificationContent、sendMessageWithFile 中多处调用。
     *
     * 调用链：buildCareRecord / buildMessage / buildCareNotificationContent / sendMessageWithFile → 本方法 → isBlank。
     *
     * 数据处理：输入 value 与 fallback → 输出 value.trim() 或 fallback。
     *
     * 业务规则：null 或全空白视为空。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：返回值会去掉首尾空格，原值中的空白不会被保留。
     */
    private String defaultText(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    /**
     * 【空串判断】判断字符串是否为 null 或全空白
     *
     * 业务作用：本类内部统一的空值判断工具。
     *
     * 调用场景：createTimelineRecord、buildCareRecord、buildMessage、parseDateTime、defaultText 中调用。
     *
     * 调用链：上述方法 → 本方法。
     *
     * 数据处理：输入字符串 → 输出 boolean。
     *
     * 业务规则：null 或 trim 后为空即视为空。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：未使用 Spring 的 StringUtils，行为与 StringUtils.isBlank 等价。
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 【护理记录转DTO】把 CareRecord 实体字段拷贝为 CareRecordDTO
     *
     * 业务作用：屏蔽实体字段，向接口层输出护理记录 DTO。
     *
     * 调用场景：getOverview 组装 timeline_wsh 时通过方法引用调用。
     *
     * 调用链：getOverview → 本方法 → CareRecordDTO。
     *
     * 数据处理：逐字段拷贝 id_wsh、order_id_wsh、pet_id_wsh、keeper_id_wsh、type_wsh、content_wsh、images_wsh、record_time_wsh、created_at_wsh。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：入参为 null 时抛 NullPointerException，本方法未做判空。
     *
     * 注意事项：与 OrderFulfillmentController 中的同名方法逻辑完全重复，两处需同步维护。
     */
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

    /**
     * 【权限上下文】记录订单、照看者、商家及各角色判定结果
     *
     * 业务作用：作为内部值对象在权限校验后向业务方法传递订单与角色信息，并提供角色判定辅助方法。
     *
     * 调用场景：requireAccess 构造，requireWritableCareAccess 及各业务方法读取。
     *
     * 调用链：requireAccess → new OrderAccess → canRead / keeperUserId / merchantUserId / isParticipantUser / roleName。
     *
     * 数据处理：持有 PetOrder、Keeper、Merchant 及 admin/owner/keeperUser/merchantUser 布尔标记。
     *
     * 业务规则：canRead 为四者任一；isParticipantUser 比对订单 owner、照看者 user_id、商家 user_id。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：record 内部方法（canRead 等）无访问修饰符，仅本类可见；keeper/merchant 可能为 null，读取其 user_id 前已做判空。
     */
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
