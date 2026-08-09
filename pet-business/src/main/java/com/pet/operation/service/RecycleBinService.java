package com.pet.operation.service;

import java.util.List;
import java.util.Map;

/**
 * 回收站服务接口，提供软删除记录的查询、恢复和物理删除功能。
 * <p>
 * 支持对平台 20+ 张核心业务表进行软删除管理，包括宠物、商家、看护人、
 * 订单、支付、退款、投诉、评论等。每张表通过 {@code deleted_wsh} 字段标记删除状态。
 */
public interface RecycleBinService {
    /**
     * 查询指定表中所有已软删除的记录（最多 200 条），按 ID 倒序
     *
     * @param tableName 表名（必须在 {@link #listSoftDeletableTables()} 中）
     * @return 已删除记录的列表，每条记录以字段名-值的 Map 形式返回
     * @throws com.pet.common.BusinessException 表名不在支持列表中时抛出
     */
    List<Map<String, Object>> listDeleted(String tableName);

    /**
     * 恢复指定表中一条已软删除的记录（将 deleted_wsh 置为 0）
     *
     * @param tableName 表名
     * @param id        记录 ID
     * @throws com.pet.common.BusinessException 记录不存在或未被删除时抛出
     */
    void restore(String tableName, Long id);

    /**
     * 软删除指定表中的一条记录（将 deleted_wsh 置为 1）
     *
     * @param tableName 表名
     * @param id        记录 ID
     * @throws com.pet.common.BusinessException 记录不存在时抛出
     */
    void softDelete(String tableName, Long id);

    /**
     * 获取当前系统所有支持软删除操作的表名列表
     *
     * @return 支持软删除的表名集合
     */
    List<String> listSoftDeletableTables();
}

