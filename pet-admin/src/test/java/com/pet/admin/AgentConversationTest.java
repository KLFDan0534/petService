package com.pet.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.ai.dto.AgentMessageRequestDTO;
import com.pet.ai.dto.AgentSessionDetail;
import com.pet.ai.dto.AgentSessionSummaryVO;
import com.pet.ai.dto.AgentTurnResult;
import com.pet.ai.service.AiChatHistoryService;
import com.pet.ai.service.LlmChatService;
import com.pet.ai.service.RagService;
import com.pet.ai.service.impl.AgentServiceImpl;
import com.pet.boarding.dto.MerchantDTO;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.service.MerchantService;
import com.pet.boarding.service.ServiceItemService;
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.service.KeeperService;
import com.pet.boarding.vo.KeeperVO;
import com.pet.common.StatusCode;
import com.pet.mq.MessageSender;
import com.pet.order.dto.OrderPreviewRequestDTO;
import com.pet.order.dto.OrderQuoteDTO;
import com.pet.order.service.OrderService;
import com.pet.order.service.PaymentService;
import com.pet.pet.dto.PetDTO;
import com.pet.pet.service.PetService;
import com.pet.system.entity.User;
import com.pet.system.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 会话式智能下单：多轮状态累积契约。
 * <p>
 * 核心命题：多轮 = 状态被反复 patch，而不是历史文本被反复喂给模型。
 * 因此「改成 5 晚」必须只改夜数，宠物 / 门店 / 日期原样保留，金额由后端重新报价。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("智能下单 · 多轮会话")
class AgentConversationTest {

    private static final Long USER_ID = 7L;
    private static final Long PET_ID = 11L;
    private static final Long MERCHANT_ID = 21L;
    private static final Long KEEPER_ID = 31L;
    private static final Long SERVICE_ID = 41L;

    /** 首轮：宠物团团、3 夜、寄养 */
    private static final String JSON_FIRST = """
            {"pet_name":"团团","pet_type":"金毛","service_type":"BOARDING","start_date":null,"nights":3,"merchant_keyword":null,"max_price_per_night":null,"unit":"night"}""";
    /** 续轮：只说「改成 5 晚」，其余字段必须为 null（服务端沿用旧值） */
    private static final String JSON_FIVE_NIGHTS = """
            {"pet_name":null,"pet_type":null,"service_type":null,"start_date":null,"nights":5,"merchant_keyword":null,"max_price_per_night":null,"unit":null}""";
    /** 续轮：只说「还是 3 晚吧」 */
    private static final String JSON_THREE_NIGHTS = """
            {"nights":3}""";

    @Mock private PetService petService;
    @Mock private MerchantService merchantService;
    @Mock private KeeperService keeperService;
    @Mock private ServiceItemService serviceItemService;
    @Mock private RagService ragService;
    @Mock private UserService userService;
    @Mock private OrderService orderService;
    @Mock private PaymentService paymentService;
    @Mock private MessageSender messageSender;
    @Mock private LlmChatService aiChatService;
    @Mock private AiChatHistoryService chatHistoryService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private InMemoryAgentSessionStore sessionStore;
    private AgentServiceImpl agentService;

    @BeforeEach
    void setUp() {
        sessionStore = new InMemoryAgentSessionStore();
        agentService = new AgentServiceImpl(
                petService, merchantService, keeperService, serviceItemService, userService,
                orderService, paymentService, messageSender, aiChatService, ragService, objectMapper,
                sessionStore, chatHistoryService, null);
    }

    // ── 会话生命周期 ─────────────────────────────────────────

    @Test
    @DisplayName("没发消息时历史会话列表为空（空会话不入库）")
    void noSessionBeforeAnyMessage() {
        assertTrue(agentService.listSessions(USER_ID).isEmpty());
        assertEquals(0, sessionStore.insertCount.get());
    }

    @Test
    @DisplayName("首条消息成功后才出现 1 条会话，并落两条消息")
    void firstMessageCreatesSessionAfterSuccess() {
        stubPlanFlow();
        when(aiChatService.chat(anyList())).thenReturn(JSON_FIRST);
        when(orderService.previewOrder(eq(USER_ID), any())).thenReturn(quote(new BigDecimal("450.00")));

        AgentTurnResult turn = agentService.message(USER_ID, request(null, "帮团团订 3 晚寄养"));

        assertEquals("await_confirm", turn.getStatus_wsh());
        assertNotNull(turn.getSession_id_wsh());
        assertNotNull(turn.getPlan_card());
        assertEquals(Integer.valueOf(3), turn.getPlan_card().getDays_wsh());

        List<AgentSessionSummaryVO> sessions = agentService.listSessions(USER_ID);
        assertEquals(1, sessions.size());
        assertEquals("帮团团订 3 晚寄养", sessions.get(0).getTitle_wsh());
        assertEquals("AWAIT_CONFIRM", sessions.get(0).getStatus_wsh());

        verify(chatHistoryService).saveSimple(USER_ID, turn.getSession_id_wsh(), "user", "帮团团订 3 晚寄养");
        verify(chatHistoryService).saveSimple(eq(USER_ID), eq(turn.getSession_id_wsh()), eq("assistant"), anyString());
    }

    @Test
    @DisplayName("首条消息模型两次都失败：不入库、不返回会话ID")
    void firstMessageLlmFailureLeavesNoSession() {
        when(aiChatService.chat(anyList())).thenReturn(null, null);

        AgentTurnResult turn = agentService.message(USER_ID, request(null, "随便说点什么"));

        assertEquals("failed", turn.getStatus_wsh());
        assertEquals("ask_requirement", turn.getNext_action_wsh());
        assertNull(turn.getSession_id_wsh());
        assertTrue(turn.getRequires_user_input_wsh());
        assertTrue(agentService.listSessions(USER_ID).isEmpty());
        assertEquals(0, sessionStore.insertCount.get());
        // 恰好请求两次：原始一次 + 修复一次，不多不少
        verify(aiChatService, times(2)).chat(anyList());
    }

    @Test
    @DisplayName("模型输出不合法时恰好重试 1 次，第二次合法即成功")
    void repairRetriedExactlyOnce() {
        stubPlanFlow();
        when(aiChatService.chat(anyList())).thenReturn("这不是 JSON", JSON_FIRST);
        when(orderService.previewOrder(eq(USER_ID), any())).thenReturn(quote(new BigDecimal("450.00")));

        AgentTurnResult turn = agentService.message(USER_ID, request(null, "帮团团订 3 晚"));

        assertEquals("await_confirm", turn.getStatus_wsh());
        verify(aiChatService, times(2)).chat(anyList());
    }

    // ── 多轮修订 ─────────────────────────────────────────────

    @Test
    @DisplayName("第 2 轮只说「改成 5 晚」：夜数变 5，宠物/门店保留，金额重新报价")
    void secondRoundOnlyPatchesNights() {
        stubPlanFlow();
        when(chatHistoryService.getSessionMessages(anyLong(), anyString())).thenReturn(List.of());
        when(aiChatService.chat(anyList())).thenReturn(JSON_FIRST, JSON_FIVE_NIGHTS);
        when(orderService.previewOrder(eq(USER_ID), any()))
                .thenReturn(quote(new BigDecimal("450.00")))
                .thenReturn(quote(new BigDecimal("750.00")));

        AgentTurnResult first = agentService.message(USER_ID, request(null, "帮团团订 3 晚寄养"));
        String sessionId = first.getSession_id_wsh();

        AgentTurnResult second = agentService.message(USER_ID, request(sessionId, "改成 5 晚吧"));

        assertEquals("await_confirm", second.getStatus_wsh());
        assertEquals(sessionId, second.getSession_id_wsh());
        assertEquals(Integer.valueOf(5), second.getPlan_card().getDays_wsh());
        // 未提及字段沿用：宠物、门店、看护人、服务都还在
        assertEquals("团团", second.getPlan_card().getPet_name_wsh());
        assertEquals("安心寄养", second.getPlan_card().getMerchant_name_wsh());
        assertEquals(KEEPER_ID, second.getPlan_card().getKeeper_id_wsh());
        // 金额只来自后端报价，且第 2 轮重新算过
        assertEquals(new BigDecimal("750.00"), second.getPlan_card().getTotal_price_wsh());
        verify(orderService, times(2)).previewOrder(eq(USER_ID), any());

        // 会话仍然只有 1 条，历史消息 4 条
        assertEquals(1, agentService.listSessions(USER_ID).size());
    }

    @Test
    @DisplayName("第 3 轮「还是 3 晚吧」能回退到 3 夜")
    void thirdRoundRevertsNights() {
        stubPlanFlow();
        when(chatHistoryService.getSessionMessages(anyLong(), anyString())).thenReturn(List.of());
        when(aiChatService.chat(anyList())).thenReturn(JSON_FIRST, JSON_FIVE_NIGHTS, JSON_THREE_NIGHTS);
        when(orderService.previewOrder(eq(USER_ID), any())).thenReturn(quote(new BigDecimal("450.00")));

        String sessionId = agentService.message(USER_ID, request(null, "帮团团订 3 晚")).getSession_id_wsh();
        agentService.message(USER_ID, request(sessionId, "改成 5 晚"));
        AgentTurnResult third = agentService.message(USER_ID, request(sessionId, "算了还是 3 晚吧"));

        assertEquals(Integer.valueOf(3), third.getPlan_card().getDays_wsh());
        assertEquals("团团", third.getPlan_card().getPet_name_wsh());
    }

    @Test
    @DisplayName("第 2 轮换门店：merchant_keyword 生效，门店随之改变")
    void secondRoundSwitchesMerchant() {
        when(petService.getPetsByOwner(USER_ID)).thenReturn(List.of(pet()));
        when(merchantService.searchNearby(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(merchant(), merchantB()));
        when(keeperService.searchNearby(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(keeper(), keeperB()));
        when(serviceItemService.listEnabledByMerchant(any())).thenReturn(List.of(serviceItem()));
        when(chatHistoryService.getSessionMessages(anyLong(), anyString())).thenReturn(List.of());
        when(aiChatService.chat(anyList())).thenReturn(JSON_FIRST,
                """
                        {"merchant_keyword":"衡山路"}""");
        when(orderService.previewOrder(eq(USER_ID), any())).thenReturn(quote(new BigDecimal("450.00")));

        String sessionId = agentService.message(USER_ID, request(null, "帮团团订 3 晚")).getSession_id_wsh();
        AgentTurnResult second = agentService.message(USER_ID, request(sessionId, "换到衡山路那家"));

        assertEquals("衡山路宠物", second.getPlan_card().getMerchant_name_wsh());
        assertEquals(Integer.valueOf(3), second.getPlan_card().getDays_wsh());
    }

    @Test
    @DisplayName("续轮两次都解析失败：上一版方案不丢，只追加澄清消息")
    void failedRoundKeepsPreviousPlan() {
        stubPlanFlow();
        when(chatHistoryService.getSessionMessages(anyLong(), anyString())).thenReturn(List.of());
        when(aiChatService.chat(anyList())).thenReturn(JSON_FIRST, "乱码", "还是乱码");
        when(orderService.previewOrder(eq(USER_ID), any())).thenReturn(quote(new BigDecimal("450.00")));

        String sessionId = agentService.message(USER_ID, request(null, "帮团团订 3 晚")).getSession_id_wsh();
        AgentTurnResult second = agentService.message(USER_ID, request(sessionId, "嗯嗯嗯"));

        assertEquals("failed", second.getStatus_wsh());
        assertEquals(sessionId, second.getSession_id_wsh());

        AgentSessionDetail detail = agentService.getSession(USER_ID, sessionId);
        assertEquals("AWAIT_CONFIRM", detail.getStatus_wsh());
        assertNotNull(detail.getPlan_card(), "解析失败不应清空已有方案");
        assertEquals(Integer.valueOf(3), detail.getPlan_card().getDays_wsh());
    }

    @Test
    @DisplayName("必填信息缺失时进入 COLLECTING 并给出 next_action（会话仍然保留）")
    void missingNightsGoesToCollecting() {
        when(petService.getPetsByOwner(USER_ID)).thenReturn(List.of(pet()));
        when(chatHistoryService.getSessionMessages(anyLong(), anyString())).thenReturn(List.of());
        when(aiChatService.chat(anyList())).thenReturn("""
                {"pet_name":"团团","service_type":"BOARDING","unit":"night"}""");

        AgentTurnResult turn = agentService.message(USER_ID, request(null, "帮团团订寄养"));

        assertEquals("collecting", turn.getStatus_wsh());
        assertEquals("ask_days", turn.getNext_action_wsh());
        assertNull(turn.getPlan_card());
        assertNotNull(turn.getSession_id_wsh());
        assertEquals("COLLECTING", agentService.listSessions(USER_ID).get(0).getStatus_wsh());

        // 补一句「3 晚」即可在原会话上继续
        when(aiChatService.chat(anyList())).thenReturn("""
                {"nights":3}""");
        when(merchantService.searchNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(merchant()));
        when(keeperService.searchNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(keeper()));
        when(serviceItemService.listEnabledByMerchant(any())).thenReturn(List.of(serviceItem()));
        when(orderService.previewOrder(eq(USER_ID), any())).thenReturn(quote(new BigDecimal("450.00")));

        AgentTurnResult second = agentService.message(USER_ID, request(turn.getSession_id_wsh(), "住 3 晚"));
        assertEquals("await_confirm", second.getStatus_wsh());
        assertEquals(Integer.valueOf(3), second.getPlan_card().getDays_wsh());
        assertEquals("团团", second.getPlan_card().getPet_name_wsh());
    }

    @Test
    @DisplayName("已下单的会话拒绝继续对话（一会话一单）")
    void confirmedSessionRejectsFurtherMessages() {
        stubPlanFlow();
        when(aiChatService.chat(anyList())).thenReturn(JSON_FIRST);
        when(orderService.previewOrder(eq(USER_ID), any())).thenReturn(quote(new BigDecimal("450.00")));
        when(userService.getById(USER_ID)).thenReturn(user());
        when(orderService.createOrder(eq(USER_ID), any())).thenReturn(orderDto());
        when(paymentService.createPayment(eq(USER_ID), anyString(), eq("balance"))).thenReturn(payment());

        String sessionId = agentService.message(USER_ID, request(null, "帮团团订 3 晚")).getSession_id_wsh();
        agentService.confirm(USER_ID, sessionId, false, null);

        com.pet.common.BusinessException e = org.junit.jupiter.api.Assertions.assertThrows(
                com.pet.common.BusinessException.class,
                () -> agentService.message(USER_ID, request(sessionId, "再改 5 晚")));
        assertTrue(e.getMessage().contains("已完成下单"));
    }

    @Test
    @DisplayName("并发下单：本轮处理期间会话被确认，状态写入不得覆盖 CONFIRMED")
    void concurrentConfirmIsNotOverwrittenByTurnWrite() {
        stubPlanFlow();
        when(chatHistoryService.getSessionMessages(anyLong(), anyString())).thenReturn(List.of());
        when(aiChatService.chat(anyList())).thenReturn(JSON_FIRST, JSON_FIVE_NIGHTS);
        when(orderService.previewOrder(eq(USER_ID), any())).thenReturn(quote(new BigDecimal("450.00")));

        String sessionId = agentService.message(USER_ID, request(null, "帮团团订 3 晚")).getSession_id_wsh();

        // 模拟另一个请求在这一轮处理期间先完成了下单
        sessionStore.simulateConcurrentConfirm = true;

        com.pet.common.BusinessException e = org.junit.jupiter.api.Assertions.assertThrows(
                com.pet.common.BusinessException.class,
                () -> agentService.message(USER_ID, request(sessionId, "改成 5 晚")));
        assertTrue(e.getMessage().contains("已完成下单"));

        assertEquals("CONFIRMED", agentService.listSessions(USER_ID).get(0).getStatus_wsh());
        // 只有首轮的两条消息，本轮被整体回滚
        verify(chatHistoryService, times(2)).saveSimple(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("前端预生成会话ID：首轮成功后用的就是这个ID（重发不会多建会话）")
    void clientProvidedSessionIdIsUsed() {
        stubPlanFlow();
        when(aiChatService.chat(anyList())).thenReturn(JSON_FIRST);
        when(orderService.previewOrder(eq(USER_ID), any())).thenReturn(quote(new BigDecimal("450.00")));

        AgentTurnResult turn = agentService.message(USER_ID, request("client-session-1", "帮团团订 3 晚"));

        assertEquals("client-session-1", turn.getSession_id_wsh());
        List<AgentSessionSummaryVO> sessions = agentService.listSessions(USER_ID);
        assertEquals(1, sessions.size());
        assertEquals("client-session-1", sessions.get(0).getSession_id_wsh());
    }

    @Test
    @DisplayName("同一 request_id 重发：不重复调模型、不重复记消息")
    void duplicateRequestIsReplayed() {
        stubPlanFlow();
        when(aiChatService.chat(anyList())).thenReturn(JSON_FIRST);
        when(orderService.previewOrder(eq(USER_ID), any())).thenReturn(quote(new BigDecimal("450.00")));

        AgentTurnResult first = agentService.message(USER_ID, request(null, "帮团团订 3 晚", "req-1"));
        AgentTurnResult again = agentService.message(
                USER_ID, request(first.getSession_id_wsh(), "帮团团订 3 晚", "req-1"));

        assertEquals(first.getSession_id_wsh(), again.getSession_id_wsh());
        assertEquals("await_confirm", again.getStatus_wsh());
        assertNotNull(again.getPlan_card());
        assertEquals(first.getPlan_card().getTotal_price_wsh(), again.getPlan_card().getTotal_price_wsh());
        // 模型只调了一次，消息也只落了两条（一问一答）
        verify(aiChatService, times(1)).chat(anyList());
        verify(chatHistoryService, times(2)).saveSimple(anyLong(), anyString(), anyString(), anyString());
        assertEquals(1, agentService.listSessions(USER_ID).size());
    }

    @Test
    @DisplayName("跨天会话里用户给过的日期已过期：清掉并请他重说，不把过期日期带到下单")
    void expiredStartDateAsksForNewDate() {        when(petService.getPetsByOwner(USER_ID)).thenReturn(List.of(pet()));
        when(aiChatService.chat(anyList())).thenReturn("""
                {"pet_name":"团团","service_type":"BOARDING","start_date":"2020-01-01","nights":3,"unit":"night"}""");

        AgentTurnResult turn = agentService.message(USER_ID, request(null, "2020 年 1 月 1 日订 3 晚"));

        assertEquals("collecting", turn.getStatus_wsh());
        assertEquals("ask_days", turn.getNext_action_wsh());
        assertTrue(turn.getReply_wsh().contains("过期"));
    }

    @Test
    @DisplayName("nights 是小数时按格式错误处理，只重试 1 次后降级")
    void fractionalNightsIsRejectedAsInvalid() {
        when(aiChatService.chat(anyList())).thenReturn("{\"nights\":3.5}", "{\"nights\":3.5}");

        AgentTurnResult turn = agentService.message(USER_ID, request(null, "住 3 天半吧"));

        assertEquals("failed", turn.getStatus_wsh());
        assertNull(turn.getSession_id_wsh());
        verify(aiChatService, times(2)).chat(anyList());
    }

    @Test
    @DisplayName("删除会话后详情不可读，列表清空")
    void deleteSessionRemovesEverything() {
        stubPlanFlow();
        when(aiChatService.chat(anyList())).thenReturn(JSON_FIRST);
        when(orderService.previewOrder(eq(USER_ID), any())).thenReturn(quote(new BigDecimal("450.00")));

        String sessionId = agentService.message(USER_ID, request(null, "帮团团订 3 晚")).getSession_id_wsh();
        agentService.deleteSession(USER_ID, sessionId);

        assertTrue(agentService.listSessions(USER_ID).isEmpty());
        verify(chatHistoryService).clearSession(USER_ID, sessionId);
    }

    // ── 工具 ────────────────────────────────────────────────

    private AgentMessageRequestDTO request(String sessionId, String content) {
        return request(sessionId, content, null);
    }

    private AgentMessageRequestDTO request(String sessionId, String content, String requestId) {
        AgentMessageRequestDTO dto = new AgentMessageRequestDTO();
        dto.setSession_id_wsh(sessionId);
        dto.setRequest_id_wsh(requestId);
        dto.setContent_wsh(content);
        dto.setLatitude_wsh(30.0);
        dto.setLongitude_wsh(120.0);
        return dto;
    }

    @Test
    @DisplayName("会话ID已被别人占用：返回可读的 400，不是 500，也不覆盖别人的会话")
    void occupiedSessionIdIsRejected() {
        stubPlanFlow();
        when(aiChatService.chat(anyList())).thenReturn(JSON_FIRST);
        when(orderService.previewOrder(anyLong(), any())).thenReturn(quote(new BigDecimal("450.00")));

        String sid = agentService.message(USER_ID, request("shared-id", "帮团团订 3 晚")).getSession_id_wsh();
        assertEquals("shared-id", sid);

        // 另一个用户拿着同一个 ID 进来：库里查不到（按用户过滤），插入时撞主键
        com.pet.common.BusinessException e = org.junit.jupiter.api.Assertions.assertThrows(
                com.pet.common.BusinessException.class,
                () -> agentService.message(999L, request("shared-id", "帮团团订 3 晚")));

        assertEquals(400, e.getCode());
        // 原会话不受影响
        assertEquals("AWAIT_CONFIRM", agentService.listSessions(USER_ID).get(0).getStatus_wsh());
        assertTrue(agentService.listSessions(999L).isEmpty());
    }

    @Test
    @DisplayName("带事务模板的生产分支同样能落库（覆盖 TransactionTemplate 那条路径）")
    void transactionalPathPersists() {
        AgentServiceImpl withTx = new AgentServiceImpl(
                petService, merchantService, keeperService, serviceItemService, userService,
                orderService, paymentService, messageSender, aiChatService, ragService, objectMapper,
                sessionStore, chatHistoryService, new NoopTransactionManager());

        stubPlanFlow();
        when(aiChatService.chat(anyList())).thenReturn(JSON_FIRST);
        when(orderService.previewOrder(eq(USER_ID), any())).thenReturn(quote(new BigDecimal("450.00")));

        AgentTurnResult turn = withTx.message(USER_ID, request(null, "帮团团订 3 晚"));

        assertEquals("await_confirm", turn.getStatus_wsh());
        assertEquals(1, sessionStore.insertCount.get());
        assertEquals(1, withTx.listSessions(USER_ID).size());
        verify(chatHistoryService, times(2)).saveSimple(anyLong(), anyString(), anyString(), anyString());
    }

    /** 只做「开/提交/回滚」的空事务管理器，用来验证带事务模板的分支能正常走完。 */
    private static final class NoopTransactionManager implements org.springframework.transaction.PlatformTransactionManager {
        @Override
        public org.springframework.transaction.TransactionStatus getTransaction(
                org.springframework.transaction.TransactionDefinition definition) {
            return new org.springframework.transaction.support.SimpleTransactionStatus(true);
        }

        @Override
        public void commit(org.springframework.transaction.TransactionStatus status) {
        }

        @Override
        public void rollback(org.springframework.transaction.TransactionStatus status) {
        }
    }

    private void stubPlanFlow() {
        when(petService.getPetsByOwner(USER_ID)).thenReturn(List.of(pet()));
        when(merchantService.searchNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(merchant()));
        when(keeperService.searchNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(keeper()));
        when(serviceItemService.listEnabledByMerchant(any())).thenReturn(List.of(serviceItem()));
    }

    private OrderQuoteDTO quote(BigDecimal finalAmount) {
        OrderQuoteDTO quote = new OrderQuoteDTO();
        quote.setBilling_unit_wsh("night");
        quote.setUnit_price_wsh(new BigDecimal("150"));
        quote.setTotal_amount_wsh(finalAmount);
        quote.setFinal_amount_wsh(finalAmount);
        return quote;
    }

    private User user() {
        User user = new User();
        user.setId_wsh(USER_ID);
        user.setUsername_wsh("owner");
        user.setNickname_wsh("宠物主人");
        user.setPhone_wsh("13800000000");
        user.setLatitude_wsh(BigDecimal.valueOf(30.0));
        user.setLongitude_wsh(BigDecimal.valueOf(120.0));
        return user;
    }

    private PetDTO pet() {
        PetDTO pet = new PetDTO();
        pet.setId_wsh(PET_ID);
        pet.setOwner_id_wsh(USER_ID);
        pet.setName_wsh("团团");
        pet.setType_wsh("狗");
        pet.setBreed_wsh("金毛");
        return pet;
    }

    private MerchantDTO merchant() {
        MerchantDTO merchant = new MerchantDTO();
        merchant.setId_wsh(MERCHANT_ID);
        merchant.setName_wsh("安心寄养");
        merchant.setAddress_wsh("测试地址");
        merchant.setRating_wsh(BigDecimal.valueOf(4.8));
        return merchant;
    }

    private MerchantDTO merchantB() {
        MerchantDTO merchant = new MerchantDTO();
        merchant.setId_wsh(22L);
        merchant.setName_wsh("衡山路宠物");
        merchant.setAddress_wsh("衡山路 1 号");
        merchant.setRating_wsh(BigDecimal.valueOf(4.5));
        return merchant;
    }

    private KeeperVO keeper() {
        KeeperVO keeper = new KeeperVO();
        keeper.setId_wsh(KEEPER_ID);
        keeper.setName_wsh("小王");
        keeper.setMerchant_id_wsh(MERCHANT_ID);
        keeper.setMerchant_name_wsh("安心寄养");
        keeper.setRating_wsh(BigDecimal.valueOf(4.9));
        keeper.setDistance_wsh(1.2);
        keeper.setCompletion_rate_wsh(BigDecimal.valueOf(98));
        keeper.setComplaint_rate_wsh(BigDecimal.ZERO);
        return keeper;
    }

    private KeeperVO keeperB() {
        KeeperVO keeper = new KeeperVO();
        keeper.setId_wsh(32L);
        keeper.setName_wsh("小李");
        keeper.setMerchant_id_wsh(22L);
        keeper.setMerchant_name_wsh("衡山路宠物");
        keeper.setRating_wsh(BigDecimal.valueOf(4.5));
        keeper.setDistance_wsh(0.8);
        keeper.setCompletion_rate_wsh(BigDecimal.valueOf(95));
        keeper.setComplaint_rate_wsh(BigDecimal.ZERO);
        return keeper;
    }

    private ServiceItem serviceItem() {
        ServiceItem service = new ServiceItem();
        service.setId_wsh(SERVICE_ID);
        service.setMerchant_id_wsh(MERCHANT_ID);
        service.setName_wsh("标准寄养");
        service.setType_wsh("BOARDING_STANDARD");
        service.setUnit_wsh("night");
        service.setPrice_wsh(new BigDecimal("150"));
        service.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        return service;
    }

    private com.pet.order.dto.OrderDTO orderDto() {
        com.pet.order.dto.OrderDTO order = new com.pet.order.dto.OrderDTO();
        order.setOrder_no_wsh("ORD-AI-CONV");
        return order;
    }

    private com.pet.order.entity.Payment payment() {
        com.pet.order.entity.Payment payment = new com.pet.order.entity.Payment();
        payment.setPay_no_wsh("PAY-AI-CONV");
        payment.setOrder_no_wsh("ORD-AI-CONV");
        payment.setStatus_wsh("pending");
        return payment;
    }
}
