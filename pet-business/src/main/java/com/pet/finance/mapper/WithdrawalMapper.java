package com.pet.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.finance.entity.Withdrawal;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WithdrawalMapper extends BaseMapper<Withdrawal> {
}
