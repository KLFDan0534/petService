package com.pet.finance.service;

import com.pet.finance.dto.WalletDTO;
import com.pet.finance.entity.Wallet;
import java.math.BigDecimal;
import java.util.List;

public interface WalletService {
    /**
     * 【业务名称】获取用户钱包（自动创建）
     * 业务作用：根据用户 ID 获取钱包，若不存在则自动创建新钱包（余额为 0）。
     * 调用场景：用户首次进入钱包页、任何需要钱包信息的操作。
     * 调用链：WalletService.getByUserId() → WalletMapper.selectOne() → 不存在则 WalletMapper.createWallet()。
     * 数据处理：每个用户系统内只有唯一一个钱包。
     * 业务规则：不存在自动创建。
     * 状态影响：若钱包不存在则新增一条钱包记录。
     * 异常情况：无。
     * 注意事项：自动创建为方便调用方，无需预先创建钱包。
     *
     * @param userId 用户 ID
     * @return 钱包实体（新建或已有）
     */
    Wallet getByUserId(Long userId);
    /**
     * 【业务名称】按钱包 ID 查询
     * 业务作用：根据钱包 ID 查询钱包详情。
     * 调用场景：后台管理查询钱包、钱包详情页。
     * 调用链：WalletService.getById() → WalletMapper.selectById()。
     * 数据处理：主键查询。
     * 业务规则：不存在时抛出 BusinessException。
     * 状态影响：无。
     * 异常情况：钱包不存在时抛出 BusinessException("钱包不存在")。
     * 注意事项：无。
     *
     * @param id 钱包 ID
     * @return 钱包实体，不存在时抛出 BusinessException
     */
    Wallet getById(Long id);
    /**
     * 【业务名称】增加钱包余额（已废弃）
     * 业务作用：直接增加钱包余额（无流水记录）。
     * 调用场景：已废弃，请使用 AccountingService。
     * 调用链：WalletService.addBalance() → WalletMapper.addBalance()。
     * 数据处理：直接操作余额字段。
     * 业务规则：已废弃，涉及业务资金变动时请使用 AccountingService 保证流水记录和幂等性。
     * 状态影响：增加钱包余额。
     * 异常情况：无。
     * 注意事项：@Deprecated，无流水记录，不幂等。
     *
     * @param userId 用户 ID
     * @param amount 增加金额
     * @deprecated 涉及业务资金变动时请使用 AccountingService，保证流水记录和幂等性
     */
    @Deprecated
    void addBalance(Long userId, BigDecimal amount);

    /**
     * 【业务名称】扣除钱包余额（已废弃）
     * 业务作用：直接扣除钱包余额（含可用余额校验）。
     * 调用场景：已废弃，请使用 AccountingService。
     * 调用链：WalletService.deductBalance() → WalletMapper.deductBalance()。
     * 数据处理：直接操作余额字段。
     * 业务规则：已废弃，涉及业务资金变动时请使用 AccountingService。
     * 状态影响：减少钱包余额。
     * 异常情况：余额不足时抛出 BusinessException("余额不足")。
     * 注意事项：@Deprecated，无流水记录，不幂等。
     *
     * @param userId 用户 ID
     * @param amount 扣除金额
     * @deprecated 涉及业务资金变动时请使用 AccountingService，保证流水记录和幂等性
     */
    @Deprecated
    void deductBalance(Long userId, BigDecimal amount);

    /**
     * 【业务名称】冻结钱包金额（已废弃）
     * 业务作用：冻结钱包中的指定金额（增加冻结金额，可用余额相应减少）。
     * 调用场景：已废弃，请使用 AccountingService。
     * 调用链：WalletService.freeze() → WalletMapper.freeze()。
     * 数据处理：从 balance 中转入 frozen_amount。
     * 业务规则：已废弃，涉及业务资金变动时请使用 AccountingService。
     * 状态影响：增加冻结金额，可用余额减少。
     * 异常情况：可用余额不足时抛出 BusinessException("可用余额不足")。
     * 注意事项：@Deprecated，无流水记录，不幂等。
     *
     * @param userId 用户 ID
     * @param amount 冻结金额
     * @deprecated 涉及业务资金变动时请使用 AccountingService，保证流水记录和幂等性
     */
    @Deprecated
    void freeze(Long userId, BigDecimal amount);

    /**
     * 【业务名称】解冻钱包金额（已废弃）
     * 业务作用：解冻钱包中已冻结的金额（减少冻结金额，可用余额恢复）。
     * 调用场景：已废弃，请使用 AccountingService。
     * 调用链：WalletService.unfreeze() → WalletMapper.unfreeze()。
     * 数据处理：从 frozen_amount 释放回 balance。
     * 业务规则：已废弃，涉及业务资金变动时请使用 AccountingService。
     * 状态影响：减少冻结金额，可用余额恢复。
     * 异常情况：无。
     * 注意事项：@Deprecated，无流水记录，不幂等。
     *
     * @param userId 用户 ID
     * @param amount 解冻金额
     * @deprecated 涉及业务资金变动时请使用 AccountingService，保证流水记录和幂等性
     */
    @Deprecated
    void unfreeze(Long userId, BigDecimal amount);

    /**
     * 【业务名称】消耗冻结金额（已废弃）
     * 业务作用：将冻结金额从冻结区转出并扣减余额，完成冻结资金的最终扣款。
     * 调用场景：已废弃，请使用 AccountingService.consumeFrozen()。
     * 调用链：WalletService.transferFrozenToBalance() → WalletMapper.transferFrozenToBalance()。
     * 数据处理：同时扣减 balance 和 frozen_amount。
     * 业务规则：已废弃，涉及业务资金变动时请使用 AccountingService。
     * 状态影响：冻结金额和余额同时减少。
     * 异常情况：无。
     * 注意事项：@Deprecated，用于提现等场景审核通过后最终扣款。
     *
     * @param userId 用户 ID
     * @param amount 转账金额
     * @deprecated 涉及业务资金变动时请使用 AccountingService，保证流水记录和幂等性
     */
    @Deprecated
    void transferFrozenToBalance(Long userId, BigDecimal amount);
    /**
     * 【业务名称】获取所有钱包列表
     * 业务作用：获取所有钱包列表（按创建时间倒序，最多 1000 条）。
     * 调用场景：管理后台查看所有用户钱包概览。
     * 调用链：WalletService.listAll() → WalletMapper.selectList()。
     * 数据处理：按创建时间倒序，限制 1000 条。
     * 业务规则：最多返回 1000 条。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：默认上限 1000 条。
     *
     * @return 钱包列表
     */
    List<Wallet> listAll();

    /**
     * 【业务名称】钱包实体转 DTO
     * 业务作用：将钱包实体转换为 DTO（脱敏或精简字段）。
     * 调用场景：对外暴露钱包信息时。
     * 调用链：WalletService.toDTO()。
     * 数据处理：字段拷贝。
     * 业务规则：入参为 null 时返回 null。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：仅暴露 ID、用户 ID、余额、冻结金额、创建时间。
     *
     * @param entity 钱包实体
     * @return 钱包 DTO
     */
    WalletDTO toDTO(Wallet entity);
}
