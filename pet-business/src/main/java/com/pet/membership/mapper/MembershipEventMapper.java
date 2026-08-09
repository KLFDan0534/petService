package com.pet.membership.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.membership.entity.MembershipEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for the {@link MembershipEvent} entity.
 * Provides CRUD operations for the membership_event_wsh table.
 */
@Mapper
public interface MembershipEventMapper extends BaseMapper<MembershipEvent> {
}
