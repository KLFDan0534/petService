package com.pet.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.common.BusinessException;
import com.pet.customer.dto.ComplaintCreateRequestDTO;
import com.pet.customer.dto.ComplaintDTO;
import com.pet.customer.dto.ComplaintEvidenceDTO;
import com.pet.customer.dto.ComplaintListRequestDTO;
import com.pet.customer.entity.ChatMessage;
import com.pet.customer.entity.Complaint;
import com.pet.customer.entity.ComplaintMessage;
import com.pet.customer.mapper.ChatMessageMapper;
import com.pet.customer.mapper.ComplaintMapper;
import com.pet.customer.mapper.ComplaintMessageMapper;
import com.pet.customer.service.ChatEventBroadcaster;
import com.pet.customer.service.IssueAccess;
import com.pet.customer.service.IssueNotifier;
import com.pet.customer.service.MerchantCustomerServiceService;
import com.pet.customer.service.impl.ComplaintServiceImpl;
import com.pet.mq.MessageSender;
import com.pet.operation.service.NotificationService;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.pet.entity.CareRecord;
import com.pet.pet.mapper.CareRecordMapper;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 投诉业务契约。
 * <p>
 * 覆盖：创建校验（归属/防滥用/目标归属）、权限（管理员/商家/客服范围）、
 * 消息可见性与发送、受理、处理状态机（D1 守卫与幂等）、通知只发一次（D2）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("投诉业务契约")
class ComplaintServiceTest {

    private static final long OWNER_ID = 10L;
    private static final long ORDER_ID = 100L;
    private static final long MERCHANT_ID = 30L;
    private static final long CUSTOMER_SERVICE_USER_ID = 40L;
    private static final long ADMIN_ID = 1L;
    private static final long STRANGER_ID = 99L;

    @Mock private ComplaintMapper complaintMapper;
    @Mock private ComplaintMessageMapper complaintMessageMapper;
    @Mock private UserMapper userMapper;
    @Mock private NotificationService notificationService;
    @Mock private OrderMapper orderMapper;
    @Mock private ChatMessageMapper chatMessageMapper;
    @Mock private CareRecordMapper careRecordMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private KeeperMapper keeperMapper;
    @Mock private MerchantCustomerServiceService merchantCustomerServiceService;
    @Mock private MessageSender messageSender;
    @Mock private ChatEventBroadcaster chatEventBroadcaster;

    private IssueAccess issueAccess;
    private IssueNotifier issueNotifier;

    @BeforeEach
    void setUp() {
        issueAccess = new IssueAccess(merchantMapper, merchantCustomerServiceService);
        issueNotifier = new IssueNotifier(notificationService, merchantCustomerServiceService);
        // CAS 更新默认成功；个别用例会覆盖成 0 模拟并发
        lenient().when(complaintMapper.casUpdateStatus(anyLong(), anyString(), anyString(), any())).thenReturn(1);
    }

    // ── 创建 ────────────────────────────────────────────────

    @Test
    @DisplayName("订单投诉必须由订单主人发起")
    void orderComplaintRequiresOrderOwner() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order(ORDER_ID, STRANGER_ID));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().create(requestForOrder(), OWNER_ID));

        assertEquals(403, exception.getCode());
        verify(complaintMapper, never()).insert(any(Complaint.class));
    }

    @Test
    @DisplayName("订单投诉默认把看护人作为投诉目标，并带出证据摘要")
    void orderComplaintDefaultsTargetAndReturnsEvidenceSummary() {
        PetOrder order = order(ORDER_ID, OWNER_ID);
        order.setKeeper_id_wsh(20L);
        order.setMerchant_id_wsh(MERCHANT_ID);
        order.setOrder_no_wsh("ORD100");
        order.setStatus_wsh("in_progress");
        ChatMessage latestMessage = new ChatMessage();
        latestMessage.setCreated_at_wsh(LocalDateTime.of(2026, 7, 4, 10, 0));
        CareRecord latestCare = new CareRecord();
        latestCare.setRecord_time_wsh(LocalDateTime.of(2026, 7, 4, 11, 0));

        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(merchant(MERCHANT_ID));
        Keeper keeper = new Keeper();
        keeper.setId_wsh(20L);
        keeper.setMerchant_id_wsh(MERCHANT_ID);
        when(keeperMapper.selectById(20L)).thenReturn(keeper);
        when(chatMessageMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);
        when(careRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(chatMessageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(latestMessage);
        when(careRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(latestCare);

        ComplaintDTO result = service().create(requestForOrder(), OWNER_ID);

        ArgumentCaptor<Complaint> captor = ArgumentCaptor.forClass(Complaint.class);
        verify(complaintMapper).insert(captor.capture());
        assertEquals(20L, captor.getValue().getTarget_id_wsh());
        assertEquals("keeper", captor.getValue().getTarget_type_wsh());
        assertEquals(MERCHANT_ID, captor.getValue().getMerchant_id_wsh());
        assertEquals("pending", captor.getValue().getStatus_wsh());
        assertNotNull(result.getEvidence_summary_wsh());
        assertEquals("ORD100", result.getEvidence_summary_wsh().getOrder_no_wsh());
        assertEquals("in_progress", result.getEvidence_summary_wsh().getOrder_status_wsh());
        assertEquals(3L, result.getEvidence_summary_wsh().getChat_message_count_wsh());
        assertEquals(2L, result.getEvidence_summary_wsh().getCare_record_count_wsh());
    }

    @Test
    @DisplayName("10 分钟内第 4 条投诉被限流（429）")
    void createIsRateLimited() {
        PetOrder order = order(ORDER_ID, OWNER_ID);
        order.setMerchant_id_wsh(MERCHANT_ID);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(merchant(MERCHANT_ID));
        // 第一条 selectCount 是 10 分钟内计数
        when(complaintMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().create(requestForOrder(), OWNER_ID));

        assertEquals(429, exception.getCode());
        verify(complaintMapper, never()).insert(any(Complaint.class));
    }

    @Test
    @DisplayName("同一商家已有处理中的投诉时不允许重复提交")
    void createRejectsDuplicateInProgressComplaint() {
        PetOrder order = order(ORDER_ID, OWNER_ID);
        order.setMerchant_id_wsh(MERCHANT_ID);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(merchant(MERCHANT_ID));
        // 第一次返回 0（未限流），第二次返回 1（已存在处理中的投诉）
        when(complaintMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L, 1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().create(requestForOrder(), OWNER_ID));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("重复提交"));
        verify(complaintMapper, never()).insert(any(Complaint.class));
    }

    @Test
    @DisplayName("投诉标题超过 100 字直接拒绝")
    void createRejectsTooLongTitle() {
        PetOrder order = order(ORDER_ID, OWNER_ID);
        order.setMerchant_id_wsh(MERCHANT_ID);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(merchant(MERCHANT_ID));
        ComplaintCreateRequestDTO request = requestForOrder();
        request.setTitle_wsh("投".repeat(101));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().create(request, OWNER_ID));

        assertEquals(400, exception.getCode());
        verify(complaintMapper, never()).insert(any(Complaint.class));
    }

    @Test
    @DisplayName("投诉的看护人不属于所选商家时拒绝")
    void createRejectsKeeperFromAnotherMerchant() {
        PetOrder order = order(ORDER_ID, OWNER_ID);
        order.setMerchant_id_wsh(MERCHANT_ID);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(merchant(MERCHANT_ID));
        Keeper keeper = new Keeper();
        keeper.setId_wsh(20L);
        keeper.setMerchant_id_wsh(777L);
        when(keeperMapper.selectById(20L)).thenReturn(keeper);
        ComplaintCreateRequestDTO request = requestForOrder();
        request.setTarget_id_wsh(20L);
        request.setTarget_type_wsh("keeper");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().create(request, OWNER_ID));

        assertEquals(403, exception.getCode());
        verify(complaintMapper, never()).insert(any(Complaint.class));
    }

    // ── 权限 ────────────────────────────────────────────────

    @Test
    @DisplayName("已授权客服可处理本商家投诉")
    void approvedCustomerServiceCanProcessOwnMerchantComplaint() {
        Complaint complaint = merchantComplaint(MERCHANT_ID);
        when(complaintMapper.selectById(1L)).thenReturn(complaint);
        when(merchantCustomerServiceService.getApprovedMerchantIds(CUSTOMER_SERVICE_USER_ID))
                .thenReturn(Set.of(MERCHANT_ID));

        ComplaintDTO result = service().processForStaff(
                1L, "handled by merchant support", "resolved",
                CUSTOMER_SERVICE_USER_ID, false, false, true);

        assertEquals("resolved", complaint.getStatus_wsh());
        assertEquals("handled by merchant support", complaint.getResult_wsh());
        assertEquals(MERCHANT_ID, result.getMerchant_id_wsh());
        // 状态迁移走条件更新（CAS），不再整行 updateById
        verify(complaintMapper).casUpdateStatus(anyLong(), anyString(), anyString(), any());
        verify(complaintMapper, never()).updateById(any(Complaint.class));
    }

    @Test
    @DisplayName("客服无权处理其他商家的投诉")
    void customerServiceCannotProcessAnotherMerchantComplaint() {
        when(complaintMapper.selectById(1L)).thenReturn(merchantComplaint(MERCHANT_ID));
        when(merchantCustomerServiceService.getApprovedMerchantIds(CUSTOMER_SERVICE_USER_ID))
                .thenReturn(Set.of(999L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().processForStaff(1L, "outside scope", "resolved",
                        CUSTOMER_SERVICE_USER_ID, false, false, true));

        assertEquals(403, exception.getCode());
        verify(complaintMapper, never()).casUpdateStatus(anyLong(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("商家只能看到自己商家的投诉，范围为空时返回空页")
    void listPageForStaffReturnsEmptyWhenNoScope() {
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(merchantCustomerServiceService.getApprovedMerchantIds(STRANGER_ID)).thenReturn(Set.of());

        ComplaintListRequestDTO request = new ComplaintListRequestDTO();
        request.setPage(1);
        request.setSize(10);

        var page = service().listPageForStaff(request, STRANGER_ID, false, true, true);

        assertEquals(0, page.getRecords().size());
        assertEquals(0, page.getTotal());
        verify(complaintMapper, never()).selectPage(any(), any());
    }

    // ── 消息 ────────────────────────────────────────────────

    @Test
    @DisplayName("投诉人本人可以读取消息，并把自己的未读标记为已读")
    void ownerCanReadMessagesAndMarkRead() {
        when(complaintMapper.selectById(1L)).thenReturn(merchantComplaint(MERCHANT_ID));
        when(complaintMessageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        service().listMessages(1L, OWNER_ID, false, false, false);

        verify(complaintMessageMapper).markReadByComplaintId(1L, OWNER_ID);
    }

    @Test
    @DisplayName("无关用户读取投诉消息被拒绝")
    void strangerCannotReadMessages() {
        when(complaintMapper.selectById(1L)).thenReturn(merchantComplaint(MERCHANT_ID));
        when(merchantCustomerServiceService.getApprovedMerchantIds(STRANGER_ID)).thenReturn(Set.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().listMessages(1L, STRANGER_ID, false, false, true));

        assertEquals(403, exception.getCode());
        verify(complaintMessageMapper, never()).markReadByComplaintId(any(), any());
    }

    @Test
    @DisplayName("投诉人发言会通知该商家的客服，并推送 SSE 线程消息")
    void ownerMessageNotifiesCustomerService() {
        when(complaintMapper.selectById(1L)).thenReturn(merchantComplaint(MERCHANT_ID));
        when(merchantCustomerServiceService.getApprovedCsUserIds(MERCHANT_ID))
                .thenReturn(Set.of(CUSTOMER_SERVICE_USER_ID));
        when(userMapper.selectById(OWNER_ID)).thenReturn(user(OWNER_ID, "owner"));

        service().sendMessage(1L, OWNER_ID, "补充说明", null, false, false, false);

        ArgumentCaptor<ComplaintMessage> captor = ArgumentCaptor.forClass(ComplaintMessage.class);
        verify(complaintMessageMapper).insert(captor.capture());
        assertEquals(OWNER_ID, captor.getValue().getFrom_user_id_wsh());
        verify(notificationService, times(1)).create(any());
        verify(chatEventBroadcaster).broadcastThreadMessage(
                eq("complaint"), eq(1L), eq(CUSTOMER_SERVICE_USER_ID), eq(OWNER_ID),
                eq("补充说明"), isNull(), any());
    }

    @Test
    @DisplayName("消息内容与附件都为空时拒绝")
    void emptyMessageIsRejected() {
        when(complaintMapper.selectById(1L)).thenReturn(merchantComplaint(MERCHANT_ID));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().sendMessage(1L, OWNER_ID, "  ", null, false, false, false));

        assertEquals(400, exception.getCode());
        verify(complaintMessageMapper, never()).insert(any(ComplaintMessage.class));
    }

    // ── 证据 ────────────────────────────────────────────────

    @Test
    @DisplayName("证据包包含订单摘要、聊天记录与护理记录")
    void evidenceIncludesRecentChatAndCareRecords() {
        Complaint complaint = new Complaint();
        complaint.setId_wsh(1L);
        complaint.setOrder_id_wsh(ORDER_ID);
        complaint.setOwner_id_wsh(OWNER_ID);
        complaint.setTitle_wsh("Service issue");
        complaint.setContent_wsh("Need customer service review");

        PetOrder order = order(ORDER_ID, OWNER_ID);
        order.setOrder_no_wsh("ORD100");
        order.setStatus_wsh("in_progress");

        ChatMessage message = new ChatMessage();
        message.setId_wsh(11L);
        message.setOrder_id_wsh(ORDER_ID);
        message.setContent_wsh("Please check this");
        CareRecord record = new CareRecord();
        record.setId_wsh(21L);
        record.setOrder_id_wsh(ORDER_ID);
        record.setContent_wsh("Lunch photo");

        when(complaintMapper.selectById(1L)).thenReturn(complaint);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(chatMessageMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(careRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(chatMessageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(message));
        when(careRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

        ComplaintEvidenceDTO evidence = service().getEvidenceForStaff(1L, ADMIN_ID, true, false, false);

        assertEquals("ORD100", evidence.getSummary_wsh().getOrder_no_wsh());
        assertEquals(1, evidence.getChat_messages_wsh().size());
        assertEquals("Please check this", evidence.getChat_messages_wsh().get(0).getContent_wsh());
        assertEquals(1, evidence.getCare_records_wsh().size());
        assertEquals("Lunch photo", evidence.getCare_records_wsh().get(0).getContent_wsh());
    }

    // ── D1 状态守卫 ─────────────────────────────────────────

    @Test
    @DisplayName("D1：已处理的投诉不能再被驳回")
    void resolvedComplaintCannotBeRejected() {
        Complaint complaint = merchantComplaint(MERCHANT_ID);
        complaint.setStatus_wsh("resolved");
        complaint.setResult_wsh("已退款");
        when(complaintMapper.selectById(1L)).thenReturn(complaint);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().processForStaff(1L, "改判驳回", "rejected", ADMIN_ID, true, false, false));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("已处理完成"));
        verify(complaintMapper, never()).casUpdateStatus(anyLong(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("D1：已驳回的投诉不能再被处理完成")
    void rejectedComplaintCannotBeResolved() {
        Complaint complaint = merchantComplaint(MERCHANT_ID);
        complaint.setStatus_wsh("rejected");
        complaint.setResult_wsh("证据不足");
        when(complaintMapper.selectById(1L)).thenReturn(complaint);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().processForStaff(1L, "改判通过", "resolved", ADMIN_ID, true, false, false));

        assertEquals(400, exception.getCode());
        verify(complaintMapper, never()).casUpdateStatus(anyLong(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("D1：重复提交相同结论按幂等返回，不写库、不发 MQ")
    void repeatedResolveWithSameResultIsIdempotent() {
        Complaint complaint = merchantComplaint(MERCHANT_ID);
        complaint.setStatus_wsh("resolved");
        complaint.setResult_wsh("已退款");
        when(complaintMapper.selectById(1L)).thenReturn(complaint);

        ComplaintDTO result = service().processForStaff(1L, "已退款", "resolved", ADMIN_ID, true, false, false);

        assertEquals("resolved", result.getStatus_wsh());
        assertEquals("已退款", result.getResult_wsh());
        verify(complaintMapper, never()).casUpdateStatus(anyLong(), anyString(), anyString(), any());
        verify(messageSender, never()).sendComplaintProcess(any());
    }

    @Test
    @DisplayName("D1：终态下用不同结论覆盖被拒绝")
    void terminalStateCannotBeOverwrittenWithDifferentResult() {
        Complaint complaint = merchantComplaint(MERCHANT_ID);
        complaint.setStatus_wsh("resolved");
        complaint.setResult_wsh("已退款");
        when(complaintMapper.selectById(1L)).thenReturn(complaint);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().processForStaff(1L, "改判成别的原因", "resolved", ADMIN_ID, true, false, false));

        assertEquals(400, exception.getCode());
        verify(complaintMapper, never()).casUpdateStatus(anyLong(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("D1：并发下 CAS 失败但结果一致，按幂等返回成功")
    void concurrentSameResultIsIdempotent() {
        Complaint complaint = merchantComplaint(MERCHANT_ID);
        when(complaintMapper.selectById(1L))
                .thenReturn(complaint)                                    // 首次读取
                .thenReturn(merchantComplaintWith(MERCHANT_ID, "resolved", "已退款")); // CAS 失败后重读
        when(complaintMapper.casUpdateStatus(anyLong(), anyString(), anyString(), any())).thenReturn(0);

        ComplaintDTO result = service().processForStaff(1L, "已退款", "resolved", ADMIN_ID, true, false, false);

        assertEquals("resolved", result.getStatus_wsh());
        verify(messageSender, never()).sendComplaintProcess(any());
    }

    @Test
    @DisplayName("D1：并发下 CAS 失败且结果不同，要求刷新")
    void concurrentDifferentResultAsksForRefresh() {
        Complaint complaint = merchantComplaint(MERCHANT_ID);
        when(complaintMapper.selectById(1L))
                .thenReturn(complaint)
                .thenReturn(merchantComplaintWith(MERCHANT_ID, "rejected", "证据不足"));
        when(complaintMapper.casUpdateStatus(anyLong(), anyString(), anyString(), any())).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().processForStaff(1L, "已退款", "resolved", ADMIN_ID, true, false, false));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("状态已变化"));
    }

    @Test
    @DisplayName("D1：非法状态值（未知状态）不允许处理")
    void unknownCurrentStatusIsRejected() {
        Complaint complaint = merchantComplaint(MERCHANT_ID);
        complaint.setStatus_wsh("archived");
        when(complaintMapper.selectById(1L)).thenReturn(complaint);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().processForStaff(1L, "x", "resolved", ADMIN_ID, true, false, false));

        assertEquals(400, exception.getCode());
    }

    // ── D2 通知只发一次 ─────────────────────────────────────

    @Test
    @DisplayName("D2：处理投诉不再同步发站内通知，统一交给 MQ 消费方")
    void resolveDoesNotCreateSyncNotification() {
        Complaint complaint = merchantComplaint(MERCHANT_ID);
        when(complaintMapper.selectById(1L)).thenReturn(complaint);

        service().processForStaff(1L, "已退款", "resolved", ADMIN_ID, true, false, false);

        verify(notificationService, never()).create(any());
        verify(messageSender, times(1)).sendComplaintProcess(1L);
    }

    @Test
    @DisplayName("受理投诉仍然发一条受理通知，并把状态推到 processing")
    void acceptMovesToProcessingAndNotifies() {
        Complaint complaint = merchantComplaint(MERCHANT_ID);
        when(complaintMapper.selectById(1L)).thenReturn(complaint);

        ComplaintDTO result = service().accept(1L, ADMIN_ID, true, false, false);

        assertEquals("processing", result.getStatus_wsh());
        assertEquals("processing", complaint.getStatus_wsh());
        verify(complaintMapper).casUpdateStatus(anyLong(), anyString(), anyString(), any());
        verify(notificationService, times(1)).create(any());
    }

    @Test
    @DisplayName("并发受理时 CAS 失败，返回「仅待处理可受理」")
    void concurrentAcceptFailsWhenStatusChanged() {
        Complaint complaint = merchantComplaint(MERCHANT_ID);
        when(complaintMapper.selectById(1L)).thenReturn(complaint);
        when(complaintMapper.casUpdateStatus(anyLong(), anyString(), anyString(), any())).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().accept(1L, ADMIN_ID, true, false, false));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("仅待处理"));
        verify(notificationService, never()).create(any());
    }

    // ── 工具 ────────────────────────────────────────────────

    private ComplaintServiceImpl service() {
        return new ComplaintServiceImpl(
                complaintMapper,
                complaintMessageMapper,
                userMapper,
                notificationService,
                orderMapper,
                chatMessageMapper,
                careRecordMapper,
                merchantMapper,
                keeperMapper,
                merchantCustomerServiceService,
                messageSender,
                chatEventBroadcaster,
                issueAccess,
                issueNotifier);
    }

    private ComplaintCreateRequestDTO requestForOrder() {
        ComplaintCreateRequestDTO request = new ComplaintCreateRequestDTO();
        request.setOrder_id_wsh(ORDER_ID);
        request.setTitle_wsh("Service issue");
        request.setContent_wsh("Need customer service review");
        return request;
    }

    private PetOrder order(Long orderId, Long ownerId) {
        PetOrder order = new PetOrder();
        order.setId_wsh(orderId);
        order.setOwner_id_wsh(ownerId);
        return order;
    }

    private Complaint merchantComplaint(Long merchantId) {
        return merchantComplaintWith(merchantId, "pending", null);
    }

    private Complaint merchantComplaintWith(Long merchantId, String status, String result) {
        Complaint complaint = new Complaint();
        complaint.setId_wsh(1L);
        complaint.setMerchant_id_wsh(merchantId);
        complaint.setOwner_id_wsh(OWNER_ID);
        complaint.setTitle_wsh("Service issue");
        complaint.setContent_wsh("Need customer service review");
        complaint.setStatus_wsh(status);
        complaint.setResult_wsh(result);
        return complaint;
    }

    private Merchant merchant(Long id) {
        Merchant merchant = new Merchant();
        merchant.setId_wsh(id);
        return merchant;
    }

    private User user(Long id, String nickname) {
        User user = new User();
        user.setId_wsh(id);
        user.setNickname_wsh(nickname);
        return user;
    }
}
