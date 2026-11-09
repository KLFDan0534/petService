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

    /**
     * 【构造方法】注入提现 Mapper 与账务服务
     *
     * 业务作用：由 Spring 注入 WithdrawalMapper 与 AccountingService，供提现申请、审批与资金冻结解冻使用。
     *
     * 调用场景：Spring 容器启动创建该 Service Bean 时调用。
     *
     * 调用链：Spring 容器 → 本构造方法 → 两个 final 字段赋值。
     *
     * 数据处理：仅字段赋值。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：依赖缺失时由 Spring 抛出 Bean 创建异常。
     *
     * 注意事项：依赖为 final 字段。
     */
    public WithdrawalServiceImpl(WithdrawalMapper withdrawalMapper, AccountingService accountingService) {
        this.withdrawalMapper = withdrawalMapper;
        this.accountingService = accountingService;
    }

    /**
     * 【业务名称】按用户查询提现记录（实现）
     * 业务作用：查询用户的历史提现记录。
     * 调用场景：用户查看提现记录。
     * 调用链：listByUser() → WithdrawalMapper.selectList()。
     * 数据处理：按 user_id 匹配，按创建时间倒序。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
    @Override
    public List<Withdrawal> listByUser(Long userId) {
        log.info("调用 listByUser()");
        return withdrawalMapper.selectList(
                new LambdaQueryWrapper<Withdrawal>()
                        .eq(Withdrawal::getUser_id_wsh, userId)
                        .orderByDesc(Withdrawal::getCreated_at_wsh));
    }

    /**
     * 【业务名称】查询全部提现记录（实现）
     * 业务作用：查询所有提现记录。
     * 调用场景：后台管理。
     * 调用链：listAll() → WithdrawalMapper.selectList()。
     * 数据处理：按创建时间倒序全量查询。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
    @Override
    public List<Withdrawal> listAll() {
        log.info("调用 listAll()");
        return withdrawalMapper.selectList(
                new LambdaQueryWrapper<Withdrawal>().orderByDesc(Withdrawal::getCreated_at_wsh));
    }

    /**
     * 【业务名称】分页查询提现记录（实现）
     * 业务作用：分页查询提现记录。
     * 调用场景：后台分页管理。
     * 调用链：listPage() → WithdrawalMapper.selectPage()。
     * 数据处理：分页查询，按创建时间倒序。
     * 业务规则：支持分页参数。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
    @Override
    public IPage<Withdrawal> listPage(PageRequestDTO pageParam) {
        log.info("调用 listPage()");
        Page<Withdrawal> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        return withdrawalMapper.selectPage(page,
                new LambdaQueryWrapper<Withdrawal>().orderByDesc(Withdrawal::getCreated_at_wsh));
    }

    /**
     * 【业务名称】申请提现（实现）
     * 业务作用：用户申请提现，创建记录并冻结金额。
     * 调用场景：用户钱包提现。
     * 调用链：apply() → 构造 Withdrawal → insert() → AccountingService.freeze()。
     * 数据处理：创建 pending 状态记录 → 调用 AccountingService.freeze() 冻结金额。
     * 业务规则：金额必须 > 0。
     * 状态影响：新增提现记录；钱包冻结对应金额。
     * 异常情况：金额不合法抛 BusinessException(400)。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public Withdrawal apply(Long userId, BigDecimal amount, String bankName, String bankCard, String accountName) {
        log.info("调用 apply()");
        // 提现金额必须大于 0
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
        // 落单后立即冻结对应金额
        accountingService.freeze(userId, amount, "withdraw", null,
                "withdrawal", String.valueOf(wd.getId_wsh()),
                "withdraw:freeze:" + wd.getId_wsh(), "提现申请冻结 - " + bankCard);
        return wd;
    }

    /**
     * 【业务名称】审批提现（实现）
     * 业务作用：审批通过提现申请。
     * 调用场景：后台审批。
     * 调用链：approve() → getById() → 校验状态 → updateById()。
     * 数据处理：更新状态为 approved，设置审批备注。
     * 业务规则：仅 pending 状态可审批。
     * 状态影响：提现状态 pending → approved。
     * 异常情况：非 pending 抛 BusinessException(400)。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public Withdrawal approve(Long id, String remark) {
        log.info("调用 approve()");
        Withdrawal wd = getById(id);
        // 仅待审核的提现可批准
        if (!"pending".equals(wd.getStatus_wsh())) {
            throw new BusinessException(400, "仅待审核提现可批准");
        }
        wd.setStatus_wsh("approved");
        if (remark != null) wd.setRemark_wsh(remark);
        withdrawalMapper.updateById(wd);
        return wd;
    }

    /**
     * 【业务名称】驳回提现（实现）
     * 业务作用：驳回提现申请，自动解冻金额。
     * 调用场景：后台驳回。
     * 调用链：reject() → getById() → 校验状态 → updateById() → AccountingService.unfreeze()。
     * 数据处理：更新状态为 rejected → 调用解冻接口。
     * 业务规则：仅 pending 状态可驳回。
     * 状态影响：提现状态 pending → rejected；钱包解冻。
     * 异常情况：非 pending 抛 BusinessException(400)。
     * 注意事项：@Transactional 保证事务一致性。
     */
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
        // 驳回时解冻申请时冻结的金额
        accountingService.unfreeze(wd.getUser_id_wsh(), wd.getAmount_wsh(), "withdraw", null,
                "withdrawal", String.valueOf(wd.getId_wsh()),
                "withdraw:unfreeze:" + wd.getId_wsh(), "提现驳回解冻 - " + wd.getBank_card_wsh());
        return wd;
    }

    /**
     * 【业务名称】完成提现（实现）
     * 业务作用：完成提现，消耗冻结金额。
     * 调用场景：后台确认打款。
     * 调用链：complete() → getById() → 校验状态 → updateById() → AccountingService.consumeFrozen()。
     * 数据处理：更新状态为 completed → 调用消耗冻结接口。
     * 业务规则：仅 approved 状态可完成。
     * 状态影响：提现状态 approved → completed；冻结金额消耗。
     * 异常情况：非 approved 抛 BusinessException(400)。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public Withdrawal complete(Long id) {
        log.info("调用 complete()");
        Withdrawal wd = getById(id);
        // 仅已批准的提现可完成打款
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

    /**
     * 【业务名称】提现转 DTO（实现）
     * 业务作用：将提现实体转换为 DTO。
     * 调用场景：对外暴露提现信息。
     * 调用链：toDTO() → 字段拷贝。
     * 数据处理：字段逐一拷贝。
     * 业务规则：入参为 null 时返回 null。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
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

    /**
     * 【加载提现单】按ID取提现记录并在缺失时抛错
     *
     * 业务作用：approve、reject、complete 三个状态流转方法共用的加载与存在性校验。
     *
     * 调用场景：approve、reject、complete 中调用。
     *
     * 调用链：approve / reject / complete → 本方法 → withdrawalMapper.selectById。
     *
     * 数据处理：输入 id → 输出 Withdrawal 实体。
     *
     * 业务规则：提现记录必须存在。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：记录不存在抛 BusinessException(404, "提现记录不存在")。
     *
     * 注意事项：id 为 null 时 selectById 返回 null，同样抛 404 而非 400；本方法为 private，与接口中可能的同名查询方法无关。
     */
    private Withdrawal getById(Long id) {
        Withdrawal wd = withdrawalMapper.selectById(id);
        if (wd == null) throw new BusinessException(404, "提现记录不存在");
        return wd;
    }
}
