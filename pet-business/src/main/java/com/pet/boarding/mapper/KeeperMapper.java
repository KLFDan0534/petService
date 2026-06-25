package com.pet.boarding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.boarding.entity.Keeper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 看护者数据访问层
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Mapper
public interface KeeperMapper extends BaseMapper<Keeper> {

    /**
     * 根据商家ID查询看护员列表
     * @param merchantId 商家ID
     * @return 看护员列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @Select("SELECT " +
            "id_wsh, merchant_id_wsh, user_id_wsh, " +
            "name_wsh, phone_wsh, avatar_wsh, " +
            "experience_years_wsh, rating_wsh, " +
            "completion_rate_wsh, complaint_rate_wsh, " +
            "price_per_day_wsh, max_pets_wsh, " +
            "current_pets_wsh, bio_wsh, status_wsh, " +
            "deleted_wsh, created_at_wsh, updated_at_wsh " +
            "FROM keeper_wsh WHERE merchant_id_wsh = #{merchantId} AND deleted_wsh = 0")
    List<Keeper> findByMerchantId(@Param("merchantId") Long merchantId);
}
