package com.pet.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.order.entity.Tip;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for {@link Tip} entity.
 * <p>
 * Provides CRUD operations on the {@code tip_wsh} table.
 * Tips represent gratuities sent from pet owners to keepers
 * after an order is completed.
 */
@Mapper
public interface TipMapper extends BaseMapper<Tip> {
}
