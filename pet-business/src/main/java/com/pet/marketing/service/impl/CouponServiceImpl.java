package com.pet.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.common.BusinessException;
import com.pet.marketing.dto.CouponDiscountResult;
import com.pet.marketing.dto.CouponGrantRequestDTO;
import com.pet.marketing.dto.CouponQuoteDTO;
import com.pet.marketing.dto.CouponQuoteRequestDTO;
import com.pet.marketing.dto.CouponTemplateCreateRequestDTO;
import com.pet.marketing.dto.CouponTemplateDTO;
import com.pet.marketing.dto.UserCouponDTO;
import com.pet.marketing.entity.CouponTemplate;
import com.pet.marketing.entity.CouponUsage;
import com.pet.marketing.entity.UserCoupon;
import com.pet.marketing.mapper.CouponTemplateMapper;
import com.pet.marketing.mapper.CouponUsageMapper;
import com.pet.marketing.mapper.UserCouponMapper;
import com.pet.marketing.service.CouponService;
import com.pet.order.mapper.OrderMapper;
import com.pet.pet.mapper.PetMapper;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 优惠券服务实现，提供优惠券模板 CRUD、批量发放、用户领券、
 * 订单优惠预览及优惠券锁定/使用/释放等全生命周期管理。
 * <p>优惠券库存通过原子操作保障不超发；优惠券锁定/释放与订单状态联动；
 * 条件发放支持按注册时间、消费金额、订单数、宠物数等多维度筛选用户。</p>
 */
@Service
public class CouponServiceImpl implements CouponService {
    /** 模板状态：启用 */
    private static final int STATUS_ENABLED = 1;
    /** 模板状态：禁用 */
    private static final int STATUS_DISABLED = 0;
    /** 优惠类型：固定金额减免 */
    private static final String TYPE_AMOUNT = "amount";
    /** 优惠类型：折扣率减免 */
    private static final String TYPE_PERCENT = "percent";
    /** 用户优惠券状态：可用 */
    private static final String STATUS_AVAILABLE = "available";
    /** 用户优惠券状态：已锁定（下单占用） */
    private static final String STATUS_LOCKED = "locked";
    /** 用户优惠券状态：已使用 */
    private static final String STATUS_USED = "used";
    /** 优惠券来源：用户自行领取 */
    private static final String SOURCE_CLAIM = "claim";
    /** 优惠券来源：管理员发放 */
    private static final String SOURCE_ADMIN = "admin";
    /** 优惠金额承担方：平台 */
    private static final String FUNDING_PLATFORM = "platform";

    private final CouponTemplateMapper templateMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponUsageMapper usageMapper;
    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final PetMapper petMapper;
    private final ObjectMapper objectMapper;

    /**
     * 【构造函数：注入优惠券相关依赖】
     *
     * 业务作用：由 Spring 注入模板、用户券、核销记录、用户、订单、宠物 Mapper 与 JSON 序列化器。
     *
     * 调用场景：容器启动创建 CouponServiceImpl Bean 时自动调用。
     *
     * 调用链：Spring IoC → CouponServiceImpl(构造) → 各 Mapper 字段赋值。
     *
     * 数据处理：仅做字段赋值，无数据读写。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：全部为 final 字段构造器注入，本类不提供 setter。
     */
    public CouponServiceImpl(CouponTemplateMapper templateMapper,
                             UserCouponMapper userCouponMapper,
                             CouponUsageMapper usageMapper,
                             UserMapper userMapper,
                             OrderMapper orderMapper,
                             PetMapper petMapper,
                             ObjectMapper objectMapper) {
        this.templateMapper = templateMapper;
        this.userCouponMapper = userCouponMapper;
        this.usageMapper = usageMapper;
        this.userMapper = userMapper;
        this.orderMapper = orderMapper;
        this.petMapper = petMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 【查询优惠券模板列表】
     *
     * 业务作用：按需返回启用且在有效期内的模板，或返回全部模板。
     *
     * 调用场景：管理端模板管理页传 activeOnly=false；用户端可领券列表传 true。
     *
     * 调用链：CouponController → CouponServiceImpl.listTemplates → CouponTemplateMapper.selectList → toTemplateDTO。
     *
     * 数据处理：查询 coupon_template_wsh；activeOnly 为 true 时附加 status_wsh=1、valid_from_wsh<=now、valid_to_wsh>=now 三个条件，统一按 created_at_wsh 倒序。
     *
     * 业务规则：activeOnly=false 时不过滤状态与有效期，禁用和过期模板也会返回。
     *
     * 状态影响：只读，不写库。
     *
     * 异常情况：无显式异常，数据库异常由框架抛出。
     *
     * 注意事项：activeOnly 同时作为三个查询条件的开关参数，传 false 时这三条 SQL 条件都不生效。
     */
    @Override
    public List<CouponTemplateDTO> listTemplates(boolean activeOnly) {
        LocalDateTime now = LocalDateTime.now();
        // activeOnly 同时控制状态与有效期三个条件
        List<CouponTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<CouponTemplate>()
                        .eq(activeOnly, CouponTemplate::getStatus_wsh, STATUS_ENABLED)
                        .le(activeOnly, CouponTemplate::getValid_from_wsh, now)
                        .ge(activeOnly, CouponTemplate::getValid_to_wsh, now)
                        .orderByDesc(CouponTemplate::getCreated_at_wsh));
        return templates.stream().map(this::toTemplateDTO).collect(Collectors.toList());
    }

    /**
     * 【创建优惠券模板】
     *
     * 业务作用：校验并落库一张新的优惠券模板。
     *
     * 调用场景：管理端新增优惠券活动。
     *
     * 调用链：CouponController → CouponServiceImpl.createTemplate → normalizeType/validateTemplateRequest → CouponTemplateMapper.insert。
     *
     * 数据处理：请求字段经 normalizeType、moneyOrZero、positiveMoney、normalizeRate、nonNegativeInteger、positiveIntegerOrDefault、normalizeStatus、normalizeScope 归一后写入 coupon_template_wsh；issued_quantity_wsh 初始化为 0，created_by_wsh 记录管理员 ID。
     *
     * 业务规则：amount 类型必须填正的 discount_amount_wsh；percent 类型必须填 (0,1) 区间的 discount_rate_wsh；有效期起止不能为空且止必须晚于起；merchant 范围必须带 merchant_id_wsh，platform 范围则强制把 merchant_id_wsh 置空。
     *
     * 状态影响：向 coupon_template_wsh 插入一行。
     *
     * 异常情况：request 为空、有效期非法、类型不支持、金额或折扣率非法、商家券缺商家 ID 时抛 BusinessException(400)。
     *
     * 注意事项：非当前类型的金额字段被显式写成 0 或 null，避免残留脏值。
     */
    @Transactional
    @Override
    public CouponTemplate createTemplate(Long adminId, CouponTemplateCreateRequestDTO request) {
        if (request == null) {
            throw new BusinessException(400, "Coupon request cannot be empty");
        }
        String type = normalizeType(request.getType_wsh());
        validateTemplateRequest(request, type);

        CouponTemplate template = new CouponTemplate();
        template.setName_wsh(request.getName_wsh().trim());
        template.setType_wsh(type);
        template.setThreshold_amount_wsh(moneyOrZero(request.getThreshold_amount_wsh()));
        // 非当前类型的金额字段置 0 或 null，避免残留脏值
        template.setDiscount_amount_wsh(TYPE_AMOUNT.equals(type) ? positiveMoney(request.getDiscount_amount_wsh()) : BigDecimal.ZERO);
        template.setDiscount_rate_wsh(TYPE_PERCENT.equals(type) ? normalizeRate(request.getDiscount_rate_wsh()) : null);
        template.setMax_discount_amount_wsh(moneyOrNull(request.getMax_discount_amount_wsh()));
        template.setTotal_quantity_wsh(nonNegativeInteger(request.getTotal_quantity_wsh()));
        template.setIssued_quantity_wsh(0);
        template.setPer_user_limit_wsh(positiveIntegerOrDefault(request.getPer_user_limit_wsh(), 1));
        template.setValid_from_wsh(request.getValid_from_wsh());
        template.setValid_to_wsh(request.getValid_to_wsh());
        template.setStatus_wsh(request.getStatus_wsh() == null ? STATUS_ENABLED : normalizeStatus(request.getStatus_wsh()));
        template.setScope_type_wsh(normalizeScope(request.getScope_type_wsh()));
        // 平台券强制清空商家 ID
        template.setMerchant_id_wsh(template.getScope_type_wsh().equals("merchant") ? request.getMerchant_id_wsh() : null);
        template.setCreated_by_wsh(adminId);
        template.setRemark_wsh(request.getRemark_wsh());
        templateMapper.insert(template);
        return template;
    }

    /**
     * 【启用或停用优惠券模板】
     *
     * 业务作用：把指定模板的状态改为启用(1)或禁用(0)。
     *
     * 调用场景：管理端上下架优惠券活动。
     *
     * 调用链：CouponController → CouponServiceImpl.updateTemplateStatus → requireTemplate → CouponTemplateMapper.updateById。
     *
     * 数据处理：读出模板实体，覆盖 status_wsh 后整行更新。
     *
     * 业务规则：状态只允许 0 或 1，由 normalizeStatus 校验。
     *
     * 状态影响：更新 coupon_template_wsh 的 status_wsh。
     *
     * 异常情况：templateId 为空抛 400，模板不存在抛 404，状态值非法抛 400。
     *
     * 注意事项：停用后已领出的用户券在试算与可用列表阶段会被 requireUsableTemplate / isTemplateAvailable 拦截，无法继续使用。
     */
    @Transactional
    @Override
    public CouponTemplate updateTemplateStatus(Long templateId, Integer status) {
        CouponTemplate template = requireTemplate(templateId);
        template.setStatus_wsh(normalizeStatus(status));
        templateMapper.updateById(template);
        return template;
    }

    /**
     * 【向单个用户发放优惠券】
     *
     * 业务作用：给指定用户发放指定数量的券，返回实际成功发放张数。
     *
     * 调用场景：管理端定向发券。
     *
     * 调用链：CouponController → CouponServiceImpl.grantToUser → UserMapper.selectById → requestQuantity → issueOne（循环）。
     *
     * 数据处理：先校验 user_id_wsh 非空且用户存在，数量取 quantity_wsh（缺省 1），逐张调用 issueOne 写入 user_coupon_wsh。
     *
     * 业务规则：每人限领由模板 per_user_limit_wsh 控制，超出时 issueOne 返回 false 而不抛异常。
     *
     * 状态影响：写 user_coupon_wsh，并在 issueOne 中累加 coupon_template_wsh.issued_quantity_wsh。
     *
     * 异常情况：user_id_wsh 为空抛 400，用户不存在抛 404，库存不足抛 400（来自 issueOne）。
     *
     * 注意事项：返回值可能小于请求数量，调用方不应假定全部发放成功。
     */
    @Transactional
    @Override
    public int grantToUser(Long adminId, Long templateId, CouponGrantRequestDTO request) {
        Long userId = request != null ? request.getUser_id_wsh() : null;
        if (userId == null) {
            throw new BusinessException(400, "User id cannot be empty");
        }
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException(404, "User does not exist");
        }
        int quantity = requestQuantity(request);
        int issued = 0;
        // 逐张发放，超出限领的会被跳过
        for (int i = 0; i < quantity; i++) {
            if (issueOne(templateId, userId, SOURCE_ADMIN)) {
                issued++;
            }
        }
        return issued;
    }

    /**
     * 【向全部正常用户发放优惠券】
     *
     * 业务作用：对 status_wsh=1 的所有用户各发放指定数量的券。
     *
     * 调用场景：管理端全量发券活动。
     *
     * 调用链：CouponController → CouponServiceImpl.grantToAllUsers → UserMapper.selectList → issueOne（双层循环）。
     *
     * 数据处理：一次查出全部正常状态用户，逐个用户按数量循环调用 issueOne。
     *
     * 业务规则：只发给 status_wsh=1 的用户；每人限领仍受模板 per_user_limit_wsh 限制。
     *
     * 状态影响：批量写 user_coupon_wsh 并累加模板发放数。
     *
     * 异常情况：数量非法抛 400（requestQuantity → positiveIntegerOrDefault），库存不足抛 400。
     *
     * 注意事项：全量用户一次性载入内存并逐条插入，用户量大时耗时较长，且整体处于同一事务。
     */
    @Transactional
    @Override
    public int grantToAllUsers(Long adminId, Long templateId, CouponGrantRequestDTO request) {
        int quantity = requestQuantity(request);
        // 只发给状态正常的用户
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getStatus_wsh, 1));
        int issued = 0;
        for (User user : users) {
            for (int i = 0; i < quantity; i++) {
                if (issueOne(templateId, user.getId_wsh(), SOURCE_ADMIN)) {
                    issued++;
                }
            }
        }
        return issued;
    }

    /**
     * 【按条件筛选用户发放优惠券】
     *
     * 业务作用：按注册时间、累计消费、订单数、宠物数、最近下单时间等条件筛选用户后发券。
     *
     * 调用场景：管理端精细化营销发券。
     *
     * 调用链：CouponController → CouponServiceImpl.grantByCondition → hasNegativeCondition → loadOrderStats/loadPetCounts → toIdSet → hasGrantCondition/matchesAnyCondition → issueOne。
     *
     * 数据处理：预加载全部用户的订单统计与宠物数量，先剔除 exclude_user_ids_wsh，再判断是否命中任一条件（或命中 include_user_ids_wsh 白名单），命中者按数量循环 issueOne。
     *
     * 业务规则：条件之间是「或」关系，命中任一即发放；include_user_ids_wsh 视为无条件命中；exclude_user_ids_wsh 优先级最高直接跳过；无任何条件时（hasCondition=false）对全部正常用户发放。
     *
     * 状态影响：写 user_coupon_wsh 并累加模板发放数。
     *
     * 异常情况：min_total_spend_wsh、min_order_count_wsh、min_pet_count_wsh 为负数时直接返回 0 且不发放；数量非法抛 400；库存不足抛 400。
     *
     * 注意事项：负数条件不做报错而是静默返回 0，调用方需自行判断；订单统计与宠物数量是全表聚合，数据量大时查询较重。
     */
    @Transactional
    @Override
    public int grantByCondition(Long adminId, Long templateId, CouponGrantRequestDTO request) {
        int quantity = requestQuantity(request);
        // 条件为负数视为非法请求，静默返回 0
        if (hasNegativeCondition(request)) {
            return 0;
        }

        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getStatus_wsh, 1));
        if (users == null || users.isEmpty()) {
            return 0;
        }

        Map<Long, CouponGrantOrderStats> orderStats = loadOrderStats();
        Map<Long, Long> petCounts = loadPetCounts();
        Set<Long> includeIds = toIdSet(request != null ? request.getInclude_user_ids_wsh() : null);
        Set<Long> excludeIds = toIdSet(request != null ? request.getExclude_user_ids_wsh() : null);
        boolean hasCondition = hasGrantCondition(request, includeIds);

        int issued = 0;
        for (User user : users) {
            Long userId = user.getId_wsh();
            // 排除名单优先级最高，直接跳过
            if (userId == null || excludeIds.contains(userId)) {
                continue;
            }
            // 有筛选条件时只发给命中的用户
            if (hasCondition && !matchesAnyCondition(user, request, includeIds, orderStats.get(userId), petCounts.getOrDefault(userId, 0L))) {
                continue;
            }
            for (int i = 0; i < quantity; i++) {
                if (issueOne(templateId, userId, SOURCE_ADMIN)) {
                    issued++;
                }
            }
        }
        return issued;
    }

    /**
     * 【用户主动领取优惠券】
     *
     * 业务作用：用户自助领取一张优惠券，成功后返回最新生成的用户券记录。
     *
     * 调用场景：用户端领券中心点击领取。
     *
     * 调用链：CouponController → CouponServiceImpl.claim → issueOne → UserCouponMapper.selectOne。
     *
     * 数据处理：issueOne 校验限领与库存并插入 user_coupon_wsh，随后按 user_id_wsh + template_id_wsh 倒序取最新一条返回。
     *
     * 业务规则：每人限领受模板 per_user_limit_wsh 约束，超限即视为领取失败。
     *
     * 状态影响：写 user_coupon_wsh，累加 coupon_template_wsh.issued_quantity_wsh。
     *
     * 异常情况：超限或库存不足时 issueOne 返回 false，此处抛 BusinessException(400, "Coupon claim limit reached")；模板不存在或不可用由 issueOne 内部抛 404/400。
     *
     * 注意事项：异常文案统一为「领取次数已达上限」，但库存不足时实际是 issueOne 抛出的「库存不足」，两者文案并不一致。
     */
    @Transactional
    @Override
    public UserCoupon claim(Long userId, Long templateId) {
        // 限领超限或库存不足都按领取失败处理
        if (!issueOne(templateId, userId, SOURCE_CLAIM)) {
            throw new BusinessException(400, "Coupon claim limit reached");
        }
        return userCouponMapper.selectOne(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUser_id_wsh, userId)
                        .eq(UserCoupon::getTemplate_id_wsh, templateId)
                        .orderByDesc(UserCoupon::getCreated_at_wsh)
                        .last("LIMIT 1"));
    }

    /**
     * 【查询我的全部优惠券】
     *
     * 业务作用：返回用户名下所有用户券（含可用、锁定、已使用、已过期），并补充模板信息。
     *
     * 调用场景：用户端「我的优惠券」页。
     *
     * 调用链：CouponController → CouponServiceImpl.listMyCoupons → UserCouponMapper.selectList → enrich → toUserCouponDTO。
     *
     * 数据处理：按 user_id_wsh 查询 user_coupon_wsh，按 created_at_wsh 倒序，再批量补模板字段。
     *
     * 业务规则：不做状态与有效期过滤。
     *
     * 状态影响：只读。
     *
     * 异常情况：无。
     *
     * 注意事项：需要可用券列表时应调用 listAvailableCoupons。
     */
    @Override
    public List<UserCouponDTO> listMyCoupons(Long userId) {
        List<UserCoupon> coupons = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUser_id_wsh, userId)
                        .orderByDesc(UserCoupon::getCreated_at_wsh));
        return enrich(coupons);
    }

    /**
     * 【查询当前可用的优惠券】
     *
     * 业务作用：在「我的优惠券」基础上按状态、有效期、模板可用性、适用范围、门槛金额过滤出可用于当前订单的券。
     *
     * 调用场景：用户下单页展示可用券列表。
     *
     * 调用链：CouponController → CouponServiceImpl.listAvailableCoupons → listMyCoupons → isTemplateAvailable → isScopeMatched。
     *
     * 数据处理：复用 listMyCoupons 的结果，链式过滤：status_wsh=available、未过期、模板启用且在有效期内、范围匹配、订单金额不小于 threshold_amount_wsh。
     *
     * 业务规则：expire_at_wsh 为空视为不过期；platform 范围券对所有商家可用，merchant 范围券要求 merchant_id_wsh 一致；门槛比较时订单金额为空按 0 处理。
     *
     * 状态影响：只读。
     *
     * 异常情况：无。
     *
     * 注意事项：只做筛选不做金额试算，实际抵扣需调用 quote/previewForOrder；每张券都会触发一次模板查询（isTemplateAvailable），券多时存在 N+1 查询。
     */
    @Override
    public List<UserCouponDTO> listAvailableCoupons(Long userId, Long merchantId, BigDecimal orderAmount) {
        LocalDateTime now = LocalDateTime.now();
        BigDecimal amount = moneyOrZero(orderAmount);
        // 依次过滤状态、有效期、模板可用、范围与门槛
        return listMyCoupons(userId).stream()
                .filter(c -> STATUS_AVAILABLE.equals(c.getStatus_wsh()))
                .filter(c -> c.getExpire_at_wsh() == null || !c.getExpire_at_wsh().isBefore(now))
                .filter(c -> c.getTemplate_id_wsh() != null && isTemplateAvailable(c.getTemplate_id_wsh(), now))
                .filter(c -> isScopeMatched(c.getScope_type_wsh(), c.getMerchant_id_wsh(), merchantId))
                .filter(c -> amount.compareTo(moneyOrZero(c.getThreshold_amount_wsh())) >= 0)
                .collect(Collectors.toList());
    }

    /**
     * 【优惠券试算（对外报价）】
     *
     * 业务作用：把内部试算结果 CouponDiscountResult 转换成前端展示用的 CouponQuoteDTO。
     *
     * 调用场景：用户下单页选择优惠券时实时试算。
     *
     * 调用链：CouponController → CouponServiceImpl.quote → previewForOrder → 组装 CouponQuoteDTO。
     *
     * 数据处理：透传 previewForOrder 的券 ID、模板 ID、券名、券抵扣、平台补贴、结算金额、最终金额；原始总额与长住优惠为空时补 0；funding_party_wsh 固定为 platform。
     *
     * 业务规则：优惠成本由平台承担，故 funding_party_wsh 恒为 platform。
     *
     * 状态影响：只读，不锁券。
     *
     * 异常情况：request 为空抛 400；券不可用、已过期、商家不匹配、未达门槛由 previewForOrder 抛 400；券不属于当前用户抛 404。
     *
     * 注意事项：试算不占用券，真正下单仍需调用 lockForOrder。
     */
    @Override
    public CouponQuoteDTO quote(Long userId, CouponQuoteRequestDTO request) {
        if (request == null) {
            throw new BusinessException(400, "Quote request cannot be empty");
        }
        CouponDiscountResult result = previewForOrder(
                userId,
                request.getUser_coupon_id_wsh(),
                request.getTotal_amount_wsh(),
                request.getLong_stay_discount_wsh(),
                request.getMerchant_id_wsh(),
                request.getService_id_wsh());
        CouponQuoteDTO dto = new CouponQuoteDTO();
        dto.setUser_coupon_id_wsh(result.getUser_coupon_id_wsh());
        dto.setTemplate_id_wsh(result.getTemplate_id_wsh());
        dto.setCoupon_name_wsh(result.getCoupon_name_wsh());
        dto.setTotal_amount_wsh(moneyOrZero(request.getTotal_amount_wsh()));
        dto.setLong_stay_discount_wsh(moneyOrZero(request.getLong_stay_discount_wsh()));
        dto.setCoupon_discount_wsh(result.getCoupon_discount_wsh());
        dto.setPlatform_subsidy_wsh(result.getPlatform_subsidy_wsh());
        dto.setSettlement_amount_wsh(result.getSettlement_amount_wsh());
        dto.setFinal_amount_wsh(result.getFinal_amount_wsh());
        dto.setFunding_party_wsh(FUNDING_PLATFORM);
        return dto;
    }

    /**
     * 【下单前优惠试算】
     *
     * 业务作用：计算长住优惠后的结算金额，再叠加所选优惠券的抵扣，返回完整试算结果。
     *
     * 调用场景：下单页试算、quote 内部调用。
     *
     * 调用链：CouponService.previewForOrder → requireUserCoupon → requireUsableTemplate → isScopeMatched → calculateCouponDiscount → toSnapshot。
     *
     * 数据处理：结算金额 = 总额 - min(长住优惠, 总额)，保留 2 位；券抵扣由 calculateCouponDiscount 计算；最终金额 = 结算金额 - 券抵扣，最低为 0；快照由 toSnapshot 序列化为 JSON。
     *
     * 业务规则：userCouponId 为空时直接返回无券结果；券必须属于当前用户、状态 available、未过期、范围匹配、结算金额达到门槛。
     *
     * 状态影响：只读，不修改 user_coupon_wsh。
     *
     * 异常情况：券不存在或不属于当前用户抛 404；券状态非 available、已过期、商家不匹配、未达门槛抛 400；券类型不支持抛 400。
     *
     * 注意事项：长住优惠超过总额时会被截断为总额；快照序列化失败时静默返回 "{}"。
     */
    @Override
    public CouponDiscountResult previewForOrder(Long userId,
                                                Long userCouponId,
                                                BigDecimal totalAmount,
                                                BigDecimal longStayDiscount,
                                                Long merchantId,
                                                Long serviceId) {
        BigDecimal total = moneyOrZero(totalAmount);
        BigDecimal baseDiscount = moneyOrZero(longStayDiscount);
        // 长住优惠最多抵到订单总额为止
        if (baseDiscount.compareTo(total) > 0) {
            baseDiscount = total;
        }
        BigDecimal settlementAmount = total.subtract(baseDiscount).setScale(2, RoundingMode.HALF_UP);
        CouponDiscountResult result = emptyResult(settlementAmount);
        if (userCouponId == null) {
            return result;
        }

        UserCoupon userCoupon = requireUserCoupon(userCouponId, userId);
        CouponTemplate template = requireUsableTemplate(userCoupon.getTemplate_id_wsh());
        if (!STATUS_AVAILABLE.equals(userCoupon.getStatus_wsh())) {
            throw new BusinessException(400, "Coupon is not available");
        }
        if (userCoupon.getExpire_at_wsh() != null && userCoupon.getExpire_at_wsh().isBefore(LocalDateTime.now())) {
            throw new BusinessException(400, "Coupon has expired");
        }
        if (!isScopeMatched(template.getScope_type_wsh(), template.getMerchant_id_wsh(), merchantId)) {
            throw new BusinessException(400, "Coupon cannot be used for this merchant");
        }
        if (settlementAmount.compareTo(moneyOrZero(template.getThreshold_amount_wsh())) < 0) {
            throw new BusinessException(400, "Order amount does not meet coupon threshold");
        }

        BigDecimal couponDiscount = calculateCouponDiscount(template, settlementAmount);
        BigDecimal finalAmount = settlementAmount.subtract(couponDiscount);
        // 抵扣后金额兜底为 0，不出现负数
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }
        result.setUser_coupon_id_wsh(userCoupon.getId_wsh());
        result.setTemplate_id_wsh(template.getId_wsh());
        result.setCoupon_name_wsh(template.getName_wsh());
        result.setCoupon_discount_wsh(couponDiscount);
        result.setPlatform_subsidy_wsh(couponDiscount);
        result.setSettlement_amount_wsh(settlementAmount);
        result.setFinal_amount_wsh(finalAmount.setScale(2, RoundingMode.HALF_UP));
        result.setSnapshot_wsh(toSnapshot(template, couponDiscount, settlementAmount, result.getFinal_amount_wsh()));
        return result;
    }

    /**
     * 【下单锁定优惠券】
     *
     * 业务作用：把用户券从 available 原子地改为 locked，并绑定订单。
     *
     * 调用场景：创建订单时占用优惠券。
     *
     * 调用链：OrderService → CouponService.lockForOrder → UserCouponMapper.lockForOrder。
     *
     * 数据处理：userCouponId 为空直接返回；否则带 user_id_wsh 与状态条件执行原子 UPDATE，写入 order_id_wsh、order_no_wsh、discount_amount_wsh。
     *
     * 业务规则：抵扣金额必须大于 0；只有属于该用户且仍为可用状态的券才能锁定。
     *
     * 状态影响：更新 user_coupon_wsh 状态为 locked 并绑定订单。
     *
     * 异常情况：抵扣金额不大于 0 抛 400；更新行数为 0（券已被用或状态变化）抛 400。
     *
     * 注意事项：依赖 Mapper 的原子 UPDATE 做并发控制，不是先查后改；未传券时不报错，便于无券下单。
     */
    @Transactional
    @Override
    public void lockForOrder(Long userId, Long userCouponId, Long orderId, String orderNo, BigDecimal discountAmount) {
        if (userCouponId == null) {
            return;
        }
        BigDecimal discount = moneyOrZero(discountAmount);
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "Coupon discount must be greater than zero");
        }
        // 原子更新锁定，避免并发重复占用同一张券
        int updated = userCouponMapper.lockForOrder(userCouponId, userId, orderId, orderNo, discount);
        if (updated == 0) {
            throw new BusinessException(400, "Coupon is no longer available");
        }
    }

    /**
     * 【订单支付成功后将优惠券标记为已使用】
     *
     * 业务作用：把订单锁定的券改为 used，并写入一条核销记录。
     *
     * 调用场景：订单支付成功后由支付链路调用。
     *
     * 调用链：PaymentServiceImpl.pay → CouponService.markUsedForOrder → UserCouponMapper.markUsedByOrderId → CouponUsageMapper.insert。
     *
     * 数据处理：先按 order_id_wsh + status_wsh=locked 找到券，再原子更新为 used，最后向 coupon_usage_wsh 插入核销明细（金额、承担方 platform、状态 used）。
     *
     * 业务规则：订单没有锁定中的券时直接返回，不报错。
     *
     * 状态影响：更新 user_coupon_wsh 状态，新增 coupon_usage_wsh 记录。
     *
     * 异常情况：更新行数为 0（状态被并发修改）抛 400；核销记录唯一键冲突被捕获后忽略。
     *
     * 注意事项：核销记录插入用 DuplicateKeyException 兜底实现幂等，重复支付回调不会产生第二条记录。
     */
    @Transactional
    @Override
    public void markUsedForOrder(Long orderId, String orderNo) {
        UserCoupon coupon = userCouponMapper.selectOne(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getOrder_id_wsh, orderId)
                        .eq(UserCoupon::getStatus_wsh, STATUS_LOCKED)
                        .last("LIMIT 1"));
        if (coupon == null) {
            return;
        }
        int updated = userCouponMapper.markUsedByOrderId(orderId);
        if (updated == 0) {
            throw new BusinessException(400, "Coupon status changed");
        }
        CouponUsage usage = new CouponUsage();
        usage.setUser_coupon_id_wsh(coupon.getId_wsh());
        usage.setTemplate_id_wsh(coupon.getTemplate_id_wsh());
        usage.setUser_id_wsh(coupon.getUser_id_wsh());
        usage.setOrder_id_wsh(orderId);
        usage.setOrder_no_wsh(orderNo);
        usage.setDiscount_amount_wsh(moneyOrZero(coupon.getDiscount_amount_wsh()));
        usage.setFunding_party_wsh(FUNDING_PLATFORM);
        usage.setStatus_wsh(STATUS_USED);
        try {
            usageMapper.insert(usage);
        // 唯一键冲突视为重复回调，直接忽略
        } catch (DuplicateKeyException ignored) {
        }
    }

    /**
     * 【订单取消后释放优惠券】
     *
     * 业务作用：把订单占用的券恢复为可用状态。
     *
     * 调用场景：订单取消或超时未支付时调用。
     *
     * 调用链：OrderService → CouponService.releaseForOrder → UserCouponMapper.releaseByOrderId。
     *
     * 数据处理：orderId 为空直接返回，否则按订单 ID 批量释放。
     *
     * 业务规则：只释放仍处于 locked 状态的券，由 Mapper SQL 保证。
     *
     * 状态影响：更新 user_coupon_wsh 状态回到 available 并清空订单绑定。
     *
     * 异常情况：无显式异常。
     *
     * 注意事项：重复调用是安全的，已释放的券不会被再次影响。
     */
    @Transactional
    @Override
    public void releaseForOrder(Long orderId) {
        if (orderId == null) {
            return;
        }
        userCouponMapper.releaseByOrderId(orderId);
    }

    /**
     * 【判断发券请求是否带有筛选条件】
     *
     * 业务作用：检查请求中是否存在任一有效筛选条件。
     *
     * 调用场景：grantByCondition 决定是否需要按条件过滤用户。
     *
     * 调用链：CouponServiceImpl.grantByCondition → hasGrantCondition → positive。
     *
     * 数据处理：注册时间区间、累计消费、订单数、宠物数、最近下单时间任一非空或为正，或 include_user_ids_wsh 非空，即视为有条件。
     *
     * 业务规则：request 为 null 时视为无条件。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：返回 false 表示无条件，此时 grantByCondition 会对全部正常用户发券。
     */
    private boolean hasGrantCondition(CouponGrantRequestDTO request, Set<Long> includeIds) {
        if (request == null) {
            return false;
        }
        return request.getRegistered_from_wsh() != null
                || request.getRegistered_to_wsh() != null
                || positive(request.getMin_total_spend_wsh())
                || positive(request.getMin_order_count_wsh())
                || positive(request.getMin_pet_count_wsh())
                || request.getLast_order_from_wsh() != null
                || request.getLast_order_to_wsh() != null
                || !includeIds.isEmpty();
    }

    /**
     * 【判断用户是否命中任一发券条件】
     *
     * 业务作用：对单个用户逐一比对各类筛选条件，命中任一即返回 true。
     *
     * 调用场景：grantByCondition 遍历用户时逐个判断。
     *
     * 调用链：grantByCondition → matchesAnyCondition → inRange。
     *
     * 数据处理：依次判断 include_user_ids_wsh 白名单、注册时间区间、累计消费、订单数、宠物数、最近下单时间。
     *
     * 业务规则：条件之间为「或」；orderStats 为 null 时消费、订单数、最近下单三类条件不命中；petCount 为 null 时宠物数条件不命中。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：时间区间比较统一由 inRange 处理边界（左闭右闭）。
     */
    private boolean matchesAnyCondition(User user,
                                        CouponGrantRequestDTO request,
                                        Set<Long> includeIds,
                                        CouponGrantOrderStats orderStats,
                                        Long petCount) {
        if (request == null) {
            return false;
        }
        Long userId = user.getId_wsh();
        if (includeIds.contains(userId)) {
            return true;
        }
        if ((request.getRegistered_from_wsh() != null || request.getRegistered_to_wsh() != null)
                && inRange(user.getCreated_at_wsh(), request.getRegistered_from_wsh(), request.getRegistered_to_wsh())) {
            return true;
        }
        if (positive(request.getMin_total_spend_wsh())
                && orderStats != null
                && orderStats.totalSpend().compareTo(moneyOrZero(request.getMin_total_spend_wsh())) >= 0) {
            return true;
        }
        if (positive(request.getMin_order_count_wsh())
                && orderStats != null
                && orderStats.orderCount() >= request.getMin_order_count_wsh()) {
            return true;
        }
        if (positive(request.getMin_pet_count_wsh()) && petCount >= request.getMin_pet_count_wsh()) {
            return true;
        }
        return (request.getLast_order_from_wsh() != null || request.getLast_order_to_wsh() != null)
                && orderStats != null
                && inRange(orderStats.lastOrderAt(), request.getLast_order_from_wsh(), request.getLast_order_to_wsh());
    }

    /**
     * 【判断发券请求是否包含负数条件】
     *
     * 业务作用：检测 min_total_spend_wsh、min_order_count_wsh、min_pet_count_wsh 是否为负。
     *
     * 调用场景：grantByCondition 入口处做前置校验。
     *
     * 调用链：grantByCondition → hasNegativeCondition → negative。
     *
     * 数据处理：任一字段为负数即返回 true。
     *
     * 业务规则：负数条件视为非法请求。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：命中时 grantByCondition 直接返回 0 而不抛异常，属于静默失败。
     */
    private boolean hasNegativeCondition(CouponGrantRequestDTO request) {
        if (request == null) {
            return false;
        }
        return negative(request.getMin_total_spend_wsh())
                || negative(request.getMin_order_count_wsh())
                || negative(request.getMin_pet_count_wsh());
    }

    /**
     * 【加载用户订单统计】
     *
     * 业务作用：一次性查出所有用户的累计消费、订单数、最近下单时间。
     *
     * 调用场景：grantByCondition 条件发券前预加载。
     *
     * 调用链：grantByCondition → loadOrderStats → OrderMapper.selectCouponGrantOrderStats → longValue/bigDecimalValue/dateTimeValue。
     *
     * 数据处理：把聚合查询结果逐行映射为 userId → CouponGrantOrderStats；user_id_wsh 为空的行跳过。
     *
     * 业务规则：金额统一保留 2 位小数；order_count_wsh 缺省为 0。
     *
     * 状态影响：只读。
     *
     * 异常情况：字段为不可解析的字符串时 longValue 会抛 NumberFormatException。
     *
     * 注意事项：一次性把全表聚合结果载入内存，用户量大时内存占用高。
     */
    private Map<Long, CouponGrantOrderStats> loadOrderStats() {
        Map<Long, CouponGrantOrderStats> stats = new LinkedHashMap<>();
        for (Map<String, Object> row : orderMapper.selectCouponGrantOrderStats()) {
            Long userId = longValue(row, "user_id_wsh");
            if (userId == null) {
                continue;
            }
            stats.put(userId, new CouponGrantOrderStats(
                    bigDecimalValue(row, "total_spend_wsh"),
                    longValue(row, "order_count_wsh", 0L),
                    dateTimeValue(row, "last_order_at_wsh")));
        }
        return stats;
    }

    /**
     * 【加载用户宠物数量统计】
     *
     * 业务作用：查出每个用户的宠物数量。
     *
     * 调用场景：grantByCondition 判断 min_pet_count_wsh 条件。
     *
     * 调用链：grantByCondition → loadPetCounts → PetMapper.selectCouponGrantPetCounts → longValue。
     *
     * 数据处理：聚合结果映射为 userId → petCount，user_id_wsh 为空的行跳过。
     *
     * 业务规则：pet_count_wsh 缺省为 0。
     *
     * 状态影响：只读。
     *
     * 异常情况：同 loadOrderStats，字符串解析失败可能抛 NumberFormatException。
     *
     * 注意事项：无。
     */
    private Map<Long, Long> loadPetCounts() {
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (Map<String, Object> row : petMapper.selectCouponGrantPetCounts()) {
            Long userId = longValue(row, "user_id_wsh");
            if (userId != null) {
                counts.put(userId, longValue(row, "pet_count_wsh", 0L));
            }
        }
        return counts;
    }

    /**
     * 【集合转 ID 集合】
     *
     * 业务作用：把请求里的用户 ID 集合转为去重、去 null 的 HashSet。
     *
     * 调用场景：grantByCondition 处理 include/exclude 用户 ID。
     *
     * 调用链：grantByCondition → toIdSet。
     *
     * 数据处理：null 或空集合返回 Set.of()；否则过滤 null 后收集为 HashSet。
     *
     * 业务规则：重复 ID 自动去重。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：返回的 Set.of() 为不可变集合，调用方不可修改。
     */
    private Set<Long> toIdSet(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return ids.stream().filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * 【判断时间是否落在区间内】
     *
     * 业务作用：判断给定时间是否位于 [from, to] 区间。
     *
     * 调用场景：按注册时间、最近下单时间筛选用户。
     *
     * 调用链：matchesAnyCondition → inRange。
     *
     * 数据处理：value 为 null 返回 false；from 非空且 value 早于 from 返回 false；to 为空视为无上界。
     *
     * 业务规则：区间为左闭右闭。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：from 与 to 同时为空时，只要 value 非空即返回 true。
     */
    private boolean inRange(LocalDateTime value, LocalDateTime from, LocalDateTime to) {
        if (value == null) {
            return false;
        }
        if (from != null && value.isBefore(from)) {
            return false;
        }
        return to == null || !value.isAfter(to);
    }

    /**
     * 【判断金额是否为正数】
     *
     * 业务作用：判断 BigDecimal 是否非空且大于 0。
     *
     * 调用场景：hasGrantCondition 与 matchesAnyCondition 判断金额类条件是否启用。
     *
     * 调用链：hasGrantCondition / matchesAnyCondition → positive(BigDecimal)。
     *
     * 数据处理：null 返回 false，否则比较是否大于 0。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：与 positive(Integer) 构成重载，按实参类型选择。
     */
    private boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 【判断整数是否为正数】
     *
     * 业务作用：判断 Integer 是否非空且大于 0。
     *
     * 调用场景：hasGrantCondition 与 matchesAnyCondition 判断订单数、宠物数条件是否启用。
     *
     * 调用链：hasGrantCondition / matchesAnyCondition → positive(Integer)。
     *
     * 数据处理：null 返回 false，否则比较是否大于 0。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：与 positive(BigDecimal) 构成重载。
     */
    private boolean positive(Integer value) {
        return value != null && value > 0;
    }

    /**
     * 【判断金额是否为负数】
     *
     * 业务作用：判断 BigDecimal 是否非空且小于 0。
     *
     * 调用场景：hasNegativeCondition 校验金额类条件是否为负。
     *
     * 调用链：hasNegativeCondition → negative(BigDecimal)。
     *
     * 数据处理：null 返回 false，否则比较是否小于 0。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：与 negative(Integer) 构成重载。
     */
    private boolean negative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * 【判断整数是否为负数】
     *
     * 业务作用：判断 Integer 是否非空且小于 0。
     *
     * 调用场景：hasNegativeCondition 校验订单数、宠物数条件是否为负。
     *
     * 调用链：hasNegativeCondition → negative(Integer)。
     *
     * 数据处理：null 返回 false，否则比较是否小于 0。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：与 negative(BigDecimal) 构成重载。
     */
    private boolean negative(Integer value) {
        return value != null && value < 0;
    }

    /**
     * 【从查询结果行中读取金额】
     *
     * 业务作用：把聚合查询返回的任意数值列转成保留 2 位的 BigDecimal。
     *
     * 调用场景：loadOrderStats 解析 total_spend_wsh。
     *
     * 调用链：loadOrderStats → bigDecimalValue → rowValue。
     *
     * 数据处理：BigDecimal 直接 setScale(2)；Number 先转 double 再 setScale(2)；其他类型返回 0.00。
     *
     * 业务规则：统一四舍五入到 2 位小数。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：非数值类型静默按 0 处理，不会报错。
     */
    private BigDecimal bigDecimalValue(Map<String, Object> row, String key) {
        Object value = rowValue(row, key);
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 【从查询结果行中读取 Long】
     *
     * 业务作用：读取指定列并转为 Long，取不到时返回 null。
     *
     * 调用场景：loadOrderStats 与 loadPetCounts 读取 user_id_wsh。
     *
     * 调用链：loadOrderStats / loadPetCounts → longValue(row,key) → longValue(row,key,null)。
     *
     * 数据处理：委托三参重载，默认值为 null。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：同三参重载。
     *
     * 注意事项：与三参版本构成重载。
     */
    private Long longValue(Map<String, Object> row, String key) {
        return longValue(row, key, null);
    }

    /**
     * 【从查询结果行中读取 Long（带默认值）】
     *
     * 业务作用：读取指定列并转为 Long，取不到时返回默认值。
     *
     * 调用场景：读取 order_count_wsh、pet_count_wsh 等计数字段。
     *
     * 调用链：loadOrderStats / loadPetCounts → longValue → rowValue。
     *
     * 数据处理：Number 直接 longValue；非空字符串用 Long.parseLong；其余返回默认值。
     *
     * 业务规则：字符串必须可解析为数字。
     *
     * 状态影响：无。
     *
     * 异常情况：字符串不是合法数字时抛 NumberFormatException。
     *
     * 注意事项：未捕获异常，脏数据会中断发券流程。
     */
    private Long longValue(Map<String, Object> row, String key, Long defaultValue) {
        Object value = rowValue(row, key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        return defaultValue;
    }

    /**
     * 【从查询结果行中读取时间】
     *
     * 业务作用：把结果列转为 LocalDateTime。
     *
     * 调用场景：loadOrderStats 解析 last_order_at_wsh。
     *
     * 调用链：loadOrderStats → dateTimeValue → rowValue。
     *
     * 数据处理：LocalDateTime 直接返回；Timestamp 转 LocalDateTime；其他类型返回 null。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：只识别这两种类型，java.util.Date 等其他类型返回 null。
     */
    private LocalDateTime dateTimeValue(Map<String, Object> row, String key) {
        Object value = rowValue(row, key);
        if (value instanceof LocalDateTime time) {
            return time;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return null;
    }

    /**
     * 【按列名取值（大小写兼容）】
     *
     * 业务作用：从 Map 结果行中取列值，兼容不同数据库返回的列名大小写。
     *
     * 调用场景：所有结果行解析方法。
     *
     * 调用链：bigDecimalValue / longValue / dateTimeValue → rowValue。
     *
     * 数据处理：先按原名取，再按全大写取，最后按全小写取。
     *
     * 业务规则：优先级为原名 > 大写 > 小写。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：三次取值都可能返回 null，最终结果为 null。
     */
    private Object rowValue(Map<String, Object> row, String key) {
        if (row.containsKey(key)) {
            return row.get(key);
        }
        String upperKey = key.toUpperCase();
        if (row.containsKey(upperKey)) {
            return row.get(upperKey);
        }
        return row.get(key.toLowerCase());
    }

    /**
     * 【用户订单统计快照】
     *
     * 业务作用：承载单个用户的累计消费、订单数、最近下单时间。
     *
     * 调用场景：grantByCondition 条件判断时按用户取用。
     *
     * 调用链：loadOrderStats → new CouponGrantOrderStats → matchesAnyCondition。
     *
     * 数据处理：仅作数据载体，无逻辑。
     *
     * 业务规则：orderCount 使用原始 long，totalSpend 为保留 2 位的 BigDecimal。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：私有内部 record，仅在发券条件筛选流程中使用。
     */
    private record CouponGrantOrderStats(BigDecimal totalSpend, long orderCount, LocalDateTime lastOrderAt) {
    }

    /**
     * 【发放一张优惠券】
     *
     * 业务作用：校验模板可用性、每人限领与库存后，写入一条用户券。
     *
     * 调用场景：grantToUser、grantToAllUsers、grantByCondition、claim 的内部循环。
     *
     * 调用链：grantXxx / claim → issueOne → requireUsableTemplate → UserCouponMapper.selectCount → CouponTemplateMapper.increaseIssuedQuantity → UserCouponMapper.insert。
     *
     * 数据处理：先统计该用户已持有该模板的券数，未超 per_user_limit_wsh 才继续；再原子累加 issued_quantity_wsh，成功则插入 user_coupon_wsh（状态 available、来源 source、过期时间取模板 valid_to_wsh）。
     *
     * 业务规则：每人限领取模板 per_user_limit_wsh，为空按 1；库存累加失败即视为库存不足。
     *
     * 状态影响：更新 coupon_template_wsh.issued_quantity_wsh，新增 user_coupon_wsh。
     *
     * 异常情况：模板不存在或不可用抛 404/400；库存不足抛 BusinessException(400, "Coupon stock is not enough")；限领超限返回 false 不抛异常。
     *
     * 注意事项：方法加了 synchronized，只在单实例内串行，集群部署仍需依赖数据库原子更新；限领统计与插入之间非原子，并发下可能超出限领。
     */
    private synchronized boolean issueOne(Long templateId, Long userId, String source) {
        CouponTemplate template = requireUsableTemplate(templateId);
        Long owned = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getTemplate_id_wsh, templateId)
                        .eq(UserCoupon::getUser_id_wsh, userId));
        int limit = template.getPer_user_limit_wsh() == null ? 1 : template.getPer_user_limit_wsh();
        // 已达每人限领上限，不再发放
        if (owned != null && owned >= limit) {
            return false;
        }
        // 原子累加库存，失败即视为库存不足
        if (templateMapper.increaseIssuedQuantity(templateId, 1) == 0) {
            throw new BusinessException(400, "Coupon stock is not enough");
        }
        UserCoupon coupon = new UserCoupon();
        coupon.setTemplate_id_wsh(templateId);
        coupon.setUser_id_wsh(userId);
        coupon.setStatus_wsh(STATUS_AVAILABLE);
        coupon.setSource_wsh(source);
        coupon.setExpire_at_wsh(template.getValid_to_wsh());
        userCouponMapper.insert(coupon);
        return true;
    }

    /**
     * 【批量补充用户券的模板信息】
     *
     * 业务作用：为一批用户券批量查询模板并转成 DTO。
     *
     * 调用场景：listMyCoupons。
     *
     * 调用链：listMyCoupons → enrich → CouponTemplateMapper.selectBatchIds → toUserCouponDTO。
     *
     * 数据处理：先收集非空 template_id_wsh，批量查模板转成 Map，再逐条转换；空集合返回 List.of()。
     *
     * 业务规则：模板查不到时 DTO 只保留券自身字段。
     *
     * 状态影响：只读。
     *
     * 异常情况：无。
     *
     * 注意事项：使用 selectBatchIds 避免逐条查询。
     */
    private List<UserCouponDTO> enrich(List<UserCoupon> coupons) {
        if (coupons == null || coupons.isEmpty()) {
            return List.of();
        }
        // 批量查模板，避免逐条查询
        Set<Long> templateIds = coupons.stream()
                .map(UserCoupon::getTemplate_id_wsh)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, CouponTemplate> templateMap = templateIds.isEmpty()
                ? Map.of()
                : templateMapper.selectBatchIds(templateIds).stream()
                .collect(Collectors.toMap(CouponTemplate::getId_wsh, Function.identity(), (a, b) -> a));
        return coupons.stream().map(c -> toUserCouponDTO(c, templateMap.get(c.getTemplate_id_wsh()))).collect(Collectors.toList());
    }

    /**
     * 【获取优惠券模板（不存在则抛异常）】
     *
     * 业务作用：按 ID 查模板，为空或不存在时抛业务异常。
     *
     * 调用场景：updateTemplateStatus、requireUsableTemplate。
     *
     * 调用链：updateTemplateStatus / requireUsableTemplate → requireTemplate → CouponTemplateMapper.selectById。
     *
     * 数据处理：主键查询单条。
     *
     * 业务规则：ID 不能为空，模板必须存在。
     *
     * 状态影响：只读。
     *
     * 异常情况：templateId 为空抛 400，模板不存在抛 404。
     *
     * 注意事项：不做状态与有效期校验，需要时改用 requireUsableTemplate。
     */
    private CouponTemplate requireTemplate(Long templateId) {
        if (templateId == null) {
            throw new BusinessException(400, "Coupon template id cannot be empty");
        }
        CouponTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException(404, "Coupon template does not exist");
        }
        return template;
    }

    /**
     * 【获取可用的优惠券模板】
     *
     * 业务作用：在 requireTemplate 基础上校验启用状态与有效期。
     *
     * 调用场景：issueOne、previewForOrder。
     *
     * 调用链：issueOne / previewForOrder → requireUsableTemplate → requireTemplate。
     *
     * 数据处理：读取模板后依次比较 status_wsh、valid_from_wsh、valid_to_wsh 与当前时间。
     *
     * 业务规则：必须 status_wsh=1、当前时间不早于 valid_from_wsh、不晚于 valid_to_wsh。
     *
     * 状态影响：只读。
     *
     * 异常情况：模板不存在抛 404；禁用、未生效、已过期分别抛 400。
     *
     * 注意事项：与 isTemplateAvailable 判断逻辑等价，区别是一个抛异常一个返回布尔。
     */
    private CouponTemplate requireUsableTemplate(Long templateId) {
        CouponTemplate template = requireTemplate(templateId);
        LocalDateTime now = LocalDateTime.now();
        // 禁用、未生效、已过期分别拦截
        if (template.getStatus_wsh() == null || template.getStatus_wsh() != STATUS_ENABLED) {
            throw new BusinessException(400, "Coupon template is disabled");
        }
        if (template.getValid_from_wsh() != null && template.getValid_from_wsh().isAfter(now)) {
            throw new BusinessException(400, "Coupon template is not active yet");
        }
        if (template.getValid_to_wsh() != null && template.getValid_to_wsh().isBefore(now)) {
            throw new BusinessException(400, "Coupon template has expired");
        }
        return template;
    }

    /**
     * 【判断模板当前是否可用】
     *
     * 业务作用：以布尔形式判断模板是否启用且在有效期内。
     *
     * 调用场景：listAvailableCoupons 过滤券。
     *
     * 调用链：listAvailableCoupons → isTemplateAvailable → CouponTemplateMapper.selectById。
     *
     * 数据处理：查模板后比较状态与有效期。
     *
     * 业务规则：status_wsh 必须为 1；valid_from_wsh 不能晚于当前时间；valid_to_wsh 不能早于当前时间。
     *
     * 状态影响：只读。
     *
     * 异常情况：无，模板不存在返回 false。
     *
     * 注意事项：每张券调用一次，列表场景存在 N+1 查询。
     */
    private boolean isTemplateAvailable(Long templateId, LocalDateTime now) {
        CouponTemplate template = templateMapper.selectById(templateId);
        if (template == null || template.getStatus_wsh() == null || template.getStatus_wsh() != STATUS_ENABLED) {
            return false;
        }
        if (template.getValid_from_wsh() != null && template.getValid_from_wsh().isAfter(now)) {
            return false;
        }
        return template.getValid_to_wsh() == null || !template.getValid_to_wsh().isBefore(now);
    }

    /**
     * 【获取用户券并校验归属】
     *
     * 业务作用：按 ID 查用户券，并校验其属于指定用户。
     *
     * 调用场景：previewForOrder。
     *
     * 调用链：previewForOrder → requireUserCoupon → UserCouponMapper.selectById。
     *
     * 数据处理：主键查询后比对 user_id_wsh。
     *
     * 业务规则：券必须存在且 user_id_wsh 与入参一致。
     *
     * 状态影响：只读。
     *
     * 异常情况：userCouponId 为空抛 400；券不存在或不属于该用户抛 404。
     *
     * 注意事项：越权访问与券不存在共用 404 文案，对外不区分。
     */
    private UserCoupon requireUserCoupon(Long userCouponId, Long userId) {
        if (userCouponId == null) {
            throw new BusinessException(400, "User coupon id cannot be empty");
        }
        UserCoupon coupon = userCouponMapper.selectById(userCouponId);
        if (coupon == null || !Objects.equals(coupon.getUser_id_wsh(), userId)) {
            throw new BusinessException(404, "Coupon does not exist");
        }
        return coupon;
    }

    /**
     * 【构造无券的试算结果】
     *
     * 业务作用：生成券抵扣为 0 的默认试算结果。
     *
     * 调用场景：previewForOrder 在未选择优惠券时返回。
     *
     * 调用链：previewForOrder → emptyResult。
     *
     * 数据处理：券抵扣与平台补贴置 0.00；结算金额与最终金额都等于传入的结算金额；快照置 "{}"。
     *
     * 业务规则：金额统一保留 2 位小数。
     *
     * 状态影响：无。
     *
     * 异常情况：settlementAmount 为 null 时 setScale 会抛 NullPointerException。
     *
     * 注意事项：调用方需保证结算金额非空，previewForOrder 中已用 moneyOrZero 兜底。
     */
    private CouponDiscountResult emptyResult(BigDecimal settlementAmount) {
        CouponDiscountResult result = new CouponDiscountResult();
        result.setCoupon_discount_wsh(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        result.setPlatform_subsidy_wsh(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        result.setSettlement_amount_wsh(settlementAmount.setScale(2, RoundingMode.HALF_UP));
        result.setFinal_amount_wsh(settlementAmount.setScale(2, RoundingMode.HALF_UP));
        result.setSnapshot_wsh("{}");
        return result;
    }

    /**
     * 【计算优惠券抵扣金额】
     *
     * 业务作用：按模板类型计算抵扣，并做上限与金额封顶。
     *
     * 调用场景：previewForOrder。
     *
     * 调用链：previewForOrder → calculateCouponDiscount → normalizeRate / moneyOrZero / moneyOrNull。
     *
     * 数据处理：amount 类型直接取 discount_amount_wsh；percent 类型按 金额 × (1 - 折扣率) 计算；随后受 max_discount_amount_wsh 限制，且不超过订单金额。
     *
     * 业务规则：抵扣不得超过最大抵扣金额（配置为正数时生效），也不得超过订单金额。
     *
     * 状态影响：无。
     *
     * 异常情况：模板类型不是 amount/percent 时抛 400；折扣率非法时 normalizeRate 抛 400。
     *
     * 注意事项：percent 的语义是「折扣率」而非「减免率」，0.8 表示打 8 折、抵扣 20%。
     */
    private BigDecimal calculateCouponDiscount(CouponTemplate template, BigDecimal amount) {
        BigDecimal discount;
        // 满减直接取面额，折扣按折扣率折算
        if (TYPE_AMOUNT.equals(template.getType_wsh())) {
            discount = moneyOrZero(template.getDiscount_amount_wsh());
        } else if (TYPE_PERCENT.equals(template.getType_wsh())) {
            BigDecimal rate = normalizeRate(template.getDiscount_rate_wsh());
            discount = amount.multiply(BigDecimal.ONE.subtract(rate));
        } else {
            throw new BusinessException(400, "Unsupported coupon type");
        }
        BigDecimal maxDiscount = moneyOrNull(template.getMax_discount_amount_wsh());
        // 抵扣不超过配置的最大抵扣额
        if (maxDiscount != null && maxDiscount.compareTo(BigDecimal.ZERO) > 0 && discount.compareTo(maxDiscount) > 0) {
            discount = maxDiscount;
        }
        // 抵扣不超过订单金额本身
        if (discount.compareTo(amount) > 0) {
            discount = amount;
        }
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 【生成优惠快照 JSON】
     *
     * 业务作用：把模板与试算金额序列化成下单快照，供订单保存。
     *
     * 调用场景：previewForOrder。
     *
     * 调用链：previewForOrder → toSnapshot → ObjectMapper.writeValueAsString。
     *
     * 数据处理：按固定顺序写入承担方、模板 ID、券名、类型、券抵扣、平台补贴、结算金额、最终金额。
     *
     * 业务规则：承担方固定为 platform。
     *
     * 状态影响：无。
     *
     * 异常情况：序列化异常被捕获，返回 "{}"。
     *
     * 注意事项：静默吞掉异常，快照可能退化为空对象。
     */
    private String toSnapshot(CouponTemplate template,
                              BigDecimal couponDiscount,
                              BigDecimal settlementAmount,
                              BigDecimal finalAmount) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("funding_party_wsh", FUNDING_PLATFORM);
        data.put("template_id_wsh", template.getId_wsh());
        data.put("name_wsh", template.getName_wsh());
        data.put("type_wsh", template.getType_wsh());
        data.put("coupon_discount_wsh", couponDiscount);
        data.put("platform_subsidy_wsh", couponDiscount);
        data.put("settlement_amount_wsh", settlementAmount);
        data.put("final_amount_wsh", finalAmount);
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * 【判断优惠券适用范围是否匹配】
     *
     * 业务作用：比较模板范围与当前下单商家。
     *
     * 调用场景：previewForOrder、listAvailableCoupons。
     *
     * 调用链：previewForOrder / listAvailableCoupons → isScopeMatched → normalizeScope。
     *
     * 数据处理：platform 范围直接返回 true；merchant 范围要求 merchantId 非空且与 scopeMerchantId 相等。
     *
     * 业务规则：merchant 范围券只在归属商家可用。
     *
     * 状态影响：无。
     *
     * 异常情况：scopeType 非法时 normalizeScope 抛 400。
     *
     * 注意事项：merchantId 为 null（未指定商家场景）时，merchant 范围券一律不匹配。
     */
    private boolean isScopeMatched(String scopeType, Long scopeMerchantId, Long merchantId) {
        String scope = normalizeScope(scopeType);
        // 平台券对所有商家通用
        if ("platform".equals(scope)) {
            return true;
        }
        return merchantId != null && Objects.equals(scopeMerchantId, merchantId);
    }

    /**
     * 【校验优惠券模板创建请求】
     *
     * 业务作用：在落库前校验有效期、类型相关金额与商家范围。
     *
     * 调用场景：createTemplate。
     *
     * 调用链：createTemplate → validateTemplateRequest → positiveMoney / normalizeRate / normalizeScope。
     *
     * 数据处理：校验起止时间非空且止晚于起；amount 类型校验正金额，percent 类型校验折扣率；merchant 范围要求 merchant_id_wsh 非空。
     *
     * 业务规则：类型只允许 amount 或 percent；有效期必须合法；商家券必须指定商家。
     *
     * 状态影响：无。
     *
     * 异常情况：有效期为空或不合法、类型不支持、金额或折扣率非法、商家券缺商家 ID 均抛 400。
     *
     * 注意事项：仅做校验不返回值，校验通过后 createTemplate 还会再归一化一次字段。
     */
    private void validateTemplateRequest(CouponTemplateCreateRequestDTO request, String type) {
        if (request.getValid_from_wsh() == null || request.getValid_to_wsh() == null) {
            throw new BusinessException(400, "Coupon validity cannot be empty");
        }
        if (!request.getValid_to_wsh().isAfter(request.getValid_from_wsh())) {
            throw new BusinessException(400, "Coupon valid-to time must be after valid-from time");
        }
        if (TYPE_AMOUNT.equals(type)) {
            positiveMoney(request.getDiscount_amount_wsh());
        } else if (TYPE_PERCENT.equals(type)) {
            normalizeRate(request.getDiscount_rate_wsh());
        } else {
            throw new BusinessException(400, "Unsupported coupon type");
        }
        String scope = normalizeScope(request.getScope_type_wsh());
        if ("merchant".equals(scope) && request.getMerchant_id_wsh() == null) {
            throw new BusinessException(400, "Merchant coupon requires merchant id");
        }
    }

    /**
     * 【模板实体转 DTO】
     *
     * 业务作用：用 BeanUtils 拷贝同名属性把模板实体转成 DTO。
     *
     * 调用场景：listTemplates。
     *
     * 调用链：listTemplates → toTemplateDTO。
     *
     * 数据处理：属性名一致即自动拷贝。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：依赖实体与 DTO 字段同名，字段改名后需同步。
     */
    private CouponTemplateDTO toTemplateDTO(CouponTemplate template) {
        CouponTemplateDTO dto = new CouponTemplateDTO();
        BeanUtils.copyProperties(template, dto);
        return dto;
    }

    /**
     * 【用户券实体转 DTO】
     *
     * 业务作用：拷贝券自身字段，并用模板补齐券名、类型、门槛、折扣等展示字段。
     *
     * 调用场景：enrich。
     *
     * 调用链：enrich → toUserCouponDTO。
     *
     * 数据处理：先拷贝用户券字段，模板非空时覆盖模板相关字段，模板面额写入 discount_amount_template_wsh。
     *
     * 业务规则：模板为空时只返回券自身字段。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：模板金额写入 discount_amount_template_wsh 而非券上的 discount_amount_wsh，避免覆盖实际抵扣额。
     */
    private UserCouponDTO toUserCouponDTO(UserCoupon coupon, CouponTemplate template) {
        UserCouponDTO dto = new UserCouponDTO();
        BeanUtils.copyProperties(coupon, dto);
        // 模板缺失时只返回券自身字段
        if (template != null) {
            dto.setName_wsh(template.getName_wsh());
            dto.setType_wsh(template.getType_wsh());
            dto.setThreshold_amount_wsh(template.getThreshold_amount_wsh());
            dto.setDiscount_amount_template_wsh(template.getDiscount_amount_wsh());
            dto.setDiscount_rate_wsh(template.getDiscount_rate_wsh());
            dto.setMax_discount_amount_wsh(template.getMax_discount_amount_wsh());
            dto.setScope_type_wsh(template.getScope_type_wsh());
            dto.setMerchant_id_wsh(template.getMerchant_id_wsh());
        }
        return dto;
    }

    /**
     * 【归一化优惠券类型】
     *
     * 业务作用：去除空白并转小写。
     *
     * 调用场景：createTemplate。
     *
     * 调用链：createTemplate → normalizeType。
     *
     * 数据处理：null 返回空串，否则 trim 后转小写。
     *
     * 业务规则：不校验合法性，合法性由 validateTemplateRequest 负责。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：null 被转成空串后会在后续校验中因类型不支持而抛 400。
     */
    private String normalizeType(String type) {
        return type == null ? "" : type.trim().toLowerCase();
    }

    /**
     * 【归一化适用范围】
     *
     * 业务作用：把范围值归一为 platform 或 merchant。
     *
     * 调用场景：createTemplate、isScopeMatched、validateTemplateRequest。
     *
     * 调用链：createTemplate / isScopeMatched / validateTemplateRequest → normalizeScope。
     *
     * 数据处理：空值默认 platform，否则 trim 转小写，并校验是否在允许集合内。
     *
     * 业务规则：只允许 platform、merchant。
     *
     * 状态影响：无。
     *
     * 异常情况：不在允许集合内抛 400。
     *
     * 注意事项：空值不报错而是默认 platform，可能把漏填范围的请求当成平台券。
     */
    private String normalizeScope(String scope) {
        String value = scope == null || scope.isBlank() ? "platform" : scope.trim().toLowerCase();
        if (!List.of("platform", "merchant").contains(value)) {
            throw new BusinessException(400, "Unsupported coupon scope");
        }
        return value;
    }

    /**
     * 【归一化模板状态】
     *
     * 业务作用：把状态值校验为 0 或 1。
     *
     * 调用场景：createTemplate、updateTemplateStatus。
     *
     * 调用链：createTemplate / updateTemplateStatus → normalizeStatus。
     *
     * 数据处理：null 默认 1（启用），其余校验必须是 0 或 1。
     *
     * 业务规则：只允许启用(1)与禁用(0)。
     *
     * 状态影响：无。
     *
     * 异常情况：其他值抛 400。
     *
     * 注意事项：null 视为启用，创建时不传状态会直接上架。
     */
    private Integer normalizeStatus(Integer status) {
        int value = status == null ? STATUS_ENABLED : status;
        if (value != STATUS_ENABLED && value != STATUS_DISABLED) {
            throw new BusinessException(400, "Unsupported coupon status");
        }
        return value;
    }

    /**
     * 【校验非负整数】
     *
     * 业务作用：校验总发放量为非负整数。
     *
     * 调用场景：createTemplate 设置 total_quantity_wsh。
     *
     * 调用链：createTemplate → nonNegativeInteger。
     *
     * 数据处理：null 原样返回 null，负数抛异常。
     *
     * 业务规则：不得为负。
     *
     * 状态影响：无。
     *
     * 异常情况：负数抛 400。
     *
     * 注意事项：返回 null 表示未配置总量，具体语义由后续库存 SQL 决定。
     */
    private Integer nonNegativeInteger(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            throw new BusinessException(400, "Quantity cannot be negative");
        }
        return value;
    }

    /**
     * 【校验正整数并给默认值】
     *
     * 业务作用：为空时返回默认值，非正数时抛异常。
     *
     * 调用场景：createTemplate 的 per_user_limit_wsh、requestQuantity 的 quantity_wsh。
     *
     * 调用链：createTemplate / requestQuantity → positiveIntegerOrDefault。
     *
     * 数据处理：null 返回默认值，不大于 0 抛异常，否则原样返回。
     *
     * 业务规则：必须大于 0。
     *
     * 状态影响：无。
     *
     * 异常情况：不大于 0 抛 400。
     *
     * 注意事项：默认值由调用方传入，限领默认 1、发券数量默认 1。
     */
    private Integer positiveIntegerOrDefault(Integer value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value <= 0) {
            throw new BusinessException(400, "Limit must be greater than zero");
        }
        return value;
    }

    /**
     * 【解析发券数量】
     *
     * 业务作用：从发券请求中取数量，缺省为 1。
     *
     * 调用场景：grantToUser、grantToAllUsers、grantByCondition。
     *
     * 调用链：grantXxx → requestQuantity → positiveIntegerOrDefault。
     *
     * 数据处理：request 或 quantity_wsh 为 null 时返回 1。
     *
     * 业务规则：数量必须大于 0。
     *
     * 状态影响：无。
     *
     * 异常情况：数量不大于 0 抛 400。
     *
     * 注意事项：一次请求的数量会被复制给每个命中的用户。
     */
    private int requestQuantity(CouponGrantRequestDTO request) {
        return positiveIntegerOrDefault(request != null ? request.getQuantity_wsh() : null, 1);
    }

    /**
     * 【归一化折扣率】
     *
     * 业务作用：校验折扣率并保留 2 位小数。
     *
     * 调用场景：createTemplate、validateTemplateRequest、calculateCouponDiscount。
     *
     * 调用链：createTemplate / calculateCouponDiscount → normalizeRate。
     *
     * 数据处理：setScale(2)，并校验位于 (0,1) 开区间。
     *
     * 业务规则：折扣率必须大于 0 且小于 1。
     *
     * 状态影响：无。
     *
     * 异常情况：为 null 或不在区间内抛 400。
     *
     * 注意事项：1.0 也会被拒绝，即不支持「不打折」的券。
     */
    private BigDecimal normalizeRate(BigDecimal rate) {
        if (rate == null) {
            throw new BusinessException(400, "Discount rate cannot be empty");
        }
        BigDecimal normalized = rate.setScale(2, RoundingMode.HALF_UP);
        // 折扣率必须在 0 与 1 之间且不含端点
        if (normalized.compareTo(BigDecimal.ZERO) <= 0 || normalized.compareTo(BigDecimal.ONE) >= 0) {
            throw new BusinessException(400, "Discount rate must be between 0 and 1");
        }
        return normalized;
    }

    /**
     * 【校验金额为正】
     *
     * 业务作用：校验金额非空且大于 0，并保留 2 位小数。
     *
     * 调用场景：createTemplate、validateTemplateRequest。
     *
     * 调用链：createTemplate / validateTemplateRequest → positiveMoney → moneyOrNull。
     *
     * 数据处理：先经 moneyOrNull 归一（含非负校验），再判断是否大于 0。
     *
     * 业务规则：金额必须大于 0。
     *
     * 状态影响：无。
     *
     * 异常情况：为空、为负、为 0 均抛 400。
     *
     * 注意事项：负数会先在 moneyOrNull 抛「金额不能为负」，异常文案与 0 的情况不同。
     */
    private BigDecimal positiveMoney(BigDecimal value) {
        BigDecimal money = moneyOrNull(value);
        if (money == null || money.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "Amount must be greater than zero");
        }
        return money;
    }

    /**
     * 【金额兜底为 0】
     *
     * 业务作用：为空时返回 0.00，否则归一为 2 位小数。
     *
     * 调用场景：试算、门槛比较、券金额读取。
     *
     * 调用链：previewForOrder / listAvailableCoupons / markUsedForOrder → moneyOrZero → moneyOrNull。
     *
     * 数据处理：null 返回 0.00；非空经 moneyOrNull 归一。
     *
     * 业务规则：不允许负数，由 moneyOrNull 校验。
     *
     * 状态影响：无。
     *
     * 异常情况：负数抛 400。
     *
     * 注意事项：常用于把可空金额转成可直接参与运算的值。
     */
    private BigDecimal moneyOrZero(BigDecimal value) {
        BigDecimal money = moneyOrNull(value);
        return money == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : money;
    }

    /**
     * 【金额归一化】
     *
     * 业务作用：保留 2 位小数并校验非负，null 原样返回。
     *
     * 调用场景：所有金额字段的归一化入口。
     *
     * 调用链：moneyOrZero / positiveMoney / createTemplate → moneyOrNull。
     *
     * 数据处理：setScale(2, HALF_UP)，再校验不小于 0。
     *
     * 业务规则：金额不得为负。
     *
     * 状态影响：无。
     *
     * 异常情况：负数抛 400。
     *
     * 注意事项：这是金额校验的最后一道防线，负数在此统一被拒绝。
     */
    private BigDecimal moneyOrNull(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal money = value.setScale(2, RoundingMode.HALF_UP);
        // 负数金额统一在这里被拒绝
        if (money.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(400, "Amount cannot be negative");
        }
        return money;
    }
}
