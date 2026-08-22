package com.pet.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.system.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.pet.system.dto.RoleCountDTO;
import java.util.List;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
    /**
     * 【按角色编码查询有效用户ID列表】
     *
     * 业务作用：查询系统中拥有指定角色编码的全部有效（未删除、未封禁）用户ID，用于按角色随机分配客服等场景
     *
     * 调用场景：智能客服转人工时随机分配一名在线客服
     *
     * 调用链：ChatServiceImpl.assignCustomerServiceAgent() ↓ selectActiveUserIdsByRoleCode() → SQL联表
     *
     * 数据处理：JOIN role_wsh + user_role_wsh + user_wsh → 过滤角色编码匹配且均未逻辑删除、用户未封禁 → 返回用户ID列表
     *
     * 业务规则：仅统计deleted_wsh=0的有效记录；用户status_wsh=1（未封禁）
     *
     * 状态影响：无
     */
    @Select("SELECT ur.user_id_wsh FROM user_role_wsh ur " +
            "JOIN role_wsh r ON ur.role_id_wsh = r.id_wsh " +
            "JOIN user_wsh u ON ur.user_id_wsh = u.id_wsh " +
            "WHERE r.code_wsh = #{roleCode} " +
            "AND ur.deleted_wsh = 0 AND r.deleted_wsh = 0 AND u.deleted_wsh = 0 AND u.status_wsh = 1")
    List<Long> selectActiveUserIdsByRoleCode(String roleCode);

    /**
     * 【按角色统计用户数量】
     *
     * 业务作用：统计系统中每个角色下关联的有效用户数量，用于角色管理仪表盘
     *
     * 调用场景：管理后台角色管理页面展示各角色人数
     *
     * 调用链：AdminController ↓ countUsersByRole() → SQL联表 + GROUP BY → List<RoleCountDTO>
     *
     * 数据处理：JOIN user_role_wsh + role_wsh → 过滤deleted_wsh=0 → GROUP BY roleCode → SELECT roleCode + COUNT(*)
     *
     * 业务规则：仅统计未逻辑删除的关联记录；一个用户拥有多个角色会分别在每个角色中被计入
     *
     * 状态影响：无
     */
    @Select("SELECT r.code_wsh AS roleCode, COUNT(*) AS cnt FROM user_role_wsh ur JOIN role_wsh r ON ur.role_id_wsh = r.id_wsh WHERE ur.deleted_wsh = 0 GROUP BY r.code_wsh")
    List<RoleCountDTO> countUsersByRole();

    /**
     * 【统计用户是否已拥有某角色】
     *
     * 业务作用：统计用户是否已经拥有指定角色的有效（未逻辑删除）关联
     *
     * 调用场景：授予角色前判读不能重复授予
     *
     * 调用链：RoleGrantService.grantRoleToUser ↓ countActiveUserRole() → SQL COUNT
     *
     * 数据处理：userId + roleId → SELECT COUNT(*) → WHERE匹配userId+roleId且deleted=0 → 返回计数
     *
     * 业务规则：仅统计deleted_wsh=0的有效记录；返回值>0表示用户已经拥有该角色
     *
     * 状态影响：无
     */
    @Select("SELECT COUNT(*) FROM user_role_wsh WHERE user_id_wsh = #{userId} AND role_id_wsh = #{roleId} AND deleted_wsh = 0")
    Long countActiveUserRole(Long userId, Long roleId);

    /**
     * 【恢复用户角色关联（软删除恢复）】
     *
     * 业务作用：将逻辑删除的用户-角色关联记录恢复为有效状态
     *
     * 调用场景：授予角色时优先恢复已有记录而非创建新记录
     *
     * 调用链：RoleGrantService.grantRoleToUser ↓ restoreUserRole() → SQL UPDATE SET deleted=0
     *
     * 数据处理：userId + roleId → UPDATE SET deleted_wsh=0 → WHERE userId+roleId匹配 → 返回影响行数
     *
     * 业务规则：不限制deleted_wsh状态（可恢复已删除的记录）；返回值为0表示没有旧记录可恢复，调用方需insert新记录
     *
     * 状态影响：将user_role_wsh表中deleted_wsh设为0（恢复有效状态）
     */
    @Update("UPDATE user_role_wsh SET deleted_wsh = 0 WHERE user_id_wsh = #{userId} AND role_id_wsh = #{roleId}")
    int restoreUserRole(Long userId, Long roleId);

    /**
     * 【删除用户除指定角色外的所有角色关联】
     *
     * 业务作用：批量逻辑删除用户的所有角色关联（可排除一个角色不删）
     *
     * 调用场景：角色变更时重置用户角色，仅保留指定的角色
     *
     * 调用链：AdminController ↓ deleteUserRolesExcept() → SQL UPDATE SET deleted=1 → WHERE userId且排除excludedRoleId
     *
     * 数据处理：userId + excludedRoleId(可为null简化排除逻辑) → UPDATE SET deleted_wsh=1 → WHERE userId + deleted=0 + (excludedRoleId为null则全部删除 / 否则排除该roleId)
     *
     * 业务规则：excludedRoleId为null时删除用户所有有效角色关联；排除的角色不会被标记删除
     *
     * 状态影响：逻辑删除user_role_wsh表指定记录（deleted_wsh=1）
     *
     * 注意事项：使用逻辑删除而非物理删除，可通过restoreUserRole恢复
     */
    @Update("UPDATE user_role_wsh SET deleted_wsh = 1 " +
            "WHERE user_id_wsh = #{userId} AND deleted_wsh = 0 " +
            "AND (#{excludedRoleId} IS NULL OR role_id_wsh <> #{excludedRoleId})")
    int deleteUserRolesExcept(@Param("userId") Long userId, @Param("excludedRoleId") Long excludedRoleId);
}
