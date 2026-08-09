package com.pet.operation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.operation.entity.FileRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件上传记录 MyBatis-Plus Mapper 接口
 */
@Mapper
public interface FileRecordMapper extends BaseMapper<FileRecord> {
}
