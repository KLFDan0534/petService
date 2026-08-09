package com.pet.boarding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.boarding.entity.ServiceCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 服务分类（ServiceCategory）数据访问层。
 * <p>
 * 继承 MyBatis-Plus 的 {@link BaseMapper}，提供服务分类表的基础 CRUD 操作。
 * <p>
 * <b>映射表：</b>service_category_wsh
 */
@Mapper
public interface ServiceCategoryMapper extends BaseMapper<ServiceCategory> {
}
