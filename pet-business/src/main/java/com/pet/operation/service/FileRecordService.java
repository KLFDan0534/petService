package com.pet.operation.service;

import com.pet.operation.entity.FileRecord;
import java.util.List;

public interface FileRecordService {
    /**
     * 根据用户ID获取文件记录列表
     * @param userId 用户ID
     * @return 文件记录列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<FileRecord> listByUser(Long userId);
    /**
     * 根据ID获取文件记录
     * @param id 文件记录ID
     * @return 文件记录实体
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    FileRecord getById(Long id);
    /**
     * 创建文件记录
     * @param record 文件记录实体
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void create(FileRecord record);
    /**
     * 根据ID删除文件记录
     * @param id 文件记录ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void deleteById(Long id);
}

