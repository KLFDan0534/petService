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

    /**
     * 业务作用：注入 WalletService（读钱包）、WalletMapper（原子改余额）与 TransactionMapper（写流水）。
     * 注意事项：余额变更走 Mapper 的原生 SQL，读取走 WalletService，两者职责分离。
     */
    public AccountingServiceImpl(WalletService walletService,
                                 WalletMapper walletMapper,
                                 TransactionMapper transactionMapper) {
        this.walletService = walletService;
        this.walletMapper = walletMapper;
        this.transactionMapper = transactionMapper;
    }

    /**
     * 获取系统用户 ID（实现）
     *
     * 业务作用：返回系统用户 ID 常量 0L。
     * 业务规则：系统用户 ID 固定为 0。
     * 注意事项：用于标识平台账户。
     */
    @Override
    public Long systemUserId() {
        return SYSTEM_USER_ID;
    }

    /**
     * 入账（实现）
     *
     * 业务作用：增加用户余额，校验幂等，记录交易流水。
     * 数据处理：金额 > 0 校验 → 幂等校验 → 入账前快照 → SQL 加余额 → 入账后快照 → 记录流水。
     * 业务规则：金额必须 > 0；requestId 重复则跳过。
     * 异常情况：加余额失败抛 BusinessException("钱包入账失败")。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public Wallet credit(Long userId, BigDecimal amount, String type, Long orderId,
                         String businessType, String businessId, String requestId, String description) {
        return credit(userId, amount, type, orderId, businessType, businessId, requestId, description, null);
    }

    /**
     * 业务作用：增加用户余额并记录一条入账流水，带幂等保护。
     * 数据处理：金额先经 positive 校验并保留 2 位；requestId 已存在则直接返回当前钱包；否则记录入账前快照、原子加余额、再取入账后快照写流水。
     * 业务规则：金额必须大于 0；requestId 非空且已存在时视为重复请求直接返回。
     * 异常情况：金额为空、为负、为 0 抛 400；加余额影响行数为 0 时抛 BusinessException("钱包入账失败")。
     * 注意事项：先查幂等再改余额，两步之间非原子，同一 requestId 高并发下仍可能重复入账。
     */
    @Transactional
    @Override
    public Wallet credit(Long userId, BigDecimal amount, String type, Long orderId,
                         String businessType, String businessId, String requestId, String description,
                         Long operatorId) {
        BigDecimal money = positive(amount);
        // 幂等命中直接返回，避免重复入账
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
     * 出账（实现）
     *
     * 业务作用：扣除用户余额并记录一条出账流水，带幂等与余额不足判断。
     * 数据处理：金额经 positive 校验；幂等命中直接返回；扣款成功后再取钱包快照写流水，流水金额取负值、direction=out。
     * 业务规则：金额必须大于 0；扣款 SQL 内部要求可用余额（balance - frozen）充足。
     * 异常情况：金额非法抛 400；可用余额不足抛 400「可用余额不足」；扣款失败抛「钱包扣款失败」。
     * 注意事项：扣款前先查一次钱包做可用余额预检查，并发场景下最终仍由扣款 SQL 的 WHERE 条件兜底。
     */
    @Transactional
    @Override
    public Wallet debit(Long userId, BigDecimal amount, String type, Long orderId,
                        String businessType, String businessId, String requestId, String description,
                        Long operatorId) {
        BigDecimal money = positive(amount);
        if (alreadyPosted(requestId)) return walletService.getByUserId(userId);
        Wallet before = walletService.getByUserId(userId);

        if (before.getBalance_wsh().subtract(before.getFrozen_amount_wsh()).compareTo(money) < 0)
            throw new BusinessException(400,"可用余额不足");

        // 扣款失败时区分余额不足与其它异常
        if (walletMapper.deductBalance(userId, money) == 0) {
            throw new BusinessException("钱包扣款失败");
        }
//        扣款成功,但是在前面需要判断额度是否足够
 /*           Wallet current = walletService.getByUserId(userId);
            if (current.getBalance_wsh().subtract(current.getFrozen_amount_wsh()).compareTo(money) < 0) {
                throw new BusinessException("可用余额不足");
            }
            throw new BusinessException("钱包扣款失败");*/
        Wallet after = walletService.getByUserId(userId);
        saveTransaction(after, type, money.negate(), before, after, "out", orderId, businessType, businessId, requestId, description, operatorId);
        return after;
    }

    /**
     * 转账（实现）
     *
     * 业务作用：从付款方转账到收款方，等价于 debit + credit。
     * 业务规则：金额必须 > 0。
     * 异常情况：同 debit()/credit()。
     * 注意事项：出账和入账使用 :out/:in 后缀确保幂等。
     */
    @Transactional
    @Override
    public void transfer(Long fromUserId, Long toUserId, BigDecimal amount, String type, Long orderId,
                         String businessType, String businessId, String requestId, String description) {
        // 未传请求ID时用纳秒兜底，避免两笔转账撞幂等
        String baseRequestId = requestId == null || requestId.isBlank()
                ? "transfer:" + businessType + ":" + businessId + ":" + System.nanoTime()
                : requestId;
        // 出账已记账说明整笔转账处理过
        if (alreadyPosted(baseRequestId + ":out")) return;
        BigDecimal money = positive(amount);
        debit(fromUserId, money, type, orderId, businessType, businessId, baseRequestId + ":out", description + " - out", null);
        credit(toUserId, money, type, orderId, businessType, businessId, baseRequestId + ":in", description + " - in");
    }

    /**
     * 冻结余额（实现）
     *
     * 业务作用：从可用余额转入冻结金额。
     * 数据处理：校验幂等 → 冻结 SQL → 记录流水。
     * 业务规则：可用余额必须充足。
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
     * 解冻余额（实现）
     *
     * 业务作用：将冻结金额释放回可用余额。
     * 数据处理：校验幂等 → 解冻 SQL → 记录流水。
     * 业务规则：冻结余额必须充足。
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
     * 消耗冻结金额（实现）
     *
     * 业务作用：从冻结区扣除，同时减少 balance 和 frozen_amount。
     * 数据处理：校验幂等 → 消耗冻结 SQL → 记录流水。
     * 业务规则：冻结余额必须充足。
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
     * 管理员调账（实现）
     *
     * 业务作用：管理员直接设置用户余额，记录 admin_adjust 流水。
     * 数据处理：校验幂等 → 调账前快照 → SQL 设置余额 → 调账后快照 → 计算差值 → 记录流水。
     * 业务规则：设置后的余额不可小于冻结金额。
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
        // 调账后余额不得低于已冻结金额
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

    /**
     * 业务作用：按 requestId 查询流水表，判断该笔请求是否已处理过。
     * 调用场景：credit、debit、freeze、unfreeze、consumeFrozen、setBalanceByAdmin、transfer 的幂等前置判断。
     * 数据处理：requestId 为空或空白时直接返回 false（不做幂等）；否则统计 wallet_transaction_wsh 中该 request_id_wsh 的记录数。
     * 业务规则：计数大于 0 即视为已处理。
     * 注意事项：不传 requestId 的调用没有幂等保护，重复调用会重复记账。
     */
    private boolean alreadyPosted(String requestId) {
        // 未传请求ID时不具备幂等保护
        if (requestId == null || requestId.isBlank()) return false;
        Long count = transactionMapper.selectCount(
                new LambdaQueryWrapper<Transaction>().eq(Transaction::getRequest_id_wsh, requestId));
        return count != null && count > 0;
    }

    /**
     * 业务作用：把一次余额变动的完整前后快照与业务信息写入流水表。
     * 业务规则：状态固定写 success；金额与前后快照全部取自入参。
     * 异常情况：数据库异常由框架抛出。
     * 注意事项：流水只记录成功结果，失败操作不会留下流水；调用方必须保证传入的 before/after 快照真实。
     */
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
        tx.setStatus_wsh("success"); // 失败操作不落流水
        tx.setBusiness_type_wsh(businessType);
        tx.setBusiness_id_wsh(businessId);
        tx.setRequest_id_wsh(requestId);
        tx.setOrder_id_wsh(orderId);
        tx.setDescription_wsh(description);
        tx.setOperator_id_wsh(operatorId);
        transactionMapper.insert(tx);
    }

    /**
     * 业务作用：在 notNegative 基础上要求金额严格大于 0。
     * 业务规则：金额必须大于 0。
     * 异常情况：金额为空抛 400「金额不能为空」；为负抛 400「金额不能小于0」；为 0 抛 400「金额必须大于0」。
     * 注意事项：三种非法情况文案不同，前端需按文案区分时要留意。
     */
    private BigDecimal positive(BigDecimal value) {
        BigDecimal money = notNegative(value);
        // 记账金额必须严格大于 0
        if (money.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "金额必须大于0");
        }
        return money;
    }

    /**
     * 业务作用：校验金额非空且不小于 0，并统一保留 2 位小数。
     * 业务规则：金额不得为负。
     * 异常情况：为 null 抛 400「金额不能为空」；为负抛 400「金额不能小于0」。
     * 注意事项：与 positive 的区别是允许 0，因此调账场景使用本方法。
     */
    private BigDecimal notNegative(BigDecimal value) {
        if (value == null) throw new BusinessException(400, "金额不能为空");
        BigDecimal money = value.setScale(2, RoundingMode.HALF_UP);
        if (money.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(400, "金额不能小于0");
        }
        return money;
    }
}
