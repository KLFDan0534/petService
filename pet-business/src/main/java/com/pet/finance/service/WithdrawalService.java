package com.pet.finance.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
import com.pet.finance.dto.WithdrawalDTO;
import com.pet.finance.entity.Withdrawal;
import java.math.BigDecimal;
import java.util.List;

public interface WithdrawalService {
    /**
     * 按用户查询提现记录
     *
     * 业务作用：查询该用户的历史提现记录，按创建时间倒序。
     * 注意事项：包含所有状态的提现记录。
     *
     * @param userId 用户 ID
     * @return 提现记录列表
     */
    List<Withdrawal> listByUser(Long userId);
    /**
     * 查询全部提现记录
     *
     * 业务作用：查询所有提现记录（管理员用），按创建时间倒序。
     * 注意事项：全量查询。
     *
     * @return 提现记录列表
     */
    List<Withdrawal> listAll();
    /**
     * 分页查询提现记录
     *
     * 业务作用：分页查询提现记录（管理员用）。
     * 业务规则：支持分页参数。
     *
     * @param pageParam 分页参数（页码、每页条数）
     * @return 分页提现数据
     */
    IPage<Withdrawal> listPage(PageRequestDTO pageParam);
    /**
     * 申请提现
     *
     * 业务作用：用户申请提现，创建提现记录并通过 AccountingService 冻结金额。
     * 业务规则：金额必须 > 0；冻结成功后返回 pending 状态记录。
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
     * 审批提现
     *
     * 业务作用：审批通过提现申请，更新状态为 approved。
     * 业务规则：仅状态为 pending 的记录可审批。
     * 异常情况：非 pending 状态抛 BusinessException(400)。
     * 注意事项：审批后还需调用 complete 完成打款。
     *
     * @param id     提现 ID
     * @param remark 审批备注
     * @return 更新后的提现记录（状态 approved）
     */
    Withdrawal approve(Long id, String remark);
    /**
     * 驳回提现
     *
     * 业务作用：驳回提现申请，自动解冻之前冻结的金额。
     * 业务规则：仅状态为 pending 的记录可驳回。
     * 异常情况：非 pending 状态抛 BusinessException(400)。
     * 注意事项：驳回后自动解冻金额。
     *
     * @param id     提现 ID
     * @param remark 驳回原因
     * @return 更新后的提现记录（状态 rejected）
     */
    Withdrawal reject(Long id, String remark);
    /**
     * 完成提现
     *
     * 业务作用：完成提现（标记已打款），从冻结金额中完成最终扣款。
     * 业务规则：仅状态为 approved 的记录可完成。
     * 异常情况：非 approved 状态抛 BusinessException(400)。
     * 注意事项：调用 AccountingService.consumeFrozen() 完成最终扣款。
     *
     * @param id 提现 ID
     * @return 更新后的提现记录（状态 completed）
     */
    Withdrawal complete(Long id);

    /**
     * 提现转 DTO
     *
     * 业务作用：将提现实体转换为 DTO。
     * 业务规则：入参为 null 时返回 null。
     *
     * @param entity 提现实体
     * @return 提现 DTO
     */
    WithdrawalDTO toDTO(Withdrawal entity);
}
