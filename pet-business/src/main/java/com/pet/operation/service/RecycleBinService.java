package com.pet.operation.service;

import java.util.List;
import java.util.Map;

public interface RecycleBinService {
    /**
     * 获取指定表的软删除记录列表
     * @param tableName 表名
     * @return 软删除记录列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Map<String, Object>> listDeleted(String tableName);
    /**
     * 恢复软删除记录
     * @param tableName 表名
     * @param id 记录ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void restore(String tableName, Long id);
    /**
     * 软删除记录
     * @param tableName 表名
     * @param id 记录ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void softDelete(String tableName, Long id);
    /**
     * 获取所有支持软删除的表名列表
     * @return 表名列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<String> listSoftDeletableTables();
}

