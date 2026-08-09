package com.pet.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.order.entity.PetOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * MyBatis-Plus mapper for {@link PetOrder} entity.
 * <p>
 * Provides CRUD operations on the {@code pet_order_wsh} table.
 * Includes a custom query for coupon grant order statistics
 * used by the marketing/coupon module.
 */
@Mapper
public interface OrderMapper extends BaseMapper<PetOrder> {

    /**
     * Aggregates completed order statistics grouped by owner for coupon grant eligibility.
     * Returns for each owner: total spend (final_amount or total_amount), order count,
     * and the date of their most recent completed order.
     *
     * @return list of maps containing user_id, total_spend, order_count, last_order_at
     */
    @Select("""
            SELECT owner_id_wsh AS user_id_wsh,
                   COALESCE(SUM(COALESCE(final_amount_wsh, total_amount_wsh, 0)), 0) AS total_spend_wsh,
                   COUNT(*) AS order_count_wsh,
                   MAX(created_at_wsh) AS last_order_at_wsh
            FROM pet_order_wsh
            WHERE deleted_wsh = 0
              AND status_wsh = 'completed'
            GROUP BY owner_id_wsh
            """)
    List<Map<String, Object>> selectCouponGrantOrderStats();
}
