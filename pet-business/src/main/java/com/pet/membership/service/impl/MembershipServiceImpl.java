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

    @Override
    @Transactional
    public int expireMemberships() {
        UserMembership update = new UserMembership();
        update.setStatus_wsh(STATUS_EXPIRED);
        return userMembershipMapper.update(update, new LambdaUpdateWrapper<UserMembership>()
                .eq(UserMembership::getStatus_wsh, STATUS_ACTIVE)
                .le(UserMembership::getExpires_at_wsh, LocalDateTime.now()));
    }

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
