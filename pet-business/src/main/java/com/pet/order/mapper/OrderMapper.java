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
     * 平台级订单聚合指标（后台总览看板用）。
     * <p>
     * 业务作用：一条 SQL 算出「订单总数 / 营收合计 / 待付款数 / 已完成数」，
     * 替代原先 {@code orderService.listAll()} 拉全表到内存再 stream 统计的写法。
     * 业务规则：
     * - 营收口径 = status 在 paid/confirmed/received/in_progress/completed，取 final_amount_wsh（null 记 0）
     * - 统计口径与 {@code StatisticsServiceImpl.REVENUE_STATUSES} 保持一致，改动需两边同步
     * 注意事项：聚合下推到数据库后，返回的是 1 行 4 列，不再随订单量增长而占用内存。
     *
     * @return 单行 Map：total_orders_wsh / total_revenue_wsh / pending_orders_wsh / completed_orders_wsh
     */
    @Select("""
            SELECT COUNT(*) AS total_orders_wsh,
                   COALESCE(SUM(CASE WHEN status_wsh IN ('paid','confirmed','received','in_progress','completed')
                                     THEN COALESCE(final_amount_wsh, 0) ELSE 0 END), 0) AS total_revenue_wsh,
                   COALESCE(SUM(CASE WHEN status_wsh = 'pending' THEN 1 ELSE 0 END), 0) AS pending_orders_wsh,
                   COALESCE(SUM(CASE WHEN status_wsh = 'completed' THEN 1 ELSE 0 END), 0) AS completed_orders_wsh
            FROM pet_order_wsh
            WHERE deleted_wsh = 0
            """)
    Map<String, Object> selectPlatformStats();

    /**
     * 单个主人的订单聚合指标（用户端个人看板用）。
     * <p>
     * 业务作用：一条 SQL 算出「进行中 / 已完成 / 累计消费」，替代
     * {@code orderService.listByOwner(userId)} 拉该用户全部订单再统计。
     * 业务规则：进行中 = paid/confirmed/received/in_progress；累计消费只算已付款之后的状态。
     * 注意事项：走 idx_order_owner_created 前导列，只扫该用户的行。
     *
     * @param ownerId 主人用户ID
     * @return 单行 Map：total_orders_wsh / active_orders_wsh / completed_orders_wsh / total_spent_wsh
     */
    @Select("""
            SELECT COUNT(*) AS total_orders_wsh,
                   COALESCE(SUM(CASE WHEN status_wsh IN ('paid','confirmed','received','in_progress') THEN 1 ELSE 0 END), 0) AS active_orders_wsh,
                   COALESCE(SUM(CASE WHEN status_wsh = 'completed' THEN 1 ELSE 0 END), 0) AS completed_orders_wsh,
                   COALESCE(SUM(CASE WHEN status_wsh IN ('paid','confirmed','received','in_progress','completed')
                                     THEN COALESCE(final_amount_wsh, 0) ELSE 0 END), 0) AS total_spent_wsh
            FROM pet_order_wsh
            WHERE deleted_wsh = 0
              AND owner_id_wsh = #{ownerId}
            """)
    Map<String, Object> selectOwnerStats(@Param("ownerId") Long ownerId);

    /**
     * 单个商家的订单聚合指标（商家端看板用）。
     * <p>
     * 业务作用：一条 SQL 算出「总数 / 营收 / 待处理 / 进行中 / 已完成 / 在店宠物数」，
     * 替代 {@code orderService.listByMerchant(merchantId)} 拉全量再统计。
     * 业务规则：
     * - 在店宠物数按 pet_id 去重，只统计 received/in_progress（宠物已交接进店、尚未完成）
     * - 营收口径同 {@link #selectPlatformStats()}
     * 注意事项：走 idx_order_merchant_created 前导列。
     *
     * @param merchantId 商家ID
     * @return 单行 Map：total_orders_wsh / total_revenue_wsh / pending_orders_wsh /
     *         active_orders_wsh / completed_orders_wsh / serving_pets_wsh
     */
    @Select("""
            SELECT COUNT(*) AS total_orders_wsh,
                   COALESCE(SUM(CASE WHEN status_wsh IN ('paid','confirmed','received','in_progress','completed')
                                     THEN COALESCE(final_amount_wsh, 0) ELSE 0 END), 0) AS total_revenue_wsh,
                   COALESCE(SUM(CASE WHEN status_wsh = 'pending' THEN 1 ELSE 0 END), 0) AS pending_orders_wsh,
                   COALESCE(SUM(CASE WHEN status_wsh IN ('paid','confirmed','received','in_progress') THEN 1 ELSE 0 END), 0) AS active_orders_wsh,
                   COALESCE(SUM(CASE WHEN status_wsh = 'completed' THEN 1 ELSE 0 END), 0) AS completed_orders_wsh,
                   COUNT(DISTINCT CASE WHEN status_wsh IN ('received','in_progress') THEN pet_id_wsh END) AS serving_pets_wsh
            FROM pet_order_wsh
            WHERE deleted_wsh = 0
              AND merchant_id_wsh = #{merchantId}
            """)
    Map<String, Object> selectMerchantStats(@Param("merchantId") Long merchantId);

    /**
     * 按状态统计某个主人的订单数（用户端订单页标签计数用）。
     * <p>
     * 业务作用：列表改成游标分页后，前端只能看到已加载的那一页，
     * 标签上的「待处理 3」如果在前端算就会随滚动变化。计数必须单独走这条聚合，
     * 才能保证数字与全量一致。
     * 业务规则：只统计未删除订单，按 status_wsh 分组，不返回 count 为 0 的状态。
     * 注意事项：走 idx_order_owner_created 前导列，只扫该用户的行；返回行数 = 该用户出现过的状态数（≤10）。
     *
     * @param ownerId 主人用户ID
     * @return 每行 {status_wsh, cnt_wsh}
     */
    @Select("""
            SELECT status_wsh AS status_wsh, COUNT(*) AS cnt_wsh
            FROM pet_order_wsh
            WHERE deleted_wsh = 0
              AND owner_id_wsh = #{ownerId}
            GROUP BY status_wsh
            """)
    List<Map<String, Object>> selectOwnerStatusCounts(@Param("ownerId") Long ownerId);

    /**
     * 按状态统计某个商家的订单数（商家端订单页标签计数用）。
     * <p>
     * 业务规则与 {@link #selectOwnerStatusCounts} 一致，维度换成 merchant_id_wsh。
     *
     * @param merchantId 商家ID
     * @return 每行 {status_wsh, cnt_wsh}
     */
    @Select("""
            SELECT status_wsh AS status_wsh, COUNT(*) AS cnt_wsh
            FROM pet_order_wsh
            WHERE deleted_wsh = 0
              AND merchant_id_wsh = #{merchantId}
            GROUP BY status_wsh
            """)
    List<Map<String, Object>> selectMerchantStatusCounts(@Param("merchantId") Long merchantId);

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
