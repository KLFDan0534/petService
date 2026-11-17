package com.pet.finance.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.finance.dto.TransactionDTO;
import com.pet.finance.entity.Transaction;
import com.pet.finance.mapper.TransactionMapper;
import com.pet.finance.service.TransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionMapper transactionMapper;

    /**
     * 业务规则：依赖为 final 字段。
     * 注意事项：本类只提供查询能力，交易流水写入由记账链路负责。
     */
    public TransactionServiceImpl(TransactionMapper transactionMapper) {
        this.transactionMapper = transactionMapper;
    }

    /**
     * 按用户查询交易记录（实现）
     *
     * 业务作用：查询用户全部交易流水，按创建时间倒序。
     */
    @Override
    public List<Transaction> listByUser(Long userId) {
        log.info("调用 listByUser()");
        return transactionMapper.selectList(
                new LambdaQueryWrapper<Transaction>()
                        .eq(Transaction::getUser_id_wsh, userId)
                        .orderByDesc(Transaction::getCreated_at_wsh));
    }

    /**
     * 查询全部交易记录（实现）
     *
     * 业务作用：查询全部交易流水，按创建时间倒序。
     * 注意事项：全量查询。
     */
    @Override
    public List<Transaction> listAll() {
        log.info("调用 listAll()");
        return transactionMapper.selectList(
                new LambdaQueryWrapper<Transaction>().orderByDesc(Transaction::getCreated_at_wsh));
    }

    /**
     * 按类型查询交易记录（实现）
     *
     * 业务作用：type 为空则全量；否则按 type 精确匹配，按创建时间倒序。
     * 业务规则：type 为 null 或空白时等价 listAll()。
     */
    @Override
    public List<Transaction> listAllByType(String type) {
        log.info("调用 listAllByType(), type={}", type);
        // 类型为空时退化为全量查询
        if (type == null || type.isBlank()) {
            return listAll();
        }
        return transactionMapper.selectList(
                new LambdaQueryWrapper<Transaction>()
                        .eq(Transaction::getType_wsh, type.trim())
                        .orderByDesc(Transaction::getCreated_at_wsh));
    }

    /**
     * 交易记录转 DTO（实现）
     *
     * 业务作用：将交易实体转换为 DTO，拷贝全部字段。
     * 业务规则：入参为 null 时返回 null。
     */
    @Override
    public TransactionDTO toDTO(Transaction entity) {
        if (entity == null) return null;
        TransactionDTO dto = new TransactionDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setWallet_id_wsh(entity.getWallet_id_wsh());
        dto.setUser_id_wsh(entity.getUser_id_wsh());
        dto.setType_wsh(entity.getType_wsh());
        dto.setAmount_wsh(entity.getAmount_wsh());
        dto.setBalance_before_wsh(entity.getBalance_before_wsh());
        dto.setBalance_after_wsh(entity.getBalance_after_wsh());
        dto.setFrozen_before_wsh(entity.getFrozen_before_wsh());
        dto.setFrozen_after_wsh(entity.getFrozen_after_wsh());
        dto.setDirection_wsh(entity.getDirection_wsh());
        dto.setStatus_wsh(entity.getStatus_wsh());
        dto.setBusiness_type_wsh(entity.getBusiness_type_wsh());
        dto.setBusiness_id_wsh(entity.getBusiness_id_wsh());
        dto.setRequest_id_wsh(entity.getRequest_id_wsh());
        dto.setOrder_id_wsh(entity.getOrder_id_wsh());
        dto.setDescription_wsh(entity.getDescription_wsh());
        dto.setCreated_at_wsh(entity.getCreated_at_wsh());
        return dto;
    }

    /**
     * 新增交易记录（实现）
     *
     * 业务作用：新增一条交易记录。
     * 业务规则：记录余额/冻结快照。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public void add(Transaction tx) {
        log.info("调用 add()");
        transactionMapper.insert(tx);
    }
}
