package com.pet.module.favorite.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.module.favorite.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
