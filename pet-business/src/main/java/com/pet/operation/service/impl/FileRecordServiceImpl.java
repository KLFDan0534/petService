package com.pet.operation.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.operation.entity.FileRecord;
import com.pet.operation.mapper.FileRecordMapper;
import com.pet.operation.service.FileRecordService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class FileRecordServiceImpl implements FileRecordService {

    private final FileRecordMapper fileRecordMapper;

    public FileRecordServiceImpl(FileRecordMapper fileRecordMapper) {
        this.fileRecordMapper = fileRecordMapper;
    }

    @Override
    public List<FileRecord> listByUser(Long userId) {
        log.info("listByUser() called");
        return fileRecordMapper.selectList(
                new LambdaQueryWrapper<FileRecord>()
                        .eq(FileRecord::getUser_id_wsh, userId)
                        .orderByDesc(FileRecord::getCreated_at_wsh));
    }

    @Override
    public FileRecord getById(Long id) {
        log.info("getById() called");
        return fileRecordMapper.selectById(id);
    }

    @Override
    public void create(FileRecord record) {
        log.info("create() called");
        fileRecordMapper.insert(record);
    }

    @Override
    public void deleteById(Long id) {
        log.info("deleteById() called");
        fileRecordMapper.deleteById(id);
    }
}
