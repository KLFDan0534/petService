package com.pet.module.rating.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.module.rating.entity.Rating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface RatingMapper extends BaseMapper<Rating> {

    @Select("SELECT AVG(score) FROM rating WHERE target_id = #{targetId} AND target_type = #{targetType} AND deleted = 0")
    BigDecimal getAverageScore(@Param("targetId") Long targetId, @Param("targetType") String targetType);
}
