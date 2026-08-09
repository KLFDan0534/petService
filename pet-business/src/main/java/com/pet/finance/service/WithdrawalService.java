package com.pet.finance.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
import com.pet.finance.dto.WithdrawalDTO;
import com.pet.finance.entity.Withdrawal;
import java.math.BigDecimal;
import java.util.List;

public interface WithdrawalService {
    /**
     * 【业务名称】按用户查询提现记录
     * 业务作用：查询该用户的历史提现记录，按创建时间倒序。
     * 调用场景：用户查看自己的提现记录。
     * 调用链：WithdrawalService.listByUser() → WithdrawalMapper.selectList()。
     * 数据处理：按 user_id 精确匹配，按创建时间倒序。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：包含所有状态的提现记录。
     *
     * @param userId 用户 ID
     * @return 提现记录列表
     */
    List<Withdrawal> listByUser(Long userId);
    /**
     * 【业务名称】查询全部提现记录
     * 业务作用：查询所有提现记录（管理员用），按创建时间倒序。
     * 调用场景：后台管理查看所有提现申请。
     * 调用链：WithdrawalService.listAll() → WithdrawalMapper.selectList()。
     * 数据处理：无条件全量查询。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：全量查询。
     *
     * @return 提现记录列表
     */
    List<Withdrawal> listAll();
    /**
     * 【业务名称】分页查询提现记录
     * 业务作用：分页查询提现记录（管理员用）。
     * 调用场景：后台分页浏览和筛选提现申请。
     * 调用链：WithdrawalService.listPage() → WithdrawalMapper.selectPage()。
     * 数据处理：按创建时间倒序分页。
     * 业务规则：支持分页参数。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     *
     * @param pageParam 分页参数（页码、每页条数）
     * @return 分页提现数据
     */
    IPage<Withdrawal> listPage(PageRequestDTO pageParam);
    /**
     * 【业务名称】申请提现
     * 业务作用：用户申请提现，创建提现记录并通过 AccountingService 冻结金额。
     * 调用场景：用户钱包提现。
     * 调用链：WithdrawalService.apply() → 创建 Withdrawal → AccountingService.freeze()。
     * 数据处理：创建 pending 状态提现记录 → 调用冻结接口锁定金额。
     * 业务规则：金额必须 > 0；冻结成功后返回 pending 状态记录。
     * 状态影响：新增提现记录；冻结钱包对应金额。
     * 异常情况：金额不合法抛 BusinessException(400)。
     * 注意事项：提现流程：apply → approve/→ complete。
     *
     * @param userId      用户 ID
     * @param amount      提现金额
     * @param bankName    银行名称
     * @param bankCard    银行卡号
     * @param accountName 开户人姓名
     * @return 创建的提现记录（状态 pending）
     */
    Withdrawal apply(Long userId, BigDecimal amount, String bankName, String bankCard, String accountName);
    /**
     * 【业务名称】审批提现
     * 业务作用：审批通过提现申请，更新状态为 approved。
     * 调用场景：后台审批提现申请。
     * 调用链：WithdrawalService.approve() → getById() → 校验状态 → updateById()。
     * 数据处理：仅将 pending 状态的提现更新为 approved。
     * 业务规则：仅状态为 pending 的记录可审批。
     * 状态影响：提现状态从 pending → approved。
     * 异常情况：非 pending 状态抛 BusinessException(400)。
     * 注意事项：审批后还需调用 complete 完成打款。
     *
     * @param id     提现 ID
     * @param remark 审批备注
     * @return 更新后的提现记录（状态 approved）
     */
    Withdrawal approve(Long id, String remark);
    /**
     * 【业务名称】驳回提现
     * 业务作用：驳回提现申请，自动解冻之前冻结的金额。
     * 调用场景：后台驳回提现申请。
     * 调用链：WithdrawalService.reject() → getById() → 校验状态 → updateById() → AccountingService.unfreeze()。
     * 数据处理：更新状态为 rejected → 调用解冻接口释放冻结金额。
     * 业务规则：仅状态为 pending 的记录可驳回。
     * 状态影响：提现状态从 pending → rejected；钱包冻结金额解冻。
     * 异常情况：非 pending 状态抛 BusinessException(400)。
     * 注意事项：驳回后自动解冻金额。
     *
     * @param id     提现 ID
     * @param remark 驳回原因
     * @return 更新后的提现记录（状态 rejected）
     */
    Withdrawal reject(Long id, String remark);
    /**
     * 【业务名称】完成提现
     * 业务作用：完成提现（标记已打款），从冻结金额中完成最终扣款。
     * 调用场景：后台确认已打款。
     * 调用链：WithdrawalService.complete() → getById() → 校验状态 → updateById() → AccountingService.consumeFrozen()。
     * 数据处理：更新状态为 completed → 调用消耗冻结接口扣款。
     * 业务规则：仅状态为 approved 的记录可完成。
     * 状态影响：提现状态从 approved → completed；冻结金额消耗。
     * 异常情况：非 approved 状态抛 BusinessException(400)。
     * 注意事项：调用 AccountingService.consumeFrozen() 完成最终扣款。
     *
     * @param id 提现 ID
     * @return 更新后的提现记录（状态 completed）
     */
    Withdrawal complete(Long id);

    /**
     * 【业务名称】提现转 DTO
     * 业务作用：将提现实体转换为 DTO。
     * 调用场景：对外暴露提现信息。
     * 调用链：WithdrawalService.toDTO()。
     * 数据处理：字段拷贝。
     * 业务规则：入参为 null 时返回 null。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     *
     * @param entity 提现实体
     * @return 提现 DTO
     */
    WithdrawalDTO toDTO(Withdrawal entity);
}
