package com.pet.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.system.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
    @Select("SELECT role_id_wsh, COUNT(*) AS cnt FROM user_role_wsh WHERE deleted_wsh = 0 GROUP BY role_id_wsh")
    List<Map<String, Object>> countUsersByRole();
}
