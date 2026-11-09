package com.pet.membership.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.membership.dto.MembershipDiscountDTO;
import com.pet.membership.entity.MemberPlan;
import com.pet.membership.entity.MembershipBenefitUsage;
import com.pet.membership.entity.UserMembership;
import com.pet.membership.mapper.MemberPlanMapper;
import com.pet.membership.mapper.MembershipBenefitUsageMapper;
import com.pet.membership.mapper.UserMembershipMapper;
import com.pet.membership.service.MembershipBenefitService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MembershipBenefitServiceImpl implements MembershipBenefitService {
    private static final String STATUS_ACTIVE = "active";
    private static final String USAGE_LOCKED = "locked";
    private static final String USAGE_USED = "used";
    private static final String USAGE_RELEASED = "released";
    private static final String BENEFIT_DISCOUNT = "discount";
    private static final String BENEFIT_ORDER_DISCOUNT = "order_discount";
    private static final String BUSINESS_PET_ORDER = "pet_order";

    private final UserMembershipMapper userMembershipMapper;
    private final MemberPlanMapper memberPlanMapper;
    private final MembershipBenefitUsageMapper membershipBenefitUsageMapper;
    private final ObjectMapper objectMapper;

    /**
     * 【构造会员权益服务】
     *
     * 业务作用：注入用户会员、会员套餐、权益使用三个 Mapper 与 JSON 序列化器。
     *
     * 调用场景：Spring 容器启动创建 Bean 时调用。
     *
     * 调用链：Spring 容器 → 本构造器 → 赋值各 Mapper / ObjectMapper 字段。
     *
     * 数据处理：无。
     *
     * 业务规则：依赖均为 final 字段，构造后不可替换。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：无。
     */
    public MembershipBenefitServiceImpl(UserMembershipMapper userMembershipMapper,
                                        MemberPlanMapper memberPlanMapper,
                                        MembershipBenefitUsageMapper membershipBenefitUsageMapper,
                                        ObjectMapper objectMapper) {
        this.userMembershipMapper = userMembershipMapper;
        this.memberPlanMapper = memberPlanMapper;
        this.membershipBenefitUsageMapper = membershipBenefitUsageMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * Previews the membership discount for a given order amount.
     * Looks up the user's active membership, resolves the discount rate
     * from the plan or benefit snapshot, and computes the discounted amount.
     *
     * @param userId     the user ID
     * @param baseAmount the original order amount
     * @return the discount preview DTO (with eligible=false if no active membership)
     */
    @Override
    public MembershipDiscountDTO previewOrderDiscount(Long userId, BigDecimal baseAmount) {
        BigDecimal amount = money(baseAmount);
        MembershipDiscountDTO empty = emptyResult(amount);
        // 用户或金额非法时返回无折扣结果
        if (userId == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return empty;
        }
        UserMembership membership = findActiveMembership(userId);
        if (membership == null) {
            return empty;
        }
        MemberPlan plan = membership.getPlan_id_wsh() == null ? null : memberPlanMapper.selectById(membership.getPlan_id_wsh());
        BigDecimal rate = normalizeRate(resolveDiscountRate(membership, plan));
        BigDecimal discount = amount.multiply(BigDecimal.ONE.subtract(rate)).setScale(2, RoundingMode.HALF_UP);
        // 折扣额不得超过订单原价
        if (discount.compareTo(amount) > 0) {
            discount = amount;
        }
        MembershipDiscountDTO dto = new MembershipDiscountDTO();
        dto.setEligible_wsh(true);
        dto.setMembership_id_wsh(membership.getId_wsh());
        dto.setPlan_id_wsh(membership.getPlan_id_wsh());
        dto.setPlan_code_wsh(membership.getPlan_code_wsh());
        dto.setPlan_name_wsh(resolvePlanName(membership, plan));
        dto.setLevel_wsh(membership.getLevel_wsh());
        dto.setDiscount_rate_wsh(rate);
        dto.setBase_amount_wsh(amount);
        dto.setMembership_discount_wsh(discount);
        dto.setFinal_amount_wsh(amount.subtract(discount).setScale(2, RoundingMode.HALF_UP));
        dto.setSnapshot_wsh(toSnapshot(dto));
        return dto;
    }

    /**
     * Locks the membership discount for a specific order by creating a usage record
     * in "locked" status. Idempotent — if a lock already exists for the order,
     * the operation is silently skipped.
     *
     * @param userId   the user ID
     * @param discount the discount details from preview
     * @param orderId  the order ID
     * @param orderNo  the order number
     */
    @Override
    @Transactional
    public void lockForOrder(Long userId, MembershipDiscountDTO discount, Long orderId, String orderNo) {
        if (!hasDiscount(discount) || orderId == null) {
            return;
        }
        String requestId = requestId(orderId);
        // 按订单幂等，已锁定过则直接返回
        MembershipBenefitUsage existing = findByRequestId(requestId);
        if (existing != null) {
            return;
        }
        MembershipBenefitUsage usage = new MembershipBenefitUsage();
        usage.setUser_id_wsh(userId);
        usage.setMembership_id_wsh(discount.getMembership_id_wsh());
        usage.setPlan_id_wsh(discount.getPlan_id_wsh());
        usage.setBenefit_type_wsh(BENEFIT_DISCOUNT);
        usage.setBenefit_code_wsh(BENEFIT_ORDER_DISCOUNT);
        usage.setBusiness_type_wsh(BUSINESS_PET_ORDER);
        usage.setBusiness_id_wsh(String.valueOf(orderId));
        usage.setAmount_wsh(money(discount.getMembership_discount_wsh()));
        usage.setQuantity_wsh(1);
        usage.setUsage_status_wsh(USAGE_LOCKED);
        usage.setRequest_id_wsh(requestId);
        usage.setUsage_snapshot_wsh(toUsageSnapshot(discount, orderId, orderNo));
        try {
            membershipBenefitUsageMapper.insert(usage); // 并发插入由唯一键兜底，重复时忽略
        } catch (DuplicateKeyException ignored) {
        }
    }

    /**
     * Marks the locked benefit as actually used for a completed order.
     * Transitions the usage record from "locked" to "used" status.
     *
     * @param orderId the order ID
     * @param orderNo the order number
     */
    @Override
    @Transactional
    public void markUsedForOrder(Long orderId, String orderNo) {
        if (orderId == null) {
            return;
        }
        MembershipBenefitUsage update = new MembershipBenefitUsage();
        update.setUsage_status_wsh(USAGE_USED);
        update.setUsed_at_wsh(LocalDateTime.now());
        // 仅把 locked 记录置为已使用
        membershipBenefitUsageMapper.update(update, new LambdaUpdateWrapper<MembershipBenefitUsage>()
                .eq(MembershipBenefitUsage::getBusiness_type_wsh, BUSINESS_PET_ORDER)
                .eq(MembershipBenefitUsage::getBusiness_id_wsh, String.valueOf(orderId))
                .eq(MembershipBenefitUsage::getUsage_status_wsh, USAGE_LOCKED));
    }

    /**
     * Releases the benefit lock for a cancelled or failed order.
     * Transitions the usage record to "released" so the discount can be reused.
     *
     * @param orderId the order ID
     */
    @Override
    @Transactional
    public void releaseForOrder(Long orderId) {
        if (orderId == null) {
            return;
        }
        MembershipBenefitUsage update = new MembershipBenefitUsage();
        update.setUsage_status_wsh(USAGE_RELEASED);
        // 取消订单时释放锁定与已用记录
        membershipBenefitUsageMapper.update(update, new LambdaUpdateWrapper<MembershipBenefitUsage>()
                .eq(MembershipBenefitUsage::getBusiness_type_wsh, BUSINESS_PET_ORDER)
                .eq(MembershipBenefitUsage::getBusiness_id_wsh, String.valueOf(orderId))
                .in(MembershipBenefitUsage::getUsage_status_wsh, List.of(USAGE_LOCKED, USAGE_USED)));
    }

    /**
     * Lists benefit usage records for the given user, newest first.
     *
     * @param userId the user ID
     * @return list of benefit usage records
     */
    @Override
    public List<MembershipBenefitUsage> listUsageByUser(Long userId) {
        return membershipBenefitUsageMapper.selectList(new LambdaQueryWrapper<MembershipBenefitUsage>()
                .eq(MembershipBenefitUsage::getUser_id_wsh, userId)
                .orderByDesc(MembershipBenefitUsage::getCreated_at_wsh)
                .orderByDesc(MembershipBenefitUsage::getId_wsh));
    }

    /**
     * Lists benefit usage records for admin with optional user filter.
     *
     * @param userId optional user ID filter; null returns all records
     * @return list of benefit usage records
     */
    @Override
    public List<MembershipBenefitUsage> listUsageForAdmin(Long userId) {
        return membershipBenefitUsageMapper.selectList(new LambdaQueryWrapper<MembershipBenefitUsage>()
                .eq(userId != null, MembershipBenefitUsage::getUser_id_wsh, userId)
                .orderByDesc(MembershipBenefitUsage::getCreated_at_wsh)
                .orderByDesc(MembershipBenefitUsage::getId_wsh));
    }

    /**
     * 【查询用户生效中的会员】
     *
     * 业务作用：查出用户当前处于 active 且未过期的会员记录。
     *
     * 调用场景：previewOrderDiscount 计算折扣前。
     *
     * 调用链：previewOrderDiscount() → findActiveMembership() → userMembershipMapper.selectOne()。
     *
     * 数据处理：按 user_id_wsh、status_wsh=active、expires_at_wsh 大于当前时间过滤，LIMIT 1。
     *
     * 业务规则：过期会员不参与折扣计算。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：无，查不到返回 null。
     *
     * 注意事项：未按到期日排序，依赖同一用户只存在一条 active 记录的前提。
     */
    private UserMembership findActiveMembership(Long userId) {
        return userMembershipMapper.selectOne(new LambdaQueryWrapper<UserMembership>()
                .eq(UserMembership::getUser_id_wsh, userId)
                .eq(UserMembership::getStatus_wsh, STATUS_ACTIVE)
                .gt(UserMembership::getExpires_at_wsh, LocalDateTime.now())
                .last("LIMIT 1"));
    }

    /**
     * 【按幂等键查询权益使用记录】
     *
     * 业务作用：根据幂等请求 ID 查询是否已存在权益使用记录。
     *
     * 调用场景：lockForOrder 判断该订单是否已锁定折扣。
     *
     * 调用链：lockForOrder() → findByRequestId() → membershipBenefitUsageMapper.selectOne()。
     *
     * 数据处理：按 request_id_wsh 精确匹配，LIMIT 1。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无，查不到返回 null。
     *
     * 注意事项：requestId 由 requestId(orderId) 生成，格式为 membership_discount:order:{orderId}。
     */
    private MembershipBenefitUsage findByRequestId(String requestId) {
        return membershipBenefitUsageMapper.selectOne(new LambdaQueryWrapper<MembershipBenefitUsage>()
                .eq(MembershipBenefitUsage::getRequest_id_wsh, requestId)
                .last("LIMIT 1"));
    }

    /**
     * 【判断是否含可用折扣】
     *
     * 业务作用：判断折扣 DTO 是否真的包含可以锁定的折扣金额。
     *
     * 调用场景：lockForOrder 的前置判断。
     *
     * 调用链：lockForOrder() → hasDiscount() → money()。
     *
     * 数据处理：要求 eligible_wsh 为 true、membership_id_wsh 非空、折扣金额大于 0 三者同时成立。
     *
     * 业务规则：无折扣时直接跳过锁定，不写库。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：入参为 null 时返回 false。
     */
    private boolean hasDiscount(MembershipDiscountDTO discount) {
        return discount != null
                && Boolean.TRUE.equals(discount.getEligible_wsh())
                && discount.getMembership_id_wsh() != null
                && money(discount.getMembership_discount_wsh()).compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 【解析折扣率】
     *
     * 业务作用：解析用户本次下单适用的折扣率，会员权益快照优先。
     *
     * 调用场景：previewOrderDiscount 计算折扣金额。
     *
     * 调用链：previewOrderDiscount() → resolveDiscountRate() → snapshotDecimal()。
     *
     * 数据处理：先读 benefit_snapshot_wsh 中的 discount_rate_wsh；没有则取套餐 discount_rate_wsh；都没有返回 1。
     *
     * 业务规则：快照优先，保证套餐调价后仍按下单时的权益计算。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：返回值未做 0~1 范围校验，由 normalizeRate 兜底修正。
     */
    private BigDecimal resolveDiscountRate(UserMembership membership, MemberPlan plan) {
        // 快照优先，套餐调价不影响老会员
        BigDecimal snapshotRate = snapshotDecimal(membership.getBenefit_snapshot_wsh(), "discount_rate_wsh");
        if (snapshotRate != null) {
            return snapshotRate;
        }
        return plan != null ? plan.getDiscount_rate_wsh() : BigDecimal.ONE;
    }

    /**
     * 【解析套餐名称】
     *
     * 业务作用：按快照、套餐表、套餐编码三级兜底取套餐名称。
     *
     * 调用场景：previewOrderDiscount 组装折扣 DTO。
     *
     * 调用链：previewOrderDiscount() → resolvePlanName() → snapshotText()。
     *
     * 数据处理：依次尝试快照 plan_name_wsh、套餐 name_wsh、会员记录 plan_code_wsh。
     *
     * 业务规则：保证名称尽量不为空。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：三级都取不到时返回的可能是套餐编码而非展示名。
     */
    private String resolvePlanName(UserMembership membership, MemberPlan plan) {
        String snapshotName = snapshotText(membership.getBenefit_snapshot_wsh(), "plan_name_wsh");
        if (snapshotName != null) {
            return snapshotName;
        }
        return plan != null ? plan.getName_wsh() : membership.getPlan_code_wsh();
    }

    /**
     * 【读快照数值字段】
     *
     * 业务作用：从 JSON 快照字符串中读取指定数值字段。
     *
     * 调用场景：resolveDiscountRate 读取折扣率。
     *
     * 调用链：resolveDiscountRate() → snapshotDecimal() → objectMapper.readTree()。
     *
     * 数据处理：快照为空或字段不是数值时返回 null。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：解析异常被吞掉，返回 null。
     *
     * 注意事项：字段名必须与写入快照时一致。
     */
    private BigDecimal snapshotDecimal(String snapshot, String field) {
        if (snapshot == null || snapshot.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(snapshot).get(field);
            return node != null && node.isNumber() ? node.decimalValue() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 【读快照文本字段】
     *
     * 业务作用：从 JSON 快照字符串中读取指定文本字段。
     *
     * 调用场景：resolvePlanName 读取套餐名称。
     *
     * 调用链：resolvePlanName() → snapshotText() → objectMapper.readTree()。
     *
     * 数据处理：快照为空或字段为 null 节点时返回 null。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：解析异常被吞掉，返回 null。
     *
     * 注意事项：数值类型字段也会被 asText() 转成字符串返回。
     */
    private String snapshotText(String snapshot, String field) {
        if (snapshot == null || snapshot.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(snapshot).get(field);
            return node != null && !node.isNull() ? node.asText() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 【生成折扣快照JSON】
     *
     * 业务作用：把折扣预览 DTO 序列化为 JSON 快照字符串。
     *
     * 调用场景：previewOrderDiscount 生成 snapshot_wsh。
     *
     * 调用链：previewOrderDiscount() → toSnapshot() → objectMapper.writeValueAsString()。
     *
     * 数据处理：写入会员 ID、套餐 ID/编码/名称、等级、折扣率、原价、折扣额、应付金额。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：序列化异常时返回 "{}"。
     *
     * 注意事项：该快照随后会被 lockForOrder 嵌套写入权益使用记录。
     */
    private String toSnapshot(MembershipDiscountDTO dto) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("membership_id_wsh", dto.getMembership_id_wsh());
        data.put("plan_id_wsh", dto.getPlan_id_wsh());
        data.put("plan_code_wsh", dto.getPlan_code_wsh());
        data.put("plan_name_wsh", dto.getPlan_name_wsh());
        data.put("level_wsh", dto.getLevel_wsh());
        data.put("discount_rate_wsh", dto.getDiscount_rate_wsh());
        data.put("base_amount_wsh", dto.getBase_amount_wsh());
        data.put("membership_discount_wsh", dto.getMembership_discount_wsh());
        data.put("final_amount_wsh", dto.getFinal_amount_wsh());
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * 【生成权益使用快照JSON】
     *
     * 业务作用：生成权益使用记录的快照内容，关联订单与折扣明细。
     *
     * 调用场景：lockForOrder 写入 usage_snapshot_wsh。
     *
     * 调用链：lockForOrder() → toUsageSnapshot() → objectMapper.writeValueAsString()。
     *
     * 数据处理：写入 order_id_wsh、order_no_wsh 以及嵌套的折扣快照字符串。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：序列化异常时返回 "{}"。
     *
     * 注意事项：折扣快照是作为字符串整体嵌入的，不是 JSON 子对象。
     */
    private String toUsageSnapshot(MembershipDiscountDTO discount, Long orderId, String orderNo) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("order_id_wsh", orderId);
        data.put("order_no_wsh", orderNo);
        data.put("benefit_wsh", discount.getSnapshot_wsh());
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * 【构造无折扣结果】
     *
     * 业务作用：生成表示「不享受折扣」的折扣 DTO。
     *
     * 调用场景：previewOrderDiscount 参数非法或用户无有效会员时。
     *
     * 调用链：previewOrderDiscount() → emptyResult()。
     *
     * 数据处理：eligible_wsh=false、折扣率 1、折扣额 0.00、应付金额等于原价、快照为 "{}"。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：调用方必须用 eligible_wsh 判断是否真的命中折扣。
     */
    private MembershipDiscountDTO emptyResult(BigDecimal amount) {
        MembershipDiscountDTO dto = new MembershipDiscountDTO();
        dto.setEligible_wsh(false);
        dto.setDiscount_rate_wsh(BigDecimal.ONE);
        dto.setBase_amount_wsh(amount);
        dto.setMembership_discount_wsh(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        dto.setFinal_amount_wsh(amount);
        dto.setSnapshot_wsh("{}");
        return dto;
    }

    /**
     * 【折扣率归一】
     *
     * 业务作用：把折扣率收敛到 0~1 区间并保留两位小数。
     *
     * 调用场景：previewOrderDiscount 使用折扣率前。
     *
     * 调用链：previewOrderDiscount() → normalizeRate()。
     *
     * 数据处理：null、小于等于 0、大于 1 一律回退为 1.00。
     *
     * 业务规则：非法折扣率等价于不打折。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：配置错误的折扣率会被静默修正，不会报错提示。
     */
    private BigDecimal normalizeRate(BigDecimal value) {
        // 非法折扣率一律按不打折处理
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 【金额归一】
     *
     * 业务作用：把金额统一为两位小数，null 视为 0。
     *
     * 调用场景：折扣计算与权益使用记录金额写入。
     *
     * 调用链：previewOrderDiscount() / lockForOrder() / hasDiscount() → money()。
     *
     * 数据处理：null 转 0，否则 HALF_UP 保留两位小数。
     *
     * 业务规则：统一四舍五入。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：不做正负校验。
     */
    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 【生成权益使用幂等键】
     *
     * 业务作用：按订单 ID 生成折扣使用记录的幂等键。
     *
     * 调用场景：lockForOrder 查询与写入时。
     *
     * 调用链：lockForOrder() → requestId() → findByRequestId()。
     *
     * 数据处理：拼接为 membership_discount:order:{orderId}。
     *
     * 业务规则：一个订单最多只允许一条折扣锁定记录。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：orderId 为 null 的场景由调用方提前返回，不会进入本方法。
     */
    private String requestId(Long orderId) {
        return "membership_discount:order:" + orderId;
    }
}
