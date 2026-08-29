package com.pet.operation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
import com.pet.operation.entity.FileRecord;
import java.util.List;

public interface FileRecordService {
    /**
     * 查询指定用户的所有上传文件记录，按上传时间倒序排列
     *
     * @param userId 用户ID
     * @return 该用户的文件记录列表，按创建时间倒序
     */
    List<FileRecord> listByUser(Long userId);

    /**
     * 根据主键ID获取单条文件上传记录
     *
     * @param id 文件记录ID
     * @return 文件记录实体，不存在时返回 {@code null}
     */
    FileRecord getById(Long id);

    /**
     * 创建一条新的文件上传记录（记录文件元信息，不包含文件流本身）
     *
     * @param record 待创建的文件记录实体
     */
    void create(FileRecord record);

    /**
     * 根据主键ID物理删除文件上传记录
     *
     * @param id 文件记录ID
     */
    void deleteById(Long id);

    /**
     * 管理员分页查询全部文件上传记录，支持按原始文件名模糊搜索
     *
     * @param pageParam 分页参数（页码、每页条数）
     * @param keyword   搜索关键词，按原始文件名模糊匹配，为空时查询全部
     * @return 分页文件记录，按创建时间倒序
     */
    IPage<FileRecord> pageAll(PageRequestDTO pageParam, String keyword);
}

