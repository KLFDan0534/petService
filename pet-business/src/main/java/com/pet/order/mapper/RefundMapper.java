package com.pet.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.order.entity.Refund;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for {@link Refund} entity.
 * <p>
 * Provides CRUD operations on the {@code refund_wsh} table.
 * Refunds track the lifecycle of returning funds to pet owners
 * when orders are cancelled or disputed.
 */
@Mapper
public interface RefundMapper extends BaseMapper<Refund> {
}
