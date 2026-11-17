package com.pet.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.ai.dto.AgentExecuteResult;
import com.pet.ai.dto.AgentMessageRequestDTO;
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
import com.pet.common.BusinessException;
import com.pet.common.StatusCode;
import com.pet.mq.MessageSender;
import com.pet.order.dto.OrderCreateRequestDTO;
import com.pet.order.dto.OrderDTO;
import com.pet.order.dto.OrderQuoteDTO;
import com.pet.order.entity.Payment;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 智能下单 confirm 阶段：支付授权与幂等契约。
 * <p>
 * 关键不变量：
 * 1. 不自动支付时只建「待支付订单 + 支付单」，绝不调用 pay；
 * 2. 自动支付必须校验密码，密码缺失/错误时**一个订单都不创建**，会话状态不动，可用同一会话重试；
 * 3. 同一会话只能下一单（status CAS），重复提交被拒绝。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("智能下单 · 支付授权与幂等")
class AgentPaymentAuthorizationTest {

    private static final Long USER_ID = 7L;
    private static final Long PET_ID = 11L;
    private static final Long MERCHANT_ID = 21L;
    private static final Long KEEPER_ID = 31L;
    private static final Long SERVICE_ID = 41L;
    private static final String ORDER_NO = "ORD-AI-001";
    private static final String PAY_NO = "PAY-AI-001";

    private static final String LLM_JSON = """
            {"pet_name":"团团","pet_type":"金毛","service_type":"BOARDING","start_date":null,"nights":3,"merchant_keyword":null,"max_price_per_night":null,"unit":"night"}""";

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

    @Test
    @DisplayName("不自动支付：只建待支付订单与支付单，不调用 pay、不校验密码")
    void confirmWithoutAutoPayCreatesPendingOrderAndPaymentOnly() {
        String sessionId = planSuccessfully();
        mockConfirmSuccess();

        AgentExecuteResult result = agentService.confirm(USER_ID, sessionId, false, null);

        assertEquals("pending_payment", result.getStatus());
        assertEquals("pending", result.getPayment_status_wsh());
        assertEquals(ORDER_NO, result.getOrderNo());
        assertEquals(PAY_NO, result.getPayNo());
        assertFalse(result.getRequires_user_input_wsh());
        assertTrue(result.getMessage_wsh().contains("待支付"));

        ArgumentCaptor<OrderCreateRequestDTO> captor = ArgumentCaptor.forClass(OrderCreateRequestDTO.class);
        verify(orderService).createOrder(eq(USER_ID), captor.capture());
        OrderCreateRequestDTO req = captor.getValue();
        assertEquals(PET_ID, req.getPet_id_wsh());
        assertEquals(MERCHANT_ID, req.getMerchant_id_wsh());
        assertEquals(SERVICE_ID, req.getService_id_wsh());
        assertEquals("night", req.getBilling_unit_wsh());
        assertEquals(new BigDecimal("100"), req.getExpected_unit_price_wsh());
        verify(paymentService).createPayment(USER_ID, ORDER_NO, "balance");
        verify(paymentService, never()).pay(any(), any());
        verify(userService, never()).verifyPaymentPassword(any(), any());
        // 下单结果写回对话历史，刷新页面后仍能看到订单号
        verify(chatHistoryService).saveSimple(eq(USER_ID), eq(sessionId), eq("assistant"), contains("订单号"));
    }

    @Test
    @DisplayName("自动支付 + 正确密码：校验通过后建单并支付")
    void confirmWithAutoPayAndPasswordPaysAfterAuthorization() {
        String sessionId = planSuccessfully();
        mockConfirmSuccess();

        AgentExecuteResult result = agentService.confirm(USER_ID, sessionId, true, "123456");

        assertEquals("success", result.getStatus());
        assertEquals("paid", result.getPayment_status_wsh());
        assertEquals("paid", result.getNext_action_wsh());
        assertFalse(result.getRequires_user_input_wsh());

        verify(userService).verifyPaymentPassword(USER_ID, "123456");
        verify(orderService).createOrder(eq(USER_ID), any(OrderCreateRequestDTO.class));
        verify(paymentService).pay(USER_ID, PAY_NO);
    }

    @Test
    @DisplayName("自动支付缺密码：先追问，不建单；补密码后同一会话可成功")
    void confirmWithoutPasswordAsksThenRetryWithSameSession() {
        String sessionId = planSuccessfully();
        mockConfirmSuccess();

        AgentExecuteResult first = agentService.confirm(USER_ID, sessionId, true, null);

        assertEquals("needs_user_input", first.getStatus());
        assertEquals("ask_payment_password", first.getNext_action_wsh());
        assertTrue(first.getRequires_user_input_wsh());
        assertTrue(first.getMessage_wsh().contains("支付密码"));
        assertNull(first.getOrderNo());
        verify(orderService, never()).createOrder(any(), any());
        verify(paymentService, never()).createPayment(any(), any(), any());
        verify(userService, never()).verifyPaymentPassword(any(), any());

        // 状态没有被占用，补上密码即可重试成功
        AgentExecuteResult second = agentService.confirm(USER_ID, sessionId, true, "123456");
        assertEquals("success", second.getStatus());
        verify(orderService).createOrder(eq(USER_ID), any(OrderCreateRequestDTO.class));
        verify(paymentService).pay(USER_ID, PAY_NO);
    }

    @Test
    @DisplayName("自动支付密码错误：不建单、不支付，提示重新输入")
    void confirmWithWrongPasswordAsksUserWithoutCreatingOrder() {
        String sessionId = planSuccessfully();
        doThrow(new BusinessException(403, "支付密码错误"))
                .when(userService).verifyPaymentPassword(USER_ID, "bad");

        AgentExecuteResult result = agentService.confirm(USER_ID, sessionId, true, "bad");

        assertEquals("needs_user_input", result.getStatus());
        assertEquals("ask_payment_password", result.getNext_action_wsh());
        assertEquals("支付密码错误", result.getMessage_wsh());
        assertNull(result.getOrderNo());

        verify(orderService, never()).createOrder(any(), any());
        verify(paymentService, never()).createPayment(any(), any(), any());
        verify(paymentService, never()).pay(any(), any());
    }

    @Test
    @DisplayName("重复提交同一会话：第二次被拒，订单只建一次")
    void confirmTwiceOnlyCreatesOneOrder() {
        String sessionId = planSuccessfully();
        mockConfirmSuccess();

        AgentExecuteResult first = agentService.confirm(USER_ID, sessionId, false, null);
        assertEquals("pending_payment", first.getStatus());

        BusinessException e = assertThrows(BusinessException.class,
                () -> agentService.confirm(USER_ID, sessionId, false, null));
        assertTrue(e.getMessage().contains("已下单"));

        verify(orderService, times(1)).createOrder(eq(USER_ID), any(OrderCreateRequestDTO.class));
        verify(paymentService, times(1)).createPayment(eq(USER_ID), any(), eq("balance"));
    }

    @Test
    @DisplayName("会话不存在时拒绝确认")
    void confirmWithUnknownSessionIsRejected() {
        assertThrows(BusinessException.class,
                () -> agentService.confirm(USER_ID, "not-exist-session", false, null));
    }

    @Test
    @DisplayName("还在收集信息（无方案）的会话不能确认")
    void confirmCollectingSessionIsRejected() {
        when(aiChatService.chat(anyList())).thenReturn("""
                {"pet_name":"团团","service_type":"BOARDING","unit":"night"}""");
        when(petService.getPetsByOwner(USER_ID)).thenReturn(List.of(pet()));

        String sessionId = agentService.message(USER_ID, request("帮团团订寄养")).getSession_id_wsh();

        BusinessException e = assertThrows(BusinessException.class,
                () -> agentService.confirm(USER_ID, sessionId, false, null));
        assertTrue(e.getMessage().contains("没有可确认的方案"));
    }

    // ── 工具 ────────────────────────────────────────────────

    /** 跑通一轮对话拿到会话ID（方案已就绪）。 */
    private String planSuccessfully() {
        when(petService.getPetsByOwner(USER_ID)).thenReturn(List.of(pet()));
        when(merchantService.searchNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(merchant()));
        when(keeperService.searchNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(keeper()));
        when(serviceItemService.listEnabledByMerchant(any())).thenReturn(List.of(serviceItem()));
        when(aiChatService.chat(anyList())).thenReturn(LLM_JSON);
        when(orderService.previewOrder(eq(USER_ID), any())).thenReturn(nightQuote());

        String sessionId = agentService.message(USER_ID, request("帮团团寄养 3 天")).getSession_id_wsh();
        assertEquals("AWAIT_CONFIRM", agentService.listSessions(USER_ID).get(0).getStatus_wsh());
        return sessionId;
    }

    private AgentMessageRequestDTO request(String content) {
        AgentMessageRequestDTO dto = new AgentMessageRequestDTO();
        dto.setContent_wsh(content);
        dto.setLatitude_wsh(30.0);
        dto.setLongitude_wsh(120.0);
        return dto;
    }

    private void mockConfirmSuccess() {
        when(userService.getById(USER_ID)).thenReturn(user());
        when(orderService.createOrder(eq(USER_ID), any(OrderCreateRequestDTO.class))).thenReturn(orderDto());
        when(paymentService.createPayment(eq(USER_ID), eq(ORDER_NO), eq("balance"))).thenReturn(payment());
    }

    private OrderQuoteDTO nightQuote() {
        OrderQuoteDTO quote = new OrderQuoteDTO();
        quote.setBilling_unit_wsh("night");
        quote.setQuantity_wsh(3);
        quote.setUnit_price_wsh(new BigDecimal("100"));
        quote.setTotal_amount_wsh(new BigDecimal("300.00"));
        quote.setFinal_amount_wsh(new BigDecimal("300.00"));
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

    private KeeperVO keeper() {
        KeeperVO keeper = new KeeperVO();
        keeper.setId_wsh(KEEPER_ID);
        keeper.setName_wsh("小王");
        keeper.setMerchant_id_wsh(MERCHANT_ID);
        keeper.setMerchant_name_wsh("安心寄养");
        keeper.setRating_wsh(BigDecimal.valueOf(4.9));
        keeper.setDistance_wsh(1.2);
        keeper.setExperience_years_wsh(5);
        keeper.setCompletion_rate_wsh(BigDecimal.valueOf(98));
        keeper.setComplaint_rate_wsh(BigDecimal.ZERO);
        keeper.setCurrent_pets_wsh(1);
        keeper.setMax_pets_wsh(5);
        return keeper;
    }

    private ServiceItem serviceItem() {
        ServiceItem service = new ServiceItem();
        service.setId_wsh(SERVICE_ID);
        service.setMerchant_id_wsh(MERCHANT_ID);
        service.setName_wsh("标准寄养");
        service.setType_wsh("BOARDING_STANDARD");
        service.setUnit_wsh("night");
        service.setPrice_wsh(new BigDecimal("100"));
        service.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        return service;
    }

    private OrderDTO orderDto() {
        OrderDTO order = new OrderDTO();
        order.setOrder_no_wsh(ORDER_NO);
        return order;
    }

    private Payment payment() {
        Payment payment = new Payment();
        payment.setPay_no_wsh(PAY_NO);
        payment.setOrder_no_wsh(ORDER_NO);
        payment.setStatus_wsh("pending");
        return payment;
    }
}
