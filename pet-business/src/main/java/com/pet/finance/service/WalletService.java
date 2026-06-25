package com.pet.finance.service;

import com.pet.finance.entity.Wallet;
import java.math.BigDecimal;
import java.util.List;

public interface WalletService {
    /**
     * 根据用户ID获取钱包
     * @param userId 用户ID
     * @return 钱包实体
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Wallet getByUserId(Long userId);
    /**
     * 根据ID获取钱包
     * @param id 钱包ID
     * @return 钱包实体
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Wallet getById(Long id);
    /**
     * 增加钱包余额
     * @param userId 用户ID
     * @param amount 增加金额
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void addBalance(Long userId, BigDecimal amount);
    /**
     * 扣除钱包余额
     * @param userId 用户ID
     * @param amount 扣除金额
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void deductBalance(Long userId, BigDecimal amount);
    /**
     * 冻结钱包金额
     * @param userId 用户ID
     * @param amount 冻结金额
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void freeze(Long userId, BigDecimal amount);
    /**
     * 解冻钱包金额
     * @param userId 用户ID
     * @param amount 解冻金额
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void unfreeze(Long userId, BigDecimal amount);
    /**
     * 将冻结金额转入余额
     * @param userId 用户ID
     * @param amount 转账金额
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void transferFrozenToBalance(Long userId, BigDecimal amount);
    /**
     * 获取所有钱包列表
     * @return 钱包列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Wallet> listAll();
}

