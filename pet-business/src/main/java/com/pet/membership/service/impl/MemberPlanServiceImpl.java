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

    private void ensureCodeAvailable(String code, Long excludeId) {
        Long count = memberPlanMapper.selectCount(new LambdaQueryWrapper<MemberPlan>()
                .eq(MemberPlan::getCode_wsh, code)
                .ne(excludeId != null, MemberPlan::getId_wsh, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(400, "Member plan code already exists");
        }
    }

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

    private Integer positiveInteger(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new BusinessException(400, message);
        }
        return value;
    }

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

    private BigDecimal normalizeRate(BigDecimal rate) {
        BigDecimal value = rate == null ? BigDecimal.ONE : rate.setScale(2, RoundingMode.HALF_UP);
        if (value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException(400, "Discount rate must be greater than 0 and less than or equal to 1");
        }
        return value;
    }

    private Integer normalizeStatus(Integer status) {
        if (status == null) {
            throw new BusinessException(400, "Member plan status cannot be empty");
        }
        if (status != STATUS_ENABLED && status != STATUS_DISABLED) {
            throw new BusinessException(400, "Unsupported member plan status");
        }
        return status;
    }

    private String normalizeJson(String value, String message) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        try {
            objectMapper.readTree(text);
            return text;
        } catch (Exception e) {
            throw new BusinessException(400, message);
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
