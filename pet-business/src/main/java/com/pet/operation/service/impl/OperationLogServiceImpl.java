package com.pet.operation.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.PageRequestDTO;
import com.pet.operation.entity.OperationLog;
import com.pet.operation.mapper.OperationLogMapper;
import com.pet.operation.service.OperationLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public OperationLogServiceImpl(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    /**
     * 分页查询操作日志，支持按模块、操作类型和状态筛选
     */
    @Override
    public IPage<OperationLog> page(PageRequestDTO param, String module, String operation, Integer status) {
        log.info("page() called");
        Page<OperationLog> page = new Page<>(param.getPage(), param.getSize());
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .eq(module != null && !module.isEmpty(), OperationLog::getModule_wsh, module)
                .eq(operation != null && !operation.isEmpty(), OperationLog::getOperation_wsh, operation)
                .eq(status != null, OperationLog::getStatus_wsh, status)
                .orderByDesc(OperationLog::getId_wsh);
        return operationLogMapper.selectPage(page, wrapper);
    }

    /**
     * 根据主键获取单条操作日志
     */
    @Override
    public OperationLog getById(Long id) {
        log.info("getById() called");
        return operationLogMapper.selectById(id);
    }

    /**
     * 保存操作日志
     */
    @Override
    @Transactional
    public void save(OperationLog log) {
        System.out.println("save() called");
        operationLogMapper.insert(log);
    }

    /**
     * 清理创建时间早于指定天数的历史日志
     */
    @Override
    @Transactional
    public void cleanOlderThan(int days) {
        log.info("cleanOlderThan() called");
        operationLogMapper.delete(new LambdaQueryWrapper<OperationLog>()
                .lt(OperationLog::getCreated_at_wsh, LocalDateTime.now().minusDays(days)));
    }
}
