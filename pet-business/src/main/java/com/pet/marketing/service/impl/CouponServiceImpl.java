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

    @Override
    public List<CouponTemplateDTO> listTemplates(boolean activeOnly) {
        LocalDateTime now = LocalDateTime.now();
        List<CouponTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<CouponTemplate>()
                        .eq(activeOnly, CouponTemplate::getStatus_wsh, STATUS_ENABLED)
                        .le(activeOnly, CouponTemplate::getValid_from_wsh, now)
                        .ge(activeOnly, CouponTemplate::getValid_to_wsh, now)
                        .orderByDesc(CouponTemplate::getCreated_at_wsh));
        return templates.stream().map(this::toTemplateDTO).collect(Collectors.toList());
    }

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
        template.setMerchant_id_wsh(template.getScope_type_wsh().equals("merchant") ? request.getMerchant_id_wsh() : null);
        template.setCreated_by_wsh(adminId);
        template.setRemark_wsh(request.getRemark_wsh());
        templateMapper.insert(template);
        return template;
    }

    @Transactional
    @Override
    public CouponTemplate updateTemplateStatus(Long templateId, Integer status) {
        CouponTemplate template = requireTemplate(templateId);
        template.setStatus_wsh(normalizeStatus(status));
        templateMapper.updateById(template);
        return template;
    }

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
        for (int i = 0; i < quantity; i++) {
            if (issueOne(templateId, userId, SOURCE_ADMIN)) {
                issued++;
            }
        }
        return issued;
    }

    @Transactional
    @Override
    public int grantToAllUsers(Long adminId, Long templateId, CouponGrantRequestDTO request) {
        int quantity = requestQuantity(request);
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

    @Transactional
    @Override
    public int grantByCondition(Long adminId, Long templateId, CouponGrantRequestDTO request) {
        int quantity = requestQuantity(request);
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
            if (userId == null || excludeIds.contains(userId)) {
                continue;
            }
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

    @Transactional
    @Override
    public UserCoupon claim(Long userId, Long templateId) {
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

    @Override
    public List<UserCouponDTO> listMyCoupons(Long userId) {
        List<UserCoupon> coupons = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUser_id_wsh, userId)
                        .orderByDesc(UserCoupon::getCreated_at_wsh));
        return enrich(coupons);
    }

    @Override
    public List<UserCouponDTO> listAvailableCoupons(Long userId, Long merchantId, BigDecimal orderAmount) {
        LocalDateTime now = LocalDateTime.now();
        BigDecimal amount = moneyOrZero(orderAmount);
        return listMyCoupons(userId).stream()
                .filter(c -> STATUS_AVAILABLE.equals(c.getStatus_wsh()))
                .filter(c -> c.getExpire_at_wsh() == null || !c.getExpire_at_wsh().isBefore(now))
                .filter(c -> c.getTemplate_id_wsh() != null && isTemplateAvailable(c.getTemplate_id_wsh(), now))
                .filter(c -> isScopeMatched(c.getScope_type_wsh(), c.getMerchant_id_wsh(), merchantId))
                .filter(c -> amount.compareTo(moneyOrZero(c.getThreshold_amount_wsh())) >= 0)
                .collect(Collectors.toList());
    }

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

    @Override
    public CouponDiscountResult previewForOrder(Long userId,
                                                Long userCouponId,
                                                BigDecimal totalAmount,
                                                BigDecimal longStayDiscount,
                                                Long merchantId,
                                                Long serviceId) {
        BigDecimal total = moneyOrZero(totalAmount);
        BigDecimal baseDiscount = moneyOrZero(longStayDiscount);
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
        int updated = userCouponMapper.lockForOrder(userCouponId, userId, orderId, orderNo, discount);
        if (updated == 0) {
            throw new BusinessException(400, "Coupon is no longer available");
        }
    }

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
        } catch (DuplicateKeyException ignored) {
        }
    }

    @Transactional
    @Override
    public void releaseForOrder(Long orderId) {
        if (orderId == null) {
            return;
        }
        userCouponMapper.releaseByOrderId(orderId);
    }

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

    private boolean hasNegativeCondition(CouponGrantRequestDTO request) {
        if (request == null) {
            return false;
        }
        return negative(request.getMin_total_spend_wsh())
                || negative(request.getMin_order_count_wsh())
                || negative(request.getMin_pet_count_wsh());
    }

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

    private Set<Long> toIdSet(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return ids.stream().filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
    }

    private boolean inRange(LocalDateTime value, LocalDateTime from, LocalDateTime to) {
        if (value == null) {
            return false;
        }
        if (from != null && value.isBefore(from)) {
            return false;
        }
        return to == null || !value.isAfter(to);
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean positive(Integer value) {
        return value != null && value > 0;
    }

    private boolean negative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }

    private boolean negative(Integer value) {
        return value != null && value < 0;
    }

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

    private Long longValue(Map<String, Object> row, String key) {
        return longValue(row, key, null);
    }

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

    private record CouponGrantOrderStats(BigDecimal totalSpend, long orderCount, LocalDateTime lastOrderAt) {
    }

    private synchronized boolean issueOne(Long templateId, Long userId, String source) {
        CouponTemplate template = requireUsableTemplate(templateId);
        Long owned = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getTemplate_id_wsh, templateId)
                        .eq(UserCoupon::getUser_id_wsh, userId));
        int limit = template.getPer_user_limit_wsh() == null ? 1 : template.getPer_user_limit_wsh();
        if (owned != null && owned >= limit) {
            return false;
        }
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

    private List<UserCouponDTO> enrich(List<UserCoupon> coupons) {
        if (coupons == null || coupons.isEmpty()) {
            return List.of();
        }
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

    private CouponTemplate requireUsableTemplate(Long templateId) {
        CouponTemplate template = requireTemplate(templateId);
        LocalDateTime now = LocalDateTime.now();
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

    private CouponDiscountResult emptyResult(BigDecimal settlementAmount) {
        CouponDiscountResult result = new CouponDiscountResult();
        result.setCoupon_discount_wsh(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        result.setPlatform_subsidy_wsh(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        result.setSettlement_amount_wsh(settlementAmount.setScale(2, RoundingMode.HALF_UP));
        result.setFinal_amount_wsh(settlementAmount.setScale(2, RoundingMode.HALF_UP));
        result.setSnapshot_wsh("{}");
        return result;
    }

    private BigDecimal calculateCouponDiscount(CouponTemplate template, BigDecimal amount) {
        BigDecimal discount;
        if (TYPE_AMOUNT.equals(template.getType_wsh())) {
            discount = moneyOrZero(template.getDiscount_amount_wsh());
        } else if (TYPE_PERCENT.equals(template.getType_wsh())) {
            BigDecimal rate = normalizeRate(template.getDiscount_rate_wsh());
            discount = amount.multiply(BigDecimal.ONE.subtract(rate));
        } else {
            throw new BusinessException(400, "Unsupported coupon type");
        }
        BigDecimal maxDiscount = moneyOrNull(template.getMax_discount_amount_wsh());
        if (maxDiscount != null && maxDiscount.compareTo(BigDecimal.ZERO) > 0 && discount.compareTo(maxDiscount) > 0) {
            discount = maxDiscount;
        }
        if (discount.compareTo(amount) > 0) {
            discount = amount;
        }
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

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

    private boolean isScopeMatched(String scopeType, Long scopeMerchantId, Long merchantId) {
        String scope = normalizeScope(scopeType);
        if ("platform".equals(scope)) {
            return true;
        }
        return merchantId != null && Objects.equals(scopeMerchantId, merchantId);
    }

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

    private CouponTemplateDTO toTemplateDTO(CouponTemplate template) {
        CouponTemplateDTO dto = new CouponTemplateDTO();
        BeanUtils.copyProperties(template, dto);
        return dto;
    }

    private UserCouponDTO toUserCouponDTO(UserCoupon coupon, CouponTemplate template) {
        UserCouponDTO dto = new UserCouponDTO();
        BeanUtils.copyProperties(coupon, dto);
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

    private String normalizeType(String type) {
        return type == null ? "" : type.trim().toLowerCase();
    }

    private String normalizeScope(String scope) {
        String value = scope == null || scope.isBlank() ? "platform" : scope.trim().toLowerCase();
        if (!List.of("platform", "merchant").contains(value)) {
            throw new BusinessException(400, "Unsupported coupon scope");
        }
        return value;
    }

    private Integer normalizeStatus(Integer status) {
        int value = status == null ? STATUS_ENABLED : status;
        if (value != STATUS_ENABLED && value != STATUS_DISABLED) {
            throw new BusinessException(400, "Unsupported coupon status");
        }
        return value;
    }

    private Integer nonNegativeInteger(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            throw new BusinessException(400, "Quantity cannot be negative");
        }
        return value;
    }

    private Integer positiveIntegerOrDefault(Integer value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value <= 0) {
            throw new BusinessException(400, "Limit must be greater than zero");
        }
        return value;
    }

    private int requestQuantity(CouponGrantRequestDTO request) {
        return positiveIntegerOrDefault(request != null ? request.getQuantity_wsh() : null, 1);
    }

    private BigDecimal normalizeRate(BigDecimal rate) {
        if (rate == null) {
            throw new BusinessException(400, "Discount rate cannot be empty");
        }
        BigDecimal normalized = rate.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0 || normalized.compareTo(BigDecimal.ONE) >= 0) {
            throw new BusinessException(400, "Discount rate must be between 0 and 1");
        }
        return normalized;
    }

    private BigDecimal positiveMoney(BigDecimal value) {
        BigDecimal money = moneyOrNull(value);
        if (money == null || money.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "Amount must be greater than zero");
        }
        return money;
    }

    private BigDecimal moneyOrZero(BigDecimal value) {
        BigDecimal money = moneyOrNull(value);
        return money == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : money;
    }

    private BigDecimal moneyOrNull(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal money = value.setScale(2, RoundingMode.HALF_UP);
        if (money.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(400, "Amount cannot be negative");
        }
        return money;
    }
}
