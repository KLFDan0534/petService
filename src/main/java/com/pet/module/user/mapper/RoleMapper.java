package com.pet.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.module.user.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("SELECT r.code FROM role r " +
            "JOIN user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.deleted = 0")
    List<String> selectRoleCodesByUserId(Long userId);
}
