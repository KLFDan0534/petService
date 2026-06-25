package com.pet.finance.service;

import com.pet.finance.entity.Transaction;
import java.util.List;

public interface TransactionService {
    /**
     * 根据用户ID获取交易记录列表
     * @param userId 用户ID
     * @return 交易记录列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Transaction> listByUser(Long userId);
    /**
     * 获取所有交易记录列表
     * @return 交易记录列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Transaction> listAll();
    /**
     * 添加交易记录
     * @param tx 交易记录实体
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void add(Transaction tx);
}

