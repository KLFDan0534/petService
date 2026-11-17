package com.pet.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.common.BusinessException;
import com.pet.customer.dto.TicketCreateRequestDTO;
import com.pet.customer.dto.TicketDTO;
import com.pet.customer.entity.Ticket;
import com.pet.customer.entity.TicketMessage;
import com.pet.customer.mapper.TicketMapper;
import com.pet.customer.mapper.TicketMessageMapper;
import com.pet.customer.service.ChatEventBroadcaster;
import com.pet.customer.service.IssueAccess;
import com.pet.customer.service.IssueNotifier;
import com.pet.customer.service.MerchantCustomerServiceService;
import com.pet.customer.service.impl.TicketServiceImpl;
import com.pet.operation.service.NotificationService;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工单业务契约。
 * <p>
 * 覆盖：创建（订单归属/商家校验/默认优先级）、指派、解决、关闭、状态守卫、
 * 可见性与消息权限、解决时自动补留言。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工单业务契约")
class TicketServiceTest {

    private static final long OWNER_ID = 10L;
    private static final long ORDER_ID = 100L;
    private static final long MERCHANT_ID = 30L;
    private static final long CUSTOMER_SERVICE_USER_ID = 40L;
    private static final long ADMIN_ID = 1L;
    private static final long STRANGER_ID = 99L;

    @Mock private TicketMapper ticketMapper;
    @Mock private TicketMessageMapper ticketMessageMapper;
    @Mock private UserMapper userMapper;
    @Mock private NotificationService notificationService;
    @Mock private OrderMapper orderMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private MerchantCustomerServiceService merchantCustomerServiceService;
    @Mock private ChatEventBroadcaster chatEventBroadcaster;

    private IssueAccess issueAccess;
    private IssueNotifier issueNotifier;

    @BeforeEach
    void setUp() {
        issueAccess = new IssueAccess(merchantMapper, merchantCustomerServiceService);
        issueNotifier = new IssueNotifier(notificationService, merchantCustomerServiceService);
    }

    // ── 创建 ────────────────────────────────────────────────

    @Test
    @DisplayName("关联订单创建工单时，商家以订单为准")
    void orderTicketStoresOrderMerchant() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order(ORDER_ID, OWNER_ID, MERCHANT_ID));
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(merchant(MERCHANT_ID));

        TicketDTO result = service().create(OWNER_ID, requestForOrder());

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketMapper).insert(captor.capture());
        assertEquals(OWNER_ID, captor.getValue().getUser_id_wsh());
        assertEquals(ORDER_ID, captor.getValue().getOrder_id_wsh());
        assertEquals(MERCHANT_ID, captor.getValue().getMerchant_id_wsh());
        assertEquals("pending", captor.getValue().getStatus_wsh());
        assertEquals(MERCHANT_ID, result.getMerchant_id_wsh());
    }

    @Test
    @DisplayName("未指定优先级时默认 medium")
    void createDefaultsPriorityToMedium() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order(ORDER_ID, OWNER_ID, MERCHANT_ID));
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(merchant(MERCHANT_ID));

        service().create(OWNER_ID, requestForOrder());

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketMapper).insert(captor.capture());
        assertEquals("medium", captor.getValue().getPriority_wsh());
    }

    @Test
    @DisplayName("商家不存在时拒绝创建")
    void ticketWithUnknownMerchantIsRejected() {
        TicketCreateRequestDTO request = new TicketCreateRequestDTO();
        request.setMerchant_id_wsh(MERCHANT_ID);
        request.setTitle_wsh("Need help");
        request.setContent_wsh("Question for merchant");
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().create(OWNER_ID, request));

        assertEquals(404, exception.getCode());
        verify(ticketMapper, never()).insert(any(Ticket.class));
    }

    @Test
    @DisplayName("不能用别人的订单创建工单")
    void createRejectsOrderOwnedBySomeoneElse() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order(ORDER_ID, STRANGER_ID, MERCHANT_ID));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().create(OWNER_ID, requestForOrder()));

        assertEquals(403, exception.getCode());
        verify(ticketMapper, never()).insert(any(Ticket.class));
    }

    // ── 指派 ────────────────────────────────────────────────

    @Test
    @DisplayName("指派后状态变 processing 并记录处理人")
    void assignMovesToProcessingAndRecordsAssignee() {
        Ticket ticket = merchantTicket(MERCHANT_ID);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);
        when(userMapper.selectById(CUSTOMER_SERVICE_USER_ID)).thenReturn(user(CUSTOMER_SERVICE_USER_ID));

        when(ticketMapper.casAssign(1L, CUSTOMER_SERVICE_USER_ID, "pending", "processing")).thenReturn(1);

        TicketDTO result = service().assignForStaff(1L, CUSTOMER_SERVICE_USER_ID, ADMIN_ID, true, false, false);

        assertEquals("processing", result.getStatus_wsh());
        assertEquals(CUSTOMER_SERVICE_USER_ID, ticket.getAssignee_id_wsh());
        // 处理人与状态必须在同一条条件更新里落库
        verify(ticketMapper).casAssign(1L, CUSTOMER_SERVICE_USER_ID, "pending", "processing");
        verify(ticketMapper, never()).updateById(any(Ticket.class));
        verify(notificationService, times(1)).create(any());
    }

    @Test
    @DisplayName("非 pending 的工单不能被指派")
    void assignRejectsNonPendingTicket() {
        Ticket ticket = merchantTicket(MERCHANT_ID);
        ticket.setStatus_wsh("processing");
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().assignForStaff(1L, CUSTOMER_SERVICE_USER_ID, ADMIN_ID, true, false, false));

        assertEquals(400, exception.getCode());
        verify(ticketMapper, never()).casAssign(any(), any(), any(), any());
        verify(ticketMapper, never()).updateById(any(Ticket.class));
    }

    @Test
    @DisplayName("处理人不存在时指派失败")
    void assignFailsWhenAssigneeMissing() {
        when(ticketMapper.selectById(1L)).thenReturn(merchantTicket(MERCHANT_ID));
        when(userMapper.selectById(CUSTOMER_SERVICE_USER_ID)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().assignForStaff(1L, CUSTOMER_SERVICE_USER_ID, ADMIN_ID, true, false, false));

        assertEquals(404, exception.getCode());
    }

    // ── 解决 / 关闭 ─────────────────────────────────────────

    @Test
    @DisplayName("已授权客服可解决本商家工单，并自动补一条处理结果留言")
    void approvedCustomerServiceCanResolveOwnMerchantTicket() {
        Ticket ticket = merchantTicket(MERCHANT_ID);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);
        when(merchantCustomerServiceService.getApprovedMerchantIds(CUSTOMER_SERVICE_USER_ID))
                .thenReturn(Set.of(MERCHANT_ID));

        when(ticketMapper.casResolve(1L, "handled by merchant support", "resolved", "pending", "processing"))
                .thenReturn(1);

        TicketDTO result = service().resolveForStaff(
                1L, "handled by merchant support", CUSTOMER_SERVICE_USER_ID, false, false, true);

        assertEquals("resolved", ticket.getStatus_wsh());
        assertEquals("handled by merchant support", ticket.getResult_wsh());
        assertEquals(MERCHANT_ID, result.getMerchant_id_wsh());
        verify(ticketMapper).casResolve(1L, "handled by merchant support", "resolved", "pending", "processing");
        verify(ticketMapper, never()).updateById(any(Ticket.class));
        verify(ticketMessageMapper).insert(any(TicketMessage.class));
    }

    @Test
    @DisplayName("客服无权解决其他商家的工单")
    void customerServiceCannotResolveAnotherMerchantTicket() {
        when(ticketMapper.selectById(1L)).thenReturn(merchantTicket(MERCHANT_ID));
        when(merchantCustomerServiceService.getApprovedMerchantIds(CUSTOMER_SERVICE_USER_ID))
                .thenReturn(Set.of(999L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().resolveForStaff(1L, "outside scope", CUSTOMER_SERVICE_USER_ID, false, false, true));

        assertEquals(403, exception.getCode());
        verify(ticketMapper, never()).casResolve(any(), any(), any(), any(), any());
        verify(ticketMapper, never()).updateById(any(Ticket.class));
        verify(ticketMessageMapper, never()).insert(any(TicketMessage.class));
    }

    @Test
    @DisplayName("已关闭的工单不能再解决")
    void closedTicketCannotBeResolved() {
        Ticket ticket = merchantTicket(MERCHANT_ID);
        ticket.setStatus_wsh("closed");
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().resolveForStaff(1L, "再处理一次", ADMIN_ID, true, false, false));

        assertEquals(400, exception.getCode());
        verify(ticketMessageMapper, never()).insert(any(TicketMessage.class));
    }

    @Test
    @DisplayName("已解决的工单可以关闭")
    void resolvedTicketCanBeClosed() {
        Ticket ticket = merchantTicket(MERCHANT_ID);
        ticket.setStatus_wsh("resolved");
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        when(ticketMapper.casClose(1L, "resolved", "closed")).thenReturn(1);

        TicketDTO result = service().closeForStaff(1L, ADMIN_ID, true, false, false);

        assertEquals("closed", result.getStatus_wsh());
        verify(ticketMapper).casClose(1L, "resolved", "closed");
        verify(ticketMapper, never()).updateById(any(Ticket.class));
    }

    @Test
    @DisplayName("未解决的工单不能直接关闭")
    void pendingTicketCannotBeClosed() {
        when(ticketMapper.selectById(1L)).thenReturn(merchantTicket(MERCHANT_ID));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().closeForStaff(1L, ADMIN_ID, true, false, false));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("仅已解决"));
        verify(ticketMapper, never()).casClose(any(), any(), any());
        verify(ticketMapper, never()).updateById(any(Ticket.class));
    }

    // ── 可见性 ──────────────────────────────────────────────

    @Test
    @DisplayName("工单发起人可以查看自己的工单")
    void ownerCanViewOwnTicket() {
        when(ticketMapper.selectById(1L)).thenReturn(merchantTicket(MERCHANT_ID));

        TicketDTO result = service().getByIdForUser(1L, OWNER_ID, false, false, false);

        assertEquals(1L, result.getId_wsh());
    }

    @Test
    @DisplayName("无关用户查看工单被拒绝")
    void strangerCannotViewTicket() {
        when(ticketMapper.selectById(1L)).thenReturn(merchantTicket(MERCHANT_ID));
        when(merchantCustomerServiceService.getApprovedMerchantIds(STRANGER_ID)).thenReturn(Set.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().getByIdForUser(1L, STRANGER_ID, false, false, true));

        assertEquals(403, exception.getCode());
    }

    @Test
    @DisplayName("工单不存在返回 404")
    void missingTicketReturns404() {
        when(ticketMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().getByIdForUser(1L, OWNER_ID, false, false, false));

        assertEquals(404, exception.getCode());
    }

    // ── 消息 ────────────────────────────────────────────────

    @Test
    @DisplayName("发起人可以读取工单消息")
    void ownerCanReadTicketMessages() {
        when(ticketMapper.selectById(1L)).thenReturn(merchantTicket(MERCHANT_ID));
        when(ticketMessageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        var messages = service().listMessagesForUser(1L, OWNER_ID, false, false, false);

        assertEquals(0, messages.size());
        verify(ticketMessageMapper).markReadByTicketId(1L, OWNER_ID);
    }

    @Test
    @DisplayName("无关用户不能读取工单消息")
    void strangerCannotReadTicketMessages() {
        when(ticketMapper.selectById(1L)).thenReturn(merchantTicket(MERCHANT_ID));
        when(merchantCustomerServiceService.getApprovedMerchantIds(STRANGER_ID)).thenReturn(Set.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().listMessagesForUser(1L, STRANGER_ID, false, false, true));

        assertEquals(403, exception.getCode());
        verify(ticketMessageMapper, never()).markReadByTicketId(any(), any());
    }

    @Test
    @DisplayName("无关用户不能给工单发消息")
    void strangerCannotAddTicketMessage() {
        when(ticketMapper.selectById(1L)).thenReturn(merchantTicket(MERCHANT_ID));
        when(merchantCustomerServiceService.getApprovedMerchantIds(STRANGER_ID)).thenReturn(Set.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().addMessageForUser(1L, STRANGER_ID, false, false, true, "hi", null));

        assertEquals(403, exception.getCode());
        verify(ticketMessageMapper, never()).insert(any(TicketMessage.class));
    }

    @Test
    @DisplayName("发起人发消息会推给该商家客服")
    void ownerMessageBroadcastsToCustomerService() {
        when(ticketMapper.selectById(1L)).thenReturn(merchantTicket(MERCHANT_ID));
        when(merchantCustomerServiceService.getApprovedCsUserIds(MERCHANT_ID))
                .thenReturn(Set.of(CUSTOMER_SERVICE_USER_ID));

        service().addMessageForUser(1L, OWNER_ID, false, false, false, "补充材料", null);

        verify(ticketMessageMapper).insert(any(TicketMessage.class));
        verify(chatEventBroadcaster).broadcastThreadMessage(
                eq("ticket"), eq(1L), eq(CUSTOMER_SERVICE_USER_ID), eq(OWNER_ID),
                eq("补充材料"), isNull(), any());
    }

    // ── 状态并发安全（CAS） ─────────────────────────────────

    @Test
    @DisplayName("同一处理人重复指派是幂等的：不写库、不重复通知")
    void assignSameAssigneeTwiceIsIdempotent() {
        Ticket ticket = merchantTicket(MERCHANT_ID);
        ticket.setStatus_wsh("processing");
        ticket.setAssignee_id_wsh(CUSTOMER_SERVICE_USER_ID);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        TicketDTO result = service().assignForStaff(
                1L, CUSTOMER_SERVICE_USER_ID, ADMIN_ID, true, false, false);

        assertEquals("processing", result.getStatus_wsh());
        verify(ticketMapper, never()).casAssign(any(), any(), any(), any());
        verify(notificationService, never()).create(any());
    }

    @Test
    @DisplayName("两个客服同时指派：后到者被拒绝，不覆盖先到者的处理人")
    void assignLosesRaceToAnotherAssignee() {
        Ticket stale = merchantTicket(MERCHANT_ID);
        Ticket latest = merchantTicket(MERCHANT_ID);
        latest.setStatus_wsh("processing");
        latest.setAssignee_id_wsh(88L);
        // assignForStaff 读一次做鉴权、委派给 assign 再读一次，CAS 失败后再读一次最新值
        when(ticketMapper.selectById(1L)).thenReturn(stale, stale, latest);
        when(userMapper.selectById(CUSTOMER_SERVICE_USER_ID)).thenReturn(user(CUSTOMER_SERVICE_USER_ID));
        when(ticketMapper.casAssign(1L, CUSTOMER_SERVICE_USER_ID, "pending", "processing")).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().assignForStaff(1L, CUSTOMER_SERVICE_USER_ID, ADMIN_ID, true, false, false));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("已被指派给其他人"));
        verify(notificationService, never()).create(any());
    }

    @Test
    @DisplayName("指派时工单已被关闭：CAS 失败后按状态冲突报错")
    void assignFailsWhenStatusChangedConcurrently() {
        Ticket stale = merchantTicket(MERCHANT_ID);
        Ticket latest = merchantTicket(MERCHANT_ID);
        latest.setStatus_wsh("closed");
        when(ticketMapper.selectById(1L)).thenReturn(stale, stale, latest);
        when(userMapper.selectById(CUSTOMER_SERVICE_USER_ID)).thenReturn(user(CUSTOMER_SERVICE_USER_ID));
        when(ticketMapper.casAssign(1L, CUSTOMER_SERVICE_USER_ID, "pending", "processing")).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().assignForStaff(1L, CUSTOMER_SERVICE_USER_ID, ADMIN_ID, true, false, false));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("状态已变化"));
    }

    @Test
    @DisplayName("同一结论重复解决是幂等的：不再补留言、不再发通知")
    void resolveSameResultTwiceIsIdempotent() {
        Ticket ticket = merchantTicket(MERCHANT_ID);
        ticket.setStatus_wsh("resolved");
        ticket.setResult_wsh("已处理");
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        TicketDTO result = service().resolveForStaff(1L, "已处理", ADMIN_ID, true, false, false);

        assertEquals("resolved", result.getStatus_wsh());
        verify(ticketMapper, never()).casResolve(any(), any(), any(), any(), any());
        verify(ticketMessageMapper, never()).insert(any(TicketMessage.class));
        verify(notificationService, never()).create(any());
    }

    @Test
    @DisplayName("已解决的工单不允许用不同结论覆盖")
    void resolveRejectsDifferentResult() {
        Ticket ticket = merchantTicket(MERCHANT_ID);
        ticket.setStatus_wsh("resolved");
        ticket.setResult_wsh("已处理");
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().resolveForStaff(1L, "换一个结论", ADMIN_ID, true, false, false));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("不能修改处理结果"));
        verify(ticketMapper, never()).casResolve(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("并发解决同结论：CAS 失败后按幂等返回，不产生第二条留言")
    void resolveConcurrentSameResultIsIdempotent() {
        Ticket stale = merchantTicket(MERCHANT_ID);
        Ticket latest = merchantTicket(MERCHANT_ID);
        latest.setStatus_wsh("resolved");
        latest.setResult_wsh("已处理");
        when(ticketMapper.selectById(1L)).thenReturn(stale, stale, latest);
        when(ticketMapper.casResolve(1L, "已处理", "resolved", "pending", "processing")).thenReturn(0);

        TicketDTO result = service().resolveForStaff(1L, "已处理", ADMIN_ID, true, false, false);

        assertEquals("resolved", result.getStatus_wsh());
        verify(ticketMessageMapper, never()).insert(any(TicketMessage.class));
        verify(notificationService, never()).create(any());
    }

    @Test
    @DisplayName("并发解决不同结论：后到者被拒绝")
    void resolveConcurrentDifferentResultIsRejected() {
        Ticket stale = merchantTicket(MERCHANT_ID);
        Ticket latest = merchantTicket(MERCHANT_ID);
        latest.setStatus_wsh("resolved");
        latest.setResult_wsh("别人写的结论");
        when(ticketMapper.selectById(1L)).thenReturn(stale, stale, latest);
        when(ticketMapper.casResolve(1L, "我的结论", "resolved", "pending", "processing")).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().resolveForStaff(1L, "我的结论", ADMIN_ID, true, false, false));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("状态已变化"));
        verify(ticketMessageMapper, never()).insert(any(TicketMessage.class));
    }

    @Test
    @DisplayName("已关闭的工单重复关闭是幂等的：不重复通知")
    void closeTwiceIsIdempotent() {
        Ticket ticket = merchantTicket(MERCHANT_ID);
        ticket.setStatus_wsh("closed");
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        TicketDTO result = service().closeForStaff(1L, ADMIN_ID, true, false, false);

        assertEquals("closed", result.getStatus_wsh());
        verify(ticketMapper, never()).casClose(any(), any(), any());
        verify(notificationService, never()).create(any());
    }

    @Test
    @DisplayName("并发关闭：CAS 失败后按幂等返回")
    void closeConcurrentIsIdempotent() {
        Ticket stale = merchantTicket(MERCHANT_ID);
        stale.setStatus_wsh("resolved");
        Ticket latest = merchantTicket(MERCHANT_ID);
        latest.setStatus_wsh("closed");
        when(ticketMapper.selectById(1L)).thenReturn(stale, stale, latest);
        when(ticketMapper.casClose(1L, "resolved", "closed")).thenReturn(0);

        TicketDTO result = service().closeForStaff(1L, ADMIN_ID, true, false, false);

        assertEquals("closed", result.getStatus_wsh());
        verify(notificationService, never()).create(any());
    }

    @Test
    @DisplayName("解决成功只发一条通知")
    void resolveSendsExactlyOneNotification() {
        Ticket ticket = merchantTicket(MERCHANT_ID);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);
        when(ticketMapper.casResolve(1L, "完成", "resolved", "pending", "processing")).thenReturn(1);

        service().resolveForStaff(1L, "完成", ADMIN_ID, true, false, false);

        verify(notificationService, times(1)).create(any());
    }

    // ── 工具 ────────────────────────────────────────────────

    private TicketServiceImpl service() {
        return new TicketServiceImpl(
                ticketMapper,
                ticketMessageMapper,
                userMapper,
                notificationService,
                orderMapper,
                merchantMapper,
                merchantCustomerServiceService,
                chatEventBroadcaster,
                issueAccess,
                issueNotifier);
    }

    private TicketCreateRequestDTO requestForOrder() {
        TicketCreateRequestDTO request = new TicketCreateRequestDTO();
        request.setOrder_id_wsh(ORDER_ID);
        request.setTitle_wsh("Need help");
        request.setContent_wsh("Question about my order");
        return request;
    }

    private PetOrder order(Long orderId, Long ownerId, Long merchantId) {
        PetOrder order = new PetOrder();
        order.setId_wsh(orderId);
        order.setOwner_id_wsh(ownerId);
        order.setMerchant_id_wsh(merchantId);
        return order;
    }

    private Ticket merchantTicket(Long merchantId) {
        Ticket ticket = new Ticket();
        ticket.setId_wsh(1L);
        ticket.setUser_id_wsh(OWNER_ID);
        ticket.setMerchant_id_wsh(merchantId);
        ticket.setTitle_wsh("Need help");
        ticket.setContent_wsh("Question about my order");
        ticket.setStatus_wsh("pending");
        return ticket;
    }

    private Merchant merchant(Long id) {
        Merchant merchant = new Merchant();
        merchant.setId_wsh(id);
        return merchant;
    }

    private User user(Long id) {
        User user = new User();
        user.setId_wsh(id);
        user.setNickname_wsh("客服");
        return user;
    }
}
