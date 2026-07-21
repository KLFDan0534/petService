package com.pet.boarding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.boarding.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商家数据访问层
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {

    /**
     * 搜索附近商家
     * @param lat 纬度
     * @param lng 经度
     * @param radius 搜索半径（公里）
     * @return 附近商家列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @Select("SELECT " +
            "id_wsh, user_id_wsh, name_wsh, phone_wsh, " +
            "address_wsh, latitude_wsh, longitude_wsh, " +
            "description_wsh, business_license_wsh, " +
            "rating_wsh, status_wsh, store_mode_wsh, store_status_wsh, deleted_wsh, " +
            "created_at_wsh, updated_at_wsh, " +
            "(6371 * acos(cos(radians(#{lat})) * cos(radians(latitude_wsh)) * " +
            "cos(radians(longitude_wsh) - radians(#{lng})) + sin(radians(#{lat})) * sin(radians(latitude_wsh)))) AS distance_wsh " +
            "FROM merchant_wsh WHERE status_wsh = 1 AND deleted_wsh = 0 " +
            "HAVING distance_wsh <= #{radius} ORDER BY distance_wsh")
    List<Merchant> searchNearby(@Param("lat") double lat, @Param("lng") double lng, @Param("radius") double radius);
}
