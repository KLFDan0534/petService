package com.pet.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.finance.entity.Wallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface WalletMapper extends BaseMapper<Wallet> {
    /**
     * 创建钱包
     * @param userId 用户ID
     * @return 影响行数
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @Insert("INSERT INTO wallet_wsh (user_id_wsh, balance_wsh, frozen_amount_wsh, created_at_wsh, updated_at_wsh) " +
            "SELECT #{userId}, 0, 0, NOW(), NOW() " +
            "WHERE NOT EXISTS (SELECT 1 FROM wallet_wsh WHERE user_id_wsh = #{userId} AND deleted_wsh = 0)")
    int createWallet(@Param("userId") Long userId);

    /**
     * 增加余额
     * @param userId 用户ID
     * @param amount 增加金额
     * @return 影响行数
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @Update("UPDATE wallet_wsh SET balance_wsh = balance_wsh + #{amount}, updated_at_wsh = NOW() WHERE user_id_wsh = #{userId}")
    int addBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 扣除余额（含余额校验）
     * @param userId 用户ID
     * @param amount 扣除金额
     * @return 影响行数
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @Update("UPDATE wallet_wsh SET balance_wsh = balance_wsh - #{amount}, updated_at_wsh = NOW() WHERE user_id_wsh = #{userId} AND (balance_wsh - frozen_amount_wsh) >= #{amount}")
    int deductBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 冻结金额
     * @param userId 用户ID
     * @param amount 冻结金额
     * @return 影响行数
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @Update("UPDATE wallet_wsh SET frozen_amount_wsh = frozen_amount_wsh + #{amount}, updated_at_wsh = NOW() WHERE user_id_wsh = #{userId} AND (balance_wsh - frozen_amount_wsh) >= #{amount}")
    int freeze(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 解冻金额
     * @param userId 用户ID
     * @param amount 解冻金额
     * @return 影响行数
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @Update("UPDATE wallet_wsh SET frozen_amount_wsh = frozen_amount_wsh - #{amount}, updated_at_wsh = NOW() WHERE user_id_wsh = #{userId} AND frozen_amount_wsh >= #{amount}")
    int unfreeze(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 将冻结金额转入余额（完成扣款）
     * @param userId 用户ID
     * @param amount 转账金额
     * @return 影响行数
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @Update("UPDATE wallet_wsh SET balance_wsh = balance_wsh - #{amount}, frozen_amount_wsh = frozen_amount_wsh - #{amount}, updated_at_wsh = NOW() WHERE user_id_wsh = #{userId} AND frozen_amount_wsh >= #{amount}")
    int transferFrozenToBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    @Update("UPDATE wallet_wsh SET balance_wsh = #{balance}, updated_at_wsh = NOW() WHERE user_id_wsh = #{userId}")
    int setBalance(@Param("userId") Long userId, @Param("balance") BigDecimal balance);
}
