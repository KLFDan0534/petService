package com.pet.finance.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.finance.dto.TransactionDTO;
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

    @Override
    public TransactionDTO toDTO(Transaction entity) {
        if (entity == null) return null;
        TransactionDTO dto = new TransactionDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setWallet_id_wsh(entity.getWallet_id_wsh());
        dto.setUser_id_wsh(entity.getUser_id_wsh());
        dto.setType_wsh(entity.getType_wsh());
        dto.setAmount_wsh(entity.getAmount_wsh());
        dto.setBalance_before_wsh(entity.getBalance_before_wsh());
        dto.setBalance_after_wsh(entity.getBalance_after_wsh());
        dto.setFrozen_before_wsh(entity.getFrozen_before_wsh());
        dto.setFrozen_after_wsh(entity.getFrozen_after_wsh());
        dto.setDirection_wsh(entity.getDirection_wsh());
        dto.setStatus_wsh(entity.getStatus_wsh());
        dto.setBusiness_type_wsh(entity.getBusiness_type_wsh());
        dto.setBusiness_id_wsh(entity.getBusiness_id_wsh());
        dto.setRequest_id_wsh(entity.getRequest_id_wsh());
        dto.setOrder_id_wsh(entity.getOrder_id_wsh());
        dto.setDescription_wsh(entity.getDescription_wsh());
        dto.setCreated_at_wsh(entity.getCreated_at_wsh());
        return dto;
    }

    @Transactional
    @Override
    public void add(Transaction tx) {
        log.info("调用 add()");
        transactionMapper.insert(tx);
    }
}
