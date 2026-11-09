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
import com.pet.order.dto.OrderPreviewRequestDTO;
import com.pet.order.dto.OrderQuoteDTO;
import com.pet.order.entity.Payment;
import com.pet.order.pricing.OrderPricing;
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
import java.time.LocalDateTime;
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

    /**
     * 【智能下单服务构造器】
     *
     * 业务作用：注入宠物、商家、看护者、服务、用户、订单、支付、消息、大模型与方案存储等依赖。
     *
     * 调用场景：Spring 容器启动时由依赖注入自动调用。
     *
     * 调用链：Spring IoC → 构造器 → 逐字段赋值。
     *
     * 数据处理：仅字段赋值。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：依赖较多，说明该类同时承担需求解析、匹配排序与下单编排。
     */
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

    /**
     * 【构建寄养方案】
     *
     * 业务作用：完成 plan 阶段全部编排：定位、需求解析、类型门控、宠物匹配、日期校验、商家与看护者筛选排序、服务挑选、生成方案快照。
     *
     * 调用场景：plan 方法中调用，异常由 plan 统一转换为业务状态。
     *
     * 调用链：plan → buildPlan → resolveLocation / extractRequirement / matchPet / searchNearby / rankKeepers / pickKeeperAndService。
     *
     * 数据处理：坐标优先取请求参数，缺失时回退用户档案；结束日期 = 开始日期 + 天数；单价取服务价，缺失时取看护者日单价。
     *
     * 业务规则：仅支持 day 计费与寄养类服务；开始日期必须晚于今天；天数 1 到 365；搜索半径 5 公里。
     *
     * 状态影响：无（不落库），仅生成内存快照。
     *
     * 异常情况：缺信息 → NeedUserInputException 并带 next_action；LLM 不可用 → BusinessException(500)。
     *
     * 注意事项：过程中向 logs 追加步骤日志，最终原样返回给前端展示。
     */
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

        // 3. 服务类型门控：仅支持按夜寄养
        String unit = BookingUnit.normalize(prompt.getUnit());
        if (unit != null && !BookingUnit.NIGHT.equals(unit)) {
            throw new NeedUserInputException("智能下单目前仅支持按夜寄养，按次/按小时服务请手动下单。", "unsupported_service_type");
        }
        if (prompt.getServiceType() != null && NON_BOARDING_TYPES.contains(prompt.getServiceType().trim().toUpperCase())) {
            throw new NeedUserInputException("智能下单目前仅支持寄养类服务，洗护/遛宠/训练/医疗请手动下单。", "unsupported_service_type");
        }
        logs.add("服务类型: 按夜寄养");

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

        // 5. 入住日期与预订夜数
        LocalDate start = parseStartDate(prompt.getStartDate());
        if (start == null) {
            start = LocalDate.now().plusDays(1);
        }
        if (!start.isAfter(LocalDate.now())) {
            throw new NeedUserInputException("入住日期需晚于今天，请核对日期后重试。", "ask_days");
        }
        Integer requestedNights = prompt.getNights() != null ? prompt.getNights() : prompt.getDays();
        int nights = requestedNights == null ? 0 : requestedNights;
        if (nights <= 0) {
            throw new NeedUserInputException("请补充预订夜数，例如 3 晚。", "ask_days");
        }
        if (nights > MAX_DAYS) {
            throw new NeedUserInputException("预订夜数不能超过 365 夜，请调整夜数。", "ask_days");
        }
        // 按夜计价：离店日 = 入住日 + 夜数（不是「含首尾」的 days-1）
        LocalDate checkoutDate = start.plusDays(nights);
        logs.add("日期: " + start + " 入住 ~ " + checkoutDate + " 离店（" + nights + " 夜）");

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

        // 8. 挑选看护人 + night 寄养服务
        Map<Long, List<ServiceItem>> serviceCache = new HashMap<>();
        Picked picked = pickKeeperAndService(keepers, merchantMap, serviceCache, prompt);
        if (picked == null) {
            throw new NeedUserInputException("该区域暂无提供按夜寄养服务的门店，请调整位置或预算后重试。", "retry_later");
        }
        logs.add("推荐: " + picked.keeper.getName_wsh() + " - " + picked.service.getName_wsh());

        // 9. 生成快照
        // 价格唯一来源：服务单价（看护者个人定价已下线）
        BigDecimal unitPrice = picked.service.getPrice_wsh();
        if (unitPrice == null) {
            throw new NeedUserInputException("该服务暂未设置价格，请选择其它服务。", "retry_later");
        }
        // 金额一律以后端报价为准：AI 不再用「单价 × 天数」自己乘
        BigDecimal totalPrice = previewNightTotal(userId, picked.service, pet.getId_wsh(), start, nights);

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
        snapshot.setUnit_wsh(BookingUnit.NIGHT);
        snapshot.setStart_date_wsh(start);
        snapshot.setEnd_date_wsh(checkoutDate);
        snapshot.setDays_wsh(nights);
        snapshot.setUnit_price_wsh(unitPrice);
        snapshot.setTotal_price_wsh(totalPrice);
        snapshot.setDistance_wsh(picked.keeper.getDistance_wsh());
        snapshot.setUser_input_wsh(userInput);
        return snapshot;
    }

    /**
     * 调用后端报价接口取按夜寄养的本次预付金额。
     * <p>
     * AI 不再自己乘价：提前送达加收、长住折扣、券与会员都由 quoteOrder 同一套规则算，
     * 保证「AI 说多少 = 下单扣多少」。
     */
    private BigDecimal previewNightTotal(Long userId, ServiceItem service, Long petId,
                                         LocalDate checkinDate, int nights) {
        OrderPreviewRequestDTO preview = new OrderPreviewRequestDTO();
        preview.setPet_id_wsh(petId);
        preview.setService_id_wsh(service.getId_wsh());
        preview.setMerchant_id_wsh(service.getMerchant_id_wsh());
        preview.setBilling_unit_wsh(BookingUnit.NIGHT);
        preview.setStart_date_wsh(checkinDate);
        preview.setEnd_date_wsh(checkinDate.plusDays(nights));
        preview.setQuantity_wsh(nights);
        // 默认 14:00 送达、离店日 14:30 接走（标准锚点 + 免费宽限）
        preview.setDelivery_time_wsh(checkinDate.atTime(OrderPricing.CHECKIN_ANCHOR_TIME));
        preview.setPickup_time_wsh(checkinDate.plusDays(nights)
                .atTime(OrderPricing.CHECKIN_ANCHOR_TIME)
                .plusMinutes(OrderPricing.CHECKOUT_GRACE_MINUTES));
        OrderQuoteDTO quote = orderService.previewOrder(userId, preview);
        BigDecimal total = quote.getFinal_amount_wsh();
        if (total == null) {
            throw new NeedUserInputException("该服务暂未设置价格，请选择其它服务。", "retry_later");
        }
        return total;
    }

    /** 评分排序看护人（算法与重构前一致）。 */
    private void rankKeepers(List<KeeperVO> keepers) {
        double maxRating = maxOf(keepers, k -> dv(k.getRating_wsh()), 5.0);
        double maxDistance = maxOf(keepers, k -> dv(k.getDistance_wsh()), 1.0);
        double maxComplaint = maxOf(keepers, k -> dv(k.getComplaint_rate_wsh()), 1.0);
        double maxCompletion = maxOf(keepers, k -> dv(k.getCompletion_rate_wsh()), 100.0);

        Map<Long, Double> scores = new LinkedHashMap<>();
        for (KeeperVO keeper : keepers) {
            double ratingScore = boundedRatio(dv(keeper.getRating_wsh()), maxRating) * 40;
            double distanceScore = (1 - boundedRatio(dv(keeper.getDistance_wsh()), maxDistance)) * 20;
            // 价格不再参与排序：单价属于服务而非看护者，同商家不同看护者同价
            double complaintScore = (1 - boundedRatio(dv(keeper.getComplaint_rate_wsh()), maxComplaint)) * 10;
            double completionScore = boundedRatio(dv(keeper.getCompletion_rate_wsh()), maxCompletion) * 10;
            double total = ratingScore + distanceScore + complaintScore + completionScore;
            scores.put(keeper.getId_wsh(), Math.round(Math.max(0.0, total) * 100.0) / 100.0);
        }
        keepers.sort((a, b) -> Double.compare(scores.getOrDefault(b.getId_wsh(), 0.0), scores.getOrDefault(a.getId_wsh(), 0.0)));
    }

    /** 依次尝试评分排序后的看护人，返回首个该商家有按夜寄养服务的组合。 */
    private Picked pickKeeperAndService(List<KeeperVO> rankedKeepers, Map<Long, Merchant> merchantMap,
                                        Map<Long, List<ServiceItem>> serviceCache, AgentPromptResult prompt) {
        for (KeeperVO keeper : rankedKeepers) {
            Merchant merchant = merchantMap.get(keeper.getMerchant_id_wsh());
            if (merchant == null) continue;
            ServiceItem service = pickNightService(servicesOf(keeper.getMerchant_id_wsh(), serviceCache), prompt);
            if (service != null) {
                return new Picked(keeper, merchant, service);
            }
        }
        return null;
    }

    /** 获取指定商家的启用服务（含非 night 单元，由 pickNightService 过滤）。 */
    private List<ServiceItem> servicesOf(Long merchantId, Map<Long, List<ServiceItem>> cache) {
        return cache.computeIfAbsent(merchantId, id -> serviceItemMapper.selectList(
                new LambdaQueryWrapper<ServiceItem>()
                        .eq(ServiceItem::getMerchant_id_wsh, id)
                        .eq(ServiceItem::getStatus_wsh, StatusCode.SERVICE_ENABLED.getValue())));
    }

    /** 从启用服务中挑选 night 单元、类型匹配、预算内的最便宜服务；无匹配返回 null。 */
    private ServiceItem pickNightService(List<ServiceItem> services, AgentPromptResult prompt) {
        if (services == null || services.isEmpty()) return null;
        List<ServiceItem> night = services.stream()
                .filter(s -> BookingUnit.NIGHT.equals(BookingUnit.normalize(s.getUnit_wsh())))
                .collect(Collectors.toList());
        if (night.isEmpty()) return null;

        List<ServiceItem> filtered = night;
        if (prompt.getServiceType() != null) {
            String type = prompt.getServiceType().trim().toUpperCase();
            if (type.startsWith(SUPPORTED_SERVICE_TYPE)) {
                filtered = night.stream()
                        .filter(s -> s.getType_wsh() != null && lower(s.getType_wsh()).startsWith("boarding"))
                        .collect(Collectors.toList());
            }
            if (filtered.isEmpty()) return null;
        }
        BigDecimal budget = prompt.getMaxPricePerNight() != null
                ? prompt.getMaxPricePerNight() : prompt.getMaxPricePerDay();
        if (budget != null) {
            BigDecimal limit = budget;
            List<ServiceItem> affordable = filtered.stream()
                    .filter(s -> s.getPrice_wsh() != null && s.getPrice_wsh().compareTo(limit) <= 0)
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

    /**
     * 【解析大模型 JSON 输出】
     *
     * 业务作用：把大模型返回的文本解析为结构化需求对象，容忍 Markdown 代码块包裹。
     *
     * 调用场景：extractRequirement 拿到模型输出后调用。
     *
     * 调用链：extractRequirement → parseJson → objectMapper.readValue。
     *
     * 数据处理：以 ``` 开头时截取首个换行到最后一个围栏之间的内容。
     *
     * 业务规则：解析失败返回 null，由上层判定为无法解析。
     *
     * 状态影响：无。
     *
     * 异常情况：解析异常被捕获并记 warn，返回 null。
     *
     * 注意事项：不做字段级校验，缺字段保持 null 由后续逻辑判断。
     */
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

    /**
     * 【构建需求提取提示词】
     *
     * 业务作用：生成系统提示词，约束大模型只输出约定字段的结构化 JSON。
     *
     * 调用场景：extractRequirement 组装消息列表时调用。
     *
     * 调用链：extractRequirement → buildExtractionPrompt → 注入当前日期。
     *
     * 数据处理：把当前日期填入模板，供模型换算相对时间。
     *
     * 业务规则：字段枚举固定为 BOARDING、GROOMING、TRAINING、WALK、MEDICAL；单位固定为 day、session、hour。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：提示词要求不确定字段输出 null，避免模型编造。
     */
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
                   - start_date：入住日期，格式 yyyy-MM-dd；用户给出具体日期或可推算日期则输出，否则 null。
                   - nights：预订夜数（正整数）；用户说“住 3 天/3 晚”都按 3 夜理解；给出起止日期（如“9月15日到18日”）时按相差天数计算；未提及则为 null。
                   - merchant_keyword：用户偏好的门店名/地名/商圈关键词；没有则为 null。
                   - max_price_per_night：用户提到的每夜价格预算上限（数字）；没有则为 null。
                   - unit：计费单位，只能是 "night"、"session"、"hour"；寄养类固定 "night"。
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
                        "支付已完成，订单已进入待分配池，等待照护师接单。", "paid", false);
            }

            step9SendNotification(order.getOrder_no_wsh());
            consumeOnCommit(planToken, userId);
            return buildConfirmResult(snapshot, order, payment, logs, "pending_payment",
                    "订单已创建，当前为待支付状态。支付后会进入待分配池，等待照护师接单。", "manual_payment", false);
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

    /**
     * 【方案转下单请求】
     *
     * 业务作用：把方案快照转换为订单创建请求，补齐送接地址、时间窗与紧急联系人。
     *
     * 调用场景：confirm 阶段创建订单前调用。
     *
     * 调用链：confirm → buildOrderRequest → orderService.createOrder。
     *
     * 数据处理：送接地址均取商家地址；送达时间固定入住日 14:00，接走时间固定离店日 14:30。
     *
     * 业务规则：联系人优先取实名，其次昵称，最后用户名；备注中的用户输入做 HTML 转义。
     *
     * 状态影响：无（仅构造请求对象）。
     *
     * 异常情况：方案缺少日期会抛空指针。
     *
     * 注意事项：14:00/14:30 是平台固定的入住锚点与免费接走截止，不再使用旧的 10:00/18:00。
     */
    private OrderCreateRequestDTO buildOrderRequest(AgentPlanSnapshot s, User owner) {
        LocalDate start = s.getStart_date_wsh();
        LocalDate checkoutDate = s.getEnd_date_wsh();
        int nights = s.getDays_wsh() == null ? 1 : s.getDays_wsh();
        LocalDateTime deliveryAt = start.atTime(OrderPricing.CHECKIN_ANCHOR_TIME);
        LocalDateTime pickupAt = checkoutDate.atTime(OrderPricing.CHECKIN_ANCHOR_TIME)
                .plusMinutes(OrderPricing.CHECKOUT_GRACE_MINUTES);
        OrderCreateRequestDTO request = new OrderCreateRequestDTO();
        request.setPet_id_wsh(s.getPet_id_wsh());
        request.setMerchant_id_wsh(s.getMerchant_id_wsh());
        request.setService_id_wsh(s.getService_id_wsh());
        request.setBilling_unit_wsh(s.getUnit_wsh());
        request.setExpected_unit_price_wsh(s.getUnit_price_wsh());
        request.setStart_date_wsh(start);
        request.setEnd_date_wsh(checkoutDate);
        request.setQuantity_wsh(nights);
        request.setDelivery_address_wsh(s.getMerchant_address_wsh());
        request.setDelivery_location_source_wsh("merchant");
        request.setPickup_address_wsh(s.getMerchant_address_wsh());
        request.setPickup_location_source_wsh("merchant");
        request.setDelivery_time_wsh(deliveryAt);
        request.setReceiver_available_start_wsh(deliveryAt);
        request.setReceiver_available_end_wsh(deliveryAt.plusMinutes(30));
        request.setPickup_time_wsh(pickupAt);
        request.setEmergency_contact_name_wsh(defaultText(owner.getReal_name_wsh(), defaultText(owner.getNickname_wsh(), owner.getUsername_wsh())));
        request.setEmergency_contact_phone_wsh(owner.getPhone_wsh());
        request.setRemark_wsh("AI Agent order - " + HtmlUtils.htmlEscape(s.getUser_input_wsh()));
        return request;
    }

    /**
     * 【构建确认结果】
     *
     * 业务作用：组装 confirm 阶段的返回体，携带订单号、支付单号与状态。
     *
     * 调用场景：confirm 成功路径调用。
     *
     * 调用链：confirm → buildConfirmResult。
     *
     * 数据处理：订单非空时写入订单号；支付单非空时写入支付单号，并按状态映射 paid 或 pending。
     *
     * 业务规则：仅 status 为 success 时支付状态记为 paid。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：日志列表直接复用调用方传入的同一实例。
     */
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

    /**
     * 【校验自动支付授权】
     *
     * 业务作用：自动支付前校验支付密码，密码缺失或错误时中断为待用户补充。
     *
     * 调用场景：confirm 中 autoPay 为 true 时调用。
     *
     * 调用链：confirm → verifyAutoPayAuthorization → userService.verifyPaymentPassword。
     *
     * 数据处理：密码为空直接中断；业务异常按错误码转换。
     *
     * 业务规则：仅 400 与 403 转为需用户输入，其余异常继续上抛。
     *
     * 状态影响：无。
     *
     * 异常情况：密码缺失或错误 → NeedUserInputException(ask_payment_password)。
     *
     * 注意事项：转换后不会泄露具体校验失败原因之外的信息。
     */
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

    /**
     * 【发送订单创建通知】
     *
     * 业务作用：订单创建成功后发送 MQ 通知，触发后续异步流程。
     *
     * 调用场景：confirm 创建订单并落库后调用。
     *
     * 调用链：confirm → step9SendNotification → messageSender.sendOrderCreate。
     *
     * 数据处理：仅传递订单号。
     *
     * 业务规则：通知失败不影响下单结果。
     *
     * 状态影响：投递一条订单创建消息。
     *
     * 异常情况：发送异常被捕获并记 warn，不抛出。
     *
     * 注意事项：因此 MQ 故障时下游可能收不到通知，需要补偿机制兜底。
     */
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

    /**
     * 【解析开始日期】
     *
     * 业务作用：把模型输出的日期字符串解析为 LocalDate。
     *
     * 调用场景：buildPlan 处理日期时调用。
     *
     * 调用链：buildPlan → parseStartDate → LocalDate.parse。
     *
     * 数据处理：空白直接返回 null，格式非法同样返回 null。
     *
     * 业务规则：只接受 ISO 格式 yyyy-MM-dd。
     *
     * 状态影响：无。
     *
     * 异常情况：解析异常被吞掉，返回 null。
     *
     * 注意事项：返回 null 时上层默认取明天。
     */
    private LocalDate parseStartDate(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 【回填方案字段】
     *
     * 业务作用：把方案快照中的展示字段批量复制到 plan 返回体。
     *
     * 调用场景：plan 成功路径调用。
     *
     * 调用链：plan → fillPlanFields。
     *
     * 数据处理：逐字段拷贝宠物、商家、看护者、服务、单位、日期、天数与价格。
     *
     * 业务规则：不包含方案 token，token 由 plan 单独设置。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：新增快照字段时需同步在此回填，否则前端拿不到。
     */
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

    /**
     * 【取归一化基准最大值】
     *
     * 业务作用：计算列表中某个指标的最大值，作为评分归一化的分母。
     *
     * 调用场景：rankKeepers 计算各项基准时调用。
     *
     * 调用链：rankKeepers → maxOf。
     *
     * 数据处理：最大值非有限或小于等于 0 时返回兜底值。
     *
     * 业务规则：兜底值由调用方按指标语义指定，如评分 5.0、完成率 100.0。
     *
     * 状态影响：无。
     *
     * 异常情况：空列表时返回兜底值。
     *
     * 注意事项：兜底可避免除零导致评分异常。
     */
    private double maxOf(List<KeeperVO> rows, java.util.function.ToDoubleFunction<KeeperVO> fn, double fallback) {
        double max = rows.stream().mapToDouble(fn).max().orElse(0.0);
        return (!Double.isFinite(max) || max <= 0.0) ? fallback : max;
    }

    /**
     * 【计算有界比值】
     *
     * 业务作用：计算 value 与 denominator 的比值并截断到 0 到 1 之间。
     *
     * 调用场景：rankKeepers 计算各维度得分时调用。
     *
     * 调用链：rankKeepers → boundedRatio。
     *
     * 数据处理：非有限值或分母不大于 0 时返回 0。
     *
     * 业务规则：结果恒在 0 到 1 之间。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：无。
     */
    private double boundedRatio(double value, double denominator) {
        if (!Double.isFinite(value) || !Double.isFinite(denominator) || denominator <= 0.0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value / denominator));
    }

    /**
     * 【数值安全取值】
     *
     * 业务作用：把可能为 null 的 Number 转为 double，null 按 0 处理。
     *
     * 调用场景：评分排序与指标计算时读取 VO 中的数值字段。
     *
     * 调用链：rankKeepers → dv。
     *
     * 数据处理：null → 0.0，其余取 doubleValue。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：null 与真实 0 无法区分，评分时按最差值处理。
     */
    private double dv(Number value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    /**
     * 【文本小写归一】
     *
     * 业务作用：把文本统一转小写用于忽略大小写的包含匹配。
     *
     * 调用场景：商家关键词过滤、宠物名与品种匹配时调用。
     *
     * 调用链：buildPlan / matchPet / pickDayService → lower。
     *
     * 数据处理：null 转空串，其余转小写。
     *
     * 业务规则：使用默认 Locale，未指定 ROOT。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：土耳其语等特殊 Locale 下大小写转换可能异常，服务端 Locale 固定为中文时无影响。
     */
    private String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    /**
     * 【文本兜底取值】
     *
     * 业务作用：文本非空白时返回 trim 结果，否则返回兜底值。
     *
     * 调用场景：buildOrderRequest 拼接紧急联系人姓名时调用。
     *
     * 调用链：buildOrderRequest → defaultText。
     *
     * 数据处理：使用 StringUtils.hasText 判断。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：可嵌套调用实现多级兜底。
     */
    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    /**
     * 【异常消息脱敏】
     *
     * 业务作用：把异常转换为可直接展示给用户的提示文本。
     *
     * 调用场景：plan 与 confirm 的兜底异常分支调用。
     *
     * 调用链：plan / confirm → safeMessage。
     *
     * 数据处理：数据库访问异常统一返回系统内部错误，其余取异常消息。
     *
     * 业务规则：消息为空或空白时返回通用失败提示。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：避免把 SQL 或堆栈信息暴露给前端。
     */
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

        /**
         * 【看护者与服务的组合】
         *
         * 业务作用：承载一次挑选结果，包含看护者、所属商家与匹配到的服务。
         *
         * 调用场景：pickKeeperAndService 命中后构造并返回。
         *
         * 调用链：pickKeeperAndService → new Picked → buildPlan。
         *
         * 数据处理：三个字段均为 final，构造后不可变。
         *
         * 业务规则：无。
         *
         * 状态影响：无。
         *
         * 异常情况：无。
         *
         * 注意事项：内部静态类，仅在本类内使用。
         */
        private Picked(KeeperVO keeper, Merchant merchant, ServiceItem service) {
            this.keeper = keeper;
            this.merchant = merchant;
            this.service = service;
        }
    }

    /** 缺信息中断流程：返回 needs_user_input 状态并提示用户补充。 */
    private static class NeedUserInputException extends BusinessException {
        private final String nextAction;

        /**
         * 【需用户补充信息的异常】
         *
         * 业务作用：表示流程因信息不足中断，需要用户补充后再继续。
         *
         * 调用场景：buildPlan、confirm 中缺少宠物、日期、位置、密码等时抛出。
         *
         * 调用链：buildPlan / confirm → new NeedUserInputException(message, nextAction)。
         *
         * 数据处理：业务码固定 400，nextAction 单独保存供前端引导。
         *
         * 业务规则：异常本身不作为失败处理，而是转换为 needs_user_input 状态。
         *
         * 状态影响：无。
         *
         * 异常情况：无。
         *
         * 注意事项：继承 BusinessException，若未被捕获会被全局处理器当成 400 返回。
         */
        private NeedUserInputException(String message, String nextAction) {
            super(400, message);
            this.nextAction = nextAction;
        }

        /**
         * 【获取下一步动作】
         *
         * 业务作用：返回前端应引导用户执行的动作标识。
         *
         * 调用场景：plan 与 confirm 捕获该异常后读取。
         *
         * 调用链：plan / confirm → getNextAction。
         *
         * 数据处理：直接返回 nextAction 字段。
         *
         * 业务规则：取值如 ask_days、ask_location、complete_profile、ask_payment_password。
         *
         * 状态影响：无。
         *
         * 异常情况：无。
         *
         * 注意事项：新增动作标识需与前端约定一致。
         */
        private String getNextAction() {
            return nextAction;
        }
    }
}