package com.pet.pet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.pet.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分类数据访问层
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
