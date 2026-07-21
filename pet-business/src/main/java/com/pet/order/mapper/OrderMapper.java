package com.pet.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.order.entity.PetOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 订单数据访问层
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Mapper
public interface OrderMapper extends BaseMapper<PetOrder> {
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
