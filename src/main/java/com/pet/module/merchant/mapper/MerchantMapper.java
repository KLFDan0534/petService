package com.pet.module.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.module.merchant.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {

    @Select("SELECT *, " +
            "(6371 * acos(cos(radians(#{lat})) * cos(radians(latitude)) * " +
            "cos(radians(longitude) - radians(#{lng})) + sin(radians(#{lat})) * sin(radians(latitude)))) AS distance " +
            "FROM merchant WHERE status = 1 AND deleted = 0 " +
            "HAVING distance <= #{radius} ORDER BY distance")
    List<Merchant> searchNearby(@Param("lat") double lat, @Param("lng") double lng, @Param("radius") double radius);
}
