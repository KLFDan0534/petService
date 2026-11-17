package com.pet.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.system.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 业务作用：联表查询用户在系统中拥有的所有角色编码，用于权限标识
     * 业务规则：仅查询deleted_wsh=0的有效记录；角色和用户-角色关联均做逻辑删除过滤
     * 注意事项：此方法被多个业务流程高频调用，是核心权限查询语句
     */
    @Select("SELECT r.code_wsh FROM role_wsh r " +
            "JOIN user_role_wsh ur ON r.id_wsh = ur.role_id_wsh " +
            "WHERE ur.user_id_wsh = #{userId} AND ur.deleted_wsh = 0 AND r.deleted_wsh = 0")
    List<String> selectRoleCodesByUserId(Long userId);
}
