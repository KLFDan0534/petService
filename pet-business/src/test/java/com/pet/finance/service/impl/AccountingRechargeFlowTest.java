package com.pet.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.finance.entity.Transaction;
import com.pet.finance.entity.Wallet;
import com.pet.finance.mapper.TransactionMapper;
import com.pet.finance.mapper.WalletMapper;
import com.pet.finance.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 充值 + 余额支付账务流单元测试。
 * 覆盖：正常充值、非法金额（0/负数）、幂等重复请求、充值记录、余额扣款、余额不足不扣款。
 */
@ExtendWith(MockitoExtension.class)
class AccountingRechargeFlowTest {

    @Mock private WalletMapper walletMapper;
    @Mock private WalletService walletService;
    @Mock private TransactionMapper transactionMapper;

    private AccountingServiceImpl accounting;

    @BeforeEach
    void setUp() {
        accounting = new AccountingServiceImpl(walletService, walletMapper, transactionMapper);
    }

    private Wallet wallet(long id, long userId, String balance) {
        Wallet w = new Wallet();
        w.setId_wsh(id);
        w.setUser_id_wsh(userId);
        w.setBalance_wsh(new BigDecimal(balance));
        w.setFrozen_amount_wsh(BigDecimal.ZERO);
        return w;
    }

    @Test
    void rechargeNormalIncreasesBalanceAndWritesRechargeRecord() {
        when(transactionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(walletService.getByUserId(7L)).thenReturn(wallet(1L, 7L, "0.00"), wallet(1L, 7L, "50.00"));
        when(walletMapper.addBalance(eq(7L), eq(new BigDecimal("50.00")))).thenReturn(1);

        Wallet after = accounting.credit(7L, new BigDecimal("50.00"), "recharge", null,
                "wallet_recharge", "7", "recharge:7:req1", "用户余额充值");

        assertEquals(new BigDecimal("50.00"), after.getBalance_wsh());
        ArgumentCaptor<Transaction> tx = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionMapper).insert(tx.capture());
        assertEquals("recharge", tx.getValue().getType_wsh());
        assertEquals("in", tx.getValue().getDirection_wsh());
        assertEquals(new BigDecimal("50.00"), tx.getValue().getAmount_wsh());
        assertEquals(new BigDecimal("0.00"), tx.getValue().getBalance_before_wsh());
        assertEquals(new BigDecimal("50.00"), tx.getValue().getBalance_after_wsh());
    }

    @Test
    void rechargeRejectsZeroAndNegativeAndNullAmount() {
        assertThrows(BusinessException.class, () -> accounting.credit(7L, BigDecimal.ZERO, "recharge", null, "wallet_recharge", "7", "r0", "充值"));
        assertThrows(BusinessException.class, () -> accounting.credit(7L, new BigDecimal("-5.00"), "recharge", null, "wallet_recharge", "7", "r1", "充值"));
        assertThrows(BusinessException.class, () -> accounting.credit(7L, null, "recharge", null, "wallet_recharge", "7", "r2", "充值"));
        verify(transactionMapper, never()).insert(any(Transaction.class));
    }

    @Test
    void rechargeDuplicateRequestIsIdempotent() {
        when(transactionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(walletService.getByUserId(7L)).thenReturn(wallet(1L, 7L, "100.00"));

        Wallet after = accounting.credit(7L, new BigDecimal("50.00"), "recharge", null,
                "wallet_recharge", "7", "recharge-dup", "用户余额充值");

        assertEquals(new BigDecimal("100.00"), after.getBalance_wsh());
        verify(walletMapper, never()).addBalance(any(), any());
        verify(transactionMapper, never()).insert(any(Transaction.class));
    }

    @Test
    void balancePaymentDebitsOwner() {
        when(transactionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(walletService.getByUserId(7L)).thenReturn(wallet(1L, 7L, "100.00"), wallet(1L, 7L, "80.00"));
        when(walletMapper.deductBalance(eq(7L), eq(new BigDecimal("20.00")))).thenReturn(1);

        Wallet after = accounting.debit(7L, new BigDecimal("20.00"), "payment", 55L,
                "payment", "550", "payment:55:owner", "订单余额支付");

        assertEquals(new BigDecimal("80.00"), after.getBalance_wsh());
        ArgumentCaptor<Transaction> tx = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionMapper).insert(tx.capture());
        assertEquals("out", tx.getValue().getDirection_wsh());
        assertEquals(new BigDecimal("-20.00"), tx.getValue().getAmount_wsh());
        assertEquals(Long.valueOf(55L), tx.getValue().getOrder_id_wsh());
    }

    @Test
    void balancePaymentInsufficientThrowsAndNoRecord() {
        when(transactionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(walletService.getByUserId(7L)).thenReturn(wallet(1L, 7L, "10.00"));
        when(walletMapper.deductBalance(eq(7L), any(BigDecimal.class))).thenReturn(0);

        assertThrows(BusinessException.class, () -> accounting.debit(7L, new BigDecimal("20.00"), "payment", 55L,
                "payment", "550", "payment:55:owner", "订单余额支付"));

        verify(transactionMapper, never()).insert(any(Transaction.class));
    }

    @Test
    void balancePaymentDuplicateIsIdempotent() {
        when(transactionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(walletService.getByUserId(7L)).thenReturn(wallet(1L, 7L, "80.00"));

        Wallet after = accounting.debit(7L, new BigDecimal("20.00"), "payment", 55L,
                "payment", "550", "payment:55:owner", "订单余额支付");

        assertEquals(new BigDecimal("80.00"), after.getBalance_wsh());
        verify(walletMapper, never()).deductBalance(any(), any());
        verify(transactionMapper, never()).insert(any(Transaction.class));
    }
}