package com.pet.finance.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.finance.entity.Transaction;
import com.pet.finance.mapper.TransactionMapper;
import com.pet.finance.service.TransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionMapper transactionMapper;

    public TransactionServiceImpl(TransactionMapper transactionMapper) {
        this.transactionMapper = transactionMapper;
    }

    @Override
    public List<Transaction> listByUser(Long userId) {
        log.info("调用 listByUser()");
        return transactionMapper.selectList(
                new LambdaQueryWrapper<Transaction>()
                        .eq(Transaction::getUser_id_wsh, userId)
                        .orderByDesc(Transaction::getCreated_at_wsh));
    }

    @Override
    public List<Transaction> listAll() {
        log.info("调用 listAll()");
        return transactionMapper.selectList(
                new LambdaQueryWrapper<Transaction>().orderByDesc(Transaction::getCreated_at_wsh));
    }

    @Transactional
    @Override
    public void add(Transaction tx) {
        log.info("调用 add()");
        transactionMapper.insert(tx);
    }
}
