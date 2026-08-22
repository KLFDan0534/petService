package com.pet.finance.service.impl;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.finance.dto.WalletDTO;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.common.PageRequestDTO;

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

    /**
     * 【业务名称】获取用户钱包（实现-自动创建）
     * 业务作用：获取用户钱包，不存在则自动创建（余额 0，冻结金额 0）。
     * 调用场景：用户进入钱包页、资金操作前置调用。
     * 调用链：getByUserId() → WalletMapper.selectOne() → createWallet() → 递归调用自身 → selectOne()。
     * 数据处理：按 user_id 精确匹配。
     * 业务规则：钱包不存在则自动创建后再次查询。
     * 状态影响：若钱包不存在则新增一条钱包记录。
     * 异常情况：无。
     * 注意事项：创建后再查询确保获取到完整数据。
     */
    @Override
    public Wallet getByUserId(Long userId) {
        log.info("调用 getByUserId()");
        Wallet w = walletMapper.selectOne(
                new LambdaQueryWrapper<Wallet>().eq(Wallet::getUser_id_wsh, userId));
        if (w == null) {
            walletMapper.createWallet(userId);
            return getByUserId(userId);
        }
        return w;
    }

    /**
     * 【业务名称】按 ID 查询钱包（实现）
     * 业务作用：根据钱包 ID 查询钱包。
     * 调用场景：后台管理查询。
     * 调用链：getById() → WalletMapper.selectById()。
     * 数据处理：主键查询。
     * 业务规则：不存在则抛异常。
     * 状态影响：无。
     * 异常情况：不存在时抛出 BusinessException("钱包不存在")。
     * 注意事项：无。
     */
    @Override
    public Wallet getById(Long id) {
        log.info("调用 getById()");
        Wallet w = walletMapper.selectById(id);
        if (w == null) throw new BusinessException("钱包不存在");
        return w;
    }

    /**
     * 【业务名称】增加余额（实现-已废弃）
     * 业务作用：直接增加钱包余额（无流水记录）。
     * 调用场景：已废弃。
     * 调用链：addBalance() → WalletMapper.addBalance()。
     * 数据处理：直接 SQL 增加 balance。
     * 业务规则：已废弃，请使用 AccountingService.credit()。
     * 状态影响：增加钱包余额。
     * 异常情况：无。
     * 注意事项：@Deprecated，无流水记录，不幂等。
     */
    @Transactional
    @Override
    @Deprecated
    public void addBalance(Long userId, BigDecimal amount) {
        log.info("调用 addBalance()");
        walletMapper.addBalance(userId, amount);
    }

    /**
     * 【业务名称】扣除余额（实现-已废弃）
     * 业务作用：直接扣除钱包余额（含可用余额校验）。
     * 调用场景：已废弃。
     * 调用链：deductBalance() → WalletMapper.deductBalance()。
     * 数据处理：直接 SQL 扣减 balance，校验影响行数。
     * 业务规则：已废弃，请使用 AccountingService.debit()。
     * 状态影响：减少钱包余额。
     * 异常情况：余额不足时抛出 BusinessException("余额不足")；扣款失败时抛 BusinessException("扣款失败")。
     * 注意事项：@Deprecated，无流水记录，不幂等。
     */
    @Transactional
    @Override
    @Deprecated
    public void deductBalance(Long userId, BigDecimal amount) {
        log.info("调用 deductBalance()");
        int affected = walletMapper.deductBalance(userId, amount);
        if (affected == 0) {
            Wallet w = getByUserId(userId);
            if (w.getBalance_wsh().compareTo(amount) < 0)
                throw new BusinessException("余额不足");
            throw new BusinessException("扣款失败");
        }
    }

    /**
     * 【业务名称】冻结金额（实现-已废弃）
     * 业务作用：冻结钱包金额，将 balance 中的一部分转入 frozen_amount。
     * 调用场景：已废弃。
     * 调用链：freeze() → WalletMapper.freeze()。
     * 数据处理：SQL 更新 balance 和 frozen_amount。
     * 业务规则：可用余额需足够。
     * 状态影响：增加冻结金额，可用余额减少。
     * 异常情况：可用余额不足时抛出 BusinessException("可用余额不足")。
     * 注意事项：@Deprecated，无流水记录，不幂等。
     */
    @Transactional
    @Override
    @Deprecated
    public void freeze(Long userId, BigDecimal amount) {
        log.info("调用 freeze()");
        int affected = walletMapper.freeze(userId, amount);
        if (affected == 0) {
            Wallet w = getByUserId(userId);
            if (w.getBalance_wsh().subtract(w.getFrozen_amount_wsh()).compareTo(amount) < 0)
                throw new BusinessException("可用余额不足");
            throw new BusinessException("冻结失败");
        }
    }

    /**
     * 【业务名称】解冻金额（实现-已废弃）
     * 业务作用：解冻钱包金额，将 frozen_amount 中的指定金额释放回可用余额。
     * 调用场景：已废弃。
     * 调用链：unfreeze() → WalletMapper.unfreeze()。
     * 数据处理：SQL 更新 balance 和 frozen_amount。
     * 业务规则：已废弃，请使用 AccountingService.unfreeze()。
     * 状态影响：减少冻结金额，可用余额恢复。
     * 异常情况：无。
     * 注意事项：@Deprecated，无流水记录，不幂等。
     */
    @Transactional
    @Override
    @Deprecated
    public void unfreeze(Long userId, BigDecimal amount) {
        log.info("调用 unfreeze()");
        walletMapper.unfreeze(userId, amount);
    }

    /**
     * 【业务名称】消耗冻结金额（实现-已废弃）
     * 业务作用：将冻结金额从冻结区扣除，同时扣减 balance 和 frozen_amount。
     * 调用场景：已废弃，用于提现审核通过后的最终扣款。
     * 调用链：transferFrozenToBalance() → WalletMapper.transferFrozenToBalance()。
     * 数据处理：SQL 同时扣减 balance 和 frozen_amount。
     * 业务规则：已废弃，请使用 AccountingService.consumeFrozen()。
     * 状态影响：冻结金额和余额同时减少。
     * 异常情况：无。
     * 注意事项：@Deprecated，无流水记录，不幂等。
     */
    @Transactional
    @Override
    @Deprecated
    public void transferFrozenToBalance(Long userId, BigDecimal amount) {
        log.info("调用 transferFrozenToBalance()");
        walletMapper.transferFrozenToBalance(userId, amount);
    }

    /**
     * 【业务名称】钱包转 DTO（实现）
     * 业务作用：将钱包实体转换为 DTO，仅暴露 ID、用户 ID、余额、冻结金额、创建时间。
     * 调用场景：对外暴露钱包信息。
     * 调用链：toDTO() → 字段拷贝。
     * 数据处理：字段逐一拷贝。
     * 业务规则：入参为 null 时返回 null。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
    @Override
    public WalletDTO toDTO(Wallet entity) {
        if (entity == null) return null;
        WalletDTO dto = new WalletDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setUser_id_wsh(entity.getUser_id_wsh());
        dto.setBalance_wsh(entity.getBalance_wsh());
        dto.setFrozen_amount_wsh(entity.getFrozen_amount_wsh());
        dto.setCreated_at_wsh(entity.getCreated_at_wsh());
        return dto;
    }

    /**
     * 【业务名称】获取所有钱包（实现）
     * 业务作用：获取所有钱包列表（按创建时间倒序，最多 1000 条）。
     * 调用场景：管理后台查看所有用户钱包概览。
     * 调用链：listAll() → WalletMapper.selectList()。
     * 数据处理：按创建时间倒序，限制 1000 条。
     * 业务规则：最多返回 1000 条。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
    @Override
    public List<Wallet> listAll() {
        log.info("调用 listAll()");
        return walletMapper.selectList(
                new LambdaQueryWrapper<Wallet>()
                        .orderByDesc(Wallet::getCreated_at_wsh)
                        .last("LIMIT 1000"));
    }

    /**
     * 【业务名称】分页获取钱包列表（实现）
     * 业务作用：分页获取所有钱包，关联用户名，支持按 id / balance / created 动态排序。
     * 调用场景：管理后台钱包列表分页。
     * 调用链：listPage() → WalletMapper.selectWalletPage()。
     * 数据处理：按白名单排序字段与方向分页。
     * 业务规则：支持分页参数（page/size）与排序参数（sort_by_wsh / order_wsh）；排序字段/方向均经白名单校验。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
    @Override
    public IPage<WalletDTO> listPage(PageRequestDTO pageParam) {
        log.info("调用 listPage() sort={} order={}",
                pageParam.getSort_by_wsh(), pageParam.getOrder_wsh());
        Page<WalletDTO> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        return walletMapper.selectWalletPage(page,
                resolveOrderColumn(pageParam.getSort_by_wsh()),
                resolveOrderDirection(pageParam.getOrder_wsh()));
    }

    private String resolveOrderColumn(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return "w.created_at_wsh";
        switch (sortBy.trim().toLowerCase()) {
            case "id": return "w.id_wsh";
            case "user_id": return "w.user_id_wsh";
            case "balance": return "w.balance_wsh";
            case "created":
            default: return "w.created_at_wsh";
        }
    }

    private String resolveOrderDirection(String order) {
        return (order != null && "asc".equalsIgnoreCase(order.trim())) ? "ASC" : "DESC";
    }
}
