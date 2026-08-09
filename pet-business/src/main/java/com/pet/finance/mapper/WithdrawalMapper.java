package com.pet.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.finance.entity.Withdrawal;
import org.apache.ibatis.annotations.Mapper;

/**
 * 提现记录数据访问层，基于 MyBatis-Plus 提供提现表的基础 CRUD。
 * 提现记录管理用户资金提取的完整生命周期：pending → approved → completed / rejected。
 *
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Mapper
public interface WithdrawalMapper extends BaseMapper<Withdrawal> {
}
