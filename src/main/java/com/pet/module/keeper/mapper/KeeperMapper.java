package com.pet.module.keeper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.module.keeper.entity.Keeper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KeeperMapper extends BaseMapper<Keeper> {

    @Select("SELECT * FROM keeper WHERE merchant_id = #{merchantId} AND deleted = 0")
    List<Keeper> findByMerchantId(@Param("merchantId") Long merchantId);
}
