package com.pet.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.order.entity.PetOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
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

    /**
     * 按主键加行锁读取订单（SELECT ... FOR UPDATE）。
     * <p>
     * 用于接走确认、补费支付、退款申请、确认接收等必须串行化同一订单资金/状态变更的路径。
     * 锁顺序约定：订单行锁 → 支付单 → 钱包；keeper 容量锁排在订单锁之后。
     *
     * @param id 订单主键
     * @return 订单实体（不存在或已逻辑删除时为 null）
     */
    @Select("""
            SELECT * FROM pet_order_wsh
            WHERE id_wsh = #{id} AND deleted_wsh = 0
            FOR UPDATE
            """)
    PetOrder selectByIdForUpdate(@Param("id") Long id);

    /**
     * 按订单号加行锁读取订单（SELECT ... FOR UPDATE）。
     *
     * @param orderNo 订单号
     * @return 订单实体（不存在或已逻辑删除时为 null）
     */
    @Select("""
            SELECT * FROM pet_order_wsh
            WHERE order_no_wsh = #{orderNo} AND deleted_wsh = 0
            FOR UPDATE
            """)
    PetOrder selectByOrderNoForUpdate(@Param("orderNo") String orderNo);
}
