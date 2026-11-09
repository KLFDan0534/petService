package com.pet.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.ai.dto.AgentExecuteResult;
import com.pet.ai.dto.AgentPlanResult;
import com.pet.ai.service.LlmChatService;
import com.pet.ai.service.PlanTokenStore;
import com.pet.ai.service.impl.AgentServiceImpl;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.mapper.ServiceItemMapper;
import com.pet.boarding.service.KeeperService;
import com.pet.boarding.vo.KeeperVO;
import com.pet.common.BusinessException;
import com.pet.common.StatusCode;
import com.pet.mq.MessageSender;
import com.pet.order.dto.OrderCreateRequestDTO;
import com.pet.order.dto.OrderDTO;
import com.pet.order.dto.OrderPreviewRequestDTO;
import com.pet.order.dto.OrderQuoteDTO;
import com.pet.order.entity.Payment;
import com.pet.order.service.OrderService;
import com.pet.order.service.PaymentService;
import com.pet.pet.entity.Pet;
import com.pet.pet.mapper.PetMapper;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import com.pet.system.service.UserService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 智能下单按夜规划契约（U9）。
 * <p>
 * 覆盖：AI 只选 night 服务、夜数 N 直接作为预订夜数、离店日 = 入住日 + N、
 * 默认 14:00 送达 / 14:30 接走、金额取自后端 previewOrder（不由模型生成）。
 */
@ExtendWith(MockitoExtension.class)
class AgentNightBookingTest {

    private static final Long USER_ID = 7L;
    private static final Long PET_ID = 11L;
    private static final Long MERCHANT_ID = 21L;
    private static final Long KEEPER_ID = 31L;
    private static final Long SERVICE_ID = 41L;

    private static final String LLM_JSON = """
            {"pet_name":"团团","pet_type":"金毛","service_type":"BOARDING","start_date":null,"nights":3,"merchant_keyword":null,"max_price_per_night":null,"unit":"night"}""";

    @Mock private PetMapper petMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private KeeperService keeperService;
    @Mock private ServiceItemMapper serviceItemMapper;
    @Mock private UserMapper userMapper;
    @Mock private UserService userService;
    @Mock private OrderService orderService;
    @Mock private PaymentService paymentService;
    @Mock private MessageSender messageSender;
    @Mock private LlmChatService aiChatService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PlanTokenStore planTokenStore = new PlanTokenStore();

    private AgentServiceImpl agentService;

    @BeforeEach
    void setUp() {
        agentService = new AgentServiceImpl(
                petMapper, merchantMapper, keeperService, serviceItemMapper, userMapper, userService,
                orderService, paymentService, messageSender, aiChatService, objectMapper, planTokenStore);
    }

    private void stubPlanFlow() {
        when(petMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(pet()));
        when(merchantMapper.searchNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(merchant()));
        when(keeperService.searchNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(keeper()));
        when(serviceItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(serviceItem()));
        when(aiChatService.chat(anyList())).thenReturn(LLM_JSON);
        // 后端报价：3 夜 × 150 = 450（不含晚接，晚接实际接走时结算）
        when(orderService.previewOrder(eq(USER_ID), any())).thenReturn(quote(new BigDecimal("450.00")));
    }

    @Test
    void threeNightsCheckOutOnTheFourthDay() {
        stubPlanFlow();

        AgentPlanResult plan = agentService.plan(USER_ID, "帮团团订 3 晚寄养", 30.0, 120.0, null);

        assertEquals("plan_generated", plan.getStatus_wsh());
        assertEquals("night", plan.getUnit_wsh());
        assertEquals(Integer.valueOf(3), plan.getDays_wsh());
        LocalDate checkin = plan.getStart_date_wsh();
        // 3 夜 → 离店日是第 4 天（入住日 + 3），不是「含首尾的第 3 天」
        assertEquals(checkin.plusDays(3), plan.getEnd_date_wsh());
    }

    @Test
    void planUsesBackendQuoteInsteadOfModelArithmetic() {
        stubPlanFlow();

        AgentPlanResult plan = agentService.plan(USER_ID, "帮团团订 3 晚寄养", 30.0, 120.0, null);

        // 模型只给夜数与预算，金额必须等于 previewOrder 的结果
        assertEquals(new BigDecimal("450.00"), plan.getTotal_price_wsh());
        ArgumentCaptor<OrderPreviewRequestDTO> captor = ArgumentCaptor.forClass(OrderPreviewRequestDTO.class);
        verify(orderService).previewOrder(eq(USER_ID), captor.capture());
        OrderPreviewRequestDTO request = captor.getValue();
        assertEquals("night", request.getBilling_unit_wsh());
        assertEquals(Integer.valueOf(3), request.getQuantity_wsh());
        assertEquals(request.getStart_date_wsh().plusDays(3), request.getEnd_date_wsh());
    }

    @Test
    void confirmSendsNightRequestWithDefaultAnchors() {
        stubPlanFlow();
        when(userMapper.selectById(USER_ID)).thenReturn(user());
        when(orderService.createOrder(eq(USER_ID), any(OrderCreateRequestDTO.class))).thenReturn(orderDto());
        when(paymentService.createPayment(eq(USER_ID), eq("ORD-AI-NIGHT"), eq("balance"))).thenReturn(payment());

        AgentPlanResult plan = agentService.plan(USER_ID, "帮团团订 3 晚寄养", 30.0, 120.0, null);
        AgentExecuteResult result = agentService.confirm(USER_ID, plan.getPlan_token_wsh(), false, null);

        assertEquals("pending_payment", result.getStatus());
        ArgumentCaptor<OrderCreateRequestDTO> captor = ArgumentCaptor.forClass(OrderCreateRequestDTO.class);
        verify(orderService).createOrder(eq(USER_ID), captor.capture());
        OrderCreateRequestDTO request = captor.getValue();
        LocalDate checkin = plan.getStart_date_wsh();
        assertEquals("night", request.getBilling_unit_wsh());
        assertEquals(Integer.valueOf(3), request.getQuantity_wsh());
        assertEquals(checkin, request.getStart_date_wsh());
        assertEquals(checkin.plusDays(3), request.getEnd_date_wsh());
        // 平台固定锚点：入住日 14:00 送达，离店日 14:30 接走
        assertEquals(LocalDateTime.of(checkin, java.time.LocalTime.of(14, 0)), request.getDelivery_time_wsh());
        assertEquals(LocalDateTime.of(checkin.plusDays(3), java.time.LocalTime.of(14, 30)), request.getPickup_time_wsh());
        // 下单价格仍按服务单价二次校验
        assertEquals(new BigDecimal("150"), request.getExpected_unit_price_wsh());
    }

    @Test
    void rejectsNonNightUnitRequest() {
        when(aiChatService.chat(anyList())).thenReturn("""
                {"pet_name":"团团","service_type":"BOARDING","nights":3,"unit":"hour"}""");

        // 按小时不属于按夜寄养：plan 阶段直接要求用户改用手动下单，不生成方案
        AgentPlanResult result = agentService.plan(USER_ID, "按小时订", 30.0, 120.0, null);

        assertEquals("needs_user_input", result.getStatus_wsh());
        assertEquals("unsupported_service_type", result.getNext_action_wsh());
        verify(orderService, never()).previewOrder(any(), any());
        verify(orderService, never()).createOrder(any(), any());
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

    private Pet pet() {
        Pet pet = new Pet();
        pet.setId_wsh(PET_ID);
        pet.setOwner_id_wsh(USER_ID);
        pet.setName_wsh("团团");
        pet.setType_wsh("狗");
        pet.setBreed_wsh("金毛");
        return pet;
    }

    private Merchant merchant() {
        Merchant merchant = new Merchant();
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
