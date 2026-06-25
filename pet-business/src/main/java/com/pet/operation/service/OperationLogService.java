package com.pet.operation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageParam;
import com.pet.operation.entity.OperationLog;

public interface OperationLogService {
    /**
     * 分页查询操作日志
     * @param param 分页参数
     * @param module 模块名称
     * @param operation 操作类型
     * @param status 状态
     * @return 分页操作日志数据
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    IPage<OperationLog> page(PageParam param, String module, String operation, Integer status);
    /**
     * 根据ID获取操作日志
     * @param id 日志ID
     * @return 操作日志实体
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    OperationLog getById(Long id);
    /**
     * 保存操作日志
     * @param log 操作日志实体
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void save(OperationLog log);
    /**
     * 清理指定天数前的操作日志
     * @param days 保留天数
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void cleanOlderThan(int days);
}

