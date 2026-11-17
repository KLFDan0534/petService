package com.pet.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.system.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 业务作用：用同一条加密后的密码更新所有用户的密码字段。
     * 数据处理：encodedPassword 为已加密的 BCrypt 密文，直接覆盖 password_wsh。
     * 业务规则：只更新未删除用户（由 @TableLogic 自动追加 deleted_wsh 条件）。
     * 注意事项：不带 where 条件的批量更新，影响行数为全表用户数。
     */
    @Update("UPDATE user_wsh "
            + "SET password_wsh = #{encodedPassword}, updated_at_wsh = updated_at_wsh")
    int resetAllPasswords(@Param("encodedPassword") String encodedPassword);
}
