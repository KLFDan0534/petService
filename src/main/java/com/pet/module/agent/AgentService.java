package com.pet.module.agent;

import com.pet.common.BusinessException;
import com.pet.module.keeper.dto.KeeperVO;
import com.pet.module.keeper.entity.Keeper;
import com.pet.module.keeper.mapper.KeeperMapper;
import com.pet.module.keeper.service.KeeperService;
import com.pet.module.merchant.entity.Merchant;
import com.pet.module.merchant.mapper.MerchantMapper;
import com.pet.module.order.dto.CreateOrderRequest;
import com.pet.module.order.entity.PetOrder;
import com.pet.module.order.mapper.OrderMapper;
import com.pet.module.order.service.OrderService;
import com.pet.module.payment.entity.Payment;
import com.pet.module.payment.mapper.PaymentMapper;
import com.pet.module.payment.service.PaymentService;
import com.pet.module.pet.entity.Pet;
import com.pet.module.pet.mapper.PetMapper;
import com.pet.mq.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final PetMapper petMapper;
    private final MerchantMapper merchantMapper;
    private final KeeperMapper keeperMapper;
    private final KeeperService keeperService;
    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;
    private final MessageSender messageSender;

    public AgentService(PetMapper petMapper, MerchantMapper merchantMapper,
                        KeeperMapper keeperMapper, KeeperService keeperService,
                        OrderService orderService, OrderMapper orderMapper,
                        PaymentService paymentService, PaymentMapper paymentMapper,
                        MessageSender messageSender) {
        this.petMapper = petMapper;
        this.merchantMapper = merchantMapper;
        this.keeperMapper = keeperMapper;
        this.keeperService = keeperService;
        this.orderService = orderService;
        this.orderMapper = orderMapper;
        this.paymentService = paymentService;
        this.paymentMapper = paymentMapper;
        this.messageSender = messageSender;
    }

    public Map<String, Object> execute(Long userId, String userInput,
                                        Double latitude, Double longitude) {
        AgentContext ctx = new AgentContext();
        ctx.setUserId(userId);
        ctx.setUserInput(userInput);
        ctx.setUserLatitude(latitude);
        ctx.setUserLongitude(longitude);
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
            step7CreateOrder(ctx);
            step8Pay(ctx);
            step9SendNotification(ctx);
            step10Complete(ctx);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("orderNo", ctx.getOrderNo());
            result.put("payNo", ctx.getPayNo());
            result.put("selectedKeeper", ctx.getSelectedKeeper());
            result.put("petName", ctx.getPetName());
            result.put("days", ctx.getDays());
            result.put("logs", ctx.getLogs());
            return result;
        } catch (Exception e) {
            log.error("Agent execution failed at step {}: {}", ctx.getCurrentStep(), e.getMessage());
            ctx.setStatus("failed");
            ctx.setError(e.getMessage());
            ctx.getLogs().add("Step " + ctx.getCurrentStep() + " FAILED: " + e.getMessage());

            Map<String, Object> result = new HashMap<>();
            result.put("status", "failed");
            result.put("currentStep", ctx.getCurrentStep());
            result.put("error", e.getMessage());
            result.put("logs", ctx.getLogs());
            return result;
        }
    }

    private void step1IdentifyRequirement(AgentContext ctx) {
        ctx.setCurrentStep(1);
        String input = ctx.getUserInput();
        ctx.getLogs().add("Step 1: 识别需求 - " + input);

        Pattern petPattern = Pattern.compile("(泰迪|金毛|哈士奇|柯基|拉布拉多|萨摩耶|柴犬|博美|贵宾|边牧|猫|布偶猫|英短|美短|加菲猫|波斯猫|宠物|狗|猫)");
        Matcher petMatcher = petPattern.matcher(input);
        if (petMatcher.find()) {
            ctx.setPetType(petMatcher.group(1));
        } else {
            throw new BusinessException("无法识别宠物类型，请明确宠物品种");
        }

        Pattern dayPattern = Pattern.compile("(\\d+)\\s*天");
        Matcher dayMatcher = dayPattern.matcher(input);
        if (dayMatcher.find()) {
            ctx.setDays(Integer.parseInt(dayMatcher.group(1)));
        } else {
            throw new BusinessException("无法识别寄养天数，请明确寄养天数");
        }

        ctx.getLogs().add("  识别到宠物: " + ctx.getPetType() + ", 天数: " + ctx.getDays());
    }

    private void step2QueryPetProfile(AgentContext ctx) {
        ctx.setCurrentStep(2);
        ctx.getLogs().add("Step 2: 查询宠物档案");

        List<Pet> pets = petMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Pet>()
                        .eq(Pet::getOwnerId, ctx.getUserId()));
        if (pets.isEmpty()) {
            throw new BusinessException("未找到宠物档案，请先添加宠物信息");
        }

        Pet matchedPet = null;
        for (Pet pet : pets) {
            if (pet.getType() != null && ctx.getPetType() != null) {
                if (pet.getType().contains(ctx.getPetType()) || ctx.getPetType().contains(pet.getType())) {
                    matchedPet = pet;
                    break;
                }
                if (pet.getBreed() != null && pet.getBreed().contains(ctx.getPetType())) {
                    matchedPet = pet;
                    break;
                }
            }
        }
        if (matchedPet == null && !pets.isEmpty()) {
            matchedPet = pets.get(0);
        }
        if (matchedPet == null) {
            throw new BusinessException("未找到匹配的宠物档案");
        }

        ctx.setPetId(matchedPet.getId());
        ctx.setPetName(matchedPet.getName());
        ctx.getLogs().add("  找到宠物: " + matchedPet.getName() + "(" + matchedPet.getBreed() + ")");
    }

    private void step3SearchMerchants(AgentContext ctx) {
        ctx.setCurrentStep(3);
        ctx.getLogs().add("Step 3: 查询附近商家");

        if (ctx.getUserLatitude() == null || ctx.getUserLongitude() == null) {
            throw new BusinessException("缺少位置信息，请提供位置");
        }

        List<Merchant> merchants = merchantMapper.searchNearby(
                ctx.getUserLatitude(), ctx.getUserLongitude(), 5.0);
        if (merchants.isEmpty()) {
            throw new BusinessException("附近5公里范围内无商家");
        }

        List<Map<String, Object>> merchantList = merchants.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("name", m.getName());
            map.put("rating", m.getRating());
            map.put("address", m.getAddress());
            return map;
        }).collect(Collectors.toList());

        ctx.setMerchants(merchantList);
        ctx.getLogs().add("  找到 " + merchantList.size() + " 家商家");
    }

    private void step4SearchKeepers(AgentContext ctx) {
        ctx.setCurrentStep(4);
        ctx.getLogs().add("Step 4: 查询寄养员");

        List<KeeperVO> keepers = keeperService.searchNearby(
                ctx.getUserLatitude(), ctx.getUserLongitude(), 5.0);

        if (keepers.isEmpty()) {
            throw new BusinessException("附近无可用寄养员");
        }

        List<Map<String, Object>> keeperList = keepers.stream().map(k -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", k.getId());
            map.put("name", k.getName());
            map.put("merchantId", k.getMerchantId());
            map.put("merchantName", k.getMerchantName());
            map.put("rating", k.getRating());
            map.put("pricePerDay", k.getPricePerDay());
            map.put("distance", k.getDistance());
            map.put("experienceYears", k.getExperienceYears());
            map.put("completionRate", k.getCompletionRate());
            map.put("complaintRate", k.getComplaintRate());
            map.put("currentPets", k.getCurrentPets());
            map.put("maxPets", k.getMaxPets());
            return map;
        }).collect(Collectors.toList());

        ctx.setKeepers(keeperList);
        ctx.getLogs().add("  找到 " + keeperList.size() + " 位寄养员");
    }

    @SuppressWarnings("unchecked")
    private void step5RankKeepers(AgentContext ctx) {
        ctx.setCurrentStep(5);
        ctx.getLogs().add("Step 5: 综合评分排序（评分40% 距离20% 价格20% 投诉率10% 完成率10%）");

        List<Map<String, Object>> keepers = ctx.getKeepers();

        double maxRating = keepers.stream()
                .mapToDouble(k -> ((Number) k.get("rating")).doubleValue())
                .max().orElse(5.0);
        double maxDistance = keepers.stream()
                .mapToDouble(k -> ((Number) k.get("distance")).doubleValue())
                .max().orElse(1.0);
        double maxPrice = keepers.stream()
                .mapToDouble(k -> ((Number) k.get("pricePerDay")).doubleValue())
                .max().orElse(1.0);
        double maxComplaint = keepers.stream()
                .mapToDouble(k -> ((Number) k.getOrDefault("complaintRate", 0)).doubleValue())
                .max().orElse(1.0);
        double maxCompletion = keepers.stream()
                .mapToDouble(k -> ((Number) k.getOrDefault("completionRate", 100)).doubleValue())
                .max().orElse(100.0);

        for (Map<String, Object> k : keepers) {
            double ratingScore = ((Number) k.get("rating")).doubleValue() / maxRating * 40;
            double distanceScore = (1 - ((Number) k.get("distance")).doubleValue() / maxDistance) * 20;
            double priceScore = (1 - ((Number) k.get("pricePerDay")).doubleValue() / maxPrice) * 20;
            double complaintScore = (1 - ((Number) k.getOrDefault("complaintRate", 0)).doubleValue() / maxComplaint) * 10;
            double completionScore = ((Number) k.getOrDefault("completionRate", 100)).doubleValue() / maxCompletion * 10;
            double totalScore = ratingScore + distanceScore + priceScore + complaintScore + completionScore;
            k.put("totalScore", Math.round(totalScore * 100.0) / 100.0);
        }

        keepers.sort((a, b) -> Double.compare(
                ((Number) b.get("totalScore")).doubleValue(),
                ((Number) a.get("totalScore")).doubleValue()));

        ctx.getLogs().add("  排序完成，最高分: " + keepers.get(0).get("totalScore"));
    }

    @SuppressWarnings("unchecked")
    private void step6GenerateRecommendation(AgentContext ctx) {
        ctx.setCurrentStep(6);
        ctx.getLogs().add("Step 6: 生成推荐方案");

        List<Map<String, Object>> keepers = ctx.getKeepers();
        if (keepers.isEmpty()) {
            throw new BusinessException("无可用寄养员");
        }

        double budgetPerDay = 300.0 / ctx.getDays();

        Map<String, Object> bestKeeper = null;
        for (Map<String, Object> k : keepers) {
            double price = ((Number) k.get("pricePerDay")).doubleValue();
            if (price <= budgetPerDay) {
                bestKeeper = k;
                break;
            }
        }

        if (bestKeeper == null) {
            bestKeeper = keepers.get(0);
        }

        ctx.setSelectedKeeper(bestKeeper);
        ctx.getLogs().add("  推荐寄养员: " + bestKeeper.get("name") +
                ", 评分: " + bestKeeper.get("rating") +
                ", 价格: " + bestKeeper.get("pricePerDay") + "/天" +
                ", 距离: " + String.format("%.2f", bestKeeper.get("distance")) + "km");
    }

    @SuppressWarnings("unchecked")
    @Transactional
    private void step7CreateOrder(AgentContext ctx) {
        ctx.setCurrentStep(7);
        ctx.getLogs().add("Step 7: 创建订单");

        Map<String, Object> keeper = ctx.getSelectedKeeper();
        Long keeperId = ((Number) keeper.get("id")).longValue();
        Long merchantId = ((Number) keeper.get("merchantId")).longValue();

        CreateOrderRequest request = new CreateOrderRequest();
        request.setPetId(ctx.getPetId());
        request.setKeeperId(keeperId);
        request.setMerchantId(merchantId);
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(ctx.getDays()));
        request.setRemark("AI Agent自动下单 - " + ctx.getUserInput());

        PetOrder order = orderService.createOrder(ctx.getUserId(), request);
        ctx.setOrderNo(order.getOrderNo());
        ctx.getLogs().add("  订单创建成功: " + order.getOrderNo() + ", 金额: " + order.getFinalAmount());
    }

    @Transactional
    private void step8Pay(AgentContext ctx) {
        ctx.setCurrentStep(8);
        ctx.getLogs().add("Step 8: 支付");

        Payment payment = paymentService.createPayment(ctx.getUserId(), ctx.getOrderNo(), "balance");
        paymentService.pay(payment.getPayNo());
        ctx.setPayNo(payment.getPayNo());
        ctx.getLogs().add("  支付成功: " + payment.getPayNo());
    }

    private void step9SendNotification(AgentContext ctx) {
        ctx.setCurrentStep(9);
        ctx.getLogs().add("Step 9: 发送MQ通知");

        messageSender.sendOrderCreate(ctx.getOrderNo());
        ctx.getLogs().add("  MQ消息已发送");
    }

    private void step10Complete(AgentContext ctx) {
        ctx.setCurrentStep(10);
        ctx.setStatus("completed");
        ctx.getLogs().add("Step 10: 完成");
        ctx.getLogs().add("==============================");
        ctx.getLogs().add("订单已自动完成!");
        ctx.getLogs().add("订单号: " + ctx.getOrderNo());
        ctx.getLogs().add("宠物: " + ctx.getPetName());
        ctx.getLogs().add("寄养员: " + ctx.getSelectedKeeper().get("name"));
        ctx.getLogs().add("天数: " + ctx.getDays() + "天");
        ctx.getLogs().add("==============================");
    }
}
