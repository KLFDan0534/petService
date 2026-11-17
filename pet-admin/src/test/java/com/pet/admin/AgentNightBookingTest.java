package com.pet.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.ai.dto.AgentExecuteResult;
import com.pet.ai.dto.AgentMessageRequestDTO;
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
import com.pet.order.dto.OrderCreateRequestDTO;
import com.pet.order.dto.OrderDTO;
import com.pet.order.dto.OrderPreviewRequestDTO;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 智能下单按夜规划契约（会话式）。
 * <p>
 * 覆盖：AI 只选 night 服务、夜数 N 直接作为预订夜数、离店日 = 入住日 + N、
 * 默认 14:00 送达 / 14:30 接走、金额取自后端 previewOrder（不由模型生成）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("智能下单 · 按夜契约")
class AgentNightBookingTest {

    private static final Long USER_ID = 7L;
    private static final Long PET_ID = 11L;
    private static final Long MERCHANT_ID = 21L;
    private static final Long KEEPER_ID = 31L;
    private static final Long SERVICE_ID = 41L;

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
    @DisplayName("3 夜 → 离店日是入住日 + 3，而不是含首尾的第 3 天")
    void threeNightsCheckOutOnTheFourthDay() {
        stubPlanFlow();
        when(aiChatService.chat(anyList())).thenReturn(LLM_JSON);

        AgentTurnResult turn = agentService.message(USER_ID, request("帮团团订 3 晚寄养"));

        assertEquals("await_confirm", turn.getStatus_wsh());
        assertEquals("night", turn.getPlan_card().getUnit_wsh());
        assertEquals(Integer.valueOf(3), turn.getPlan_card().getDays_wsh());
        LocalDate checkin = turn.getPlan_card().getStart_date_wsh();
        assertEquals(checkin.plusDays(3), turn.getPlan_card().getEnd_date_wsh());
    }

    @Test
    @DisplayName("金额等于后端报价，模型只给夜数与预算")
    void planUsesBackendQuoteInsteadOfModelArithmetic() {
        stubPlanFlow();
        when(aiChatService.chat(anyList())).thenReturn(LLM_JSON);

        AgentTurnResult turn = agentService.message(USER_ID, request("帮团团订 3 晚寄养"));

        assertEquals(new BigDecimal("450.00"), turn.getPlan_card().getTotal_price_wsh());
        ArgumentCaptor<OrderPreviewRequestDTO> captor = ArgumentCaptor.forClass(OrderPreviewRequestDTO.class);
        verify(orderService).previewOrder(eq(USER_ID), captor.capture());
        OrderPreviewRequestDTO preview = captor.getValue();
        assertEquals("night", preview.getBilling_unit_wsh());
        assertEquals(Integer.valueOf(3), preview.getQuantity_wsh());
        assertEquals(preview.getStart_date_wsh().plusDays(3), preview.getEnd_date_wsh());
    }

    @Test
    @DisplayName("确认下单沿用 14:00 送达 / 14:30 接走，并二次校验单价")
    void confirmSendsNightRequestWithDefaultAnchors() {
        stubPlanFlow();
        when(aiChatService.chat(anyList())).thenReturn(LLM_JSON);
        when(userService.getById(USER_ID)).thenReturn(user());
        when(orderService.createOrder(eq(USER_ID), any(OrderCreateRequestDTO.class))).thenReturn(orderDto());
        when(paymentService.createPayment(eq(USER_ID), eq("ORD-AI-NIGHT"), eq("balance"))).thenReturn(payment());

        AgentTurnResult turn = agentService.message(USER_ID, request("帮团团订 3 晚寄养"));
        AgentExecuteResult result = agentService.confirm(USER_ID, turn.getSession_id_wsh(), false, null);

        assertEquals("pending_payment", result.getStatus());
        ArgumentCaptor<OrderCreateRequestDTO> captor = ArgumentCaptor.forClass(OrderCreateRequestDTO.class);
        verify(orderService).createOrder(eq(USER_ID), captor.capture());
        OrderCreateRequestDTO order = captor.getValue();
        LocalDate checkin = turn.getPlan_card().getStart_date_wsh();
        assertEquals("night", order.getBilling_unit_wsh());
        assertEquals(Integer.valueOf(3), order.getQuantity_wsh());
        assertEquals(checkin, order.getStart_date_wsh());
        assertEquals(checkin.plusDays(3), order.getEnd_date_wsh());
        assertEquals(LocalDateTime.of(checkin, java.time.LocalTime.of(14, 0)), order.getDelivery_time_wsh());
        assertEquals(LocalDateTime.of(checkin.plusDays(3), java.time.LocalTime.of(14, 30)), order.getPickup_time_wsh());
        assertEquals(new BigDecimal("150"), order.getExpected_unit_price_wsh());
    }

    @Test
    @DisplayName("非 night 单位直接拒绝，不产生报价也不产生订单")
    void rejectsNonNightUnitRequest() {
        when(aiChatService.chat(anyList())).thenReturn("""
                {"pet_name":"团团","service_type":"BOARDING","nights":3,"unit":"hour"}""");

        AgentTurnResult turn = agentService.message(USER_ID, request("按小时订"));

        assertEquals("collecting", turn.getStatus_wsh());
        assertEquals("unsupported_service_type", turn.getNext_action_wsh());
        assertNull(turn.getPlan_card());
        verify(orderService, never()).previewOrder(any(), any());
        verify(orderService, never()).createOrder(any(), any());
    }

    // ── 工具 ────────────────────────────────────────────────

    private AgentMessageRequestDTO request(String content) {
        AgentMessageRequestDTO dto = new AgentMessageRequestDTO();
        dto.setContent_wsh(content);
        dto.setLatitude_wsh(30.0);
        dto.setLongitude_wsh(120.0);
        return dto;
    }

    private void stubPlanFlow() {
        when(petService.getPetsByOwner(USER_ID)).thenReturn(List.of(pet()));
        when(merchantService.searchNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(merchant()));
        when(keeperService.searchNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(keeper()));
        when(serviceItemService.listEnabledByMerchant(any())).thenReturn(List.of(serviceItem()));
        when(orderService.previewOrder(eq(USER_ID), any())).thenReturn(quote(new BigDecimal("450.00")));
    }

    private OrderQuoteDTO quote(BigDecimal finalAmount) {
        OrderQuoteDTO quote = new OrderQuoteDTO();
        quote.setBilling_unit_wsh("night");
        quote.setQuantity_wsh(3);
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
        service.setPrice_wsh(new BigDecimal("150"));
        service.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        return service;
    }

    private OrderDTO orderDto() {
        OrderDTO order = new OrderDTO();
        order.setOrder_no_wsh("ORD-AI-NIGHT");
        return order;
    }

    private Payment payment() {
        Payment payment = new Payment();
        payment.setPay_no_wsh("PAY-AI-NIGHT");
        payment.setOrder_no_wsh("ORD-AI-NIGHT");
        payment.setStatus_wsh("pending");
        return payment;
    }
}
