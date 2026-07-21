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
    @Select("SELECT r.code_wsh AS roleCode, COUNT(*) AS cnt FROM user_role_wsh ur JOIN role_wsh r ON ur.role_id_wsh = r.id_wsh WHERE ur.deleted_wsh = 0 GROUP BY r.code_wsh")
    List<RoleCountDTO> countUsersByRole();

    @Select("SELECT COUNT(*) FROM user_role_wsh WHERE user_id_wsh = #{userId} AND role_id_wsh = #{roleId} AND deleted_wsh = 0")
    Long countActiveUserRole(Long userId, Long roleId);

    @Update("UPDATE user_role_wsh SET deleted_wsh = 0 WHERE user_id_wsh = #{userId} AND role_id_wsh = #{roleId}")
    int restoreUserRole(Long userId, Long roleId);

    @Update("UPDATE user_role_wsh SET deleted_wsh = 1 " +
            "WHERE user_id_wsh = #{userId} AND deleted_wsh = 0 " +
            "AND (#{excludedRoleId} IS NULL OR role_id_wsh <> #{excludedRoleId})")
    int deleteUserRolesExcept(@Param("userId") Long userId, @Param("excludedRoleId") Long excludedRoleId);
}
