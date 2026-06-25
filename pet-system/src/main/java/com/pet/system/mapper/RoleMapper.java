package com.pet.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.system.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据用户ID查询角色编码列表
     * @param userId 用户ID
     * @return 角色编码列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @Select("SELECT r.code_wsh FROM role_wsh r " +
            "JOIN user_role_wsh ur ON r.id_wsh = ur.role_id_wsh " +
            "WHERE ur.user_id_wsh = #{userId} AND r.deleted_wsh = 0")
    List<String> selectRoleCodesByUserId(Long userId);
}
