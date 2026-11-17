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
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.service.KeeperService;
import com.pet.boarding.service.MerchantService;
import com.pet.boarding.service.ServiceItemService;
import com.pet.boarding.vo.KeeperVO;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Agent 的数据读取一律走各业务域查询服务，不再直接持有 Mapper。
 * <p>
 * 重点锁两条容易被「顺手优化」破坏的契约：
 * 1. 下单要用**未脱敏手机号**做紧急联系电话 —— 所以必须走 UserService.getById（实体），
 * 不能用 getUserInfo（VO 里手机号是 138****8000，写进订单就是脏数据）；
 * 2. 写操作只能经 OrderService.createOrder → PaymentService.createPayment，
 * Agent 自己没有任何数据库写入路径。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Agent · 业务查询服务替换契约")
class AgentQueryServiceTest {

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
    @Mock private UserService userService;
    @Mock private OrderService orderService;
    @Mock private PaymentService paymentService;
    @Mock private MessageSender messageSender;
    @Mock private LlmChatService aiChatService;
    @Mock private RagService ragService;
    @Mock private AiChatHistoryService chatHistoryService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AgentServiceImpl agentService;

    @BeforeEach
    void setUp() {
        agentService = new AgentServiceImpl(
                petService, merchantService, keeperService, serviceItemService, userService,
                orderService, paymentService, messageSender, aiChatService, ragService, objectMapper,
                new InMemoryAgentSessionStore(), chatHistoryService, null);
    }

    @Test
    @DisplayName("宠物档案走 PetService，不再直查 pet 表")
    void petsComeFromPetService() {
        stubPlanFlow();
        when(aiChatService.chat(anyList())).thenReturn(LLM_JSON);

        AgentTurnResult turn = agentService.message(USER_ID, request());

        assertEquals("await_confirm", turn.getStatus_wsh());
        verify(petService).getPetsByOwner(USER_ID);
    }

    @Test
    @DisplayName("附近商家走 MerchantService，复用同一套附近搜索")
    void merchantsComeFromMerchantService() {
        stubPlanFlow();
        when(aiChatService.chat(anyList())).thenReturn(LLM_JSON);

        agentService.message(USER_ID, request());

        verify(merchantService).searchNearby(anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("可售服务走 ServiceItemService.listEnabledByMerchant")
    void servicesComeFromServiceItemService() {
        stubPlanFlow();
        when(aiChatService.chat(anyList())).thenReturn(LLM_JSON);

        agentService.message(USER_ID, request());

        verify(serviceItemService).listEnabledByMerchant(MERCHANT_ID);
    }

    @Test
    @DisplayName("下单用未脱敏手机号与门店坐标，来源是 UserService 实体与 MerchantService")
    void confirmUsesUnmaskedPhoneAndMerchantCoords() {
        stubPlanFlow();
        when(aiChatService.chat(anyList())).thenReturn(LLM_JSON);
        when(userService.getById(USER_ID)).thenReturn(user());
        when(merchantService.listByIds(anyList())).thenReturn(List.of(merchantEntity()));
        when(orderService.createOrder(eq(USER_ID), any(OrderCreateRequestDTO.class))).thenReturn(orderDto());
        when(paymentService.createPayment(eq(USER_ID), eq("ORD-AI-NIGHT"), eq("balance"))).thenReturn(payment());

        AgentTurnResult turn = agentService.message(USER_ID, request());
        AgentExecuteResult result = agentService.confirm(USER_ID, turn.getSession_id_wsh(), false, null);

        assertEquals("pending_payment", result.getStatus());
        ArgumentCaptor<OrderCreateRequestDTO> captor = ArgumentCaptor.forClass(OrderCreateRequestDTO.class);
        verify(orderService).createOrder(eq(USER_ID), captor.capture());
        OrderCreateRequestDTO order = captor.getValue();

        assertEquals("13800000000", order.getEmergency_contact_phone_wsh(),
                "紧急联系电话必须是完整号码，不能用脱敏 VO");
        assertEquals(BigDecimal.valueOf(30.5), order.getDelivery_latitude_wsh());
        assertEquals(BigDecimal.valueOf(120.5), order.getDelivery_longitude_wsh());
        verify(paymentService).createPayment(eq(USER_ID), eq("ORD-AI-NIGHT"), eq("balance"));
    }

    @Test
    @DisplayName("门店查不到时坐标留空，不因查询方式变化而让下单失败")
    void missingMerchantKeepsNullCoords() {
        stubPlanFlow();
        when(aiChatService.chat(anyList())).thenReturn(LLM_JSON);
        when(userService.getById(USER_ID)).thenReturn(user());
        when(merchantService.listByIds(anyList())).thenReturn(List.of());
        when(orderService.createOrder(eq(USER_ID), any(OrderCreateRequestDTO.class))).thenReturn(orderDto());
        when(paymentService.createPayment(eq(USER_ID), eq("ORD-AI-NIGHT"), eq("balance"))).thenReturn(payment());

        AgentTurnResult turn = agentService.message(USER_ID, request());
        agentService.confirm(USER_ID, turn.getSession_id_wsh(), false, null);

        ArgumentCaptor<OrderCreateRequestDTO> captor = ArgumentCaptor.forClass(OrderCreateRequestDTO.class);
        verify(orderService).createOrder(eq(USER_ID), captor.capture());
        assertNull(captor.getValue().getDelivery_latitude_wsh(), "门店不存在时坐标保持 null（原有降级语义）");
    }

    @Test
    @DisplayName("确认下单仍走 OrderService + PaymentService，Agent 不直接写库")
    void writesStillGoThroughBusinessServices() {
        stubPlanFlow();
        when(aiChatService.chat(anyList())).thenReturn(LLM_JSON);
        when(userService.getById(USER_ID)).thenReturn(user());
        when(merchantService.listByIds(anyList())).thenReturn(List.of(merchantEntity()));
        when(orderService.createOrder(eq(USER_ID), any(OrderCreateRequestDTO.class))).thenReturn(orderDto());
        when(paymentService.createPayment(eq(USER_ID), eq("ORD-AI-NIGHT"), eq("balance"))).thenReturn(payment());

        AgentTurnResult turn = agentService.message(USER_ID, request());
        agentService.confirm(USER_ID, turn.getSession_id_wsh(), false, null);

        verify(orderService).createOrder(eq(USER_ID), any(OrderCreateRequestDTO.class));
        verify(paymentService).createPayment(eq(USER_ID), eq("ORD-AI-NIGHT"), eq("balance"));
        long mapperFields = java.util.Arrays.stream(AgentServiceImpl.class.getDeclaredFields())
                // 只统计持久层 Mapper（com.pet.*.mapper 包），ObjectMapper 是 JSON 序列化器，不算
                .filter(f -> f.getType().getName().contains(".mapper."))
                .count();
        assertEquals(0, mapperFields, "AgentServiceImpl 不允许持有任何持久层 Mapper 字段");
    }

    // ── 工具 ────────────────────────────────────────────────

    private AgentMessageRequestDTO request() {
        AgentMessageRequestDTO dto = new AgentMessageRequestDTO();
        dto.setContent_wsh("帮团团订 3 晚寄养");
        dto.setLatitude_wsh(30.0);
        dto.setLongitude_wsh(120.0);
        return dto;
    }

    private void stubPlanFlow() {
        when(petService.getPetsByOwner(USER_ID)).thenReturn(List.of(pet()));
        when(merchantService.searchNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(merchant()));
        when(keeperService.searchNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(keeper()));
        when(serviceItemService.listEnabledByMerchant(any())).thenReturn(List.of(serviceItem()));
        when(orderService.previewOrder(eq(USER_ID), any())).thenReturn(quote());
    }

    private OrderQuoteDTO quote() {
        OrderQuoteDTO quote = new OrderQuoteDTO();
        quote.setBilling_unit_wsh("night");
        quote.setQuantity_wsh(3);
        quote.setUnit_price_wsh(new BigDecimal("150"));
        quote.setTotal_amount_wsh(new BigDecimal("450.00"));
        quote.setFinal_amount_wsh(new BigDecimal("450.00"));
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

    private Merchant merchantEntity() {
        Merchant merchant = new Merchant();
        merchant.setId_wsh(MERCHANT_ID);
        merchant.setName_wsh("安心寄养");
        merchant.setAddress_wsh("测试地址");
        merchant.setLatitude_wsh(BigDecimal.valueOf(30.5));
        merchant.setLongitude_wsh(BigDecimal.valueOf(120.5));
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
