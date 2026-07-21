package com.pet.pet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.pet.entity.Pet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 宠物数据访问层
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Mapper
public interface PetMapper extends BaseMapper<Pet> {
    @Select("""
            SELECT owner_id_wsh AS user_id_wsh,
                   COUNT(*) AS pet_count_wsh
            FROM pet_wsh
            WHERE deleted_wsh = 0
            GROUP BY owner_id_wsh
            """)
    List<Map<String, Object>> selectCouponGrantPetCounts();
}
