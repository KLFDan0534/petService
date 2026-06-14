package com.pet.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.module.order.entity.PetOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<PetOrder> {
}
