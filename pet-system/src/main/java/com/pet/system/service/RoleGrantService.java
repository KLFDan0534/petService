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

    /**
     * 业务作用：为指定用户授予角色，若已逻辑删除则恢复而非新增
     * 业务规则：角色编码不区分大小写自动转大写；已激活的角色不重复授予；优先恢复逻辑删除的记录而非创建新记录
     * 异常情况：userId为空 → 400 BusinessException；roleCode为空 → 400 BusinessException；角色编码不存在 → BusinessException
     * 注意事项：事务保证操作原子性；具有幂等性——重复授予不会产生多条记录
     */
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
        // 已持有该角色则直接返回，保证幂等
        Long exists = userRoleMapper.countActiveUserRole(userId, role.getId_wsh());
        if (exists != null && exists > 0) {
            return;
        }
        // 优先恢复被逻辑删除的关联记录
        if (userRoleMapper.restoreUserRole(userId, role.getId_wsh()) > 0) {
            return;
        }
        UserRole userRole = new UserRole();
        userRole.setUser_id_wsh(userId);
        userRole.setRole_id_wsh(role.getId_wsh());
        userRoleMapper.insert(userRole);
    }

    /**
     * 业务作用：移除指定用户的某个角色（逻辑删除关联记录）
     * 业务规则：角色编码不区分大小写自动转大写；使用MyBatis-Plus逻辑删除而非物理删除
     * 异常情况：userId为空 → 400 BusinessException；roleCode为空 → 400 BusinessException；角色编码不存在 → BusinessException
     * 注意事项：逻辑删除的记录可通过grantRoleToUser的restoreUserRole恢复
     */
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
        // 逻辑删除关联，保留记录便于后续恢复
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUser_id_wsh, userId)
                .eq(UserRole::getRole_id_wsh, role.getId_wsh()));
    }

    /**
     * 业务作用：去掉首尾空白并统一转为大写，用于编码比对。
     * 数据处理：使用 Locale.ROOT 避免地区差异导致的大小写问题。
     * 业务规则：入参为 null 时会抛空指针，调用前需判空。
     * 异常情况：roleCode 为 null → NullPointerException。
     * 注意事项：只做格式规范化，不校验编码是否存在。
     */
    private String normalizeRoleCode(String roleCode) {
        return roleCode.trim().toUpperCase(Locale.ROOT);
    }
}
