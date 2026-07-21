package com.pet.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.ai.dto.AgentContext;
import com.pet.ai.dto.AgentExecuteResult;
import com.pet.ai.service.AgentService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AgentServiceImpl implements AgentService {

    private static final Map<String, List<String>> BREED_CN_MAP = new HashMap<>();
    private static final double SEARCH_RADIUS_KM = 5.0;

    static {
        BREED_CN_MAP.put("poodle", Arrays.asList("Poodle", "Toy Poodle", "Miniature Poodle"));
        BREED_CN_MAP.put("贵宾", Arrays.asList("Poodle", "Toy Poodle", "Miniature Poodle", "贵宾", "泰迪"));
        BREED_CN_MAP.put("泰迪", Arrays.asList("Poodle", "Toy Poodle", "Miniature Poodle", "贵宾", "泰迪"));
        BREED_CN_MAP.put("golden", Arrays.asList("Golden Retriever", "Golden"));
        BREED_CN_MAP.put("金毛", Arrays.asList("Golden Retriever", "Golden", "金毛"));
        BREED_CN_MAP.put("husky", Arrays.asList("Husky", "Siberian Husky"));
        BREED_CN_MAP.put("哈士奇", Arrays.asList("Husky", "Siberian Husky", "哈士奇"));
        BREED_CN_MAP.put("corgi", Arrays.asList("Corgi", "Pembroke", "Cardigan"));
        BREED_CN_MAP.put("柯基", Arrays.asList("Corgi", "Pembroke", "Cardigan", "柯基"));
        BREED_CN_MAP.put("labrador", Arrays.asList("Labrador", "Labrador Retriever"));
        BREED_CN_MAP.put("拉布拉多", Arrays.asList("Labrador", "Labrador Retriever", "拉布拉多"));
        BREED_CN_MAP.put("samoyed", Collections.singletonList("Samoyed"));
        BREED_CN_MAP.put("萨摩耶", Arrays.asList("Samoyed", "萨摩耶"));
        BREED_CN_MAP.put("shiba", Arrays.asList("Shiba Inu", "Shiba"));
        BREED_CN_MAP.put("柴犬", Arrays.asList("Shiba Inu", "Shiba", "柴犬"));
        BREED_CN_MAP.put("pomeranian", Collections.singletonList("Pomeranian"));
        BREED_CN_MAP.put("博美", Arrays.asList("Pomeranian", "博美"));
        BREED_CN_MAP.put("ragdoll", Collections.singletonList("Ragdoll"));
        BREED_CN_MAP.put("布偶", Arrays.asList("Ragdoll", "布偶"));
        BREED_CN_MAP.put("british", Arrays.asList("British Shorthair", "British"));
        BREED_CN_MAP.put("英短", Arrays.asList("British Shorthair", "British", "英短"));
        BREED_CN_MAP.put("american", Arrays.asList("American Shorthair", "American"));
        BREED_CN_MAP.put("美短", Arrays.asList("American Shorthair", "American", "美短"));
        BREED_CN_MAP.put("exotic", Arrays.asList("Exotic Shorthair", "Persian"));
        BREED_CN_MAP.put("persian", Collections.singletonList("Persian"));
        BREED_CN_MAP.put("波斯", Arrays.asList("Persian", "波斯"));
        BREED_CN_MAP.put("dog", Arrays.asList("Dog", "dog"));
        BREED_CN_MAP.put("狗", Arrays.asList("Dog", "dog", "狗", "犬"));
        BREED_CN_MAP.put("犬", Arrays.asList("Dog", "dog", "狗", "犬"));
        BREED_CN_MAP.put("cat", Arrays.asList("Cat", "cat"));
        BREED_CN_MAP.put("猫", Arrays.asList("Cat", "cat", "猫"));
    }

    private final PetMapper petMapper;
    private final MerchantMapper merchantMapper;
    private final KeeperService keeperService;
    private final ServiceItemMapper serviceItemMapper;
    private final UserMapper userMapper;
    private final UserService userService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final MessageSender messageSender;

    public AgentServiceImpl(PetMapper petMapper,
                            MerchantMapper merchantMapper,
                            KeeperService keeperService,
                            ServiceItemMapper serviceItemMapper,
                            UserMapper userMapper,
                            UserService userService,
                            OrderService orderService,
                            PaymentService paymentService,
                            MessageSender messageSender) {
        this.petMapper = petMapper;
        this.merchantMapper = merchantMapper;
        this.keeperService = keeperService;
        this.serviceItemMapper = serviceItemMapper;
        this.userMapper = userMapper;
        this.userService = userService;
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.messageSender = messageSender;
    }

    @Transactional
    @Override
    public AgentExecuteResult execute(Long userId, String userInput, Double latitude, Double longitude,
                                      Boolean autoPay, String paymentPassword) {
        AgentContext ctx = new AgentContext();
        ctx.setUserId(userId);
        ctx.setUserInput(userInput);
        fillLocation(ctx, latitude, longitude);
        ctx.setCurrentStep(0);
        ctx.setLogs(new ArrayList<>());
        ctx.setStatus("running");

        try {
            step1IdentifyRequirement(ctx);
            step2QueryPetProfile(ctx);
            step3SearchMerchants(ctx);
            step4SearchKeepers(ctx);
            step5RankKeepers(ctx);
            step6GenerateRecommendation(ctx);
            if (Boolean.TRUE.equals(autoPay)) {
                verifyAutoPayAuthorization(ctx, paymentPassword);
            }
            step7CreatePendingOrder(ctx);
            Payment payment = step8CreatePendingPayment(ctx);

            if (Boolean.TRUE.equals(autoPay)) {
                payWithVerifiedAuthorization(ctx, payment);
                step9SendNotification(ctx);
                return buildResult(ctx, "success", "支付已完成，订单已进入待商家处理状态。", "paid", false);
            }

            step9SendNotification(ctx);
            return buildResult(ctx, "pending_payment", "订单已创建，当前为待支付状态。请手动支付，或在授权自动支付时输入支付密码。", "manual_payment", false);
        } catch (NeedUserInputException e) {
            ctx.setStatus("needs_user_input");
            ctx.setError(e.getMessage());
            ctx.getLogs().add("Need user input: " + e.getMessage());
            return buildResult(ctx, "needs_user_input", e.getMessage(), e.nextAction, true);
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("Agent execution failed at step {}: {}", ctx.getCurrentStep(), e.getMessage(), e);
            String safeMsg = e instanceof org.springframework.dao.DataAccessException
                    ? "系统内部错误，请稍后重试。"
                    : e.getMessage();
            ctx.setStatus("failed");
            ctx.setError(safeMsg);
            ctx.getLogs().add("Step " + ctx.getCurrentStep() + " FAILED: " + safeMsg);
            return buildResult(ctx, "failed", safeMsg, "retry", false);
        }
    }

    private void step1IdentifyRequirement(AgentContext ctx) {
        ctx.setCurrentStep(1);
        String input = ctx.getUserInput();
        if (!StringUtils.hasText(input)) {
            throw new NeedUserInputException("请描述要下单的宠物类型和寄养天数，例如：金毛寄养 3 天。", "ask_requirement");
        }
        ctx.getLogs().add("Step 1: Identify requirement");

        List<String> breedKeys = new ArrayList<>(BREED_CN_MAP.keySet());
        breedKeys.sort((a, b) -> Integer.compare(b.length(), a.length()));
        String breedAlternation = breedKeys.stream().map(Pattern::quote).collect(Collectors.joining("|"));
        Pattern petPattern = Pattern.compile("(" + breedAlternation + ")", Pattern.CASE_INSENSITIVE);
        Matcher petMatcher = petPattern.matcher(input);
        if (petMatcher.find()) {
            ctx.setPetType(petMatcher.group(1).toLowerCase());
        } else {
            throw new NeedUserInputException("我还不能确定宠物类型，请补充品种或类型，例如 dog、cat、golden。", "ask_pet_type");
        }

        Pattern dayPattern = Pattern.compile("(\\d+)\\s*(天|日|day|days)?", Pattern.CASE_INSENSITIVE);
        Matcher dayMatcher = dayPattern.matcher(input);
        if (!dayMatcher.find()) {
            throw new NeedUserInputException("请补充需要寄养的天数，例如 3 天。", "ask_days");
        }
        int days = Integer.parseInt(dayMatcher.group(1));
        if (days <= 0) {
            throw new NeedUserInputException("寄养天数必须大于 0，请重新输入。", "ask_days");
        }
        if (days > 365) {
            throw new NeedUserInputException("寄养天数不能超过 365 天，请调整天数。", "ask_days");
        }
        ctx.setDays(days);
        ctx.getLogs().add("  Pet type: " + ctx.getPetType() + ", days: " + ctx.getDays());
    }

    private void fillLocation(AgentContext ctx, Double latitude, Double longitude) {
        if (latitude != null && longitude != null) {
            ctx.setUserLatitude(latitude);
            ctx.setUserLongitude(longitude);
            return;
        }
        User user = userMapper.selectById(ctx.getUserId());
        if (user != null && user.getLatitude_wsh() != null && user.getLongitude_wsh() != null) {
            ctx.setUserLatitude(user.getLatitude_wsh().doubleValue());
            ctx.setUserLongitude(user.getLongitude_wsh().doubleValue());
        }
    }

    private void step2QueryPetProfile(AgentContext ctx) {
        ctx.setCurrentStep(2);
        ctx.getLogs().add("Step 2: Query pet profile");

        List<Pet> pets = petMapper.selectList(new LambdaQueryWrapper<Pet>().eq(Pet::getOwner_id_wsh, ctx.getUserId()));
        if (pets.isEmpty()) {
            throw new NeedUserInputException("未找到你的宠物档案，请先添加宠物信息后再让 Agent 下单。", "create_pet_profile");
        }

        Pet matchedPet = matchPet(pets, ctx.getPetType());
        if (matchedPet == null) {
            matchedPet = pets.get(0);
        }
        ctx.setPetId(matchedPet.getId_wsh());
        ctx.setPetName(matchedPet.getName_wsh());
        ctx.getLogs().add("  Pet: " + matchedPet.getName_wsh());
    }

    private Pet matchPet(List<Pet> pets, String petType) {
        List<String> keywords = BREED_CN_MAP.getOrDefault(petType, Collections.emptyList());
        for (Pet pet : pets) {
            String breed = lower(pet.getBreed_wsh());
            String type = lower(pet.getType_wsh());
            for (String keyword : keywords) {
                String normalized = keyword.toLowerCase();
                if (breed.contains(normalized) || type.contains(normalized)) {
                    return pet;
                }
            }
            if (breed.contains(petType) || type.contains(petType) || lower(pet.getName_wsh()).contains(petType)) {
                return pet;
            }
        }
        return null;
    }

    private void step3SearchMerchants(AgentContext ctx) {
        ctx.setCurrentStep(3);
        ctx.getLogs().add("Step 3: Search nearby merchants");
        if (ctx.getUserLatitude() == null || ctx.getUserLongitude() == null) {
            throw new NeedUserInputException("需要你的位置信息才能推荐附近商家，请授权定位或提供经纬度。", "ask_location");
        }

        List<Merchant> merchants = merchantMapper.searchNearby(ctx.getUserLatitude(), ctx.getUserLongitude(), SEARCH_RADIUS_KM);
        if (merchants.isEmpty()) {
            throw new NeedUserInputException("附近 5 公里内没有可用商家，请扩大范围或更换位置。", "ask_location");
        }

        ctx.setMerchants(merchants.stream().map(m -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId_wsh());
            map.put("name", m.getName_wsh());
            map.put("rating", m.getRating_wsh());
            map.put("address", m.getAddress_wsh());
            return map;
        }).collect(Collectors.toList()));
        ctx.getLogs().add("  Merchants: " + ctx.getMerchants().size());
    }

    private void step4SearchKeepers(AgentContext ctx) {
        ctx.setCurrentStep(4);
        ctx.getLogs().add("Step 4: Search keepers");
        List<KeeperVO> keepers = keeperService.searchNearby(ctx.getUserLatitude(), ctx.getUserLongitude(), SEARCH_RADIUS_KM);
        if (keepers.isEmpty()) {
            throw new NeedUserInputException("附近没有可用看护人，请更换位置或稍后再试。", "ask_location");
        }

        ctx.setKeepers(keepers.stream().map(k -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", k.getId_wsh());
            map.put("name", k.getName_wsh());
            map.put("merchantId", k.getMerchant_id_wsh());
            map.put("merchantName", k.getMerchant_name_wsh());
            map.put("rating", k.getRating_wsh());
            map.put("pricePerDay", k.getPrice_per_day_wsh());
            map.put("distance", k.getDistance_wsh());
            map.put("experienceYears", k.getExperience_years_wsh());
            map.put("completionRate", k.getCompletion_rate_wsh());
            map.put("complaintRate", k.getComplaint_rate_wsh());
            map.put("currentPets", k.getCurrent_pets_wsh());
            map.put("maxPets", k.getMax_pets_wsh());
            return map;
        }).collect(Collectors.toList()));
        ctx.getLogs().add("  Keepers: " + ctx.getKeepers().size());
    }

    private void step5RankKeepers(AgentContext ctx) {
        ctx.setCurrentStep(5);
        ctx.getLogs().add("Step 5: Rank keepers");
        List<Map<String, Object>> keepers = ctx.getKeepers();
        if (keepers == null || keepers.isEmpty()) {
            throw new NeedUserInputException("暂时没有可推荐的看护人。", "retry_later");
        }

        double maxRating = positiveMaxOf(keepers, "rating", 5.0);
        double maxDistance = positiveMaxOf(keepers, "distance", 1.0);
        double maxPrice = positiveMaxOf(keepers, "pricePerDay", 1.0);
        double maxComplaint = positiveMaxOf(keepers, "complaintRate", 1.0);
        double maxCompletion = positiveMaxOf(keepers, "completionRate", 100.0);

        for (Map<String, Object> keeper : keepers) {
            double ratingScore = boundedRatio(number(keeper.get("rating"), 0.0), maxRating) * 40;
            double distanceScore = (1 - boundedRatio(number(keeper.get("distance"), maxDistance), maxDistance)) * 20;
            double priceScore = (1 - boundedRatio(number(keeper.get("pricePerDay"), maxPrice), maxPrice)) * 20;
            double complaintScore = (1 - boundedRatio(number(keeper.get("complaintRate"), maxComplaint), maxComplaint)) * 10;
            double completionScore = boundedRatio(number(keeper.get("completionRate"), 100.0), maxCompletion) * 10;
            double totalScore = ratingScore + distanceScore + priceScore + complaintScore + completionScore;
            keeper.put("totalScore", Math.round(Math.max(0.0, totalScore) * 100.0) / 100.0);
        }
        keepers.sort((a, b) -> Double.compare(number(b.get("totalScore"), 0.0), number(a.get("totalScore"), 0.0)));
    }

    private void step6GenerateRecommendation(AgentContext ctx) {
        ctx.setCurrentStep(6);
        ctx.getLogs().add("Step 6: Generate recommendation");
        List<Map<String, Object>> keepers = ctx.getKeepers();
        Map<String, Object> bestKeeper = keepers.get(0);
        double budgetPerDay = 500.0 / Math.max(ctx.getDays(), 1);
        for (Map<String, Object> keeper : keepers) {
            if (number(keeper.get("pricePerDay"), Double.MAX_VALUE) <= budgetPerDay) {
                bestKeeper = keeper;
                break;
            }
        }
        ctx.setSelectedKeeper(bestKeeper);
        ctx.getLogs().add("  Recommended keeper: " + bestKeeper.get("name"));
    }

    private void step7CreatePendingOrder(AgentContext ctx) {
        ctx.setCurrentStep(7);
        ctx.getLogs().add("Step 7: Create pending order");

        Map<String, Object> keeper = ctx.getSelectedKeeper();
        Long keeperId = ((Number) keeper.get("id")).longValue();
        Long merchantId = ((Number) keeper.get("merchantId")).longValue();
        Merchant merchant = merchantMapper.selectById(merchantId);
        User owner = userMapper.selectById(ctx.getUserId());
        if (owner == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!StringUtils.hasText(owner.getPhone_wsh())) {
            throw new NeedUserInputException("下单需要紧急联系人手机号，请先在个人资料中绑定手机号。", "complete_profile");
        }

        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(ctx.getDays());

        OrderCreateRequestDTO request = new OrderCreateRequestDTO();
        request.setPet_id_wsh(ctx.getPetId());
        request.setKeeper_id_wsh(keeperId);
        request.setMerchant_id_wsh(merchantId);
        request.setService_id_wsh(resolveServiceId(merchantId));
        request.setStart_date_wsh(start);
        request.setEnd_date_wsh(end);
        request.setDelivery_address_wsh(merchant != null ? merchant.getAddress_wsh() : null);
        request.setDelivery_location_source_wsh("merchant");
        request.setPickup_address_wsh(merchant != null ? merchant.getAddress_wsh() : null);
        request.setPickup_location_source_wsh("merchant");
        request.setDelivery_time_wsh(start.atTime(10, 0));
        request.setReceiver_available_start_wsh(start.atTime(10, 0));
        request.setReceiver_available_end_wsh(start.atTime(10, 30));
        request.setPickup_time_wsh(end.atTime(18, 0));
        request.setEmergency_contact_name_wsh(defaultText(owner.getReal_name_wsh(), defaultText(owner.getNickname_wsh(), owner.getUsername_wsh())));
        request.setEmergency_contact_phone_wsh(owner.getPhone_wsh());
        request.setRemark_wsh("AI Agent pending order - " + HtmlUtils.htmlEscape(ctx.getUserInput()));

        OrderDTO order = orderService.createOrder(ctx.getUserId(), request);
        ctx.setOrderNo(order.getOrder_no_wsh());
        ctx.getLogs().add("  Pending order created: " + order.getOrder_no_wsh());
    }

    private Payment step8CreatePendingPayment(AgentContext ctx) {
        ctx.setCurrentStep(8);
        ctx.getLogs().add("Step 8: Create pending payment");
        Payment payment = paymentService.createPayment(ctx.getUserId(), ctx.getOrderNo(), "balance");
        ctx.setPayNo(payment.getPay_no_wsh());
        ctx.getLogs().add("  Pending payment created: " + payment.getPay_no_wsh());
        return payment;
    }

    private void verifyAutoPayAuthorization(AgentContext ctx, String paymentPassword) {
        ctx.getLogs().add("Step 7: Verify payment authorization");
        if (!StringUtils.hasText(paymentPassword)) {
            throw new NeedUserInputException("如需 Agent 自动支付，请先输入支付密码；否则我会只创建待支付订单，由你手动支付。", "ask_payment_password");
        }
        try {
            userService.verifyPaymentPassword(ctx.getUserId(), paymentPassword);
        } catch (BusinessException e) {
            if (e.getCode() == 400 || e.getCode() == 403) {
                throw new NeedUserInputException(e.getMessage(), "ask_payment_password");
            }
            throw e;
        }
        ctx.getLogs().add("  Payment authorization verified");
    }

    private void payWithVerifiedAuthorization(AgentContext ctx, Payment payment) {
        paymentService.pay(ctx.getUserId(), payment.getPay_no_wsh());
        ctx.getLogs().add("  Payment completed by explicit authorization");
    }

    private void step9SendNotification(AgentContext ctx) {
        ctx.setCurrentStep(9);
        ctx.getLogs().add("Step 9: Send MQ notification");
        try {
            messageSender.sendOrderCreate(ctx.getOrderNo());
            ctx.getLogs().add("  MQ message sent");
        } catch (Exception e) {
            log.warn("Agent MQ notification failed: {}", e.getMessage());
            ctx.getLogs().add("  MQ notification failed (non-fatal): " + e.getMessage());
        }
    }

    private Long resolveServiceId(Long merchantId) {
        ServiceItem service = serviceItemMapper.selectOne(
                new LambdaQueryWrapper<ServiceItem>()
                        .eq(ServiceItem::getMerchant_id_wsh, merchantId)
                        .eq(ServiceItem::getStatus_wsh, StatusCode.SERVICE_ENABLED.getValue())
                        .last("LIMIT 1"));
        return service == null ? null : service.getId_wsh();
    }

    private AgentExecuteResult buildResult(AgentContext ctx, String status, String message, String nextAction, boolean requiresUserInput) {
        ctx.setStatus(status);
        Map<String, Object> result = new HashMap<>();
        result.put("status", status);
        result.put("message_wsh", message);
        result.put("next_action_wsh", nextAction);
        result.put("requires_user_input_wsh", requiresUserInput);
        result.put("payment_status_wsh", "success".equals(status) ? "paid" : (ctx.getPayNo() == null ? null : "pending"));
        result.put("orderNo", ctx.getOrderNo());
        result.put("payNo", ctx.getPayNo());
        result.put("selectedKeeper", ctx.getSelectedKeeper());
        result.put("petName", ctx.getPetName());
        result.put("days", ctx.getDays());
        result.put("logs", ctx.getLogs());
        result.put("currentStep", ctx.getCurrentStep());
        result.put("error", ctx.getError());
        return AgentExecuteResult.fromMap(result);
    }

    private double positiveMaxOf(List<Map<String, Object>> rows, String key, double fallback) {
        double max = rows.stream().mapToDouble(row -> number(row.get(key), 0.0)).max().orElse(0.0);
        if (!Double.isFinite(max) || max <= 0.0) {
            return fallback > 0.0 ? fallback : 1.0;
        }
        return max;
    }

    private double boundedRatio(double value, double denominator) {
        if (!Double.isFinite(value) || !Double.isFinite(denominator) || denominator <= 0.0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value / denominator));
    }

    private double number(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private static class NeedUserInputException extends BusinessException {
        private final String nextAction;

        private NeedUserInputException(String message, String nextAction) {
            super(400, message);
            this.nextAction = nextAction;
        }
    }
}
