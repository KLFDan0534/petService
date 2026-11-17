package com.pet.pet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.pet.entity.Pet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 宠物数据访问层，基于 MyBatis-Plus 提供宠物表的基础 CRUD。
 * 自定义方法 {@link #selectCouponGrantPetCounts()} 用于统计每位主人的宠物数量。
 *
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Mapper
public interface PetMapper extends BaseMapper<Pet> {
    /**
     * 统计所有未被逻辑删除的宠物，按主人 ID 分组。
     * 结果字段：user_id_wsh（主人 ID）、pet_count_wsh（宠物数量）。
     * 用于优惠券发放等场景中判断用户是否满足养宠数量条件。
     *
     * @return 每组包含 user_id_wsh 和 pet_count_wsh 的映射列表
     */
    @Select("""
            SELECT owner_id_wsh AS user_id_wsh,
                   COUNT(*) AS pet_count_wsh
            FROM pet_wsh
            WHERE deleted_wsh = 0
            GROUP BY owner_id_wsh
            """)
    List<Map<String, Object>> selectCouponGrantPetCounts();

    /**
     * 查询在本商家下过单的宠物，按 pet_id 去重。
     * 商家端「宠物管理」用：宠物归主人所有，商家没有宠物池，
     * 只能查看在本店履约过的宠物档案。
     *
     * @param merchantId 商家 ID
     * @return 该商家订单涉及的去重宠物，按 id 倒序
     */
    @Select("""
            SELECT DISTINCT p.*
            FROM pet_wsh p
            INNER JOIN pet_order_wsh o ON o.pet_id_wsh = p.id_wsh
            WHERE o.merchant_id_wsh = #{merchantId}
              AND p.deleted_wsh = 0
            ORDER BY p.id_wsh DESC
            """)
    List<Pet> selectServedByMerchant(@Param("merchantId") Long merchantId);
}
