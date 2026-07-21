package com.pet.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.BusinessException;
import com.pet.common.PageRequestDTO;
import com.pet.finance.dto.WithdrawalDTO;
import com.pet.finance.entity.Withdrawal;
import com.pet.finance.mapper.WithdrawalMapper;
import com.pet.finance.service.AccountingService;
import com.pet.finance.service.WithdrawalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class WithdrawalServiceImpl implements WithdrawalService {

    private final WithdrawalMapper withdrawalMapper;
    private final AccountingService accountingService;

    public WithdrawalServiceImpl(WithdrawalMapper withdrawalMapper, AccountingService accountingService) {
        this.withdrawalMapper = withdrawalMapper;
        this.accountingService = accountingService;
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
    public IPage<Withdrawal> listPage(PageRequestDTO pageParam) {
        log.info("调用 listPage()");
        Page<Withdrawal> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        return withdrawalMapper.selectPage(page,
                new LambdaQueryWrapper<Withdrawal>().orderByDesc(Withdrawal::getCreated_at_wsh));
    }

    @Transactional
    @Override
    public Withdrawal apply(Long userId, BigDecimal amount, String bankName, String bankCard, String accountName) {
        log.info("调用 apply()");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "提现金额必须大于0");
        }
        Withdrawal wd = new Withdrawal();
        wd.setUser_id_wsh(userId);
        wd.setAmount_wsh(amount);
        wd.setFee_wsh(BigDecimal.ZERO);
        wd.setActual_amount_wsh(amount);
        wd.setBank_name_wsh(bankName);
        wd.setBank_card_wsh(bankCard);
        wd.setAccount_name_wsh(accountName);
        wd.setStatus_wsh("pending");
        withdrawalMapper.insert(wd);
        accountingService.freeze(userId, amount, "withdraw", null,
                "withdrawal", String.valueOf(wd.getId_wsh()),
                "withdraw:freeze:" + wd.getId_wsh(), "提现申请冻结 - " + bankCard);
        return wd;
    }

    @Transactional
    @Override
    public Withdrawal approve(Long id, String remark) {
        log.info("调用 approve()");
        Withdrawal wd = getById(id);
        if (!"pending".equals(wd.getStatus_wsh())) {
            throw new BusinessException(400, "仅待审核提现可批准");
        }
        wd.setStatus_wsh("approved");
        if (remark != null) wd.setRemark_wsh(remark);
        withdrawalMapper.updateById(wd);
        return wd;
    }

    @Transactional
    @Override
    public Withdrawal reject(Long id, String remark) {
        log.info("调用 reject()");
        Withdrawal wd = getById(id);
        if (!"pending".equals(wd.getStatus_wsh())) {
            throw new BusinessException(400, "仅待审核提现可驳回");
        }
        wd.setStatus_wsh("rejected");
        wd.setRemark_wsh(remark);
        withdrawalMapper.updateById(wd);
        accountingService.unfreeze(wd.getUser_id_wsh(), wd.getAmount_wsh(), "withdraw", null,
                "withdrawal", String.valueOf(wd.getId_wsh()),
                "withdraw:unfreeze:" + wd.getId_wsh(), "提现驳回解冻 - " + wd.getBank_card_wsh());
        return wd;
    }

    @Transactional
    @Override
    public Withdrawal complete(Long id) {
        log.info("调用 complete()");
        Withdrawal wd = getById(id);
        if (!"approved".equals(wd.getStatus_wsh())) {
            throw new BusinessException(400, "仅已批准提现可完成");
        }
        wd.setStatus_wsh("completed");
        withdrawalMapper.updateById(wd);
        accountingService.consumeFrozen(wd.getUser_id_wsh(), wd.getAmount_wsh(), "withdraw", null,
                "withdrawal", String.valueOf(wd.getId_wsh()),
                "withdraw:complete:" + wd.getId_wsh(), "提现完成 - " + wd.getBank_card_wsh());
        return wd;
    }

    @Override
    public WithdrawalDTO toDTO(Withdrawal entity) {
        if (entity == null) return null;
        WithdrawalDTO dto = new WithdrawalDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setUser_id_wsh(entity.getUser_id_wsh());
        dto.setAmount_wsh(entity.getAmount_wsh());
        dto.setFee_wsh(entity.getFee_wsh());
        dto.setActual_amount_wsh(entity.getActual_amount_wsh());
        dto.setBank_name_wsh(entity.getBank_name_wsh());
        dto.setBank_card_wsh(entity.getBank_card_wsh());
        dto.setAccount_name_wsh(entity.getAccount_name_wsh());
        dto.setStatus_wsh(entity.getStatus_wsh());
        dto.setRemark_wsh(entity.getRemark_wsh());
        dto.setCreated_at_wsh(entity.getCreated_at_wsh());
        return dto;
    }

    private Withdrawal getById(Long id) {
        Withdrawal wd = withdrawalMapper.selectById(id);
        if (wd == null) throw new BusinessException(404, "提现记录不存在");
        return wd;
    }
}
