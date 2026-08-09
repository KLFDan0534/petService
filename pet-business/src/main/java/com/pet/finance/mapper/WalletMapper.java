package com.pet.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.finance.entity.Wallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 钱包数据访问层，提供钱包表的基础 CRUD 及余额变更的原子 SQL 操作。
 * 所有余额变更 SQL 均在数据库层面执行原子加减，避免并发写冲突。
 *
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Mapper
public interface WalletMapper extends BaseMapper<Wallet> {
    /**
     * 为用户创建钱包（幂等：已存在时不重复创建）。
     * 新钱包余额和冻结金额均为 0。
     *
     * @param userId 用户 ID
     * @return 影响行数（0 表示已存在，1 表示新建成功）
     */
    @Insert("INSERT INTO wallet_wsh (user_id_wsh, balance_wsh, frozen_amount_wsh, created_at_wsh, updated_at_wsh) " +
            "SELECT #{userId}, 0, 0, NOW(), NOW() " +
            "WHERE NOT EXISTS (SELECT 1 FROM wallet_wsh WHERE user_id_wsh = #{userId} AND deleted_wsh = 0)")
    int createWallet(@Param("userId") Long userId);

    /**
     * 原子增加余额（balance = balance + amount）。
     * 不校验可用余额，由上游 AccountingService 控制。
     *
     * @param userId 用户 ID
     * @param amount 增加金额
     * @return 影响行数（应为 1）
     */
    @Update("UPDATE wallet_wsh SET balance_wsh = balance_wsh + #{amount}, updated_at_wsh = NOW() WHERE user_id_wsh = #{userId}")
    int addBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 原子扣除余额，含可用余额校验。
     * SQL 条件 (balance - frozen) >= amount 确保不会导致可用余额为负。
     *
     * @param userId 用户 ID
     * @param amount 扣除金额
     * @return 影响行数（0 表示可用余额不足或记录不存在）
     */
    @Update("UPDATE wallet_wsh SET balance_wsh = balance_wsh - #{amount}, updated_at_wsh = NOW() WHERE user_id_wsh = #{userId} AND (balance_wsh - frozen_amount_wsh) >= #{amount}")
    int deductBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 原子冻结金额，增加 frozen_amount 并校验可用余额。
     * SQL 条件 (balance - frozen) >= amount 确保冻结后可用余额不为负。
     *
     * @param userId 用户 ID
     * @param amount 冻结金额
     * @return 影响行数（0 表示可用余额不足）
     */
    @Update("UPDATE wallet_wsh SET frozen_amount_wsh = frozen_amount_wsh + #{amount}, updated_at_wsh = NOW() WHERE user_id_wsh = #{userId} AND (balance_wsh - frozen_amount_wsh) >= #{amount}")
    int freeze(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 原子解冻金额，减少 frozen_amount。
     * SQL 条件 frozen >= amount 确保不会过量解冻。
     *
     * @param userId 用户 ID
     * @param amount 解冻金额
     * @return 影响行数（0 表示冻结余额不足）
     */
    @Update("UPDATE wallet_wsh SET frozen_amount_wsh = frozen_amount_wsh - #{amount}, updated_at_wsh = NOW() WHERE user_id_wsh = #{userId} AND frozen_amount_wsh >= #{amount}")
    int unfreeze(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 原子消耗冻结金额（同时扣减 balance 和 frozen_amount）。
     * 用于提现最终打款：从余额和冻结区同步扣除，SQL 条件 frozen >= amount。
     *
     * @param userId 用户 ID
     * @param amount 消耗金额
     * @return 影响行数（0 表示冻结余额不足）
     */
    @Update("UPDATE wallet_wsh SET balance_wsh = balance_wsh - #{amount}, frozen_amount_wsh = frozen_amount_wsh - #{amount}, updated_at_wsh = NOW() WHERE user_id_wsh = #{userId} AND frozen_amount_wsh >= #{amount}")
    int transferFrozenToBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 直接设置余额为指定值（覆盖式）。
     * 用于管理员调账，不校验可用余额，由上游 AccountingService 保证一致性。
     *
     * @param userId  用户 ID
     * @param balance 新余额值
     * @return 影响行数
     */
    @Update("UPDATE wallet_wsh SET balance_wsh = #{balance}, updated_at_wsh = NOW() WHERE user_id_wsh = #{userId}")
    int setBalance(@Param("userId") Long userId, @Param("balance") BigDecimal balance);
}
