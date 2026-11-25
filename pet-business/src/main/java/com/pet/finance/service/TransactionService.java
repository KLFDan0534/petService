package com.pet.finance.service;

import com.pet.common.PageResult;
import com.pet.finance.dto.TransactionDTO;
import com.pet.finance.entity.Transaction;
import java.util.List;

public interface TransactionService {
    /**
     * 按用户查询交易记录
     *
     * 业务作用：根据用户 ID 查询该用户的全部交易记录，按创建时间倒序排列。
     * 注意事项：包含所有交易类型（入账/出账/冻结/解冻）。
     *
     * @param userId 用户 ID
     * @return 交易记录列表
     */
    List<Transaction> listByUser(Long userId);
    /**
     * 查询全部交易记录
     *
     * 业务作用：查询所有交易记录（管理员用），按创建时间倒序排列。
     * 注意事项：全量查询，数据量大时可改为分页。
     *
     * @return 交易记录列表
     */
    List<Transaction> listAll();
    /**
     * 按类型查询交易记录（管理员用）
     *
     * 业务作用：按交易类型（如 recharge 充值、admin_adjust 余额调整）过滤查询全部交易记录。
     * 数据处理：type 为空则全量；否则按 type 精确匹配，按创建时间倒序。
     * 注意事项：type 过滤用于运营对账。
     *
     * @param type 交易类型，可为 null（此时等价 listAll）
     * @return 交易记录列表
     */
    List<Transaction> listAllByType(String type);

    /**
     * 分页查询全部交易记录（管理员对账用）
     *
     * 业务作用：替代 listAllByType 的全量拉取，按创建时间倒序分页。
     * 业务规则：type 为空不过滤；size 限制 1~100，越界回落到 20。
     * 注意事项：流水表随时间无限增长，后台页面必须分页，否则迟早拖垮内存。
     *
     * @param type 交易类型，可为 null
     * @param page 页码，从 1 开始
     * @param size 每页条数
     * @return 分页结果
     */
    PageResult<Transaction> pageAllByType(String type, Integer page, Integer size);
    /**
     * 新增交易记录
     *
     * 业务作用：新增一条交易记录，记录钱包余额变化前后的快照。
     * 业务规则：辅助完成资金审计链。
     * 注意事项：通常由 AccountingService 调用，不直接使用。
     *
     * @param tx 交易记录实体
     */
    void add(Transaction tx);

    /**
     * 交易记录转 DTO
     *
     * 业务作用：将交易记录实体转换为 DTO。
     * 业务规则：入参为 null 时返回 null。
     * 注意事项：拷贝全部字段供前端展示。
     *
     * @param entity 交易记录实体
     * @return 交易记录 DTO
     */
    TransactionDTO toDTO(Transaction entity);
}
