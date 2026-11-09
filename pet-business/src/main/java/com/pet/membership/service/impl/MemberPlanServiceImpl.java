package com.pet.membership.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.common.BusinessException;
import com.pet.membership.dto.MemberPlanCreateRequestDTO;
import com.pet.membership.dto.MemberPlanDTO;
import com.pet.membership.dto.MemberPlanUpdateRequestDTO;
import com.pet.membership.entity.MemberPlan;
import com.pet.membership.entity.MembershipOrder;
import com.pet.membership.entity.UserMembership;
import com.pet.membership.mapper.MemberPlanMapper;
import com.pet.membership.mapper.MembershipOrderMapper;
import com.pet.membership.mapper.UserMembershipMapper;
import com.pet.membership.service.MemberPlanService;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 【业务模块】会员套餐管理（实现）
 * 业务作用：提供会员套餐的模板 CRUD 管理，含完整的字段校验和业务规则。
 */
@Service
public class MemberPlanServiceImpl implements MemberPlanService {
    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_ENABLED = 1;
    private static final Pattern PLAN_CODE_PATTERN = Pattern.compile("[A-Z0-9_]{2,50}");

    private final MemberPlanMapper memberPlanMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final MembershipOrderMapper membershipOrderMapper;
    private final ObjectMapper objectMapper;

    /**
     * 【构造方法】注入会员套餐相关依赖
     *
     * 业务作用：由 Spring 注入套餐、用户会员、会员订单 Mapper 与 ObjectMapper。
     *
     * 调用场景：Spring 容器启动创建该 Service Bean 时调用。
     *
     * 调用链：Spring 容器 → 本构造方法 → 各 final 字段赋值。
     *
     * 数据处理：仅字段赋值。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：依赖缺失时由 Spring 抛出 Bean 创建异常。
     *
     * 注意事项：依赖为 final 字段；ObjectMapper 用于 JSON 配置字段的合法性校验。
     */
    public MemberPlanServiceImpl(MemberPlanMapper memberPlanMapper,
                                 UserMembershipMapper userMembershipMapper,
                                 MembershipOrderMapper membershipOrderMapper,
                                 ObjectMapper objectMapper) {
        this.memberPlanMapper = memberPlanMapper;
        this.userMembershipMapper = userMembershipMapper;
        this.membershipOrderMapper = membershipOrderMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 【业务名称】查询套餐列表（实现）
     * 业务作用：查询会员套餐列表，支持状态和 activeOnly 筛选。
     * 调用场景：套餐列表展示。
     * 调用链：listPlans() → selectList() → toDTO()。
     * 数据处理：按状态过滤，按 sort_order、level、id 排序。
     * 业务规则：activeOnly=true 忽略 status 参数。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
    @Override
    public List<MemberPlanDTO> listPlans(Integer status, boolean activeOnly) {
        // activeOnly 时忽略显式状态过滤
        LambdaQueryWrapper<MemberPlan> wrapper = new LambdaQueryWrapper<MemberPlan>()
                .eq(activeOnly, MemberPlan::getStatus_wsh, STATUS_ENABLED)
                .eq(!activeOnly && status != null, MemberPlan::getStatus_wsh, status != null ? normalizeStatus(status) : null)
                .orderByAsc(MemberPlan::getSort_order_wsh)
                .orderByAsc(MemberPlan::getLevel_wsh)
                .orderByDesc(MemberPlan::getId_wsh);
        return memberPlanMapper.selectList(wrapper).stream().map(this::toDTO).toList();
    }

    /**
     * 【业务名称】查询套餐详情（实现）
     * 业务作用：根据ID查询单个套餐。
     * 调用场景：套餐详情展示。
     * 调用链：getPlan() → requirePlan() → toDTO()。
     * 数据处理：按ID查询。
     * 业务规则：套餐不存在抛异常。
     * 状态影响：无。
     * 异常情况：套餐不存在抛 BusinessException(404)。
     * 注意事项：无。
     */
    @Override
    public MemberPlanDTO getPlan(Long id) {
        return toDTO(requirePlan(id));
    }

    /**
     * 【业务名称】创建套餐（实现）
     * 业务作用：创建新的会员套餐，含完整的字段校验。
     * 调用场景：管理员新增套餐。
     * 调用链：createPlan() → normalizeCode() → ensureCodeAvailable() → 校验→ insert()。
     * 数据处理：校验各字段 → 插入记录。
     * 业务规则：code 唯一；名称必填；等级大于0；价格非负；时长大于0；折扣率0-1之间；JSON配置合法。
     * 状态影响：新增一条套餐记录。
     * 异常情况：code 重复抛 400；校验失败抛 400。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public MemberPlanDTO createPlan(MemberPlanCreateRequestDTO request) {
        if (request == null) {
            throw new BusinessException(400, "Member plan request cannot be empty");
        }
        MemberPlan plan = new MemberPlan();
        plan.setCode_wsh(normalizeCode(request.getCode_wsh()));
        ensureCodeAvailable(plan.getCode_wsh(), null);
        plan.setName_wsh(normalizeName(request.getName_wsh()));
        plan.setLevel_wsh(positiveInteger(request.getLevel_wsh(), "Plan level must be greater than zero"));
        plan.setPrice_wsh(money(request.getPrice_wsh(), "Plan price cannot be empty"));
        plan.setDuration_days_wsh(positiveInteger(request.getDuration_days_wsh(), "Plan duration must be greater than zero"));
        plan.setDiscount_rate_wsh(normalizeRate(request.getDiscount_rate_wsh()));
        plan.setMonthly_coupon_config_wsh(normalizeJson(request.getMonthly_coupon_config_wsh(), "Monthly coupon config must be valid JSON"));
        plan.setBenefit_config_wsh(normalizeJson(request.getBenefit_config_wsh(), "Benefit config must be valid JSON"));
        plan.setStatus_wsh(request.getStatus_wsh() == null ? STATUS_ENABLED : normalizeStatus(request.getStatus_wsh()));
        plan.setSort_order_wsh(request.getSort_order_wsh() == null ? 0 : request.getSort_order_wsh());
        plan.setRemark_wsh(trimToNull(request.getRemark_wsh()));
        // 并发下靠唯一索引兜底重复编码
        try {
            memberPlanMapper.insert(plan);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(400, "Member plan code already exists");
        }
        return toDTO(plan);
    }

    /**
     * 【业务名称】更新套餐（实现）
     * 业务作用：更新已有套餐的信息，仅更新非 null 字段。
     * 调用场景：管理员编辑套餐。
     * 调用链：updatePlan() → requirePlan() → 按需更新 → updateById()。
     * 数据处理：校验各字段 → 按需设置 → 更新。
     * 业务规则：code 变更时校验唯一性。
     * 状态影响：更新套餐记录。
     * 异常情况：code 重复抛 400。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public MemberPlanDTO updatePlan(Long id, MemberPlanUpdateRequestDTO request) {
        if (request == null) {
            throw new BusinessException(400, "Member plan request cannot be empty");
        }
        MemberPlan plan = requirePlan(id);
        // 改编码前重新校验唯一性
        if (request.getCode_wsh() != null) {
            String code = normalizeCode(request.getCode_wsh());
            ensureCodeAvailable(code, id);
            plan.setCode_wsh(code);
        }
        if (request.getName_wsh() != null) {
            plan.setName_wsh(normalizeName(request.getName_wsh()));
        }
        if (request.getLevel_wsh() != null) {
            plan.setLevel_wsh(positiveInteger(request.getLevel_wsh(), "Plan level must be greater than zero"));
        }
        if (request.getPrice_wsh() != null) {
            plan.setPrice_wsh(money(request.getPrice_wsh(), "Plan price cannot be empty"));
        }
        if (request.getDuration_days_wsh() != null) {
            plan.setDuration_days_wsh(positiveInteger(request.getDuration_days_wsh(), "Plan duration must be greater than zero"));
        }
        if (request.getDiscount_rate_wsh() != null) {
            plan.setDiscount_rate_wsh(normalizeRate(request.getDiscount_rate_wsh()));
        }
        if (request.getMonthly_coupon_config_wsh() != null) {
            plan.setMonthly_coupon_config_wsh(normalizeJson(request.getMonthly_coupon_config_wsh(), "Monthly coupon config must be valid JSON"));
        }
        if (request.getBenefit_config_wsh() != null) {
            plan.setBenefit_config_wsh(normalizeJson(request.getBenefit_config_wsh(), "Benefit config must be valid JSON"));
        }
        if (request.getStatus_wsh() != null) {
            plan.setStatus_wsh(normalizeStatus(request.getStatus_wsh()));
        }
        if (request.getSort_order_wsh() != null) {
            plan.setSort_order_wsh(request.getSort_order_wsh());
        }
        if (request.getRemark_wsh() != null) {
            plan.setRemark_wsh(trimToNull(request.getRemark_wsh()));
        }
        try {
            memberPlanMapper.updateById(plan);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(400, "Member plan code already exists");
        }
        return toDTO(plan);
    }

    /**
     * 【业务名称】更新套餐状态（实现）
     * 业务作用：启用或禁用会员套餐。
     * 调用场景：管理员上架/下架套餐。
     * 调用链：updateStatus() → requirePlan() → updateById()。
     * 数据处理：更新状态字段。
     * 业务规则：状态仅支持 0（禁用）和 1（启用）。
     * 状态影响：套餐启用/禁用状态变更。
     * 异常情况：不支持的 status 抛 400。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public MemberPlanDTO updateStatus(Long id, Integer status) {
        MemberPlan plan = requirePlan(id);
        plan.setStatus_wsh(normalizeStatus(status));
        memberPlanMapper.updateById(plan);
        return toDTO(plan);
    }

    /**
     * 【业务名称】删除套餐（实现）
     * 业务作用：删除会员套餐，有关联记录时禁止删除。
     * 调用场景：管理员删除套餐。
     * 调用链：deletePlan() → requirePlan() → 检查关联 → deleteById()。
     * 数据处理：检查会员和订单关联表是否存在该套餐的记录。
     * 业务规则：有关联时抛出异常，应改为禁用。
     * 状态影响：物理删除套餐记录。
     * 异常情况：有关联记录抛 BusinessException(400)。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public void deletePlan(Long id) {
        requirePlan(id);
        // 有会员或订单引用时禁止删除
        Long membershipCount = userMembershipMapper.selectCount(
                new LambdaQueryWrapper<UserMembership>().eq(UserMembership::getPlan_id_wsh, id));
        if (membershipCount != null && membershipCount > 0) {
            throw new BusinessException(400, "Member plan has membership history; disable it instead");
        }
        Long orderCount = membershipOrderMapper.selectCount(
                new LambdaQueryWrapper<MembershipOrder>().eq(MembershipOrder::getPlan_id_wsh, id));
        if (orderCount != null && orderCount > 0) {
            throw new BusinessException(400, "Member plan has membership history; disable it instead");
        }
        memberPlanMapper.deleteById(id);
    }

    /**
     * 【业务名称】套餐实体转DTO（实现）
     * 业务作用：将套餐实体转换为DTO。
     * 调用场景：内部转换。
     * 调用链：toDTO()。
     * 数据处理：字段拷贝。
     * 业务规则：入参为null时返回null。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
    @Override
    public MemberPlanDTO toDTO(MemberPlan plan) {
        if (plan == null) {
            return null;
        }
        MemberPlanDTO dto = new MemberPlanDTO();
        BeanUtils.copyProperties(plan, dto);
        return dto;
    }

    /**
     * 【加载套餐】按ID取套餐实体并在缺失时抛错
     *
     * 业务作用：本类内部统一的套餐加载入口，保证后续逻辑拿到的实体一定存在。
     *
     * 调用场景：getPlan、updatePlan、updateStatus、deletePlan 中调用。
     *
     * 调用链：上述方法 → 本方法 → memberPlanMapper.selectById。
     *
     * 数据处理：输入 id → 输出 MemberPlan 实体。
     *
     * 业务规则：id 不能为空；套餐必须存在。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：id 为 null 抛 BusinessException(400)；套餐不存在抛 BusinessException(404)。
     *
     * 注意事项：无。
     */
    private MemberPlan requirePlan(Long id) {
        if (id == null) {
            throw new BusinessException(400, "Member plan id cannot be empty");
        }
        MemberPlan plan = memberPlanMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException(404, "Member plan does not exist");
        }
        return plan;
    }

    /**
     * 【套餐编码唯一性校验】确认 code 未被其它套餐占用
     *
     * 业务作用：创建与更新时防止出现重复的套餐编码。
     *
     * 调用场景：createPlan 与 updatePlan 在设置 code 后调用。
     *
     * 调用链：createPlan / updatePlan → 本方法 → memberPlanMapper.selectCount。
     *
     * 数据处理：按 code_wsh 精确匹配计数，excludeId 非空时排除该ID自身。
     *
     * 业务规则：计数大于 0 即视为重复。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：code 已存在抛 BusinessException(400)。
     *
     * 注意事项：仅在应用层校验，并发下仍可能重复，因此 createPlan/updatePlan 同时捕获 DuplicateKeyException 兜底，依赖数据库唯一索引。
     */
    private void ensureCodeAvailable(String code, Long excludeId) {
        Long count = memberPlanMapper.selectCount(new LambdaQueryWrapper<MemberPlan>()
                .eq(MemberPlan::getCode_wsh, code)
                .ne(excludeId != null, MemberPlan::getId_wsh, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(400, "Member plan code already exists");
        }
    }

    /**
     * 【规范化套餐编码】去空格转大写并校验格式
     *
     * 业务作用：统一套餐编码格式，避免大小写与空白造成重复。
     *
     * 调用场景：createPlan 与 updatePlan 设置 code_wsh 前调用。
     *
     * 调用链：createPlan / updatePlan → 本方法 → PLAN_CODE_PATTERN.matcher。
     *
     * 数据处理：输入 code → trim + toUpperCase(Locale.ROOT) → 输出规范化编码。
     *
     * 业务规则：不能为空；必须匹配 2-50 位大写字母、数字或下划线。
     *
     * 状态影响：无。
     *
     * 异常情况：为空或格式不符抛 BusinessException(400)。
     *
     * 注意事项：调用方传入的小写编码会被静默转成大写，前端回显需使用返回值。
     */
    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(400, "Member plan code cannot be empty");
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (!PLAN_CODE_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(400, "Member plan code must use 2-50 uppercase letters, numbers, or underscores");
        }
        return normalized;
    }

    /**
     * 【规范化套餐名称】去空格并校验长度
     *
     * 业务作用：保证套餐名称非空且不超过字段长度上限。
     *
     * 调用场景：createPlan 与 updatePlan 设置 name_wsh 前调用。
     *
     * 调用链：createPlan / updatePlan → 本方法。
     *
     * 数据处理：输入 name → trim → 输出。
     *
     * 业务规则：不能为空；长度不得超过 100。
     *
     * 状态影响：无。
     *
     * 异常情况：为空或超长抛 BusinessException(400)。
     *
     * 注意事项：长度按 trim 后的字符数计算，与数据库列长度需保持一致。
     */
    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(400, "Member plan name cannot be empty");
        }
        String normalized = name.trim();
        if (normalized.length() > 100) {
            throw new BusinessException(400, "Member plan name cannot exceed 100 characters");
        }
        return normalized;
    }

    /**
     * 【正整数校验】确认数值非空且大于 0
     *
     * 业务作用：校验套餐等级与有效天数等必须为正的整数字段。
     *
     * 调用场景：createPlan 与 updatePlan 校验 level_wsh、duration_days_wsh 时调用。
     *
     * 调用链：createPlan / updatePlan → 本方法。
     *
     * 数据处理：输入 value 与错误文案 → 输出原值。
     *
     * 业务规则：null 或 <= 0 均不合法。
     *
     * 状态影响：无。
     *
     * 异常情况：不合法时抛 BusinessException(400)，文案由调用方传入。
     *
     * 注意事项：错误文案由调用方提供，同一方法在不同字段上会给出不同提示。
     */
    private Integer positiveInteger(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new BusinessException(400, message);
        }
        return value;
    }

    /**
     * 【金额校验与规整】保留两位小数并禁止负数
     *
     * 业务作用：统一套餐价格的精度与取值范围。
     *
     * 调用场景：createPlan 与 updatePlan 设置 price_wsh 前调用。
     *
     * 调用链：createPlan / updatePlan → 本方法。
     *
     * 数据处理：输入 BigDecimal → setScale(2, HALF_UP) → 输出。
     *
     * 业务规则：不能为 null；四舍五入保留两位；不得小于 0（允许 0，可用于免费套餐）。
     *
     * 状态影响：无。
     *
     * 异常情况：null 时抛 BusinessException(400)（文案由调用方传入）；负数抛 BusinessException(400, "Amount cannot be negative")。
     *
     * 注意事项：四舍五入会静默改变入参精度，如 9.999 会存为 10.00。
     */
    private BigDecimal money(BigDecimal value, String nullMessage) {
        if (value == null) {
            throw new BusinessException(400, nullMessage);
        }
        BigDecimal money = value.setScale(2, RoundingMode.HALF_UP);
        if (money.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(400, "Amount cannot be negative");
        }
        return money;
    }

    /**
     * 【折扣率规范化】保留两位小数并限定在 (0,1] 区间
     *
     * 业务作用：统一套餐折扣率精度并限制取值范围。
     *
     * 调用场景：createPlan 与 updatePlan 设置 discount_rate_wsh 时调用。
     *
     * 调用链：createPlan / updatePlan → 本方法。
     *
     * 数据处理：null 时取 BigDecimal.ONE，否则 setScale(2, HALF_UP) → 输出。
     *
     * 业务规则：必须大于 0 且小于等于 1；1 表示不打折。
     *
     * 状态影响：无。
     *
     * 异常情况：越界抛 BusinessException(400)。
     *
     * 注意事项：null 会默认按 1（无折扣）处理而非报错；四舍五入后可能由 0.004 变为 0.00 从而触发越界异常。
     */
    private BigDecimal normalizeRate(BigDecimal rate) {
        BigDecimal value = rate == null ? BigDecimal.ONE : rate.setScale(2, RoundingMode.HALF_UP); // 空值按不打折处理
        if (value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException(400, "Discount rate must be greater than 0 and less than or equal to 1");
        }
        return value;
    }

    /**
     * 【状态规范化】只接受 0（禁用）与 1（启用）
     *
     * 业务作用：限制套餐状态取值范围。
     *
     * 调用场景：listPlans、createPlan、updatePlan、updateStatus 中调用。
     *
     * 调用链：listPlans / createPlan / updatePlan / updateStatus → 本方法。
     *
     * 数据处理：输入 status → 输出原值。
     *
     * 业务规则：不能为 null；只允许 STATUS_DISABLED(0) 与 STATUS_ENABLED(1)。
     *
     * 状态影响：无。
     *
     * 异常情况：null 抛 BusinessException(400)；非法值抛 BusinessException(400)。
     *
     * 注意事项：listPlans 中仅在非 activeOnly 且 status 非 null 时才调用，避免 null 触发异常。
     */
    private Integer normalizeStatus(Integer status) {
        if (status == null) {
            throw new BusinessException(400, "Member plan status cannot be empty");
        }
        if (status != STATUS_ENABLED && status != STATUS_DISABLED) {
            throw new BusinessException(400, "Unsupported member plan status");
        }
        return status;
    }

    /**
     * 【JSON配置校验】确认字符串是合法 JSON 后原样返回
     *
     * 业务作用：校验每月优惠券配置与权益配置字段的 JSON 合法性。
     *
     * 调用场景：createPlan 与 updatePlan 设置 monthly_coupon_config_wsh、benefit_config_wsh 时调用。
     *
     * 调用链：createPlan / updatePlan → 本方法 → trimToNull / objectMapper.readTree。
     *
     * 数据处理：空白值返回 null；否则用 ObjectMapper 解析一遍，成功则返回原文本。
     *
     * 业务规则：空串与全空白视为未配置，存 null；解析失败视为非法。
     *
     * 状态影响：无。
     *
     * 异常情况：JSON 非法抛 BusinessException(400)，文案由调用方传入。
     *
     * 注意事项：只校验不规范化，存储的是调用方原始文本（含多余空格），相同语义的不同写法会存成不同字符串。
     */
    private String normalizeJson(String value, String message) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        try {
            // 解析一遍确认 JSON 合法
            objectMapper.readTree(text);
            return text;
        } catch (Exception e) {
            throw new BusinessException(400, message);
        }
    }

    /**
     * 【去空格转null】空白字符串统一转为 null
     *
     * 业务作用：避免把空串写入备注等可选字段。
     *
     * 调用场景：createPlan、updatePlan 设置 remark_wsh 时调用，以及 normalizeJson 内部调用。
     *
     * 调用链：createPlan / updatePlan / normalizeJson → 本方法。
     *
     * 数据处理：输入字符串 → trim 后为空则返回 null，否则返回 trim 结果。
     *
     * 业务规则：null 或全空白返回 null。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：返回值已去首尾空格，原格式不保留。
     */
    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
