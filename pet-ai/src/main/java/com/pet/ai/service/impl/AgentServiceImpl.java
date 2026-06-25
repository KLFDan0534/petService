package com.pet.ai.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.BusinessException;
import com.pet.ai.dto.AgentContext;
import com.pet.boarding.vo.KeeperVO;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.service.KeeperService;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.order.dto.CreateOrderRequest;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.service.OrderService;
import com.pet.order.entity.Payment;
import com.pet.order.mapper.PaymentMapper;
import com.pet.order.service.PaymentService;
import com.pet.pet.entity.Pet;
import com.pet.pet.mapper.PetMapper;
import com.pet.mq.MessageSender;
import com.pet.ai.service.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;
import com.pet.ai.service.AgentService;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AgentServiceImpl implements AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentServiceImpl.class);

    private static final Map<String, List<String>> BREED_CN_MAP = new HashMap<>();
    static {
        BREED_CN_MAP.put("poodle", Arrays.asList("Poodle", "Toy Poodle", "Miniature Poodle"));
        BREED_CN_MAP.put("golden", Arrays.asList("Golden Retriever", "Golden"));
        BREED_CN_MAP.put("husky", Arrays.asList("Husky", "Siberian Husky"));
        BREED_CN_MAP.put("corgi", Arrays.asList("Corgi", "Pembroke", "Cardigan"));
        BREED_CN_MAP.put("labrador", Arrays.asList("Labrador", "Labrador Retriever"));
        BREED_CN_MAP.put("samoyed", Arrays.asList("Samoyed"));
        BREED_CN_MAP.put("shiba", Arrays.asList("Shiba Inu", "Shiba"));
        BREED_CN_MAP.put("pomeranian", Arrays.asList("Pomeranian"));
        BREED_CN_MAP.put("ragdoll", Arrays.asList("Ragdoll"));
        BREED_CN_MAP.put("british", Arrays.asList("British Shorthair", "British"));
        BREED_CN_MAP.put("american", Arrays.asList("American Shorthair", "American"));
        BREED_CN_MAP.put("exotic", Arrays.asList("Exotic Shorthair", "Persian"));
        BREED_CN_MAP.put("persian", Arrays.asList("Persian"));
        BREED_CN_MAP.put("dog", Arrays.asList("Dog", "dog"));
        BREED_CN_MAP.put("cat", Arrays.asList("Cat", "cat"));
    }

    private static final double DEFAULT_LAT = 31.2304;
    private static final double DEFAULT_LNG = 121.4737;

    private final PetMapper petMapper;
    private final MerchantMapper merchantMapper;
    private final KeeperMapper keeperMapper;
    private final KeeperService keeperService;
    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;
    private final MessageSender messageSender;

    public AgentServiceImpl(PetMapper petMapper, MerchantMapper merchantMapper,
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

    @Transactional
    @Override
    public Map<String, Object> execute(Long userId, String userInput,
                                        Double latitude, Double longitude) {
        log.info("调用 execute()");
        AgentContext ctx = new AgentContext();
        ctx.setUserId(userId);
        ctx.setUserInput(userInput);
        ctx.setUserLatitude(latitude != null ? latitude : DEFAULT_LAT);
        ctx.setUserLongitude(longitude != null ? longitude : DEFAULT_LNG);
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
            log.error("Agent执行在步骤 {} 失败: {}", ctx.getCurrentStep(), e.getMessage());
            String safeMsg = (e instanceof org.springframework.dao.DataAccessException)
                ? "系统内部错误，请稍后重试"
                : e.getMessage();
            ctx.setStatus("failed");
            ctx.setError(safeMsg);
            ctx.getLogs().add("Step " + ctx.getCurrentStep() + " FAILED: " + safeMsg);

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
        if (input == null || input.trim().isEmpty()) {
            throw new BusinessException("用户输入为空，请提供需求描述");
        }
        ctx.getLogs().add("Step 1: Identify requirement - " + input);

        List<String> breedKeys = new ArrayList<>(BREED_CN_MAP.keySet());
        breedKeys.sort((a,b) -> b.length() - a.length());
        String petPatternStr = "(" + String.join("|", breedKeys) + ")";
        Pattern petPattern = Pattern.compile(petPatternStr, Pattern.CASE_INSENSITIVE);
        Matcher petMatcher = petPattern.matcher(input);
        if (petMatcher.find()) {
            ctx.setPetType(petMatcher.group(1));
        } else {
            throw new BusinessException("无法识别宠物类型，请说明品种（如：泰迪、金毛、哈士奇）");
        }

        Pattern dayPattern = Pattern.compile("(\\d+)\\s*(day|days)?", Pattern.CASE_INSENSITIVE);
        Matcher dayMatcher = dayPattern.matcher(input);
        if (dayMatcher.find()) {
            ctx.setDays(Integer.parseInt(dayMatcher.group(1)));
        } else {
            throw new BusinessException("无法识别寄养天数，请说明需要寄养的天数");
        }

        if (ctx.getDays() <= 0) {
            throw new BusinessException("天数必须为正整数");
        }
        if (ctx.getDays() > 365) {
            throw new BusinessException("寄养天数不能超过 365 天");
        }

        ctx.getLogs().add("  Identified pet: " + ctx.getPetType() + ", days: " + ctx.getDays());
    }

    private void step2QueryPetProfile(AgentContext ctx) {
        ctx.setCurrentStep(2);
        ctx.getLogs().add("Step 2: Query pet profile");

        List<Pet> pets = petMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Pet>()
                        .eq(Pet::getOwner_id_wsh, ctx.getUserId()));
        if (pets.isEmpty()) {
            throw new BusinessException("未找到宠物档案，请先添加宠物信息");
        }

        Pet matchedPet = null;
        String petType = ctx.getPetType();

        List<String> englishKeywords = BREED_CN_MAP.getOrDefault(petType, Collections.emptyList());
        for (Pet pet : pets) {
            String breed = pet.getBreed_wsh();
            String type = pet.getType_wsh();
            if (breed != null) {
                for (String keyword : englishKeywords) {
                    if (breed.toLowerCase().contains(keyword.toLowerCase())) {
                        matchedPet = pet;
                        break;
                    }
                }
                if (matchedPet != null) break;
            }
            if (type != null) {
                if (type.toLowerCase().contains("dog")) {
                    matchedPet = pet;
                    break;
                }
                if (type.toLowerCase().contains("cat")) {
                    matchedPet = pet;
                    break;
                }
            }
        }

        if (matchedPet == null) {
            for (Pet pet : pets) {
                if (pet.getName_wsh() != null && pet.getName_wsh().contains(petType)) {
                    matchedPet = pet;
                    break;
                }
                if (pet.getBreed_wsh() != null && pet.getBreed_wsh().contains(petType)) {
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

        ctx.setPetId(matchedPet.getId_wsh());
        ctx.setPetName(matchedPet.getName_wsh());
        ctx.getLogs().add("  Found pet: " + matchedPet.getName_wsh() + "(" + matchedPet.getBreed_wsh() + ")");
    }

    private void step3SearchMerchants(AgentContext ctx) {
        ctx.setCurrentStep(3);
        ctx.getLogs().add("Step 3: Search nearby merchants");

        if (ctx.getUserLatitude() == null || ctx.getUserLongitude() == null) {
            throw new BusinessException("缺少位置信息，请提供位置");
        }

        List<Merchant> merchants = merchantMapper.searchNearby(
                ctx.getUserLatitude(), ctx.getUserLongitude(), 5.0);
        if (merchants.isEmpty()) {
            throw new BusinessException("5公里范围内没有商家");
        }

        List<Map<String, Object>> merchantList = merchants.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId_wsh());
            map.put("name", m.getName_wsh());
            map.put("rating", m.getRating_wsh());
            map.put("address", m.getAddress_wsh());
            return map;
        }).collect(Collectors.toList());

        ctx.setMerchants(merchantList);
        ctx.getLogs().add("  Found " + merchantList.size() + " merchants");
    }

    private void step4SearchKeepers(AgentContext ctx) {
        ctx.setCurrentStep(4);
        ctx.getLogs().add("Step 4: Search keepers");

        List<KeeperVO> keepers = keeperService.searchNearby(
                ctx.getUserLatitude(), ctx.getUserLongitude(), 5.0);

        if (keepers.isEmpty()) {
            throw new BusinessException("附近没有可用的看护人");
        }

        List<Map<String, Object>> keeperList = keepers.stream().map(k -> {
            Map<String, Object> map = new HashMap<>();
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
        }).collect(Collectors.toList());

        ctx.setKeepers(keeperList);
        ctx.getLogs().add("  Found " + keeperList.size() + " keepers");
    }

    @SuppressWarnings("unchecked")
    private void step5RankKeepers(AgentContext ctx) {
        ctx.setCurrentStep(5);
        ctx.getLogs().add("Step 5: Rank keepers (Rating 40% Distance 20% Price 20% Complaint 10% Completion 10%)");

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

        ctx.getLogs().add("  Ranking complete, highest score: " + keepers.get(0).get("totalScore"));
    }

    @SuppressWarnings("unchecked")
    private void step6GenerateRecommendation(AgentContext ctx) {
        ctx.setCurrentStep(6);
        ctx.getLogs().add("Step 6: Generate recommendation");

        List<Map<String, Object>> keepers = ctx.getKeepers();
        if (keepers.isEmpty()) {
            throw new BusinessException("没有可用的看护人");
        }

        String input = ctx.getUserInput();
        boolean preferHighestRating = input != null &&
            (input.toLowerCase().contains("best") || input.toLowerCase().contains("top") ||
             input.toLowerCase().contains("highest") || input.toLowerCase().contains("excellent"));

        if (preferHighestRating) {
            double maxRating = keepers.stream()
                .mapToDouble(k -> ((Number) k.get("rating")).doubleValue())
                .max().orElse(5.0);
            for (Map<String, Object> k : keepers) {
                double ratingScore = ((Number) k.get("rating")).doubleValue() / maxRating * 70;
                double distance = ((Number) k.get("distance")).doubleValue();
                double price = ((Number) k.get("pricePerDay")).doubleValue();
                double otherScore = (1 - distance / 5.0) * 15 + (1 - Math.min(price, 300) / 300) * 15;
                k.put("totalScore", Math.round((ratingScore + otherScore) * 100.0) / 100.0);
            }
            keepers.sort((a, b) -> Double.compare(
                ((Number) b.get("totalScore")).doubleValue(),
                ((Number) a.get("totalScore")).doubleValue()));
            ctx.getLogs().add("  User prefers highest rating, rating weight increased to 70%");
        }

        double budgetPerDay = 500.0 / ctx.getDays();

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
        ctx.getLogs().add("  Recommended keeper: " + bestKeeper.get("name") +
                ", rating: " + bestKeeper.get("rating") +
                ", price: " + bestKeeper.get("pricePerDay") + "/day" +
                ", distance: " + String.format("%.2f", bestKeeper.get("distance")) + "km" +
                (preferHighestRating ? " (highest rating priority)" : ""));
    }

    @SuppressWarnings("unchecked")
    private void step7CreateOrder(AgentContext ctx) {
        ctx.setCurrentStep(7);
        ctx.getLogs().add("Step 7: Create order");

        Map<String, Object> keeper = ctx.getSelectedKeeper();
        Long keeperId = ((Number) keeper.get("id")).longValue();
        Long merchantId = ((Number) keeper.get("merchantId")).longValue();

        CreateOrderRequest request = new CreateOrderRequest();
        request.setPet_id_wsh(ctx.getPetId());
        request.setKeeper_id_wsh(keeperId);
        request.setMerchant_id_wsh(merchantId);
        request.setStart_date_wsh(LocalDate.now());
        request.setEnd_date_wsh(LocalDate.now().plusDays(ctx.getDays()));
        request.setRemark_wsh("AI Agent auto order - " + HtmlUtils.htmlEscape(ctx.getUserInput()));

        PetOrder order = orderService.createOrder(ctx.getUserId(), request);
        ctx.setOrderNo(order.getOrder_no_wsh());
        ctx.getLogs().add("  Order created: " + order.getOrder_no_wsh() + ", amount: " + order.getFinal_amount_wsh());
    }

    private void step8Pay(AgentContext ctx) {
        ctx.setCurrentStep(8);
        ctx.getLogs().add("Step 8: Pay");

        Payment payment = paymentService.createPayment(ctx.getUserId(), ctx.getOrderNo(), "balance");
        paymentService.pay(payment.getPay_no_wsh());
        ctx.setPayNo(payment.getPay_no_wsh());
        ctx.getLogs().add("  Payment success: " + payment.getPay_no_wsh());
    }

    private void step9SendNotification(AgentContext ctx) {
        ctx.setCurrentStep(9);
        ctx.getLogs().add("Step 9: Send MQ notification");
        try {
            messageSender.sendOrderCreate(ctx.getOrderNo());
            ctx.getLogs().add("  MQ message sent");
        } catch (Exception e) {
            log.warn("MQ通知失败（非致命）: {}", e.getMessage());
            ctx.getLogs().add("  MQ notification failed (non-fatal): " + e.getMessage());
        }
    }

    private void step10Complete(AgentContext ctx) {
        ctx.setCurrentStep(10);
        ctx.setStatus("completed");
        ctx.getLogs().add("Step 10: Complete");
        ctx.getLogs().add("==============================");
        ctx.getLogs().add("Order auto completed");
        ctx.getLogs().add("Order No: " + ctx.getOrderNo());
        ctx.getLogs().add("Pet: " + ctx.getPetName());
        ctx.getLogs().add("Keeper: " + ctx.getSelectedKeeper().get("name"));
        ctx.getLogs().add("Days: " + ctx.getDays());
        ctx.getLogs().add("==============================");
    }
}
