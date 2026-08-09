package com.pet.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.system.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Update("UPDATE user_wsh "
            + "SET password_wsh = #{encodedPassword}, updated_at_wsh = updated_at_wsh")
    int resetAllPasswords(@Param("encodedPassword") String encodedPassword);
}
