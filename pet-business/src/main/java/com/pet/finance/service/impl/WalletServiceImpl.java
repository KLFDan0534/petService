package com.pet.finance.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.finance.entity.Wallet;
import com.pet.finance.mapper.WalletMapper;
import com.pet.finance.service.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletMapper walletMapper;

    public WalletServiceImpl(WalletMapper walletMapper) {
        this.walletMapper = walletMapper;
    }

    @Override
    public Wallet getByUserId(Long userId) {
        log.info("调用 getByUserId()");
        Wallet w = walletMapper.selectOne(
                new LambdaQueryWrapper<Wallet>().eq(Wallet::getUserIdWsh, userId));
        if (w == null) {
            walletMapper.createWallet(userId);
            return getByUserId(userId);
        }
        return w;
    }

    @Override
    public Wallet getById(Long id) {
        log.info("调用 getById()");
        Wallet w = walletMapper.selectById(id);
        if (w == null) throw new BusinessException("钱包不存在");
        return w;
    }

    @Transactional
    @Override
    public void addBalance(Long userId, BigDecimal amount) {
        log.info("调用 addBalance()");
        walletMapper.addBalance(userId, amount);
    }

    @Transactional
    @Override
    public void deductBalance(Long userId, BigDecimal amount) {
        log.info("调用 deductBalance()");
        int affected = walletMapper.deductBalance(userId, amount);
        if (affected == 0) {
            Wallet w = getByUserId(userId);
            if (w.getBalanceWsh().compareTo(amount) < 0)
                throw new BusinessException("余额不足");
            throw new BusinessException("扣款失败");
        }
    }

    @Transactional
    @Override
    public void freeze(Long userId, BigDecimal amount) {
        log.info("调用 freeze()");
        int affected = walletMapper.freeze(userId, amount);
        if (affected == 0) {
            Wallet w = getByUserId(userId);
            if (w.getBalanceWsh().subtract(w.getFrozenAmountWsh()).compareTo(amount) < 0)
                throw new BusinessException("可用余额不足");
            throw new BusinessException("冻结失败");
        }
    }

    @Transactional
    @Override
    public void unfreeze(Long userId, BigDecimal amount) {
        log.info("调用 unfreeze()");
        walletMapper.unfreeze(userId, amount);
    }

    @Transactional
    @Override
    public void transferFrozenToBalance(Long userId, BigDecimal amount) {
        log.info("调用 transferFrozenToBalance()");
        walletMapper.transferFrozenToBalance(userId, amount);
    }

    @Override
    public List<Wallet> listAll() {
        log.info("调用 listAll()");
        return walletMapper.selectList(
                new LambdaQueryWrapper<Wallet>()
                        .orderByDesc(Wallet::getCreatedAtWsh)
                        .last("LIMIT 1000"));
    }
}
