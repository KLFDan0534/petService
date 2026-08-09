package com.pet.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.customer.entity.Rating;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评价数据访问接口，提供 Rating 实体的基础 CRUD 操作。
 * 映射表 rating_wsh。
 */
@Mapper
public interface RatingMapper extends BaseMapper<Rating> {
}
