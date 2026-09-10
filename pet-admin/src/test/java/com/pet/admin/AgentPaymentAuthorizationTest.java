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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 智能下单两阶段（plan → confirm）支付授权测试。
 * <p>plan 阶段由 LLM 提取需求（mock 返回预置 JSON），confirm 阶段校验支付密码。</p>
 */
@ExtendWith(MockitoExtension.class)
class AgentPaymentAuthorizationTest {

    private static final Long USER_ID = 7L;
    private static final Long PET_ID = 11L;
    private static final Long MERCHANT_ID = 21L;
    private static final Long KEEPER_ID = 31L;
    private static final Long SERVICE_ID = 41L;
    private static final String ORDER_NO = "ORD-AI-001";
    private static final String PAY_NO = "PAY-AI-001";

    /** LLM 结构化提取返回的预置 JSON（宠物团团 / 金毛 / 寄养 3 天） */
    private static final String LLM_JSON = """
            {"pet_name":"团团","pet_type":"金毛","service_type":"BOARDING","start_date":null,"days":3,"merchant_keyword":null,"max_price_per_day":null,"unit":"day"}""";

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
                petMapper,
                merchantMapper,
                keeperService,
                serviceItemMapper,
                userMapper,
                userService,
                orderService,
                paymentService,
                messageSender,
                aiChatService,
                objectMapper,
                planTokenStore);
    }

    @Test
    void planThenConfirmWithoutAutoPayCreatesPendingOrderAndPaymentOnly() {
        mockPlanFlow();
        mockConfirmSuccess();

        AgentPlanResult plan = planSuccessfully();

        AgentExecuteResult result = agentService.confirm(USER_ID, plan.getPlan_token_wsh(), false, null);

        assertEquals("pending_payment", result.getStatus());
        assertEquals("pending", result.getPayment_status_wsh());
        assertEquals(ORDER_NO, result.getOrderNo());
        assertEquals(PAY_NO, result.getPayNo());
        assertFalse(result.getRequires_user_input_wsh());
        assertTrue(result.getMessage_wsh().contains("待支付"));

        ArgumentCaptor<OrderCreateRequestDTO> orderCaptor = ArgumentCaptor.forClass(OrderCreateRequestDTO.class);
        verify(orderService).createOrder(eq(USER_ID), orderCaptor.capture());
        OrderCreateRequestDTO req = orderCaptor.getValue();
        assertEquals(PET_ID, req.getPet_id_wsh());
        assertEquals(KEEPER_ID, req.getKeeper_id_wsh());
        assertEquals(MERCHANT_ID, req.getMerchant_id_wsh());
        assertEquals(SERVICE_ID, req.getService_id_wsh());
        assertEquals("day", req.getBilling_unit_wsh());
        assertEquals(new BigDecimal("100"), req.getExpected_unit_price_wsh());
        verify(paymentService).createPayment(USER_ID, ORDER_NO, "balance");
        verify(paymentService, never()).pay(any(), any());
        verify(userService, never()).verifyPaymentPassword(any(), any());
        verify(aiChatService).chat(anyList());
    }

    @Test
    void planThenConfirmWithAutoPayAndPasswordPaysAfterAuthorization() {
        mockPlanFlow();
        mockConfirmSuccess();

        AgentPlanResult plan = planSuccessfully();

        AgentExecuteResult result = agentService.confirm(USER_ID, plan.getPlan_token_wsh(), true, "123456");

        assertEquals("success", result.getStatus());
        assertEquals("paid", result.getPayment_status_wsh());
        assertEquals("paid", result.getNext_action_wsh());
        assertEquals(ORDER_NO, result.getOrderNo());
        assertEquals(PAY_NO, result.getPayNo());
        assertFalse(result.getRequires_user_input_wsh());

        verify(userService).verifyPaymentPassword(USER_ID, "123456");
        verify(orderService).createOrder(eq(USER_ID), any(OrderCreateRequestDTO.class));
        verify(paymentService).createPayment(USER_ID, ORDER_NO, "balance");
        verify(paymentService).pay(USER_ID, PAY_NO);
    }

    @Test
    void confirmWithAutoPayButNoPasswordAsksUserBeforeCreatingOrderAndAllowsRetryWithSameToken() {
        mockPlanFlow();
        mockConfirmSuccess();

        AgentPlanResult plan = planSuccessfully();
        String token = plan.getPlan_token_wsh();

        AgentExecuteResult first = agentService.confirm(USER_ID, token, true, null);

        assertEquals("needs_user_input", first.getStatus());
        assertEquals("ask_payment_password", first.getNext_action_wsh());
        assertTrue(first.getRequires_user_input_wsh());
        assertTrue(first.getMessage_wsh().contains("支付密码"));
        assertNull(first.getOrderNo());
        assertNull(first.getPayNo());
        verify(orderService, never()).createOrder(any(), any());
        verify(paymentService, never()).createPayment(any(), any(), any());
        verify(userService, never()).verifyPaymentPassword(any(), any());

        // token 已释放（release），补上支付密码可用同一方案重试成功
        AgentExecuteResult second = agentService.confirm(USER_ID, token, true, "123456");
        assertEquals("success", second.getStatus());
        assertEquals("paid", second.getPayment_status_wsh());
        verify(userService).verifyPaymentPassword(USER_ID, "123456");
        verify(orderService).createOrder(eq(USER_ID), any(OrderCreateRequestDTO.class));
        verify(paymentService).pay(USER_ID, PAY_NO);
    }

    @Test
    void confirmWithWrongPaymentPasswordAsksUserWithoutCreatingOrder() {
        mockPlanFlow();
        doThrow(new BusinessException(403, "支付密码错误"))
                .when(userService).verifyPaymentPassword(USER_ID, "bad");

        AgentPlanResult plan = planSuccessfully();

        AgentExecuteResult result = agentService.confirm(USER_ID, plan.getPlan_token_wsh(), true, "bad");

        assertEquals("needs_user_input", result.getStatus());
        assertEquals("ask_payment_password", result.getNext_action_wsh());
        assertTrue(result.getRequires_user_input_wsh());
        assertEquals("支付密码错误", result.getMessage_wsh());
        assertNull(result.getOrderNo());

        verify(orderService, never()).createOrder(any(), any());
        verify(paymentService, never()).createPayment(any(), any(), any());
        verify(paymentService, never()).pay(any(), any());
    }

    @Test
    void confirmWithInvalidTokenIsRejected() {
        assertThrows(BusinessException.class,
                () -> agentService.confirm(USER_ID, "not-exist-token", false, null));
    }

    /** plan 成功并返回方案（供各 confirm 用例复用）。 */
    private AgentPlanResult planSuccessfully() {
        AgentPlanResult plan = agentService.plan(USER_ID, "帮团团寄养 3 天", 30.0, 120.0, null);
        assertEquals("plan_generated", plan.getStatus_wsh());
        assertNotNull(plan.getPlan_token_wsh());
        assertEquals(PET_ID, plan.getPet_id_wsh());
        assertEquals("团团", plan.getPet_name_wsh());
        assertEquals(MERCHANT_ID, plan.getMerchant_id_wsh());
        assertEquals(KEEPER_ID, plan.getKeeper_id_wsh());
        assertEquals(SERVICE_ID, plan.getService_id_wsh());
        assertEquals("day", plan.getUnit_wsh());
        assertEquals(Integer.valueOf(3), plan.getDays_wsh());
        assertEquals(new BigDecimal("100"), plan.getUnit_price_wsh());
        assertEquals(new BigDecimal("300"), plan.getTotal_price_wsh());
        return plan;
    }

    /** plan 阶段的依赖：宠物档案 / 附近商家 / 看护人 / 商家服务 / LLM 提取。 */
    private void mockPlanFlow() {
        when(petMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(pet()));
        when(merchantMapper.searchNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(merchant()));
        when(keeperService.searchNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(keeper()));
        when(serviceItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(serviceItem()));
        when(aiChatService.chat(anyList())).thenReturn(LLM_JSON);
    }

    /** confirm 成功路径的依赖：用户资料 / 创建订单 / 创建支付。 */
    private void mockConfirmSuccess() {
        when(userMapper.selectById(USER_ID)).thenReturn(user());
        when(orderService.createOrder(eq(USER_ID), any(OrderCreateRequestDTO.class))).thenReturn(orderDto());
        when(paymentService.createPayment(USER_ID, ORDER_NO, "balance")).thenReturn(payment());
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
        keeper.setPrice_per_day_wsh(BigDecimal.valueOf(80));
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
        service.setUnit_wsh("day");
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