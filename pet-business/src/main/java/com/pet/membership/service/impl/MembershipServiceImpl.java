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

    private void refreshIfExpired(UserMembership membership) {
        if (membership.getId_wsh() == null
                || !STATUS_ACTIVE.equals(membership.getStatus_wsh())
                || membership.getExpires_at_wsh() == null
                || membership.getExpires_at_wsh().isAfter(LocalDateTime.now())) {
            return;
        }
        UserMembership update = new UserMembership();
        update.setStatus_wsh(STATUS_EXPIRED);
        userMembershipMapper.update(update, new LambdaUpdateWrapper<UserMembership>()
                .eq(UserMembership::getId_wsh, membership.getId_wsh())
                .eq(UserMembership::getStatus_wsh, STATUS_ACTIVE));
        membership.setStatus_wsh(STATUS_EXPIRED);
    }

    private void applySnapshot(UserMembershipDTO dto, String snapshot) {
        if (snapshot == null || snapshot.isBlank()) {
            return;
        }
        try {
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

    private String normalizeStatusOrNull(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String value = status.trim().toLowerCase(Locale.ROOT);
        if (!List.of(STATUS_INACTIVE, STATUS_ACTIVE, STATUS_EXPIRED, "cancelled").contains(value)) {
            throw new BusinessException(400, "不支持的会员状态");
        }
        return value;
    }
}
