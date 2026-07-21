package com.pet.finance.service;

import com.pet.finance.entity.Wallet;

import java.math.BigDecimal;

public interface AccountingService {
    Long systemUserId();

    Wallet credit(Long userId, BigDecimal amount, String type, Long orderId,
                  String businessType, String businessId, String requestId, String description);

    Wallet debit(Long userId, BigDecimal amount, String type, Long orderId,
                 String businessType, String businessId, String requestId, String description);

    void transfer(Long fromUserId, Long toUserId, BigDecimal amount, String type, Long orderId,
                  String businessType, String businessId, String requestId, String description);

    Wallet freeze(Long userId, BigDecimal amount, String type, Long orderId,
                  String businessType, String businessId, String requestId, String description);

    Wallet unfreeze(Long userId, BigDecimal amount, String type, Long orderId,
                    String businessType, String businessId, String requestId, String description);

    Wallet consumeFrozen(Long userId, BigDecimal amount, String type, Long orderId,
                         String businessType, String businessId, String requestId, String description);

    Wallet setBalanceByAdmin(Long adminId, Long userId, BigDecimal balance, String requestId, String description);
}
