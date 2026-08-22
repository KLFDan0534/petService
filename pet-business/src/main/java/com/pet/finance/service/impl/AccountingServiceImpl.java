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

    /**
     * 【业务名称】获取系统用户 ID（实现）
     * 业务作用：返回系统用户 ID 常量 0L。
     * 调用场景：平台账户资金操作。
     * 调用链：systemUserId() → 返回 0L。
     * 数据处理：固定常量。
     * 业务规则：系统用户 ID 固定为 0。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：用于标识平台账户。
     */
    @Override
    public Long systemUserId() {
        return SYSTEM_USER_ID;
    }

    /**
     * 【业务名称】入账（实现）
     * 业务作用：增加用户余额，校验幂等，记录交易流水。
     * 调用场景：充值、收款、退款入账。
     * 调用链：credit() → positive() → alreadyPosted() → WalletService.getByUserId() → WalletMapper.addBalance() → saveTransaction()。
     * 数据处理：金额 > 0 校验 → 幂等校验 → 入账前快照 → SQL 加余额 → 入账后快照 → 记录流水。
     * 业务规则：金额必须 > 0；requestId 重复则跳过。
     * 状态影响：增加 wallet.balance。
     * 异常情况：加余额失败抛 BusinessException("钱包入账失败")。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public Wallet credit(Long userId, BigDecimal amount, String type, Long orderId,
                         String businessType, String businessId, String requestId, String description) {
        return credit(userId, amount, type, orderId, businessType, businessId, requestId, description, null);
    }

    @Transactional
    @Override
    public Wallet credit(Long userId, BigDecimal amount, String type, Long orderId,
                         String businessType, String businessId, String requestId, String description,
                         Long operatorId) {
        BigDecimal money = positive(amount);
        if (alreadyPosted(requestId)) return walletService.getByUserId(userId);
        Wallet before = walletService.getByUserId(userId);
        if (walletMapper.addBalance(userId, money) == 0) {
            throw new BusinessException("钱包入账失败");
        }
        Wallet after = walletService.getByUserId(userId);
        saveTransaction(after, type, money, before, after, "in", orderId, businessType, businessId, requestId, description, operatorId);
        return after;
    }

    /**
     * 【业务名称】出账（实现）
     * 业务作用：扣除用户余额，校验幂等和可用余额，记录流水。
     * 调用场景：消费扣款等。
     * 调用链：debit() → positive() → alreadyPosted() → WalletMapper.deductBalance() → saveTransaction()。
     * 数据处理：金额 > 0 校验 → 幂等校验 → SQL 扣余额 → 记录流水。
     * 业务规则：可用余额 (balance - frozen) 必须充足。
     * 状态影响：减少 wallet.balance。
     * 异常情况：余额不足抛 BusinessException("可用余额不足")。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public Wallet debit(Long userId, BigDecimal amount, String type, Long orderId,
                        String businessType, String businessId, String requestId, String description) {
        return debit(userId, amount, type, orderId, businessType, businessId, requestId, description, null);
    }

    @Transactional
    @Override
    public Wallet debit(Long userId, BigDecimal amount, String type, Long orderId,
                        String businessType, String businessId, String requestId, String description,
                        Long operatorId) {
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
        saveTransaction(after, type, money.negate(), before, after, "out", orderId, businessType, businessId, requestId, description, operatorId);
        return after;
    }

    /**
     * 【业务名称】转账（实现）
     * 业务作用：从付款方转账到收款方，等价于 debit + credit。
     * 调用场景：平台代扣代发、服务费结算。
     * 调用链：transfer() → debit() → credit()。
     * 数据处理：先扣付款方，后加收款方，使用不同 requestId 后缀。
     * 业务规则：金额必须 > 0。
     * 状态影响：付款方余额减少，收款方余额增加。
     * 异常情况：同 debit()/credit()。
     * 注意事项：出账和入账使用 :out/:in 后缀确保幂等。
     */
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

    /**
     * 【业务名称】冻结余额（实现）
     * 业务作用：从可用余额转入冻结金额。
     * 调用场景：提现锁定。
     * 调用链：freeze() → alreadyPosted() → WalletMapper.freeze() → saveTransaction()。
     * 数据处理：校验幂等 → 冻结 SQL → 记录流水。
     * 业务规则：可用余额必须充足。
     * 状态影响：frozen_amount 增加，可用余额减少。
     * 异常情况：不足时抛 BusinessException("可用余额不足")。
     * 注意事项：不改变 balance 总额。
     */
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
        saveTransaction(after, type, money, before, after, "freeze", orderId, businessType, businessId, requestId, description, null);
        return after;
    }

    /**
     * 【业务名称】解冻余额（实现）
     * 业务作用：将冻结金额释放回可用余额。
     * 调用场景：驳回提现。
     * 调用链：unfreeze() → alreadyPosted() → WalletMapper.unfreeze() → saveTransaction()。
     * 数据处理：校验幂等 → 解冻 SQL → 记录流水。
     * 业务规则：冻结余额必须充足。
     * 状态影响：frozen_amount 减少，可用余额增加。
     * 异常情况：不足时抛 BusinessException("冻结余额不足")。
     * 注意事项：对应 freeze 的反向操作。
     */
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
        saveTransaction(after, type, money.negate(), before, after, "unfreeze", orderId, businessType, businessId, requestId, description, null);
        return after;
    }

    /**
     * 【业务名称】消耗冻结金额（实现）
     * 业务作用：从冻结区扣除，同时减少 balance 和 frozen_amount。
     * 调用场景：提现打款完成。
     * 调用链：consumeFrozen() → alreadyPosted() → WalletMapper.transferFrozenToBalance() → saveTransaction()。
     * 数据处理：校验幂等 → 消耗冻结 SQL → 记录流水。
     * 业务规则：冻结余额必须充足。
     * 状态影响：balance 和 frozen_amount 同时减少。
     * 异常情况：不足时抛 BusinessException("冻结余额不足")。
     * 注意事项：freeze → consumeFrozen 完整链路。
     */
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
        saveTransaction(after, type, money.negate(), before, after, "out", orderId, businessType, businessId, requestId, description, null);
        return after;
    }

    /**
     * 【业务名称】管理员调账（实现）
     * 业务作用：管理员直接设置用户余额，记录 admin_adjust 流水。
     * 调用场景：后台手动调账。
     * 调用链：setBalanceByAdmin() → alreadyPosted() → WalletMapper.setBalance() → saveTransaction()。
     * 数据处理：校验幂等 → 调账前快照 → SQL 设置余额 → 调账后快照 → 计算差值 → 记录流水。
     * 业务规则：设置后的余额不可小于冻结金额。
     * 状态影响：强制覆盖 wallet.balance。
     * 异常情况：用户 ID 为空抛 400；余额小于冻结金额抛异常；设余额失败抛异常。
     * 注意事项：@Transactional 保证事务一致性。
     */
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
                description == null || description.isBlank() ? "管理员调整余额: " + adminId : description,
                adminId);
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
                                 String requestId, String description, Long operatorId) {
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
        tx.setOperator_id_wsh(operatorId);
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
