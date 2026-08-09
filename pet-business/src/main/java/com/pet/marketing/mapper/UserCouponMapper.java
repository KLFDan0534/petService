package com.pet.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.marketing.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 用户优惠券数据访问接口，提供 UserCoupon 实体的基础 CRUD 操作
 * 以及优惠券锁定、释放、标记使用等状态变更方法。
 * 映射表 user_coupon_wsh。
 */
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {
    /**
     * 锁定优惠券用于订单。
     * <p>仅当优惠券状态为 "available"、未被删除、且未过期时才能成功锁定。
     * 锁定后状态变为 "locked"。</p>
     *
     * @param id 用户优惠券ID
     * @param userId 用户ID
     * @param orderId 订单ID
     * @param orderNo 订单号
     * @param discountAmount 优惠金额
     * @return 受影响的行数，0 表示优惠券不可用
     */
    @Update("""
            UPDATE user_coupon_wsh
            SET status_wsh = 'locked',
                order_id_wsh = #{orderId},
                order_no_wsh = #{orderNo},
                discount_amount_wsh = #{discountAmount},
                locked_at_wsh = NOW(),
                updated_at_wsh = NOW()
            WHERE id_wsh = #{id}
              AND user_id_wsh = #{userId}
              AND status_wsh = 'available'
              AND deleted_wsh = 0
              AND (expire_at_wsh IS NULL OR expire_at_wsh >= NOW())
            """)
    int lockForOrder(@Param("id") Long id,
                     @Param("userId") Long userId,
                     @Param("orderId") Long orderId,
                     @Param("orderNo") String orderNo,
                     @Param("discountAmount") BigDecimal discountAmount);

    /**
     * 释放订单关联的已锁定优惠券。
     * <p>将状态从 "locked" 恢复为 "available"，并清空订单关联信息。</p>
     *
     * @param orderId 订单ID
     * @return 受影响的行数
     */
    @Update("""
            UPDATE user_coupon_wsh
            SET status_wsh = 'available',
                order_id_wsh = NULL,
                order_no_wsh = NULL,
                discount_amount_wsh = NULL,
                locked_at_wsh = NULL,
                updated_at_wsh = NOW()
            WHERE order_id_wsh = #{orderId}
              AND status_wsh = 'locked'
              AND deleted_wsh = 0
            """)
    int releaseByOrderId(@Param("orderId") Long orderId);

    /**
     * 将已锁定的优惠券标记为已使用。
     * <p>状态从 "locked" 变为 "used"。</p>
     *
     * @param orderId 订单ID
     * @return 受影响的行数
     */
    @Update("""
            UPDATE user_coupon_wsh
            SET status_wsh = 'used',
                used_at_wsh = NOW(),
                updated_at_wsh = NOW()
            WHERE order_id_wsh = #{orderId}
              AND status_wsh = 'locked'
              AND deleted_wsh = 0
            """)
    int markUsedByOrderId(@Param("orderId") Long orderId);
}
