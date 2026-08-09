package com.pet.operation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
import com.pet.operation.entity.OperationLog;

/**
 * 操作日志服务接口，提供管理员操作日志的分页查询、保存和定期清理功能。
 * <p>
 * 操作日志记录管理员在后台进行的关键操作（如创建公告、审核内容等），
 * 用于安全审计和操作追溯。
 */
public interface OperationLogService {
    /**
     * 分页查询操作日志，支持按模块、操作类型和状态过滤
     *
     * @param param     分页参数（页码、每页大小）
     * @param module    模块名称（如 "notice"、"content_review"），传 null 不过滤
     * @param operation 操作类型（如 "create"、"delete"），传 null 不过滤
     * @param status    操作状态（如 0=失败, 1=成功），传 null 不过滤
     * @return 分页操作日志数据，按 ID 倒序
     */
    IPage<OperationLog> page(PageRequestDTO param, String module, String operation, Integer status);

    /**
     * 根据主键 ID 获取单条操作日志
     *
     * @param id 日志 ID
     * @return 操作日志实体
     */
    OperationLog getById(Long id);

    /**
     * 保存一条操作日志
     *
     * @param log 待保存的操作日志实体
     */
    void save(OperationLog log);

    /**
     * 清理创建时间早于指定天数的历史操作日志
     *
     * @param days 保留天数，例如传 30 表示删除 30 天前的日志
     */
    void cleanOlderThan(int days);
}

