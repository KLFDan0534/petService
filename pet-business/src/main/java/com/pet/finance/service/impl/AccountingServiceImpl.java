package com.pet.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.finance.entity.Transaction;
import com.pet.finance.entity.Wallet;
import com.pet.finance.mapper.TransactionMapper;
import com.pet.finance.mapper.WalletMapper;
import com.pet.finance.service.AccountingService;
import com.pet.finance.service.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class AccountingServiceImpl implements AccountingService {
    private static final Long SYSTEM_USER_ID = 0L;

    private final WalletService walletService;
    private final WalletMapper walletMapper;
    private final TransactionMapper transactionMapper;

    public AccountingServiceImpl(WalletService walletService,
                                 WalletMapper walletMapper,
                                 TransactionMapper transactionMapper) {
        this.walletService = walletService;
        this.walletMapper = walletMapper;
        this.transactionMapper = transactionMapper;
    }

    @Override
    public Long systemUserId() {
        return SYSTEM_USER_ID;
    }

    @Transactional
    @Override
    public Wallet credit(Long userId, BigDecimal amount, String type, Long orderId,
                         String businessType, String businessId, String requestId, String description) {
        BigDecimal money = positive(amount);
        if (alreadyPosted(requestId)) return walletService.getByUserId(userId);
        Wallet before = walletService.getByUserId(userId);
        if (walletMapper.addBalance(userId, money) == 0) {
            throw new BusinessException("钱包入账失败");
        }
        Wallet after = walletService.getByUserId(userId);
        saveTransaction(after, type, money, before, after, "in", orderId, businessType, businessId, requestId, description);
        return after;
    }

    @Transactional
    @Override
    public Wallet debit(Long userId, BigDecimal amount, String type, Long orderId,
                        String businessType, String businessId, String requestId, String description) {
        BigDecimal money = positive(amount);
        if (alreadyPosted(requestId)) return walletService.getByUserId(userId);
        Wallet before = walletService.getByUserId(userId);
        if (walletMapper.deductBalance(userId, money) == 0) {
            Wallet current = walletService.getByUserId(userId);
            if (current.getBalance_wsh().subtract(current.getFrozen_amount_wsh()).compareTo(money) < 0) {
                throw new BusinessException("可用余额不足");
            }
            throw new BusinessException("钱包扣款失败");
        }
        Wallet after = walletService.getByUserId(userId);
        saveTransaction(after, type, money.negate(), before, after, "out", orderId, businessType, businessId, requestId, description);
        return after;
    }

    @Transactional
    @Override
    public void transfer(Long fromUserId, Long toUserId, BigDecimal amount, String type, Long orderId,
                         String businessType, String businessId, String requestId, String description) {
        String baseRequestId = requestId == null || requestId.isBlank()
                ? "transfer:" + businessType + ":" + businessId + ":" + System.nanoTime()
                : requestId;
        if (alreadyPosted(baseRequestId + ":out")) return;
        BigDecimal money = positive(amount);
        debit(fromUserId, money, type, orderId, businessType, businessId, baseRequestId + ":out", description + " - out");
        credit(toUserId, money, type, orderId, businessType, businessId, baseRequestId + ":in", description + " - in");
    }

    @Transactional
    @Override
    public Wallet freeze(Long userId, BigDecimal amount, String type, Long orderId,
                         String businessType, String businessId, String requestId, String description) {
        BigDecimal money = positive(amount);
        if (alreadyPosted(requestId)) return walletService.getByUserId(userId);
        Wallet before = walletService.getByUserId(userId);
        if (walletMapper.freeze(userId, money) == 0) {
            throw new BusinessException("可用余额不足");
        }
        Wallet after = walletService.getByUserId(userId);
        saveTransaction(after, type, money, before, after, "freeze", orderId, businessType, businessId, requestId, description);
        return after;
    }

    @Transactional
    @Override
    public Wallet unfreeze(Long userId, BigDecimal amount, String type, Long orderId,
                           String businessType, String businessId, String requestId, String description) {
        BigDecimal money = positive(amount);
        if (alreadyPosted(requestId)) return walletService.getByUserId(userId);
        Wallet before = walletService.getByUserId(userId);
        if (walletMapper.unfreeze(userId, money) == 0) {
            throw new BusinessException("冻结余额不足");
        }
        Wallet after = walletService.getByUserId(userId);
        saveTransaction(after, type, money.negate(), before, after, "unfreeze", orderId, businessType, businessId, requestId, description);
        return after;
    }

    @Transactional
    @Override
    public Wallet consumeFrozen(Long userId, BigDecimal amount, String type, Long orderId,
                                String businessType, String businessId, String requestId, String description) {
        BigDecimal money = positive(amount);
        if (alreadyPosted(requestId)) return walletService.getByUserId(userId);
        Wallet before = walletService.getByUserId(userId);
        if (walletMapper.transferFrozenToBalance(userId, money) == 0) {
            throw new BusinessException("冻结余额不足");
        }
        Wallet after = walletService.getByUserId(userId);
        saveTransaction(after, type, money.negate(), before, after, "out", orderId, businessType, businessId, requestId, description);
        return after;
    }

    @Transactional
    @Override
    public Wallet setBalanceByAdmin(Long adminId, Long userId, BigDecimal balance, String requestId, String description) {
        if (userId == null) throw new BusinessException(400, "用户ID不能为空");
        BigDecimal money = notNegative(balance);
        if (alreadyPosted(requestId)) return walletService.getByUserId(userId);
        Wallet before = walletService.getByUserId(userId);
        if (before.getFrozen_amount_wsh().compareTo(money) > 0) {
            throw new BusinessException(400, "余额不能小于冻结金额");
        }
        if (walletMapper.setBalance(userId, money) == 0) {
            throw new BusinessException("设置余额失败");
        }
        Wallet after = walletService.getByUserId(userId);
        BigDecimal delta = after.getBalance_wsh().subtract(before.getBalance_wsh());
        saveTransaction(after, "admin_adjust", delta, before, after, "set",
                null, "wallet_admin", String.valueOf(userId), requestId,
                description == null || description.isBlank() ? "管理员调整余额: " + adminId : description);
        return after;
    }

    private boolean alreadyPosted(String requestId) {
        if (requestId == null || requestId.isBlank()) return false;
        Long count = transactionMapper.selectCount(
                new LambdaQueryWrapper<Transaction>().eq(Transaction::getRequest_id_wsh, requestId));
        return count != null && count > 0;
    }

    private void saveTransaction(Wallet wallet, String type, BigDecimal amount, Wallet before, Wallet after,
                                 String direction, Long orderId, String businessType, String businessId,
                                 String requestId, String description) {
        Transaction tx = new Transaction();
        tx.setWallet_id_wsh(wallet.getId_wsh());
        tx.setUser_id_wsh(wallet.getUser_id_wsh());
        tx.setType_wsh(type);
        tx.setAmount_wsh(amount);
        tx.setBalance_before_wsh(before.getBalance_wsh());
        tx.setBalance_after_wsh(after.getBalance_wsh());
        tx.setFrozen_before_wsh(before.getFrozen_amount_wsh());
        tx.setFrozen_after_wsh(after.getFrozen_amount_wsh());
        tx.setDirection_wsh(direction);
        tx.setStatus_wsh("success");
        tx.setBusiness_type_wsh(businessType);
        tx.setBusiness_id_wsh(businessId);
        tx.setRequest_id_wsh(requestId);
        tx.setOrder_id_wsh(orderId);
        tx.setDescription_wsh(description);
        transactionMapper.insert(tx);
    }

    private BigDecimal positive(BigDecimal value) {
        BigDecimal money = notNegative(value);
        if (money.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "金额必须大于0");
        }
        return money;
    }

    private BigDecimal notNegative(BigDecimal value) {
        if (value == null) throw new BusinessException(400, "金额不能为空");
        BigDecimal money = value.setScale(2, RoundingMode.HALF_UP);
        if (money.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(400, "金额不能小于0");
        }
        return money;
    }
}
