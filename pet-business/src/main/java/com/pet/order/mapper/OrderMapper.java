package com.pet.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.order.entity.PetOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单数据访问层
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Mapper
public interface OrderMapper extends BaseMapper<PetOrder> {
}
