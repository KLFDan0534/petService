package com.pet.operation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.operation.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户收藏 MyBatis-Plus Mapper 接口
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
