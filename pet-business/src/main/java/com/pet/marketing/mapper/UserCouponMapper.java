package com.pet.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.marketing.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {
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
