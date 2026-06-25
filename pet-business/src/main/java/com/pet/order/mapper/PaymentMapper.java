package com.pet.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.order.entity.Payment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付数据访问层
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
}
