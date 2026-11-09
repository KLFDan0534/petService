package com.pet.operation.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.PageRequestDTO;
import com.pet.operation.entity.FileRecord;
import com.pet.operation.mapper.FileRecordMapper;
import com.pet.operation.service.FileRecordService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Slf4j
public class FileRecordServiceImpl implements FileRecordService {

    private final FileRecordMapper fileRecordMapper;

    /**
     * 【构造文件记录服务】
     *
     * 业务作用：注入文件上传记录 Mapper。
     *
     * 调用场景：Spring 容器启动创建 Bean 时调用。
     *
     * 调用链：Spring 容器 → 本构造器 → 赋值 fileRecordMapper 字段。
     *
     * 数据处理：无。
     *
     * 业务规则：依赖为 final 字段。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：本类的 deleteById 为物理删除，与项目多数逻辑删除的写法不同。
     */
    public FileRecordServiceImpl(FileRecordMapper fileRecordMapper) {
        this.fileRecordMapper = fileRecordMapper;
    }

    /**
     * 查询指定用户的所有文件上传记录，按创建时间倒序排列
     */
    @Override
    public List<FileRecord> listByUser(Long userId) {
        log.info("listByUser() 被调用");
        return fileRecordMapper.selectList(
                new LambdaQueryWrapper<FileRecord>()
                        .eq(FileRecord::getUser_id_wsh, userId)
                        .orderByDesc(FileRecord::getCreated_at_wsh));
    }

    /**
     * 根据主键ID获取单条文件上传记录
     */
    @Override
    public FileRecord getById(Long id) {
        log.info("getById() 被调用");
        return fileRecordMapper.selectById(id);
    }

    /**
     * 创建一条新的文件上传记录
     */
    @Override
    public void create(FileRecord record) {
        log.info("create() 被调用");
        fileRecordMapper.insert(record);
    }

    /**
     * 根据主键ID物理删除文件上传记录
     */
    @Override
    public void deleteById(Long id) {
        log.info("deleteById() 被调用");
        fileRecordMapper.deleteById(id);
    }

    /**
     * 管理员分页查询全部文件上传记录，支持按原始文件名模糊搜索
     */
    @Override
    public IPage<FileRecord> pageAll(PageRequestDTO pageParam, String keyword) {
        log.info("pageAll() 被调用");
        // 关键字为空时模糊条件自动跳过
        LambdaQueryWrapper<FileRecord> wrapper = new LambdaQueryWrapper<FileRecord>()
                .like(StringUtils.hasText(keyword), FileRecord::getOriginal_name_wsh, keyword)
                .orderByDesc(FileRecord::getCreated_at_wsh);
        return fileRecordMapper.selectPage(new Page<>(pageParam.getPage(), pageParam.getSize()), wrapper);
    }
}
