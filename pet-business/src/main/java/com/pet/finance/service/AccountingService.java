package com.pet.finance.service;

import com.pet.finance.entity.Wallet;

import java.math.BigDecimal;

public interface AccountingService {
    /**
     * 【业务名称】获取系统用户 ID
     * 业务作用：获取系统用户 ID（用于平台级账务操作，如平台收款或平台账户资金变动）。
     * 调用场景：平台账户资金操作，如会员支付收款入账。
     * 调用链：AccountingService.systemUserId() → 返回常量。
     * 数据处理：返回固定常量 0L。
     * 业务规则：系统用户 ID 固定为 0。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：系统用户 ID 用于标识平台账户，对应钱包表的系统用户。
     *
     * @return 系统用户 ID 常量
     */
    Long systemUserId();

    /**
     * 【业务名称】入账
     * 业务作用：增加用户余额，自动记录交易流水并校验幂等。
     * 调用场景：用户充值、商家收款、退款入账等。
     * 调用链：credit() → alreadyPosted() → WalletMapper.addBalance() → WalletService.getByUserId() → saveTransaction()。
     * 数据处理：校验幂等 → 获取入账前钱包 → 执行加余额 SQL → 获取入账后钱包 → 记录流水。
     * 业务规则：金额必须 > 0；requestId 重复则不重复入账。
     * 状态影响：增加用户 wallet.balance。
     * 异常情况：钱包加余额失败时抛 BusinessException("钱包入账失败")。
     * 注意事项：需保证幂等性，相同 requestId 只生效一次。
     *
     * @param userId       目标用户 ID
     * @param amount       入账金额（必须 > 0）
     * @param type         交易类型标识
     * @param orderId      关联订单 ID（可选）
     * @param businessType 业务类型
     * @param businessId   业务 ID
     * @param requestId    幂等请求 ID（相同 requestId 只生效一次）
     * @param description  交易描述
     * @return 入账后的钱包
     */
    Wallet credit(Long userId, BigDecimal amount, String type, Long orderId,
                  String businessType, String businessId, String requestId, String description);

    /**
     * 【业务名称】入账（带操作人）
     * 业务作用：增加用户余额，并在流水中记录操作人用户 ID。
     * 调用场景：管理员调账中的「增加余额」操作，用于审计追踪操作人。
     * 注意事项：operatorId 仅用于记录『谁执行了本次操作』，目标用户仍为 userId。
     *
     * @return 入账后的钱包
     */
    Wallet credit(Long userId, BigDecimal amount, String type, Long orderId,
                  String businessType, String businessId, String requestId, String description, Long operatorId);

    /**
     * 【业务名称】出账
     * 业务作用：扣除用户余额，自动校验可用余额，记录流水并校验幂等。
     * 调用场景：用户消费、提现扣款等。
     * 调用链：debit() → alreadyPosted() → WalletMapper.deductBalance() → saveTransaction()。
     * 数据处理：校验幂等 → 校验可用余额 → 执行扣余额 SQL → 记录流水。
     * 业务规则：金额必须 > 0；可用余额 (balance - frozen) 必须充足。
     * 状态影响：减少用户 wallet.balance。
     * 异常情况：可用余额不足时抛 BusinessException("可用余额不足")；扣款失败时抛 BusinessException("钱包扣款失败")。
     * 注意事项：可用余额 = balance - frozen_amount。
     *
     * @param userId       目标用户 ID
     * @param amount       出账金额（必须 > 0）
     * @param type         交易类型标识
     * @param orderId      关联订单 ID（可选）
     * @param businessType 业务类型
     * @param businessId   业务 ID
     * @param requestId    幂等请求 ID
     * @param description  交易描述
     * @return 出账后的钱包
     */
    Wallet debit(Long userId, BigDecimal amount, String type, Long orderId,
                 String businessType, String businessId, String requestId, String description);

    /**
     * 【业务名称】出账（带操作人）
     * 业务作用：扣除用户余额，并在流水中记录操作人用户 ID。
     * 调用场景：管理员调账中的「扣减余额」操作，用于审计追踪操作人。
     * 注意事项：operatorId 仅用于记录『谁执行了本次操作』，目标用户仍为 userId。
     *
     * @return 出账后的钱包
     */
    Wallet debit(Long userId, BigDecimal amount, String type, Long orderId,
                 String businessType, String businessId, String requestId, String description, Long operatorId);

    /**
     * 【业务名称】用户间转账
     * 业务作用：从 fromUserId 转账到 toUserId，等价于 debit + credit。
     * 调用场景：平台代扣代发、服务费结算等。
     * 调用链：transfer() → debit(fromUser) → credit(toUser)。
     * 数据处理：先扣付款方余额，再加收款方余额，共用同一条幂等链路。
     * 业务规则：金额必须 > 0；付款方可用余额必须充足。
     * 状态影响：付款方余额减少，收款方余额增加。
     * 异常情况：同 debit() 和 credit()。
     * 注意事项：出账和入账使用不同 requestId 后缀（:out/:in）避免幂等冲突。
     *
     * @param fromUserId   付款方用户 ID
     * @param toUserId     收款方用户 ID
     * @param amount       转账金额
     * @param type         交易类型标识
     * @param orderId      关联订单 ID（可选）
     * @param businessType 业务类型
     * @param businessId   业务 ID
     * @param requestId    幂等请求 ID
     * @param description  交易描述
     */
    void transfer(Long fromUserId, Long toUserId, BigDecimal amount, String type, Long orderId,
                  String businessType, String businessId, String requestId, String description);

    /**
     * 【业务名称】冻结余额
     * 业务作用：冻结用户余额中指定金额，从可用余额转入冻结金额。
     * 调用场景：提现锁定额度、保证金冻结等。
     * 调用链：freeze() → alreadyPosted() → WalletMapper.freeze() → saveTransaction()。
     * 数据处理：校验幂等 → 冻结 SQL → 记录流水。
     * 业务规则：可用余额必须充足。
     * 状态影响：增加 frozen_amount，减少可用余额（balance 不变）。
     * 异常情况：可用余额不足时抛 BusinessException("可用余额不足")。
     * 注意事项：仅记录流水，不改变实际余额总额。
     *
     * @param userId       用户 ID
     * @param amount       冻结金额
     * @param type         交易类型标识
     * @param orderId      关联订单 ID（可选）
     * @param businessType 业务类型
     * @param businessId   业务 ID
     * @param requestId    幂等请求 ID
     * @param description  冻结描述
     * @return 冻结后的钱包
     */
    Wallet freeze(Long userId, BigDecimal amount, String type, Long orderId,
                  String businessType, String businessId, String requestId, String description);

    /**
     * 【业务名称】解冻余额
     * 业务作用：解冻用户冻结金额，释放回可用余额。
     * 调用场景：驳回提现、退还保证金等。
     * 调用链：unfreeze() → alreadyPosted() → WalletMapper.unfreeze() → saveTransaction()。
     * 数据处理：校验幂等 → 解冻 SQL → 记录流水。
     * 业务规则：冻结余额必须充足。
     * 状态影响：减少 frozen_amount，增加可用余额。
     * 异常情况：冻结余额不足时抛 BusinessException("冻结余额不足")。
     * 注意事项：对应 freeze 的反向操作。
     *
     * @param userId       用户 ID
     * @param amount       解冻金额
     * @param type         交易类型标识
     * @param orderId      关联订单 ID（可选）
     * @param businessType 业务类型
     * @param businessId   业务 ID
     * @param requestId    幂等请求 ID
     * @param description  解冻描述
     * @return 解冻后的钱包
     */
    Wallet unfreeze(Long userId, BigDecimal amount, String type, Long orderId,
                    String businessType, String businessId, String requestId, String description);

    /**
     * 【业务名称】消耗冻结金额
     * 业务作用：从冻结区扣除（减少 balance 和 frozen_amount），完成冻结资金的最终扣款。
     * 调用场景：提现打款完成等。
     * 调用链：consumeFrozen() → alreadyPosted() → WalletMapper.transferFrozenToBalance() → saveTransaction()。
     * 数据处理：校验幂等 → 消耗冻结 SQL → 记录流水。
     * 业务规则：冻结余额必须充足。
     * 状态影响：balance 和 frozen_amount 同时减少。
     * 异常情况：冻结余额不足时抛 BusinessException("冻结余额不足")。
     * 注意事项：freeze 锁定 → consumeFrozen 扣除 的完整链路。
     *
     * @param userId       用户 ID
     * @param amount       消耗金额
     * @param type         交易类型标识
     * @param orderId      关联订单 ID（可选）
     * @param businessType 业务类型
     * @param businessId   业务 ID
     * @param requestId    幂等请求 ID
     * @param description  描述
     * @return 操作后的钱包
     */
    Wallet consumeFrozen(Long userId, BigDecimal amount, String type, Long orderId,
                         String businessType, String businessId, String requestId, String description);

    /**
     * 【业务名称】管理员调账
     * 业务作用：管理员直接设置用户余额（覆盖式），记录 admin_adjust 类型流水。
     * 调用场景：后台手动调账。
     * 调用链：setBalanceByAdmin() → alreadyPosted() → WalletMapper.setBalance() → saveTransaction()。
     * 数据处理：校验幂等 → 设置余额 SQL → 计算差值 → 记录流水。
     * 业务规则：设置后的余额不可小于冻结金额。
     * 状态影响：强制覆盖 wallet.balance。
     * 异常情况：用户 ID 为空抛 BusinessException；余额小于冻结金额抛 BusinessException。
     * 注意事项：仅用于后台手动调账，需谨慎操作。
     *
     * @param adminId     执行调账的管理员用户 ID
     * @param userId      目标用户 ID
     * @param balance     目标余额
     * @param requestId   幂等请求 ID
     * @param description 调账原因描述
     * @return 设置后的钱包
     */
    Wallet setBalanceByAdmin(Long adminId, Long userId, BigDecimal balance, String requestId, String description);
}
