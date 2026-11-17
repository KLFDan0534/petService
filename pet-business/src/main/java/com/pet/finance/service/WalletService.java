package com.pet.finance.service;

import com.pet.finance.dto.WalletDTO;
import com.pet.finance.entity.Wallet;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
import java.math.BigDecimal;
import java.util.List;

public interface WalletService {
    /**
     * 获取用户钱包（自动创建）
     *
     * 业务作用：根据用户 ID 获取钱包，若不存在则自动创建新钱包（余额为 0）。
     * 业务规则：不存在自动创建。
     * 注意事项：自动创建为方便调用方，无需预先创建钱包。
     *
     * @param userId 用户 ID
     * @return 钱包实体（新建或已有）
     */
    Wallet getByUserId(Long userId);
    /**
     * 按钱包 ID 查询
     *
     * 业务作用：根据钱包 ID 查询钱包详情。
     * 业务规则：不存在时抛出 BusinessException。
     * 异常情况：钱包不存在时抛出 BusinessException("钱包不存在")。
     *
     * @param id 钱包 ID
     * @return 钱包实体，不存在时抛出 BusinessException
     */
    Wallet getById(Long id);
    /**
     * 增加钱包余额（已废弃）
     *
     * 业务作用：直接增加钱包余额（无流水记录）。
     * 业务规则：已废弃，涉及业务资金变动时请使用 AccountingService 保证流水记录和幂等性。
     * 注意事项：@Deprecated，无流水记录，不幂等。
     *
     * @param userId 用户 ID
     * @param amount 增加金额
     * @deprecated 涉及业务资金变动时请使用 AccountingService，保证流水记录和幂等性
     */
    @Deprecated
    void addBalance(Long userId, BigDecimal amount);

    /**
     * 扣除钱包余额（已废弃）
     *
     * 业务作用：直接扣除钱包余额（含可用余额校验）。
     * 业务规则：已废弃，涉及业务资金变动时请使用 AccountingService。
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
     * 冻结钱包金额（已废弃）
     *
     * 业务作用：冻结钱包中的指定金额（增加冻结金额，可用余额相应减少）。
     * 业务规则：已废弃，涉及业务资金变动时请使用 AccountingService。
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
     * 解冻钱包金额（已废弃）
     *
     * 业务作用：解冻钱包中已冻结的金额（减少冻结金额，可用余额恢复）。
     * 业务规则：已废弃，涉及业务资金变动时请使用 AccountingService。
     * 注意事项：@Deprecated，无流水记录，不幂等。
     *
     * @param userId 用户 ID
     * @param amount 解冻金额
     * @deprecated 涉及业务资金变动时请使用 AccountingService，保证流水记录和幂等性
     */
    @Deprecated
    void unfreeze(Long userId, BigDecimal amount);

    /**
     * 消耗冻结金额（已废弃）
     *
     * 业务作用：将冻结金额从冻结区转出并扣减余额，完成冻结资金的最终扣款。
     * 业务规则：已废弃，涉及业务资金变动时请使用 AccountingService。
     * 注意事项：@Deprecated，用于提现等场景审核通过后最终扣款。
     *
     * @param userId 用户 ID
     * @param amount 转账金额
     * @deprecated 涉及业务资金变动时请使用 AccountingService，保证流水记录和幂等性
     */
    @Deprecated
    void transferFrozenToBalance(Long userId, BigDecimal amount);
    /**
     * 获取所有钱包列表
     *
     * 业务作用：获取所有钱包列表（按创建时间倒序，最多 1000 条）。
     * 业务规则：最多返回 1000 条。
     * 注意事项：默认上限 1000 条。
     *
     * @return 钱包列表
     */
    List<Wallet> listAll();

    /**
     * 分页获取钱包列表
     *
     * 业务作用：分页获取所有用户钱包，关联用户名，支持按 id / balance / created 动态排序。
     * 业务规则：支持分页参数（page/size）与排序参数（sort_by_wsh / order_wsh）。
     *
     * @param pageParam 分页与排序参数（页码、每页条数、排序字段、排序方向）
     * @return 分页结果（含用户名，记录数为 DTO）
     */
    IPage<WalletDTO> listPage(PageRequestDTO pageParam);

    /**
     * 钱包实体转 DTO
     *
     * 业务作用：将钱包实体转换为 DTO（脱敏或精简字段）。
     * 业务规则：入参为 null 时返回 null。
     * 注意事项：仅暴露 ID、用户 ID、余额、冻结金额、创建时间。
     *
     * @param entity 钱包实体
     * @return 钱包 DTO
     */
    WalletDTO toDTO(Wallet entity);
}
