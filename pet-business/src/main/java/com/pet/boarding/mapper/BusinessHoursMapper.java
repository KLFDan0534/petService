package com.pet.boarding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.boarding.entity.BusinessHours;
import org.apache.ibatis.annotations.Mapper;

/**
 * 营业时间数据访问层
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Mapper
public interface BusinessHoursMapper extends BaseMapper<BusinessHours> {
}
