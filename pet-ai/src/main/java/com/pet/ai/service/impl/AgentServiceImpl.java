package com.pet.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.ai.dto.AgentExecuteResult;
import com.pet.ai.dto.AgentPlanResult;
import com.pet.ai.dto.AgentPlanSnapshot;
import com.pet.ai.dto.AgentPromptResult;
import com.pet.ai.service.AgentService;
import com.pet.ai.service.LlmChatService;
import com.pet.ai.service.PlanTokenStore;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.mapper.ServiceItemMapper;
import com.pet.boarding.service.KeeperService;
import com.pet.boarding.vo.KeeperVO;
import com.pet.common.BookingUnit;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 代理服务实现：用户一句话描述需求，Agent 生成寄养方案，用户确认后落单。
 * <p>
 * 核心设计：
 * - plan 阶段：LLM 结构化提取需求（替代原正则引擎）→ 匹配宠物/商家/看护人 → 生成方案（不落库）
 * - confirm 阶段：凭方案 token 原子认领后创建订单/支付，事务失败回滚并释放 token 供重试
 * - 服务类型门控：本轮仅支持日间寄养（day 计费）；洗护/遛宠/训练/医疗明确提示不支持
 * - 评分排序：评分40% + 距离20% + 价格20% + 投诉率10% + 完成率10%（原算法保留）
 * - 幂等：方案 token 状态机 NEW → CLAIMED →（成功 afterCommit 移除 / 失败 release 回 NEW）
 */
@Service
@Slf4j
public class AgentServiceImpl implements AgentService {

    private static final double SEARCH_RADIUS_KM = 5.0;
    private static final int MAX_DAYS = 365;
    /** 本轮支持的服务类型（前缀匹配 type_wsh） */
    private static final String SUPPORTED_SERVICE_TYPE = "BOARDING";
    /** 明确不支持的服务类型 */
    private static final Set<String> NON_BOARDING_TYPES = Set.of("GROOMING", "TRAINING", "WALK", "MEDICAL");

    private final PetMapper petMapper;
    private final MerchantMapper merchantMapper;
    private final KeeperService keeperService;
    private final ServiceItemMapper serviceItemMapper;
    private final UserMapper userMapper;
    private final UserService userService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final MessageSender messageSender;
    private final LlmChatService aiChatService;
    private final ObjectMapper objectMapper;
    private final PlanTokenStore planTokenStore;

    public AgentServiceImpl(PetMapper petMapper,
                            MerchantMapper merchantMapper,
                            KeeperService keeperService,
                            ServiceItemMapper serviceItemMapper,
                            UserMapper userMapper,
                            UserService userService,
                            OrderService orderService,
                            PaymentService paymentService,
                            MessageSender messageSender,
                            LlmChatService aiChatService,
                            ObjectMapper objectMapper,
                            PlanTokenStore planTokenStore) {
        this.petMapper = petMapper;
        this.merchantMapper = merchantMapper;
        this.keeperService = keeperService;
        this.serviceItemMapper = serviceItemMapper;
        this.userMapper = userMapper;
        this.userService = userService;
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.messageSender = messageSender;
        this.aiChatService = aiChatService;
        this.objectMapper = objectMapper;
        this.planTokenStore = planTokenStore;
    }

    // ═══════════════════════════════════════════════════════════
    // plan 阶段
    // ═══════════════════════════════════════════════════════════

    /**
     * 【业务名称】智能下单 plan 阶段
     * <p>业务作用：LLM 结构化提取用户需求，匹配宠物与附近看护人，生成推荐方案（不落库），返回方案 token 供用户确认。</p>
     * <p>业务规则：LLM 不可用/解析失败 → failed（不做规则兜底）；非寄养服务类型 → 明确提示不支持；日期晚于今天；天数 1..365；无坐标回退用户档案坐标。</p>
     * <p>状态影响：无。调用方注意：返回的状态有 plan_generated / needs_user_input / failed。</p>
     */
    @Override
    public AgentPlanResult plan(Long userId, String userInput, Double latitude, Double longitude, String address) {
        AgentPlanResult result = new AgentPlanResult();
        List<String> logs = new ArrayList<>();
        result.setLogs(logs);
        try {
            AgentPlanSnapshot snapshot = buildPlan(logs, userId, userInput, latitude, longitude);
            String token = planTokenStore.put(userId, snapshot);
            result.setPlan_token_wsh(token);
            result.setStatus_wsh("plan_generated");
            result.setMessage_wsh("已为你生成寄养方案，请核对后确认下单。");
            result.setRequires_user_input_wsh(false);
            fillPlanFields(result, snapshot);
            return result;
        } catch (NeedUserInputException e) {
            result.setStatus_wsh("needs_user_input");
            result.setRequires_user_input_wsh(true);
            result.setNext_action_wsh(e.getNextAction());
            result.setMessage_wsh(e.getMessage());
            return result;
        } catch (Exception e) {
            log.error("Agent plan 失败: {}", e.getMessage(), e);
            result.setStatus_wsh("failed");
            result.setRequires_user_input_wsh(false);
            result.setNext_action_wsh("retry_later");
            result.setMessage_wsh(safeMessage(e));
            return result;
        }
    }

    private AgentPlanSnapshot buildPlan(List<String> logs, Long userId, String userInput,
                                        Double latitude, Double longitude) {
        // 1. 位置：请求坐标 → 用户档案坐标
        double[] loc = resolveLocation(userId, latitude, longitude);
        logs.add("位置已确定");

        // 2. LLM 结构化提取
        AgentPromptResult prompt = extractRequirement(userInput);
        if (prompt == null) {
            throw new BusinessException(500, "智能下单暂时无法解析你的需求，请稍后重试或直接手动下单。");
        }
        logs.add("需求已解析: " + userInput);

        // 3. 服务类型门控：仅支持日间寄养
        String unit = BookingUnit.normalize(prompt.getUnit());
        if (unit != null && !BookingUnit.DAY.equals(unit)) {
            throw new NeedUserInputException("智能下单目前仅支持按天寄养，按次/按小时服务请手动下单。", "unsupported_service_type");
        }
        if (prompt.getServiceType() != null && NON_BOARDING_TYPES.contains(prompt.getServiceType().trim().toUpperCase())) {
            throw new NeedUserInputException("智能下单目前仅支持寄养类日间服务，洗护/遛宠/训练/医疗请手动下单。", "unsupported_service_type");
        }
        logs.add("服务类型: 日间寄养");

        // 4. 宠物匹配
        List<Pet> pets = petMapper.selectList(new LambdaQueryWrapper<Pet>().eq(Pet::getOwner_id_wsh, userId));
        if (pets.isEmpty()) {
            throw new NeedUserInputException("未找到你的宠物档案，请先添加宠物信息后再让智能下单帮你下单。", "create_pet_profile");
        }
        Pet pet = matchPet(pets, prompt);
        if (pet == null) {
            throw new NeedUserInputException("无法确定是哪只宠物，请在描述中带上宠物昵称（如：帮布丁订 3 天寄养）。", "ask_pet_identity");
        }
        logs.add("宠物: " + pet.getName_wsh());

        // 5. 日期与天数
        LocalDate start = parseStartDate(prompt.getStartDate());
        if (start == null) {
            start = LocalDate.now().plusDays(1);
        }
        if (!start.isAfter(LocalDate.now())) {
            throw new NeedUserInputException("开始日期需晚于今天，请核对日期后重试。", "ask_days");
        }
        int days = prompt.getDays() == null ? 0 : prompt.getDays();
        if (days <= 0) {
            throw new NeedUserInputException("请补充寄养天数，例如 3 天。", "ask_days");
        }
        if (days > MAX_DAYS) {
            throw new NeedUserInputException("寄养天数不能超过 365 天，请调整天数。", "ask_days");
        }
        LocalDate end = start.plusDays(days);
        logs.add("日期: " + start + " ~ " + end + "（" + days + " 天）");

        // 6. 附近商家（可筛门店偏好）
        List<Merchant> merchants = merchantMapper.searchNearby(loc[0], loc[1], SEARCH_RADIUS_KM);
        if (merchants.isEmpty()) {
            throw new NeedUserInputException("附近 5 公里内没有可用商家，请更换位置或稍后再试。", "ask_location");
        }
        if (StringUtils.hasText(prompt.getMerchantKeyword())) {
            String kw = prompt.getMerchantKeyword().trim();
            final String need = lower(kw);
            merchants = merchants.stream()
                    .filter(m -> lower(m.getName_wsh()).contains(need))
                    .collect(Collectors.toList());
            if (merchants.isEmpty()) {
                throw new NeedUserInputException("没有找到你偏好的门店（关键词：" + kw + "），请修改描述后重试。", "ask_requirement");
            }
        }
        Map<Long, Merchant> merchantMap = merchants.stream()
                .collect(Collectors.toMap(Merchant::getId_wsh, m -> m));

        // 7. 看护人（仅保留已筛选商家下的）并评分排序
        Set<Long> merchantIds = merchantMap.keySet();
        List<KeeperVO> keepers = keeperService.searchNearby(loc[0], loc[1], SEARCH_RADIUS_KM).stream()
                .filter(k -> merchantIds.contains(k.getMerchant_id_wsh()))
                .collect(Collectors.toList());
        if (keepers.isEmpty()) {
            throw new NeedUserInputException("这些门店下暂无可用看护人，请更换位置或稍后再试。", "ask_location");
        }
        rankKeepers(keepers);

        // 8. 挑选看护人 + day 寄养服务
        Map<Long, List<ServiceItem>> serviceCache = new HashMap<>();
        Picked picked = pickKeeperAndService(keepers, merchantMap, serviceCache, prompt);
        if (picked == null) {
            throw new NeedUserInputException("该区域暂无提供寄养日间服务的门店，请调整位置或预算后重试。", "retry_later");
        }
        logs.add("推荐: " + picked.keeper.getName_wsh() + " - " + picked.service.getName_wsh());

        // 9. 生成快照
        BigDecimal unitPrice = picked.service.getPrice_wsh() != null
                ? picked.service.getPrice_wsh()
                : picked.keeper.getPrice_per_day_wsh();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(days));

        AgentPlanSnapshot snapshot = new AgentPlanSnapshot();
        snapshot.setPet_id_wsh(pet.getId_wsh());
        snapshot.setPet_name_wsh(pet.getName_wsh());
        snapshot.setKeeper_id_wsh(picked.keeper.getId_wsh());
        snapshot.setKeeper_name_wsh(picked.keeper.getName_wsh());
        snapshot.setMerchant_id_wsh(picked.merchant.getId_wsh());
        snapshot.setMerchant_name_wsh(picked.merchant.getName_wsh());
        snapshot.setMerchant_address_wsh(picked.merchant.getAddress_wsh());
        snapshot.setService_id_wsh(picked.service.getId_wsh());
        snapshot.setService_name_wsh(picked.service.getName_wsh());
        snapshot.setUnit_wsh(BookingUnit.DAY);
        snapshot.setStart_date_wsh(start);
        snapshot.setEnd_date_wsh(end);
        snapshot.setDays_wsh(days);
        snapshot.setUnit_price_wsh(unitPrice);
        snapshot.setTotal_price_wsh(totalPrice);
        snapshot.setDistance_wsh(picked.keeper.getDistance_wsh());
        snapshot.setUser_input_wsh(userInput);
        return snapshot;
    }

    /** 评分排序看护人（算法与重构前一致）。 */
    private void rankKeepers(List<KeeperVO> keepers) {
        double maxRating = maxOf(keepers, k -> dv(k.getRating_wsh()), 5.0);
        double maxDistance = maxOf(keepers, k -> dv(k.getDistance_wsh()), 1.0);
        double maxPrice = maxOf(keepers, k -> dv(k.getPrice_per_day_wsh()), 1.0);
        double maxComplaint = maxOf(keepers, k -> dv(k.getComplaint_rate_wsh()), 1.0);
        double maxCompletion = maxOf(keepers, k -> dv(k.getCompletion_rate_wsh()), 100.0);

        Map<Long, Double> scores = new LinkedHashMap<>();
        for (KeeperVO keeper : keepers) {
            double ratingScore = boundedRatio(dv(keeper.getRating_wsh()), maxRating) * 40;
            double distanceScore = (1 - boundedRatio(dv(keeper.getDistance_wsh()), maxDistance)) * 20;
            double priceScore = (1 - boundedRatio(dv(keeper.getPrice_per_day_wsh()), maxPrice)) * 20;
            double complaintScore = (1 - boundedRatio(dv(keeper.getComplaint_rate_wsh()), maxComplaint)) * 10;
            double completionScore = boundedRatio(dv(keeper.getCompletion_rate_wsh()), maxCompletion) * 10;
            double total = ratingScore + distanceScore + priceScore + complaintScore + completionScore;
            scores.put(keeper.getId_wsh(), Math.round(Math.max(0.0, total) * 100.0) / 100.0);
        }
        keepers.sort((a, b) -> Double.compare(scores.getOrDefault(b.getId_wsh(), 0.0), scores.getOrDefault(a.getId_wsh(), 0.0)));
    }

    /** 依次尝试评分排序后的看护人，返回首个该商家有 day 寄养服务的组合。 */
    private Picked pickKeeperAndService(List<KeeperVO> rankedKeepers, Map<Long, Merchant> merchantMap,
                                        Map<Long, List<ServiceItem>> serviceCache, AgentPromptResult prompt) {
        for (KeeperVO keeper : rankedKeepers) {
            Merchant merchant = merchantMap.get(keeper.getMerchant_id_wsh());
            if (merchant == null) continue;
            ServiceItem service = pickDayService(dayServicesOf(keeper.getMerchant_id_wsh(), serviceCache), prompt);
            if (service != null) {
                return new Picked(keeper, merchant, service);
            }
        }
        return null;
    }

    /** 获取指定商家的启用服务（含非 day 单元，由 pickDayService 过滤）。 */
    private List<ServiceItem> dayServicesOf(Long merchantId, Map<Long, List<ServiceItem>> cache) {
        return cache.computeIfAbsent(merchantId, id -> serviceItemMapper.selectList(
                new LambdaQueryWrapper<ServiceItem>()
                        .eq(ServiceItem::getMerchant_id_wsh, id)
                        .eq(ServiceItem::getStatus_wsh, StatusCode.SERVICE_ENABLED.getValue())));
    }

    /** 从启用服务中挑选 day 单元、类型匹配、预算内的最便宜服务；无匹配返回 null。 */
    private ServiceItem pickDayService(List<ServiceItem> services, AgentPromptResult prompt) {
        if (services == null || services.isEmpty()) return null;
        List<ServiceItem> day = services.stream()
                .filter(s -> {
                    String u = BookingUnit.normalize(s.getUnit_wsh());
                    return u == null || BookingUnit.DAY.equals(u); // 历史数据 unit 为空按 day 兼容
                })
                .collect(Collectors.toList());
        if (day.isEmpty()) return null;

        List<ServiceItem> filtered = day;
        if (prompt.getServiceType() != null) {
            String type = prompt.getServiceType().trim().toUpperCase();
            if (type.startsWith(SUPPORTED_SERVICE_TYPE)) {
                filtered = day.stream()
                        .filter(s -> s.getType_wsh() != null && lower(s.getType_wsh()).startsWith("boarding"))
                        .collect(Collectors.toList());
            }
            if (filtered.isEmpty()) return null;
        }
        if (prompt.getMaxPricePerDay() != null) {
            BigDecimal budget = prompt.getMaxPricePerDay();
            List<ServiceItem> affordable = filtered.stream()
                    .filter(s -> s.getPrice_wsh() != null && s.getPrice_wsh().compareTo(budget) <= 0)
                    .collect(Collectors.toList());
            if (affordable.isEmpty()) return null;
            filtered = affordable;
        }
        return filtered.stream()
                .min(Comparator.comparing(s -> s.getPrice_wsh() == null ? BigDecimal.valueOf(Long.MAX_VALUE) : s.getPrice_wsh()))
                .orElse(null);
    }

    /** LLM 结构化提取需求；LLM 不可用/解析失败返回 null。 */
    private AgentPromptResult extractRequirement(String userInput) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", buildExtractionPrompt()));
        messages.add(Map.of("role", "user", "content", userInput));
        String raw = aiChatService.chat(messages);
        if (raw == null || raw.isBlank()) {
            log.warn("Agent 智能下单：LLM 未返回内容（可能未配置 API Key）");
            return null;
        }
        return parseJson(raw);
    }

    private AgentPromptResult parseJson(String raw) {
        try {
            String cleaned = raw.trim();
            // 去掉可能的 Markdown 代码块围栏
            if (cleaned.startsWith("```")) {
                int firstLine = cleaned.indexOf('\n');
                int last = cleaned.lastIndexOf("```");
                if (firstLine > 0 && last > firstLine) {
                    cleaned = cleaned.substring(firstLine + 1, last).trim();
                }
            }
            return objectMapper.readValue(cleaned, AgentPromptResult.class);
        } catch (Exception e) {
            log.warn("Agent 智能下单：LLM 返回无法解析为 JSON: {}", raw);
            return null;
        }
    }

    private String buildExtractionPrompt() {
        return """
                你是“宠物之家”平台的智能下单解析器。用户输入一条自然语言寄养预约需求，请提取为结构化 JSON。

                要求：
                1. 只输出一个 JSON 对象，不要输出任何其他文字、解释或 Markdown 代码块。
                2. 当前日期：%s。凡提到“今天/明天/这周/下周/几号”等相对时间，都基于当前日期计算并换算成具体日期。
                3. 输出字段定义：
                   - pet_name：宠物昵称；找不到则为 null。
                   - pet_type：宠物类型或品种（如 狗/猫/金毛/泰迪/布偶）；找不到则为 null。
                   - service_type：服务类型，只能是以下之一：BOARDING、GROOMING、TRAINING、WALK、MEDICAL；
                     用户指代寄养/住宿/托管等或无法判断时输出 BOARDING；确定是洗护/美容输出 GROOMING；遛宠输出 WALK；
                     训练输出 TRAINING；医疗输出 MEDICAL；完全无法判断输出 null。
                   - start_date：开始日期，格式 yyyy-MM-dd；用户给出具体日期或可推算日期则输出，否则 null。
                   - days：寄养天数（正整数）；用户给出起止日期（如“9月15日到18日”）时按其包含的天数计算；未提及则为 null。
                   - merchant_keyword：用户偏好的门店名/地名/商圈关键词；没有则为 null。
                   - max_price_per_day：用户提到的每日价格预算上限（数字）；没有则为 null。
                   - unit：计费单位，只能是 "day"、"session"、"hour"；寄养类默认 "day"；用户明确说按次/按小时时填对应值。
                4. 拿不准的字段一律输出 null，绝不编造。
                5. 只输出合法 JSON，字段名、类型严格按定义，不要注释、不要尾逗号。
                """.formatted(LocalDate.now());
    }

    // ═══════════════════════════════════════════════════════════
    // confirm 阶段
    // ═══════════════════════════════════════════════════════════

    /**
     * 【业务名称】智能下单 confirm 阶段
     * <p>业务作用：凭方案 token 原子认领后创建订单与支付记录；autoPay=true 时校验支付密码并直接支付。</p>
     * <p>业务规则：token 防重复下单（同一方案只允许一个执行者）；支付密码缺失/错误不创建订单；成功提交后才消费 token，失败释放 token 供重试。</p>
     * <p>状态影响：新增订单与支付记录。事务失败整体回滚。</p>
     */
    @Transactional
    @Override
    public AgentExecuteResult confirm(Long userId, String planToken, Boolean autoPay, String paymentPassword) {
        AgentPlanSnapshot snapshot = planTokenStore.claim(planToken, userId);
        List<String> logs = new ArrayList<>();
        logs.add("方案已认领");
        AgentExecuteResult result = new AgentExecuteResult();
        try {
            if (Boolean.TRUE.equals(autoPay)) {
                verifyAutoPayAuthorization(userId, paymentPassword);
            }
            User owner = userMapper.selectById(userId);
            if (owner == null) {
                throw new BusinessException(404, "用户不存在");
            }
            if (!StringUtils.hasText(owner.getPhone_wsh())) {
                throw new NeedUserInputException("下单需要紧急联系人手机号，请先在个人资料中绑定手机号。", "complete_profile");
            }
            logs.add("用户资料已校验");

            OrderDTO order = orderService.createOrder(userId, buildOrderRequest(snapshot, owner));
            logs.add("订单已创建: " + order.getOrder_no_wsh());

            Payment payment = paymentService.createPayment(userId, order.getOrder_no_wsh(), "balance");
            logs.add("支付记录已创建: " + payment.getPay_no_wsh());

            if (Boolean.TRUE.equals(autoPay)) {
                paymentService.pay(userId, payment.getPay_no_wsh());
                logs.add("支付已完成");
                step9SendNotification(order.getOrder_no_wsh());
                consumeOnCommit(planToken, userId);
                return buildConfirmResult(snapshot, order, payment, logs, "success",
                        "支付已完成，订单已进入待商家处理状态。", "paid", false);
            }

            step9SendNotification(order.getOrder_no_wsh());
            consumeOnCommit(planToken, userId);
            return buildConfirmResult(snapshot, order, payment, logs, "pending_payment",
                    "订单已创建，当前为待支付状态。请手动支付，或在授权自动支付时输入支付密码。", "manual_payment", false);
        } catch (NeedUserInputException e) {
            planTokenStore.release(planToken, userId);
            result.setStatus("needs_user_input");
            result.setRequires_user_input_wsh(true);
            result.setNext_action_wsh(e.getNextAction());
            result.setMessage_wsh(e.getMessage());
            result.setLogs(logs);
            result.setPetName(snapshot.getPet_name_wsh());
            result.setDays(snapshot.getDays_wsh());
            return result;
        } catch (Exception e) {
            planTokenStore.release(planToken, userId);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("Agent confirm 失败: {}", e.getMessage(), e);
            result.setStatus("failed");
            result.setRequires_user_input_wsh(false);
            result.setNext_action_wsh("retry_later");
            result.setMessage_wsh(safeMessage(e));
            result.setLogs(logs);
            result.setPetName(snapshot.getPet_name_wsh());
            result.setDays(snapshot.getDays_wsh());
            return result;
        }
    }

    private OrderCreateRequestDTO buildOrderRequest(AgentPlanSnapshot s, User owner) {
        LocalDate start = s.getStart_date_wsh();
        LocalDate end = s.getEnd_date_wsh();
        OrderCreateRequestDTO request = new OrderCreateRequestDTO();
        request.setPet_id_wsh(s.getPet_id_wsh());
        request.setKeeper_id_wsh(s.getKeeper_id_wsh());
        request.setMerchant_id_wsh(s.getMerchant_id_wsh());
        request.setService_id_wsh(s.getService_id_wsh());
        request.setBilling_unit_wsh(s.getUnit_wsh());
        request.setExpected_unit_price_wsh(s.getUnit_price_wsh());
        request.setStart_date_wsh(start);
        request.setEnd_date_wsh(end);
        request.setDelivery_address_wsh(s.getMerchant_address_wsh());
        request.setDelivery_location_source_wsh("merchant");
        request.setPickup_address_wsh(s.getMerchant_address_wsh());
        request.setPickup_location_source_wsh("merchant");
        request.setDelivery_time_wsh(start.atTime(10, 0));
        request.setReceiver_available_start_wsh(start.atTime(10, 0));
        request.setReceiver_available_end_wsh(start.atTime(10, 30));
        request.setPickup_time_wsh(end.atTime(18, 0));
        request.setEmergency_contact_name_wsh(defaultText(owner.getReal_name_wsh(), defaultText(owner.getNickname_wsh(), owner.getUsername_wsh())));
        request.setEmergency_contact_phone_wsh(owner.getPhone_wsh());
        request.setRemark_wsh("AI Agent order - " + HtmlUtils.htmlEscape(s.getUser_input_wsh()));
        return request;
    }

    private AgentExecuteResult buildConfirmResult(AgentPlanSnapshot s, OrderDTO order, Payment payment,
                                                  List<String> logs, String status, String message,
                                                  String nextAction, boolean requiresUserInput) {
        AgentExecuteResult result = new AgentExecuteResult();
        result.setStatus(status);
        result.setMessage_wsh(message);
        result.setNext_action_wsh(nextAction);
        result.setRequires_user_input_wsh(requiresUserInput);
        result.setLogs(logs);
        result.setPetName(s.getPet_name_wsh());
        result.setDays(s.getDays_wsh());
        if (order != null) {
            result.setOrderNo(order.getOrder_no_wsh());
        }
        if (payment != null) {
            result.setPayNo(payment.getPay_no_wsh());
            result.setPayment_status_wsh("success".equals(status) ? "paid" : "pending");
        }
        return result;
    }

    /** 事务提交成功后才消费方案 token；无活动事务（如单元测试）则立即消费。 */
    private void consumeOnCommit(String planToken, Long userId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    planTokenStore.consumeAndRemove(planToken, userId);
                }
            });
        } else {
            planTokenStore.consumeAndRemove(planToken, userId);
        }
    }

    private void verifyAutoPayAuthorization(Long userId, String paymentPassword) {
        if (!StringUtils.hasText(paymentPassword)) {
            throw new NeedUserInputException("如需智能下单自动支付，请先输入支付密码；否则我会只创建待支付订单，由你手动支付。", "ask_payment_password");
        }
        try {
            userService.verifyPaymentPassword(userId, paymentPassword);
        } catch (BusinessException e) {
            if (e.getCode() == 400 || e.getCode() == 403) {
                throw new NeedUserInputException(e.getMessage(), "ask_payment_password");
            }
            throw e;
        }
    }

    private void step9SendNotification(String orderNo) {
        try {
            messageSender.sendOrderCreate(orderNo);
            log.debug("Agent 订单通知已发送: {}", orderNo);
        } catch (Exception e) {
            log.warn("Agent 订单 MQ 通知失败（非致命）: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 辅助方法
    // ═══════════════════════════════════════════════════════════

    /** 坐标回退：请求坐标 → 用户档案坐标 → 无坐标抛 ask_location。 */
    private double[] resolveLocation(Long userId, Double latitude, Double longitude) {
        if (latitude != null && longitude != null) {
            return new double[]{latitude, longitude};
        }
        User user = userMapper.selectById(userId);
        if (user != null && user.getLatitude_wsh() != null && user.getLongitude_wsh() != null) {
            return new double[]{user.getLatitude_wsh().doubleValue(), user.getLongitude_wsh().doubleValue()};
        }
        throw new NeedUserInputException("需要你的位置信息才能推荐附近商家，请授权定位或先完善个人资料中的位置。", "ask_location");
    }

    /** 按 LLM 提取的宠物昵称/类型匹配用户宠物；无法唯一确定返回 null。 */
    private Pet matchPet(List<Pet> pets, AgentPromptResult prompt) {
        String name = prompt.getPetName();
        if (StringUtils.hasText(name)) {
            String need = lower(name);
            List<Pet> byName = pets.stream()
                    .filter(p -> lower(p.getName_wsh()).contains(need) || need.contains(lower(p.getName_wsh())))
                    .collect(Collectors.toList());
            if (byName.size() == 1) return byName.get(0);
            if (byName.size() > 1) return null; // 重名，需用户确认
        }
        String type = prompt.getPetType();
        if (StringUtils.hasText(type)) {
            String want = lower(type.trim());
            List<Pet> byType = pets.stream()
                    .filter(p -> lower(p.getBreed_wsh()).contains(want)
                            || want.contains(lower(p.getBreed_wsh()))
                            || lower(p.getType_wsh()).contains(want)
                            || want.contains(lower(p.getType_wsh())))
                    .collect(Collectors.toList());
            if (byType.size() == 1) return byType.get(0);
        }
        // 用户没提宠物名/类型，但只有一只宠物时直接使用
        return pets.size() == 1 ? pets.get(0) : null;
    }

    private LocalDate parseStartDate(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private void fillPlanFields(AgentPlanResult result, AgentPlanSnapshot s) {
        result.setPet_name_wsh(s.getPet_name_wsh());
        result.setPet_id_wsh(s.getPet_id_wsh());
        result.setMerchant_name_wsh(s.getMerchant_name_wsh());
        result.setMerchant_id_wsh(s.getMerchant_id_wsh());
        result.setKeeper_name_wsh(s.getKeeper_name_wsh());
        result.setKeeper_id_wsh(s.getKeeper_id_wsh());
        result.setService_name_wsh(s.getService_name_wsh());
        result.setService_id_wsh(s.getService_id_wsh());
        result.setUnit_wsh(s.getUnit_wsh());
        result.setStart_date_wsh(s.getStart_date_wsh());
        result.setEnd_date_wsh(s.getEnd_date_wsh());
        result.setDays_wsh(s.getDays_wsh());
        result.setUnit_price_wsh(s.getUnit_price_wsh());
        result.setTotal_price_wsh(s.getTotal_price_wsh());
        result.setDistance_wsh(s.getDistance_wsh());
    }

    private double maxOf(List<KeeperVO> rows, java.util.function.ToDoubleFunction<KeeperVO> fn, double fallback) {
        double max = rows.stream().mapToDouble(fn).max().orElse(0.0);
        return (!Double.isFinite(max) || max <= 0.0) ? fallback : max;
    }

    private double boundedRatio(double value, double denominator) {
        if (!Double.isFinite(value) || !Double.isFinite(denominator) || denominator <= 0.0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value / denominator));
    }

    private double dv(Number value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String safeMessage(Exception e) {
        return e instanceof org.springframework.dao.DataAccessException
                ? "系统内部错误，请稍后重试。"
                : (e.getMessage() == null || e.getMessage().isBlank() ? "操作失败，请稍后重试。" : e.getMessage());
    }

    /** plan 结果缺距离字段时兼容（当前快照未保存距离，返回 null 由前端忽略）。 */
    private static final class Picked {
        private final KeeperVO keeper;
        private final Merchant merchant;
        private final ServiceItem service;

        private Picked(KeeperVO keeper, Merchant merchant, ServiceItem service) {
            this.keeper = keeper;
            this.merchant = merchant;
            this.service = service;
        }
    }

    /** 缺信息中断流程：返回 needs_user_input 状态并提示用户补充。 */
    private static class NeedUserInputException extends BusinessException {
        private final String nextAction;

        private NeedUserInputException(String message, String nextAction) {
            super(400, message);
            this.nextAction = nextAction;
        }

        private String getNextAction() {
            return nextAction;
        }
    }
}