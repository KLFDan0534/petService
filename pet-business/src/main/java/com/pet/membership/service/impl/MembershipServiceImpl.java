package com.pet.membership.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.common.BusinessException;
import com.pet.membership.dto.UserMembershipDTO;
import com.pet.membership.entity.MemberPlan;
import com.pet.membership.entity.UserMembership;
import com.pet.membership.mapper.MemberPlanMapper;
import com.pet.membership.mapper.UserMembershipMapper;
import com.pet.membership.service.MembershipService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/**
 * 【业务模块】会员管理（实现）
 * 业务作用：提供用户会员状态查询、管理端列表查询和会员到期自动过期处理。
 * 会员到期自动刷新状态，保证数据实时准确。
 */
@Service
public class MembershipServiceImpl implements MembershipService {
    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_INACTIVE = "inactive";
    private static final String STATUS_EXPIRED = "expired";

    private final UserMembershipMapper userMembershipMapper;
    private final MemberPlanMapper memberPlanMapper;
    private final ObjectMapper objectMapper;

    /**
     * 【会员服务实现的构造注入】
     *
     * 业务作用：注入会员记录、套餐 Mapper 与 JSON 解析器。
     *
     * 调用场景：Spring 容器创建 MembershipServiceImpl 时。
     *
     * 调用链：Spring 容器 → 本构造方法 → 赋值 3 个 final 成员。
     *
     * 数据处理：仅保存依赖引用。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：无。
     */
    public MembershipServiceImpl(UserMembershipMapper userMembershipMapper,
                                 MemberPlanMapper memberPlanMapper,
                                 ObjectMapper objectMapper) {
        this.userMembershipMapper = userMembershipMapper;
        this.memberPlanMapper = memberPlanMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 【业务名称】获取当前会员信息（实现）
     * 业务作用：获取指定用户的当前会员信息。
     * 调用场景：用户查看会员状态。
     * 调用链：getCurrentMembership() → selectOne() → refreshIfExpired() → toDTO()。
     * 数据处理：查询一条会员记录 → 过期自动刷新 → 转为 DTO。
     * 业务规则：无会员时返回默认 inactive DTO；过期自动刷新。
     * 状态影响：会员过期时自动更新数据库状态。
     * 异常情况：用户ID为空抛 BusinessException(401)。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Override
    @Transactional
    public UserMembershipDTO getCurrentMembership(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        UserMembership membership = userMembershipMapper.selectOne(new LambdaQueryWrapper<UserMembership>()
                .eq(UserMembership::getUser_id_wsh, userId)
                .last("LIMIT 1"));
        // 无会员记录返回默认未开通状态
        if (membership == null) {
            UserMembershipDTO dto = new UserMembershipDTO();
            dto.setUser_id_wsh(userId);
            dto.setStatus_wsh(STATUS_INACTIVE);
            dto.setActive_wsh(false);
            dto.setRemaining_days_wsh(0L);
            dto.setDiscount_rate_wsh(BigDecimal.ONE);
            return dto;
        }
        refreshIfExpired(membership);
        return toDTO(membership);
    }

    /**
     * 【业务名称】管理端查询会员列表（实现）
     * 业务作用：按状态筛选查询会员列表，查询前自动过期。
     * 调用场景：后台会员管理。
     * 调用链：listMembershipsForAdmin() → expireMemberships() → selectList() → toDTO()。
     * 数据处理：过期处理后按状态筛选查询。
     * 业务规则：查询前自动过期保证数据准确。
     * 状态影响：查询前触发过期处理。
     * 异常情况：不支持的状态抛 BusinessException(400)。
     * 注意事项：无。
     */
    @Override
    @Transactional
    public List<UserMembershipDTO> listMembershipsForAdmin(String status) {
        // 查询前先批量刷新过期状态
        expireMemberships();
        String normalizedStatus = normalizeStatusOrNull(status);
        return userMembershipMapper.selectList(new LambdaQueryWrapper<UserMembership>()
                        .eq(normalizedStatus != null, UserMembership::getStatus_wsh, normalizedStatus)
                        .orderByDesc(UserMembership::getUpdated_at_wsh)
                        .orderByDesc(UserMembership::getId_wsh))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * 【业务名称】批量过期会员（实现）
     * 业务作用：将所有已过期的活跃会员标记为 expired。
     * 调用场景：定时任务或管理端查询前自动调用。
     * 调用链：expireMemberships() → update()。
     * 数据处理：批量更新 expires_at < now 且 status=active 的记录。
     * 业务规则：仅处理活跃已过期会员。
     * 状态影响：符合条件的会员状态 active → expired。
     * 异常情况：无。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Override
    @Transactional
    public int expireMemberships() {
        UserMembership update = new UserMembership();
        update.setStatus_wsh(STATUS_EXPIRED);
        return userMembershipMapper.update(update, new LambdaUpdateWrapper<UserMembership>()
                .eq(UserMembership::getStatus_wsh, STATUS_ACTIVE)
                .le(UserMembership::getExpires_at_wsh, LocalDateTime.now()));
    }

    /**
     * 【业务名称】会员实体转DTO（实现）
     * 业务作用：将会员实体转换为 DTO，丰富套餐名称、折扣率、有效状态和剩余天数。
     * 调用场景：内部转换。
     * 调用链：toDTO()。
     * 数据处理：字段拷贝 → 关联套餐 → 解析快照 → 计算有效状态和剩余天数。
     * 业务规则：折扣率为 null 时默认为 1。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
    @Override
    public UserMembershipDTO toDTO(UserMembership membership) {
        if (membership == null) {
            return null;
        }
        UserMembershipDTO dto = new UserMembershipDTO();
        BeanUtils.copyProperties(membership, dto);
        MemberPlan plan = membership.getPlan_id_wsh() == null ? null : memberPlanMapper.selectById(membership.getPlan_id_wsh());
        if (plan != null) {
            dto.setPlan_name_wsh(plan.getName_wsh());
            dto.setDiscount_rate_wsh(plan.getDiscount_rate_wsh());
        }
        applySnapshot(dto, membership.getBenefit_snapshot_wsh());
        // 状态为 active 且未过期才算有效会员
        boolean active = STATUS_ACTIVE.equals(membership.getStatus_wsh())
                && membership.getExpires_at_wsh() != null
                && membership.getExpires_at_wsh().isAfter(LocalDateTime.now());
        dto.setActive_wsh(active);
        dto.setRemaining_days_wsh(active
                ? Math.max(0L, ChronoUnit.DAYS.between(LocalDateTime.now().toLocalDate(), membership.getExpires_at_wsh().toLocalDate()))
                : 0L);
        if (dto.getDiscount_rate_wsh() == null) {
            dto.setDiscount_rate_wsh(BigDecimal.ONE);
        }
        return dto;
    }

    /**
     * 【单个会员的过期刷新】
     *
     * 业务作用：发现会员已过期时，就地把数据库状态改为 expired 并同步内存对象。
     *
     * 调用场景：getCurrentMembership() 查询单条会员后调用。
     *
     * 调用链：getCurrentMembership() → refreshIfExpired() → userMembershipMapper.update()。
     *
     * 数据处理：以 id + status=active 为条件更新 status_wsh 为 expired，随后修改入参对象的 status_wsh。
     *
     * 业务规则：id 为空、状态非 active、到期时间为空或尚未到期时直接返回，不做更新。
     *
     * 状态影响：更新 user_membership 表状态字段。
     *
     * 异常情况：无。
     *
     * 注意事项：带 status=active 条件是为了防止并发重复更新；只改状态不改 expires_at。
     */
    private void refreshIfExpired(UserMembership membership) {
        if (membership.getId_wsh() == null
                || !STATUS_ACTIVE.equals(membership.getStatus_wsh())
                || membership.getExpires_at_wsh() == null
                || membership.getExpires_at_wsh().isAfter(LocalDateTime.now())) {
            return;
        }
        // 带 active 条件更新，避免并发重复置过期
        UserMembership update = new UserMembership();
        update.setStatus_wsh(STATUS_EXPIRED);
        userMembershipMapper.update(update, new LambdaUpdateWrapper<UserMembership>()
                .eq(UserMembership::getId_wsh, membership.getId_wsh())
                .eq(UserMembership::getStatus_wsh, STATUS_ACTIVE));
        membership.setStatus_wsh(STATUS_EXPIRED);
    }

    /**
     * 【应用会员权益快照】
     *
     * 业务作用：把会员记录中的 JSON 权益快照解析后覆盖 DTO 的套餐名与折扣率。
     *
     * 调用场景：toDTO() 组装会员 DTO 时。
     *
     * 调用链：toDTO() → applySnapshot() → objectMapper.readTree()。
     *
     * 数据处理：读取 plan_name_wsh 与 discount_rate_wsh 两个字段，数值型折扣率取 decimalValue。
     *
     * 业务规则：快照为空直接返回；字段缺失或类型不符则跳过。
     *
     * 状态影响：无。
     *
     * 异常情况：JSON 解析异常被静默吞掉，不抛出也不记录日志。
     *
     * 注意事项：快照值会覆盖按 plan_id 查出的套餐信息，属于「下单时锁定权益」的设计。
     */
    private void applySnapshot(UserMembershipDTO dto, String snapshot) {
        if (snapshot == null || snapshot.isBlank()) {
            return;
        }
        try {
            // 快照值覆盖套餐信息，锁定下单时权益
            JsonNode node = objectMapper.readTree(snapshot);
            JsonNode name = node.get("plan_name_wsh");
            if (name != null && !name.isNull()) {
                dto.setPlan_name_wsh(name.asText());
            }
            JsonNode discountRate = node.get("discount_rate_wsh");
            if (discountRate != null && discountRate.isNumber()) {
                dto.setDiscount_rate_wsh(discountRate.decimalValue());
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 【规范化会员状态入参】
     *
     * 业务作用：把查询状态字符串 trim 并转小写，同时校验是否为合法状态。
     *
     * 调用场景：listMembershipsForAdmin() 处理状态筛选参数。
     *
     * 调用链：listMembershipsForAdmin() → normalizeStatusOrNull()。
     *
     * 数据处理：空值返回 null 表示不过滤；否则转小写并比对白名单 inactive/active/expired/cancelled。
     *
     * 业务规则：仅四个状态值合法。
     *
     * 状态影响：无。
     *
     * 异常情况：非法状态抛 BusinessException(400, "不支持的会员状态")。
     *
     * 注意事项：白名单中的 cancelled 没有对应的常量定义，属于硬编码字符串。
     */
    private String normalizeStatusOrNull(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String value = status.trim().toLowerCase(Locale.ROOT);
        // 仅接受白名单内的状态值
        if (!List.of(STATUS_INACTIVE, STATUS_ACTIVE, STATUS_EXPIRED, "cancelled").contains(value)) {
            throw new BusinessException(400, "不支持的会员状态");
        }
        return value;
    }
}
