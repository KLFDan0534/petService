package com.pet.pet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.pet.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 宠物分类数据访问层，基于 MyBatis-Plus 提供分类表的基础 CRUD。
 * 分类支持多级树形结构，通过 parent_id 字段关联上下级。
 *
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
