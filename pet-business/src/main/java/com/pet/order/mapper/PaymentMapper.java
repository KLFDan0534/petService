package com.pet.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.order.entity.Payment;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for {@link Payment} entity.
 * <p>
 * Provides CRUD operations on the {@code payment_wsh} table.
 * Payments track the financial transaction for each order,
 * recording amount, method, status, and payment timestamp.
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
}
