package com.pet.finance.service;

import com.pet.finance.dto.TransactionDTO;
import com.pet.finance.entity.Transaction;
import java.util.List;

public interface TransactionService {
    /**
     * 【业务名称】按用户查询交易记录
     * 业务作用：根据用户 ID 查询该用户的全部交易记录，按创建时间倒序排列。
     * 调用场景：用户查看自己的资金流水明细。
     * 调用链：TransactionService.listByUser() → TransactionMapper.selectList()。
     * 数据处理：按 user_id 精确匹配，按创建时间倒序。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：包含所有交易类型（入账/出账/冻结/解冻）。
     *
     * @param userId 用户 ID
     * @return 交易记录列表
     */
    List<Transaction> listByUser(Long userId);
    /**
     * 【业务名称】查询全部交易记录
     * 业务作用：查询所有交易记录（管理员用），按创建时间倒序排列。
     * 调用场景：后台资金审计和财务对账。
     * 调用链：TransactionService.listAll() → TransactionMapper.selectList()。
     * 数据处理：无条件全量查询，按创建时间倒序。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：全量查询，数据量大时可改为分页。
     *
     * @return 交易记录列表
     */
    List<Transaction> listAll();
    /**
     * 【业务名称】新增交易记录
     * 业务作用：新增一条交易记录，记录钱包余额变化前后的快照。
     * 调用场景：由 AccountingService 在每次资金变动时自动调用。
     * 调用链：TransactionService.add() → TransactionMapper.insert()。
     * 数据处理：插入交易记录实体。
     * 业务规则：辅助完成资金审计链。
     * 状态影响：新增一条交易流水。
     * 异常情况：无。
     * 注意事项：通常由 AccountingService 调用，不直接使用。
     *
     * @param tx 交易记录实体
     */
    void add(Transaction tx);

    /**
     * 【业务名称】交易记录转 DTO
     * 业务作用：将交易记录实体转换为 DTO。
     * 调用场景：对外暴露交易记录信息。
     * 调用链：TransactionService.toDTO()。
     * 数据处理：字段拷贝。
     * 业务规则：入参为 null 时返回 null。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：拷贝全部字段供前端展示。
     *
     * @param entity 交易记录实体
     * @return 交易记录 DTO
     */
    TransactionDTO toDTO(Transaction entity);
}
