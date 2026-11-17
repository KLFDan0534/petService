package com.pet.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.customer.dto.CsMerchantDTO;
import com.pet.customer.dto.CsThreadDTO;
import com.pet.customer.entity.Complaint;
import com.pet.customer.entity.ComplaintMessage;
import com.pet.customer.entity.Ticket;
import com.pet.customer.entity.TicketMessage;
import com.pet.customer.mapper.ChatMessageMapper;
import com.pet.customer.mapper.ComplaintMapper;
import com.pet.customer.mapper.ComplaintMessageMapper;
import com.pet.customer.mapper.TicketMapper;
import com.pet.customer.mapper.TicketMessageMapper;
import com.pet.customer.service.IssueAccess;
import com.pet.customer.service.MerchantCustomerServiceService;
import com.pet.customer.service.impl.CsWorkbenchServiceImpl;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 客服工作台契约。
 * <p>
 * 工作台是投诉与工单的第三个参与者，之前把 {@code staffMerchantIds} 又抄了一份。
 * 这里锁定：商家范围复用统一实现、两类线程合并排序、type 只负责分发、已读标记按类型路由。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("客服工作台契约")
class CsWorkbenchServiceTest {

    private static final long STAFF_ID = 40L;
    private static final long MERCHANT_ID = 30L;
    private static final long OWNER_ID = 10L;
    private static final long KEEPER_ID = 55L;

    @Mock private TicketMapper ticketMapper;
    @Mock private ComplaintMapper complaintMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private ChatMessageMapper chatMessageMapper;
    @Mock private UserMapper userMapper;
    @Mock private MerchantCustomerServiceService merchantCustomerServiceService;
    @Mock private ComplaintMessageMapper complaintMessageMapper;
    @Mock private TicketMessageMapper ticketMessageMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private KeeperMapper keeperMapper;

    private CsWorkbenchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CsWorkbenchServiceImpl(
                ticketMapper, complaintMapper, merchantMapper, chatMessageMapper, userMapper,
                merchantCustomerServiceService, complaintMessageMapper, ticketMessageMapper,
                orderMapper, keeperMapper, new IssueAccess(merchantMapper, merchantCustomerServiceService));
    }

    // ── 统计 ────────────────────────────────────────────────

    @Test
    @DisplayName("管理员统计不限商家范围，商家数取全表")
    void adminStatsAreGlobal() {
        when(ticketMapper.selectCount(any())).thenReturn(5L);
        when(complaintMapper.selectCount(any())).thenReturn(2L);
        when(merchantMapper.selectCount(isNull())).thenReturn(9L);

        var stats = service.stats(STAFF_ID, true, false, false);

        assertEquals(5L, stats.getPending_tickets_wsh());
        assertEquals(5L, stats.getMy_processing_tickets_wsh());
        assertEquals(2L, stats.getPending_complaints_wsh());
        assertEquals(9L, stats.getMerchant_count_wsh());
    }

    @Test
    @DisplayName("非管理员且无授权商家时各项统计为 0，且不查工单/投诉表")
    void statsAreZeroWhenNoScope() {
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(merchantCustomerServiceService.getApprovedMerchantIds(STAFF_ID)).thenReturn(Set.of());

        var stats = service.stats(STAFF_ID, false, true, true);

        assertEquals(0L, stats.getPending_tickets_wsh());
        assertEquals(0L, stats.getPending_complaints_wsh());
        assertEquals(0L, stats.getMerchant_count_wsh());
        // 「我处理中」按 assignee 统计，不受商家范围限制（既有语义），因此仍会查一次工单表
        assertEquals(0L, stats.getMy_processing_tickets_wsh());
        verify(complaintMapper, never()).selectCount(any());
    }

    // ── 商家列表 ────────────────────────────────────────────

    @Test
    @DisplayName("商家列表合并「本人商家」与「授权客服商家」并去重")
    void merchantsMergeAndDedupe() {
        Merchant owned = new Merchant();
        owned.setId_wsh(MERCHANT_ID);
        owned.setName_wsh("安心寄养");
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(owned);
        // 授权集合里重复出现同一个商家，验证去重
        when(merchantCustomerServiceService.getApprovedMerchantIds(STAFF_ID))
                .thenReturn(Set.of(MERCHANT_ID, 31L));
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(owned);
        Merchant other = new Merchant();
        other.setId_wsh(31L);
        other.setName_wsh("另一家");
        when(merchantMapper.selectById(31L)).thenReturn(other);

        List<CsMerchantDTO> result = service.merchants(STAFF_ID);

        assertEquals(2, result.size(), "同一商家不应重复出现");
        assertTrue(result.stream().anyMatch(m -> MERCHANT_ID == m.getMerchant_id_wsh()));
        assertTrue(result.stream().anyMatch(m -> 31L == m.getMerchant_id_wsh()));
    }

    @Test
    @DisplayName("商家已不存在时跳过，不产生空条目")
    void merchantsSkipMissing() {
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(merchantCustomerServiceService.getApprovedMerchantIds(STAFF_ID)).thenReturn(Set.of(77L));
        when(merchantMapper.selectById(77L)).thenReturn(null);

        assertTrue(service.merchants(STAFF_ID).isEmpty());
    }

    // ── 线程列表 ────────────────────────────────────────────

    @Test
    @DisplayName("无授权商家时线程列表直接为空，不查两类业务表")
    void threadsEmptyWhenNoScope() {
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(merchantCustomerServiceService.getApprovedMerchantIds(STAFF_ID)).thenReturn(Set.of());

        assertTrue(service.threads(STAFF_ID, false, true, true).isEmpty());
        verify(complaintMapper, never()).selectList(any());
        verify(ticketMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("投诉与工单合并成统一线程，并按最后消息时间倒序")
    void threadsMergeBothTypesSortedByLastTime() {
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownedMerchant());
        when(merchantCustomerServiceService.getApprovedMerchantIds(STAFF_ID)).thenReturn(Set.of());

        Complaint complaint = new Complaint();
        complaint.setId_wsh(1L);
        complaint.setMerchant_id_wsh(MERCHANT_ID);
        complaint.setOwner_id_wsh(OWNER_ID);
        complaint.setTitle_wsh("投诉标题");
        complaint.setStatus_wsh("processing");
        when(complaintMapper.selectList(any())).thenReturn(List.of(complaint));

        Ticket ticket = new Ticket();
        ticket.setId_wsh(2L);
        ticket.setMerchant_id_wsh(MERCHANT_ID);
        ticket.setUser_id_wsh(OWNER_ID);
        ticket.setTitle_wsh("工单标题");
        ticket.setStatus_wsh("pending");
        when(ticketMapper.selectList(any())).thenReturn(List.of(ticket));

        ComplaintMessage complaintMessage = new ComplaintMessage();
        complaintMessage.setComplaint_id_wsh(1L);
        complaintMessage.setContent_wsh("投诉最新留言");
        complaintMessage.setCreated_at_wsh(LocalDateTime.of(2026, 11, 1, 10, 0));
        when(complaintMessageMapper.selectLatestByComplaintIds(any()))
                .thenReturn(List.of(complaintMessage));

        TicketMessage ticketMessage = new TicketMessage();
        ticketMessage.setTicket_id_wsh(2L);
        ticketMessage.setContent_wsh("工单最新留言");
        ticketMessage.setCreated_at_wsh(LocalDateTime.of(2026, 11, 2, 10, 0));
        when(ticketMessageMapper.selectLatestByTicketIds(any())).thenReturn(List.of(ticketMessage));

        when(complaintMessageMapper.selectUnreadCountByComplaintIds(any(), anyLong()))
                .thenReturn(List.of(Map.of("complaintId", 1L, "cnt", 3L)));
        when(ticketMessageMapper.selectUnreadCountByTicketIds(any(), anyLong()))
                .thenReturn(List.of(Map.of("ticketId", 2L, "cnt", 1L)));

        List<CsThreadDTO> threads = service.threads(STAFF_ID, false, true, true);

        assertEquals(2, threads.size());
        // 工单留言更晚，应排在前面
        assertEquals("ticket", threads.get(0).getType_wsh());
        assertEquals("complaint", threads.get(1).getType_wsh());
        assertEquals(1L, threads.get(0).getUnread_count_wsh());
        assertEquals(3L, threads.get(1).getUnread_count_wsh());
        assertEquals("工单最新留言", threads.get(0).getLast_message_wsh());
    }

    @Test
    @DisplayName("type 分支只负责分发已读标记，未知类型静默忽略")
    void markThreadReadDispatchesByType() {
        service.markThreadRead(STAFF_ID, "complaint", 1L);
        verify(complaintMessageMapper).markReadByComplaintId(1L, STAFF_ID);

        service.markThreadRead(STAFF_ID, "ticket", 2L);
        verify(ticketMessageMapper).markReadByTicketId(2L, STAFF_ID);

        service.markThreadRead(STAFF_ID, "unknown", 3L);
        verify(complaintMessageMapper, never()).markReadByComplaintId(eq(3L), any());
        verify(ticketMessageMapper, never()).markReadByTicketId(eq(3L), any());
    }

    @Test
    @DisplayName("type 为空或 ID 为空时不做任何更新")
    void markThreadReadIgnoresInvalidInput() {
        service.markThreadRead(null, "complaint", 1L);
        service.markThreadRead(STAFF_ID, "complaint", null);

        verify(complaintMessageMapper, never()).markReadByComplaintId(any(), any());
    }

    // ── 线程列表：批量解析（N+1 治理） ──────────────────────

    @Test
    @DisplayName("线程列表用一次 IN 查询解析所有用户名，不再逐条 selectById")
    void threadsResolveDisplayNamesInOneBatchQuery() {
        stubScope();
        when(complaintMapper.selectList(any())).thenReturn(List.of(complaint(1L, "processing")));
        when(ticketMapper.selectList(any())).thenReturn(List.of(ticket(2L, "pending")));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user(OWNER_ID, "宠物主人")));

        List<CsThreadDTO> threads = service.threads(STAFF_ID, false, true, true);

        assertEquals(2, threads.size());
        assertEquals("宠物主人", threads.get(0).getOther_user_name_wsh());
        assertEquals("宠物主人", threads.get(1).getOther_user_name_wsh());
        // 关键：2 条线程只查 1 次用户
        verify(userMapper, times(1)).selectBatchIds(any());
        verify(userMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("用户不存在时用户名回退成 ID 字符串")
    void threadsFallBackToUserIdWhenUserMissing() {
        stubScope();
        when(complaintMapper.selectList(any())).thenReturn(List.of(complaint(1L, "pending")));
        when(ticketMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.selectBatchIds(any())).thenReturn(List.of());

        List<CsThreadDTO> threads = service.threads(STAFF_ID, false, true, true);

        assertEquals(1, threads.size());
        assertEquals(String.valueOf(OWNER_ID), threads.get(0).getOther_user_name_wsh());
    }

    @Test
    @DisplayName("投诉人自己看线程时，「对方」显示被投诉商家而不是自己")
    void threadsComplaintOwnerSeesTargetMerchant() {
        stubScope();
        Complaint complaint = complaint(1L, "processing");
        complaint.setOwner_id_wsh(STAFF_ID);
        complaint.setTarget_type_wsh("merchant");
        complaint.setTarget_id_wsh(MERCHANT_ID);
        when(complaintMapper.selectList(any())).thenReturn(List.of(complaint));
        when(ticketMapper.selectList(any())).thenReturn(List.of());

        Merchant target = new Merchant();
        target.setId_wsh(MERCHANT_ID);
        target.setUser_id_wsh(77L);
        when(merchantMapper.selectBatchIds(any())).thenReturn(List.of(target));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user(77L, "商家老板")));

        List<CsThreadDTO> threads = service.threads(STAFF_ID, false, true, true);

        assertEquals(77L, threads.get(0).getOther_user_id_wsh());
        assertEquals("商家老板", threads.get(0).getOther_user_name_wsh());
    }

    @Test
    @DisplayName("最后消息时间相同时，投诉排在工单前（稳定排序）")
    void threadsKeepStableOrderOnEqualLastTime() {
        stubScope();
        when(complaintMapper.selectList(any())).thenReturn(List.of(complaint(1L, "processing")));
        when(ticketMapper.selectList(any())).thenReturn(List.of(ticket(2L, "pending")));

        LocalDateTime same = LocalDateTime.of(2026, 11, 10, 9, 0);
        ComplaintMessage cm = new ComplaintMessage();
        cm.setComplaint_id_wsh(1L);
        cm.setContent_wsh("投诉留言");
        cm.setCreated_at_wsh(same);
        when(complaintMessageMapper.selectLatestByComplaintIds(any())).thenReturn(List.of(cm));

        TicketMessage tm = new TicketMessage();
        tm.setTicket_id_wsh(2L);
        tm.setContent_wsh("工单留言");
        tm.setCreated_at_wsh(same);
        when(ticketMessageMapper.selectLatestByTicketIds(any())).thenReturn(List.of(tm));

        List<CsThreadDTO> threads = service.threads(STAFF_ID, false, true, true);

        assertEquals(2, threads.size());
        assertEquals("complaint", threads.get(0).getType_wsh());
        assertEquals("ticket", threads.get(1).getType_wsh());
    }

    @Test
    @DisplayName("只有投诉时线程列表正常返回，不查工单消息表")
    void threadsWithOnlyComplaints() {
        stubScope();
        when(complaintMapper.selectList(any())).thenReturn(List.of(complaint(1L, "pending")));
        when(ticketMapper.selectList(any())).thenReturn(List.of());

        List<CsThreadDTO> threads = service.threads(STAFF_ID, false, true, true);

        assertEquals(1, threads.size());
        assertEquals("complaint", threads.get(0).getType_wsh());
        verify(ticketMessageMapper, never()).selectLatestByTicketIds(any());
    }

    @Test
    @DisplayName("只有工单时线程列表正常返回，不查投诉消息表")
    void threadsWithOnlyTickets() {
        stubScope();
        when(complaintMapper.selectList(any())).thenReturn(List.of());
        when(ticketMapper.selectList(any())).thenReturn(List.of(ticket(2L, "pending")));

        List<CsThreadDTO> threads = service.threads(STAFF_ID, false, true, true);

        assertEquals(1, threads.size());
        assertEquals("ticket", threads.get(0).getType_wsh());
        verify(complaintMessageMapper, never()).selectLatestByComplaintIds(any());
    }

    @Test
    @DisplayName("已结束且没有留言的投诉不成线程")
    void threadsSkipClosedWithoutMessages() {
        stubScope();
        when(complaintMapper.selectList(any())).thenReturn(List.of(complaint(1L, "resolved")));
        when(ticketMapper.selectList(any())).thenReturn(List.of());

        assertTrue(service.threads(STAFF_ID, false, true, true).isEmpty());
    }

    @Test
    @DisplayName("非管理员查询必须带商家范围条件，管理员查询不带")
    void threadsScopeMerchantIdsIntoQuery() {
        // 非管理员：范围来自授权商家 → 条件不为空
        stubScope();
        when(complaintMapper.selectList(any())).thenReturn(List.of());
        when(ticketMapper.selectList(any())).thenReturn(List.of());

        service.threads(STAFF_ID, false, true, true);

        ArgumentCaptor<LambdaQueryWrapper> staffCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(complaintMapper).selectList(staffCaptor.capture());
        assertFalse(staffCaptor.getValue().isEmptyOfWhere(),
                "非管理员查询必须按商家范围过滤，不能退化成全表查询");

        // 管理员：范围为 null → 条件为空（不限商家）
        org.mockito.Mockito.clearInvocations(complaintMapper);
        when(complaintMapper.selectList(any())).thenReturn(List.of());

        service.threads(STAFF_ID, true, false, false);

        ArgumentCaptor<LambdaQueryWrapper> adminCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(complaintMapper).selectList(adminCaptor.capture());
        assertTrue(adminCaptor.getValue().isEmptyOfWhere(),
                "管理员查询不应带商家范围条件");
    }

    @Test
    @DisplayName("会话列表同样用一次 IN 查询解析用户名")
    void conversationsResolveNamesInOneBatchQuery() {
        com.pet.customer.entity.ChatMessage m1 = new com.pet.customer.entity.ChatMessage();
        m1.setFrom_user_id_wsh(STAFF_ID);
        m1.setTo_user_id_wsh(OWNER_ID);
        m1.setContent_wsh("你好");
        m1.setCreated_at_wsh(LocalDateTime.of(2026, 11, 3, 10, 0));

        com.pet.customer.entity.ChatMessage m2 = new com.pet.customer.entity.ChatMessage();
        m2.setFrom_user_id_wsh(KEEPER_ID);
        m2.setTo_user_id_wsh(STAFF_ID);
        m2.setContent_wsh("在的");
        m2.setCreated_at_wsh(LocalDateTime.of(2026, 11, 4, 10, 0));

        when(chatMessageMapper.selectConversations(STAFF_ID)).thenReturn(List.of(m1, m2));
        when(chatMessageMapper.selectUnreadGroupBySender(STAFF_ID)).thenReturn(List.of());
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(
                user(OWNER_ID, "宠物主人"), user(KEEPER_ID, "看护者小王")));

        var conversations = service.conversations(STAFF_ID);

        assertEquals(2, conversations.size());
        assertEquals("宠物主人", conversations.get(0).getOther_user_name_wsh());
        assertEquals("看护者小王", conversations.get(1).getOther_user_name_wsh());
        verify(userMapper, times(1)).selectBatchIds(any());
        verify(userMapper, never()).selectById(any());
    }

    private void stubScope() {
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownedMerchant());
        when(merchantCustomerServiceService.getApprovedMerchantIds(STAFF_ID)).thenReturn(Set.of());
    }

    private Complaint complaint(Long id, String status) {
        Complaint complaint = new Complaint();
        complaint.setId_wsh(id);
        complaint.setMerchant_id_wsh(MERCHANT_ID);
        complaint.setOwner_id_wsh(OWNER_ID);
        complaint.setTitle_wsh("投诉标题");
        complaint.setStatus_wsh(status);
        return complaint;
    }

    private Ticket ticket(Long id, String status) {
        Ticket ticket = new Ticket();
        ticket.setId_wsh(id);
        ticket.setMerchant_id_wsh(MERCHANT_ID);
        ticket.setUser_id_wsh(OWNER_ID);
        ticket.setTitle_wsh("工单标题");
        ticket.setStatus_wsh(status);
        return ticket;
    }

    private User user(Long id, String nickname) {
        User user = new User();
        user.setId_wsh(id);
        user.setNickname_wsh(nickname);
        user.setUsername_wsh("user" + id);
        return user;
    }

    private Merchant ownedMerchant() {
        Merchant merchant = new Merchant();
        merchant.setId_wsh(MERCHANT_ID);
        merchant.setName_wsh("安心寄养");
        return merchant;
    }
}
