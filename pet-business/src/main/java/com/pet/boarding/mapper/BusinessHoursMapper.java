package com.pet.boarding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.boarding.entity.BusinessHours;
import org.apache.ibatis.annotations.Mapper;

/**
 * 营业时间（BusinessHours）数据访问层。
 * <p>
 * 继承 MyBatis-Plus 的 {@link BaseMapper}，提供营业时间表的基础 CRUD 操作。
 * <p>
 * <b>映射表：</b>business_hours_wsh
 *
 * @author: wsh
 */
@Mapper
public interface BusinessHoursMapper extends BaseMapper<BusinessHours> {
}
