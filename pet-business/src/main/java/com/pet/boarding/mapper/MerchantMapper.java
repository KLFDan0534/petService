package com.pet.boarding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.boarding.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商家（Merchant）数据访问层。
 * <p>
 * 继承 MyBatis-Plus 的 {@link BaseMapper}，提供商家表的基础 CRUD 操作。
 * 自定义方法 {@link #searchNearby} 使用 Haversine 公式进行 LBS 地理距离搜索。
 * <p>
 * <b>映射表：</b>merchant_wsh
 *
 * @author: wsh
 */
@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {

    /**
     * 基于 Haversine 公式搜索指定半径内的已审核商家。
     * <p>
     * <b>SQL 说明：</b>
     * <ul>
     *   <li>使用 6371 公里地球半径计算球面距离</li>
     *   <li>仅搜索 status = 1 (MERCHANT_APPROVED) 且未逻辑删除的商家</li>
     *   <li>通过 HAVING 子句过滤距离 <= radius 的记录</li>
     *   <li>结果按距离升序排列</li>
     *   <li>计算结果以 {@code distance_wsh} 字段返回（非持久化字段）</li>
     * </ul>
     *
     * @param lat    用户纬度
     * @param lng    用户经度
     * @param radius 搜索半径，单位：公里
     * @return 附近商家列表，结果中每个商家附带 {@link Merchant#getDistance_wsh()} 距离字段
     */
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
