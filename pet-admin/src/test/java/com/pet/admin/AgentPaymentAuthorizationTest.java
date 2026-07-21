package com.pet.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.ai.dto.AgentExecuteResult;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentPaymentAuthorizationTest {

    private static final Long USER_ID = 7L;
    private static final Long PET_ID = 11L;
    private static final Long MERCHANT_ID = 21L;
    private static final Long KEEPER_ID = 31L;
    private static final Long SERVICE_ID = 41L;
    private static final String ORDER_NO = "ORD-AI-001";
    private static final String PAY_NO = "PAY-AI-001";

    @Mock private PetMapper petMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private KeeperService keeperService;
    @Mock private ServiceItemMapper serviceItemMapper;
    @Mock private UserMapper userMapper;
    @Mock private UserService userService;
    @Mock private OrderService orderService;
    @Mock private PaymentService paymentService;
    @Mock private MessageSender messageSender;

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
                messageSender);
    }

    @Test
    void executeWithoutAutoPayCreatesPendingOrderAndPaymentOnly() {
        mockSuccessfulRecommendationFlow();

        AgentExecuteResult result = agentService.execute(USER_ID, "金毛寄养 3 天", 30.0, 120.0, false, null);

        assertEquals("pending_payment", result.getStatus());
        assertEquals("pending", result.getPayment_status_wsh());
        assertEquals(ORDER_NO, result.getOrderNo());
        assertEquals(PAY_NO, result.getPayNo());
        assertFalse(result.getRequires_user_input_wsh());
        assertTrue(result.getMessage_wsh().contains("待支付"));

        ArgumentCaptor<OrderCreateRequestDTO> orderCaptor = ArgumentCaptor.forClass(OrderCreateRequestDTO.class);
        verify(orderService).createOrder(eq(USER_ID), orderCaptor.capture());
        assertEquals(PET_ID, orderCaptor.getValue().getPet_id_wsh());
        assertEquals(KEEPER_ID, orderCaptor.getValue().getKeeper_id_wsh());
        assertEquals(MERCHANT_ID, orderCaptor.getValue().getMerchant_id_wsh());
        assertEquals(SERVICE_ID, orderCaptor.getValue().getService_id_wsh());
        verify(paymentService).createPayment(USER_ID, ORDER_NO, "balance");
        verify(paymentService, never()).pay(any(), any());
        verify(userService, never()).verifyPaymentPassword(any(), any());
    }

    @Test
    void executeWithAutoPayButNoPasswordAsksUserBeforeCreatingOrder() {
        mockRecommendationFlowBeforeOrder();

        AgentExecuteResult result = agentService.execute(USER_ID, "金毛寄养 3 天", 30.0, 120.0, true, null);

        assertEquals("needs_user_input", result.getStatus());
        assertEquals("ask_payment_password", result.getNext_action_wsh());
        assertTrue(result.getRequires_user_input_wsh());
        assertTrue(result.getMessage_wsh().contains("支付密码"));
        assertNull(result.getOrderNo());
        assertNull(result.getPayNo());

        verify(orderService, never()).createOrder(any(), any());
        verify(paymentService, never()).createPayment(any(), any(), any());
        verify(paymentService, never()).pay(any(), any());
        verify(userService, never()).verifyPaymentPassword(any(), any());
    }

    @Test
    void executeWithAutoPayAndPasswordPaysAfterAuthorization() {
        mockSuccessfulRecommendationFlow();

        AgentExecuteResult result = agentService.execute(USER_ID, "金毛寄养 3 天", 30.0, 120.0, true, "123456");

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
    void executeWithWrongPaymentPasswordAsksUserBeforeCreatingOrder() {
        mockRecommendationFlowBeforeOrder();
        doThrow(new BusinessException(403, "支付密码错误"))
                .when(userService).verifyPaymentPassword(USER_ID, "bad");

        AgentExecuteResult result = agentService.execute(USER_ID, "金毛寄养 3 天", 30.0, 120.0, true, "bad");

        assertEquals("needs_user_input", result.getStatus());
        assertEquals("ask_payment_password", result.getNext_action_wsh());
        assertTrue(result.getRequires_user_input_wsh());
        assertEquals("支付密码错误", result.getMessage_wsh());

        verify(orderService, never()).createOrder(any(), any());
        verify(paymentService, never()).createPayment(any(), any(), any());
        verify(paymentService, never()).pay(any(), any());
    }

    private void mockSuccessfulRecommendationFlow() {
        mockRecommendationFlowBeforeOrder();
        when(userMapper.selectById(USER_ID)).thenReturn(user());
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(merchant());
        when(serviceItemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(serviceItem());
        when(orderService.createOrder(eq(USER_ID), any(OrderCreateRequestDTO.class))).thenReturn(orderDto());
        when(paymentService.createPayment(USER_ID, ORDER_NO, "balance")).thenReturn(payment());
    }

    private void mockRecommendationFlowBeforeOrder() {
        when(petMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(pet()));
        when(merchantMapper.searchNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(merchant()));
        when(keeperService.searchNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(keeper()));
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
