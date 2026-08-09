package com.pet.qualification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.qualification.entity.Qualification;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for the {@link Qualification} entity.
 * Provides CRUD operations for the qualification_wsh table.
 */
@Mapper
public interface QualificationMapper extends BaseMapper<Qualification> {
}
