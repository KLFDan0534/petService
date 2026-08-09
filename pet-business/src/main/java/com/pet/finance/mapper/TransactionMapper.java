package com.pet.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.finance.entity.Transaction;
import org.apache.ibatis.annotations.Mapper;

/**
 * 交易流水数据访问层，基于 MyBatis-Plus 提供交易记录表的基础 CRUD。
 * 交易流水是钱包变动的审计依据，记录每次余额/冻结变化的快照。
 *
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Mapper
public interface TransactionMapper extends BaseMapper<Transaction> {
}
