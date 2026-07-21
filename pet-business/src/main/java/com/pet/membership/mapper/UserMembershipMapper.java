package com.pet.membership.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.membership.entity.UserMembership;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMembershipMapper extends BaseMapper<UserMembership> {
}
