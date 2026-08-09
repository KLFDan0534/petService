package com.pet.boarding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.boarding.entity.KeeperLeave;
import org.apache.ibatis.annotations.Mapper;

/**
 * 看护者休假（KeeperLeave）数据访问层。
 * <p>
 * 继承 MyBatis-Plus 的 {@link BaseMapper}，提供看护者休假表的基础 CRUD 操作。
 * <p>
 * <b>映射表：</b>keeper_leave_wsh
 */
@Mapper
public interface KeeperLeaveMapper extends BaseMapper<KeeperLeave> {
}
