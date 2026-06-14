package com.pet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.module.agent.AgentService;
import com.pet.module.ai.service.AiReportService;
import com.pet.module.chat.service.ChatService;
import com.pet.module.favorite.service.FavoriteService;
import com.pet.module.keeper.entity.Keeper;
import com.pet.module.keeper.mapper.KeeperMapper;
import com.pet.module.merchant.entity.Merchant;
import com.pet.module.merchant.mapper.MerchantMapper;
import com.pet.module.order.dto.CreateOrderRequest;
import com.pet.module.order.entity.PetOrder;
import com.pet.module.order.service.OrderService;
import com.pet.module.payment.entity.Payment;
import com.pet.module.payment.service.PaymentService;
import com.pet.module.pet.entity.Pet;
import com.pet.module.pet.mapper.PetMapper;
import com.pet.module.rag.service.RagService;
import com.pet.module.complaint.entity.Complaint;
import com.pet.module.complaint.service.ComplaintService;
import com.pet.module.rating.entity.Rating;
import com.pet.module.rating.service.RatingService;
import com.pet.module.refund.entity.Refund;
import com.pet.module.refund.service.RefundService;
import com.pet.module.user.dto.LoginRequest;
import com.pet.module.user.dto.LoginResponse;
import com.pet.module.user.dto.RegisterRequest;
import com.pet.module.user.entity.User;
import com.pet.module.user.mapper.UserMapper;
import com.pet.module.user.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FullLinkTest {

    @Autowired private UserService userService;
    @Autowired private UserMapper userMapper;
    @Autowired private PetMapper petMapper;
    @Autowired private MerchantMapper merchantMapper;
    @Autowired private KeeperMapper keeperMapper;
    @Autowired private OrderService orderService;
    @Autowired private PaymentService paymentService;
    @Autowired private RefundService refundService;
    @Autowired private RatingService ratingService;
    @Autowired private RagService ragService;
    @Autowired private AgentService agentService;
    @Autowired private AiReportService aiReportService;
    @Autowired private ChatService chatService;
    @Autowired private FavoriteService favoriteService;
    @Autowired private ComplaintService complaintService;
    @Autowired private ObjectMapper objectMapper;

    private static Long ownerId;
    private static Long petId;
    private static Long merchantId;
    private static Long keeperId;
    private static String orderNo;
    private static String payNo;

    @Test
    @Order(1)
    void test01UserRegistrationAndLogin() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("testowner");
        registerRequest.setPassword("password123");
        registerRequest.setNickname("Test Owner");
        LoginResponse response = userService.register(registerRequest);
        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        ownerId = response.getUserId();
        System.out.println("PASS: User registered, ID=" + ownerId);
    }

    @Test
    @Order(2)
    void test02MerchantCreation() {
        Merchant merchant = new Merchant();
        merchant.setUserId(ownerId);
        merchant.setName("Test Pet Boarding");
        merchant.setPhone("13800138000");
        merchant.setAddress("Beijing Chaoyang");
        merchant.setLatitude(new BigDecimal("39.9042"));
        merchant.setLongitude(new BigDecimal("116.4074"));
        merchant.setDescription("Professional pet boarding service");
        merchant.setStatus(1);
        merchantMapper.insert(merchant);
        merchantId = merchant.getId();
        assertNotNull(merchantId);
        System.out.println("PASS: Merchant created, ID=" + merchantId);
    }

    @Test
    @Order(3)
    void test03KeeperCreation() {
        Keeper keeper = new Keeper();
        keeper.setMerchantId(merchantId);
        keeper.setUserId(ownerId);
        keeper.setName("John");
        keeper.setPhone("13900139000");
        keeper.setExperienceYears(5);
        keeper.setRating(new BigDecimal("4.9"));
        keeper.setCompletionRate(new BigDecimal("98.5"));
        keeper.setComplaintRate(new BigDecimal("0.5"));
        keeper.setPricePerDay(new BigDecimal("50"));
        keeper.setMaxPets(5);
        keeper.setCurrentPets(0);
        keeper.setStatus(1);
        keeperMapper.insert(keeper);
        keeperId = keeper.getId();
        assertNotNull(keeperId);
        System.out.println("PASS: Keeper created, ID=" + keeperId);
    }

    @Test
    @Order(4)
    void test04PetCreation() {
        Pet pet = new Pet();
        pet.setOwnerId(ownerId);
        pet.setName("Buddy");
        pet.setType("泰迪");
        pet.setBreed("Toy Poodle");
        pet.setAge(3);
        pet.setWeight(new BigDecimal("5.5"));
        pet.setGender(1);
        pet.setVaccinated(1);
        petMapper.insert(pet);
        petId = pet.getId();
        assertNotNull(petId);
        System.out.println("PASS: Pet created, ID=" + petId);
    }

    @Test
    @Order(5)
    void test05OrderCreation() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setPetId(petId);
        request.setKeeperId(keeperId);
        request.setMerchantId(merchantId);
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(7));
        PetOrder order = orderService.createOrder(ownerId, request);
        assertNotNull(order);
        assertNotNull(order.getOrderNo());
        assertEquals("pending", order.getStatus());
        orderNo = order.getOrderNo();
        System.out.println("PASS: Order created, NO=" + orderNo + ", Amount=" + order.getFinalAmount());
    }

    @Test
    @Order(6)
    void test06Payment() {
        Payment payment = paymentService.createPayment(ownerId, orderNo, "wechat");
        assertNotNull(payment);
        assertNotNull(payment.getPayNo());
        payNo = payment.getPayNo();
        paymentService.pay(payNo);
        Payment paid = paymentService.getByOrderNo(orderNo);
        assertEquals("success", paid.getStatus());
        System.out.println("PASS: Payment completed, PayNO=" + payNo);
    }

    @Test
    @Order(7)
    void test07OrderCompletion() {
        orderService.completeOrder(orderNo);
        PetOrder order = orderService.getByOrderNo(orderNo);
        assertEquals("completed", order.getStatus());
        System.out.println("PASS: Order completed");
    }

    @Test
    @Order(8)
    void test08Rating() {
        Rating rating = new Rating();
        rating.setOrderId(orderService.getByOrderNo(orderNo).getId());
        rating.setTargetId(keeperId);
        rating.setTargetType("keeper");
        rating.setScore(5);
        rating.setContent("Excellent service!");
        Rating created = ratingService.createRating(ownerId, rating);
        assertNotNull(created);
        assertNotNull(created.getId());
        System.out.println("PASS: Rating created");
    }

    @Test
    @Order(9)
    void test09RefundFlow() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setPetId(petId);
        request.setKeeperId(keeperId);
        request.setMerchantId(merchantId);
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(3));
        PetOrder order = orderService.createOrder(ownerId, request);
        String refundOrderNo = order.getOrderNo();

        Payment payment = paymentService.createPayment(ownerId, refundOrderNo, "balance");
        paymentService.pay(payment.getPayNo());

        Refund refund = refundService.createRefund(ownerId, refundOrderNo, "Changed plans");
        assertNotNull(refund);
        assertEquals("pending", refund.getStatus());

        refundService.approveRefund(refund.getId());
        refundService.completeRefund(refund.getId());
        Refund completed = refundService.listByOwner(ownerId).stream()
                .filter(r -> r.getOrderNo().equals(refundOrderNo))
                .findFirst().orElse(null);
        assertNotNull(completed);
        assertEquals("completed", completed.getStatus());
        System.out.println("PASS: Refund flow completed");
    }

    @Test
    @Order(10)
    void test10RagKnowledgeBase() {
        String answer = ragService.answer("寄养需要什么疫苗");
        assertNotNull(answer);
        assertTrue(answer.contains("疫苗") || answer.contains("接种"));
        System.out.println("PASS: RAG knowledge base query: " + answer.substring(0, Math.min(50, answer.length())));
    }

    @Test
    @Order(11)
    void test11AgentExecute() {
        Map<String, Object> result = agentService.execute(ownerId,
                "给我的泰迪寄养7天", 39.9042, 116.4074);
        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertNotNull(result.get("orderNo"));
        System.out.println("PASS: Agent executed, Order=" + result.get("orderNo"));
    }

    @Test
    @Order(12)
    void test12UserLoginAgain() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testowner");
        request.setPassword("password123");
        LoginResponse response = userService.login(request);
        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        System.out.println("PASS: User login verified");
    }

    @Test
    @Order(13)
    void test13MerchantSearch() {
        java.util.List<Merchant> merchants = merchantMapper.searchNearby(39.9042, 116.4074, 10);
        assertFalse(merchants.isEmpty());
        System.out.println("PASS: Merchant nearby search, found " + merchants.size());
    }

    @Test
    @Order(14)
    void test14KeeperSearch() {
        java.util.List<com.pet.module.keeper.dto.KeeperVO> keepers =
                new com.pet.module.keeper.service.KeeperService(
                        keeperMapper, merchantMapper)
                        .searchNearby(39.9042, 116.4074, 10);
        assertFalse(keepers.isEmpty());
        System.out.println("PASS: Keeper nearby search, found " + keepers.size());
    }

    @Test
    @Order(15)
    void test15OrderForAgent() {
        Map<String, Object> result = agentService.execute(ownerId,
                "我要寄养宠物7天，预算300以内", 39.9042, 116.4074);
        assertNotNull(result);
        assertEquals("success", result.get("status"));
        System.out.println("PASS: Agent order with budget constraint");
    }

    @Test
    @Order(16)
    void test16AiReport() {
        com.pet.module.ai.entity.AiReport care = aiReportService.generateCareSuggestion(petId, keeperId,
                orderService.getByOrderNo(orderNo).getId());
        assertNotNull(care);
        assertNotNull(care.getId());
        assertEquals("care", care.getType());

        com.pet.module.ai.entity.AiReport boarding = aiReportService.generateBoardingReport(petId, keeperId,
                orderService.getByOrderNo(orderNo).getId());
        assertNotNull(boarding);
        assertNotNull(boarding.getId());
        assertEquals("final", boarding.getType());

        System.out.println("PASS: AI reports generated");
    }

    @Test
    @Order(17)
    void test17FavoriteToggle() {
        favoriteService.toggle(ownerId, merchantId, "merchant");
        assertTrue(favoriteService.isFavorited(ownerId, merchantId, "merchant"));
        favoriteService.toggle(ownerId, merchantId, "merchant");
        assertFalse(favoriteService.isFavorited(ownerId, merchantId, "merchant"));
        System.out.println("PASS: Favorite toggle works");
    }

    @Test
    @Order(18)
    void test18ChatMessage() {
        com.pet.module.chat.entity.ChatMessage msg = new com.pet.module.chat.entity.ChatMessage();
        msg.setFromUserId(ownerId);
        msg.setToUserId(keeperId);
        msg.setContent("Hello, how is my pet?");
        chatService.sendMessage(msg);
        var unread = chatService.getUnreadMessages(keeperId);
        assertFalse(unread.isEmpty());
        var messages = chatService.getConversation(ownerId, keeperId, null);
        assertFalse(messages.isEmpty());
        chatService.markConversationAsRead(ownerId, keeperId, null);
        assertEquals(0, chatService.getUnreadMessages(keeperId).size());
        System.out.println("PASS: Chat message flow works");
    }

    @Test
    @Order(19)
    void test19Complaint() {
        Complaint complaint = new Complaint();
        complaint.setOwnerId(ownerId);
        complaint.setOrderId(orderService.getByOrderNo(orderNo).getId());
        complaint.setTitle("Test complaint");
        complaint.setContent("This is a test complaint for verification");
        Complaint created = complaintService.create(complaint);
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("pending", created.getStatus());
        System.out.println("PASS: Complaint created");
    }
}
