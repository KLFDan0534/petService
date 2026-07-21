package com.pet.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.system.entity.Role;
import com.pet.system.entity.UserRole;
import com.pet.system.mapper.RoleMapper;
import com.pet.system.mapper.UserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
public class RoleGrantService {

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public RoleGrantService(RoleMapper roleMapper, UserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Transactional
    public void grantRoleToUser(Long userId, String roleCode) {
        if (userId == null) {
            throw new BusinessException(400, "user_id_wsh不能为空");
        }
        if (!StringUtils.hasText(roleCode)) {
            throw new BusinessException(400, "code_wsh不能为空");
        }
        String normalizedCode = normalizeRoleCode(roleCode);
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getCode_wsh, normalizedCode)
                .last("LIMIT 1"));
        if (role == null) {
            throw new BusinessException("角色不存在: " + normalizedCode);
        }
        Long exists = userRoleMapper.countActiveUserRole(userId, role.getId_wsh());
        if (exists != null && exists > 0) {
            return;
        }
        if (userRoleMapper.restoreUserRole(userId, role.getId_wsh()) > 0) {
            return;
        }
        UserRole userRole = new UserRole();
        userRole.setUser_id_wsh(userId);
        userRole.setRole_id_wsh(role.getId_wsh());
        userRoleMapper.insert(userRole);
    }

    @Transactional
    public void revokeRoleFromUser(Long userId, String roleCode) {
        if (userId == null) {
            throw new BusinessException(400, "user_id_wsh cannot be null");
        }
        if (!StringUtils.hasText(roleCode)) {
            throw new BusinessException(400, "code_wsh cannot be blank");
        }
        String normalizedCode = normalizeRoleCode(roleCode);
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getCode_wsh, normalizedCode)
                .last("LIMIT 1"));
        if (role == null) {
            throw new BusinessException("role not found: " + normalizedCode);
        }
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUser_id_wsh, userId)
                .eq(UserRole::getRole_id_wsh, role.getId_wsh()));
    }

    private String normalizeRoleCode(String roleCode) {
        return roleCode.trim().toUpperCase(Locale.ROOT);
    }
}
