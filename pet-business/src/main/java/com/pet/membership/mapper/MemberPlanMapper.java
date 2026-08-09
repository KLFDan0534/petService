package com.pet.membership.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.membership.entity.MemberPlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for the {@link MemberPlan} entity.
 * Provides CRUD operations for the member_plan_wsh table.
 */
@Mapper
public interface MemberPlanMapper extends BaseMapper<MemberPlan> {
}
