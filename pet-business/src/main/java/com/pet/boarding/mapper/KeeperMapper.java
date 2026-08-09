package com.pet.boarding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.boarding.entity.Keeper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 看护者（Keeper）数据访问层。
 * <p>
 * 继承 MyBatis-Plus 的 {@link BaseMapper}，提供看护者表的基础 CRUD 操作。
 * 自定义方法 {@link #findByMerchantId} 根据商家 ID 查询旗下的看护者列表。
 * <p>
 * <b>映射表：</b>keeper_wsh
 *
 * @author: wsh
 */
@Mapper
public interface KeeperMapper extends BaseMapper<Keeper> {

    /**
     * 根据商家 ID 查询旗下所有未删除的看护者。
     * <p>
     * <b>业务说明：</b>查询指定商家的全量看护者（含 PENDING、ACTIVE、OFFLINE、RESIGNED 等状态），
     * 不区分状态。如需筛选特定状态的看护者，使用 {@link BaseMapper#selectList} 搭配 LambdaQueryWrapper。
     *
     * @param merchantId 商家 ID
     * @return 该商家下的看护者列表，不含已逻辑删除的记录
     */
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
