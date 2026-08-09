package com.pet.boarding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.boarding.entity.KeeperAttendance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 看护者考勤（KeeperAttendance）数据访问层。
 * <p>
 * 继承 MyBatis-Plus 的 {@link BaseMapper}，提供考勤签到表的基础 CRUD 操作。
 * <p>
 * <b>映射表：</b>keeper_attendance_wsh
 */
@Mapper
public interface KeeperAttendanceMapper extends BaseMapper<KeeperAttendance> {
}
