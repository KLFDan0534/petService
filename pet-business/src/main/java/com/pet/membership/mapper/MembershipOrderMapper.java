package com.pet.membership.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.membership.entity.MembershipOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for the {@link MembershipOrder} entity.
 * Provides CRUD operations for the membership_order_wsh table.
 */
@Mapper
public interface MembershipOrderMapper extends BaseMapper<MembershipOrder> {
}
