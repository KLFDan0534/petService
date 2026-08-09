package com.pet.membership.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.membership.entity.MembershipBenefitUsage;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for the {@link MembershipBenefitUsage} entity.
 * Provides CRUD operations for the membership_benefit_usage_wsh table.
 */
@Mapper
public interface MembershipBenefitUsageMapper extends BaseMapper<MembershipBenefitUsage> {
}
