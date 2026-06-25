package com.pet.finance.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.BusinessException;
import com.pet.common.PageParam;
import com.pet.finance.entity.Transaction;
import com.pet.finance.entity.Withdrawal;
import com.pet.finance.mapper.WithdrawalMapper;
import com.pet.finance.service.TransactionService;
import com.pet.finance.service.WalletService;
import com.pet.finance.service.WithdrawalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class WithdrawalServiceImpl implements WithdrawalService {

    private final WithdrawalMapper withdrawalMapper;
    private final WalletService walletService;
    private final TransactionService transactionService;

    public WithdrawalServiceImpl(WithdrawalMapper withdrawalMapper,
                                  WalletService walletService,
                                  TransactionService transactionService) {
        this.withdrawalMapper = withdrawalMapper;
        this.walletService = walletService;
        this.transactionService = transactionService;
    }

    @Override
    public List<Withdrawal> listByUser(Long userId) {
        log.info("调用 listByUser()");
        return withdrawalMapper.selectList(
                new LambdaQueryWrapper<Withdrawal>()
                        .eq(Withdrawal::getUser_id_wsh, userId)
                        .orderByDesc(Withdrawal::getCreated_at_wsh));
    }

    @Override
    public List<Withdrawal> listAll() {
        log.info("调用 listAll()");
        return withdrawalMapper.selectList(
                new LambdaQueryWrapper<Withdrawal>().orderByDesc(Withdrawal::getCreated_at_wsh));
    }

    @Override
    public IPage<Withdrawal> listPage(PageParam pageParam) {
        log.info("调用 listPage()");
        Page<Withdrawal> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        return withdrawalMapper.selectPage(page,
                new LambdaQueryWrapper<Withdrawal>().orderByDesc(Withdrawal::getCreated_at_wsh));
    }

    @Transactional
    @Override
    public Withdrawal apply(Long userId, BigDecimal amount, String bankName, String bankCard, String accountName) {
        log.info("调用 apply()");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new BusinessException("提现金额必须大于 0");
        Withdrawal wd = new Withdrawal();
        wd.setUser_id_wsh(userId);
        wd.setAmount_wsh(amount);
        wd.setFee_wsh(BigDecimal.ZERO);
        wd.setActual_amount_wsh(amount);
        wd.setBank_name_wsh(bankName);
        wd.setBank_card_wsh(bankCard);
        wd.setAccount_name_wsh(accountName);
        wd.setStatus_wsh("pending");
        walletService.freeze(userId, amount);
        withdrawalMapper.insert(wd);
        return wd;
    }

    @Transactional
    @Override
    public Withdrawal approve(Long id, String remark) {
        log.info("调用 approve()");
        Withdrawal wd = getById(id);
        if (!"pending".equals(wd.getStatus_wsh())) throw new BusinessException("仅待审核提现可批准");
        wd.setStatus_wsh("approved");
        if (remark != null) wd.setRemark_wsh(remark);
        withdrawalMapper.updateById(wd);
        walletService.transferFrozenToBalance(wd.getUser_id_wsh(), wd.getAmount_wsh());
        var wallet = walletService.getByUserId(wd.getUser_id_wsh());
        var tx = new Transaction();
        tx.setWallet_id_wsh(wallet.getIdWsh()); tx.setUser_id_wsh(wd.getUser_id_wsh());
        tx.setType_wsh("withdraw"); tx.setAmount_wsh(wd.getAmount_wsh().negate());
        tx.setDescription_wsh("提现已批准 - " + wd.getBank_card_wsh());
        transactionService.add(tx);
        return wd;
    }

    @Transactional
    @Override
    public Withdrawal reject(Long id, String remark) {
        log.info("调用 reject()");
        Withdrawal wd = getById(id);
        if (!"pending".equals(wd.getStatus_wsh())) throw new BusinessException("仅待审核提现可拒绝");
        wd.setStatus_wsh("rejected");
        wd.setRemark_wsh(remark);
        walletService.unfreeze(wd.getUser_id_wsh(), wd.getAmount_wsh());
        withdrawalMapper.updateById(wd);
        return wd;
    }

    @Transactional
    @Override
    public Withdrawal complete(Long id) {
        log.info("调用 complete()");
        Withdrawal wd = getById(id);
        if (!"approved".equals(wd.getStatus_wsh())) throw new BusinessException("仅已批准提现可完成");
        wd.setStatus_wsh("completed");
        withdrawalMapper.updateById(wd);
        return wd;
    }

    private Withdrawal getById(Long id) {
        Withdrawal wd = withdrawalMapper.selectById(id);
        if (wd == null) throw new BusinessException("提现记录不存在");
        return wd;
    }
}
